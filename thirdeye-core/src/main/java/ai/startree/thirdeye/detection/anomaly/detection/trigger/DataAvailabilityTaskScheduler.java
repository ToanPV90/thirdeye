/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.detection.anomaly.detection.trigger;

import static ai.startree.thirdeye.detection.TaskUtils.createDetectionTask;

import ai.startree.thirdeye.CoreConstants;
import ai.startree.thirdeye.datasource.ThirdEyeCacheRegistry;
import ai.startree.thirdeye.detection.TaskUtils;
import ai.startree.thirdeye.detection.anomaly.detection.trigger.utils.DataAvailabilitySchedulingConfiguration;
import ai.startree.thirdeye.notification.formatter.DetectionConfigFormatter;
import ai.startree.thirdeye.spi.datalayer.bao.AlertManager;
import ai.startree.thirdeye.spi.datalayer.bao.DatasetConfigManager;
import ai.startree.thirdeye.spi.datalayer.bao.MetricConfigManager;
import ai.startree.thirdeye.spi.datalayer.bao.TaskManager;
import ai.startree.thirdeye.spi.datalayer.dto.AlertDTO;
import ai.startree.thirdeye.spi.datalayer.dto.DatasetConfigDTO;
import ai.startree.thirdeye.spi.datalayer.dto.TaskDTO;
import ai.startree.thirdeye.spi.rootcause.impl.MetricEntity;
import ai.startree.thirdeye.spi.task.TaskStatus;
import ai.startree.thirdeye.spi.task.TaskType;
import ai.startree.thirdeye.task.DetectionPipelineTaskInfo;
import ai.startree.thirdeye.task.TaskInfoFactory;
import ai.startree.thirdeye.util.ThirdEyeUtils;
import ai.startree.thirdeye.util.ThirdeyeMetricsUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is to schedule detection tasks based on data availability events.
 */
@Singleton
public class DataAvailabilityTaskScheduler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(DataAvailabilityTaskScheduler.class);
  private final ScheduledExecutorService executorService;
  private final long sleepPerRunInSec;
  private final long fallBackTimeInSec;
  private final long schedulingWindowInSec;
  private final long scheduleDelayInSec;

  // Maintains mapping from each detection to the detection end time of it's last run.
  // Fallback runs based on the last task run (successful or not).
  private final Map<Long, Long> detectionIdToLastTaskEndTimeMap;

  private final TaskManager taskManager;
  private final AlertManager alertManager;
  private final DatasetConfigManager datasetConfigManager;
  private final ThirdEyeCacheRegistry thirdEyeCacheRegistry;
  private final MetricConfigManager metricConfigManager;

  @Inject
  public DataAvailabilityTaskScheduler(
      final DataAvailabilitySchedulingConfiguration config,
      final TaskManager taskManager,
      final AlertManager alertManager,
      final DatasetConfigManager datasetConfigManager,
      final ThirdEyeCacheRegistry thirdEyeCacheRegistry,
      final MetricConfigManager metricConfigManager) {
    this.sleepPerRunInSec = config.getSchedulerDelayInSec();
    this.fallBackTimeInSec = config.getTaskTriggerFallBackTimeInSec();
    this.schedulingWindowInSec = config.getSchedulingWindowInSec();
    this.scheduleDelayInSec = config.getScheduleDelayInSec();

    this.detectionIdToLastTaskEndTimeMap = new HashMap<>();
    this.executorService = Executors.newSingleThreadScheduledExecutor();
    this.taskManager = taskManager;
    this.alertManager = alertManager;
    this.datasetConfigManager = datasetConfigManager;
    this.thirdEyeCacheRegistry = thirdEyeCacheRegistry;
    this.metricConfigManager = metricConfigManager;
  }

  /**
   * Runs every @{link DataAvailabilitySchedulingConfiguration.scheduleDelayInSec}
   */
  @Override
  public void run() {
    Map<AlertDTO, Set<String>> detection2DatasetMap = new HashMap<>();
    Map<String, DatasetConfigDTO> datasetConfigMap = new HashMap<>();
    populateDetectionMapAndDatasetConfigMap(detection2DatasetMap, datasetConfigMap);
    Map<Long, TaskDTO> runningDetection = retrieveRunningDetectionTasks();
    int taskCount = 0;
    long detectionEndTime = System.currentTimeMillis();
    for (AlertDTO detectionConfig : detection2DatasetMap.keySet()) {
      try {
        long detectionConfigId = detectionConfig.getId();
        DetectionPipelineTaskInfo taskInfo = TaskUtils
            .buildTaskInfoFromDetectionConfig(detectionConfig,
                detectionEndTime,
                thirdEyeCacheRegistry,
                datasetConfigManager,
                metricConfigManager);
        if (!runningDetection.containsKey(detectionConfigId)) {
          if (isAllDatasetUpdated(detectionConfig, detection2DatasetMap.get(detectionConfig),
              datasetConfigMap)) {
            if (isWithinSchedulingWindow(detection2DatasetMap.get(detectionConfig),
                datasetConfigMap)) {
              //TODO: additional check is required if detection is based on aggregated value across multiple data points
              createDetectionTask(taskInfo, taskManager);
              detectionIdToLastTaskEndTimeMap.put(detectionConfig.getId(), taskInfo.getEnd());
              ThirdeyeMetricsUtil.eventScheduledTaskCounter.inc();
              taskCount++;
            } else {
              LOG.warn("Unable to schedule a task for {}, because it is out of scheduling window.",
                  detectionConfigId);
            }
          }

          // Note: Fallback SLA & Data availability SLA are independent of each other.
          // For example, if an event doesn't arrive within 24 hours, do a fallback.
          // On the other hand, a user can setup an SLA alert if there is no data for 3 days.
          if (needFallback(detectionConfig)) {
            LOG.info("Scheduling a task for detection {} due to the fallback mechanism.",
                detectionConfigId);
            createDetectionTask(taskInfo, taskManager);

            detectionIdToLastTaskEndTimeMap.put(detectionConfig.getId(), taskInfo.getEnd());
            ThirdeyeMetricsUtil.eventScheduledTaskFallbackCounter.inc();
            taskCount++;
          }
        } else {
          LOG.info(
              "Skipping creating detection task for detection {} because task {} is not finished.",
              detectionConfigId, runningDetection.get(detectionConfigId));
        }
      } catch (Exception e) {
        LOG.error("Error in scheduling a detection...", e);
      }
    }
    LOG.info("Schedule {} tasks in this run...", taskCount);
  }

  public void start() {
    executorService.scheduleWithFixedDelay(this, 0, sleepPerRunInSec, TimeUnit.SECONDS);
  }

  public void close() {
    executorService.shutdownNow();
  }

  private void populateDetectionMapAndDatasetConfigMap(
      Map<AlertDTO, Set<String>> dataset2DetectionMap,
      Map<String, DatasetConfigDTO> datasetConfigMap) {
    Map<Long, Set<String>> metricCache = new HashMap<>();
    List<AlertDTO> detectionConfigs = alertManager.findAllActive()
        .stream().filter(AlertDTO::isDataAvailabilitySchedule)
        .collect(Collectors.toList());
    for (AlertDTO detectionConfig : detectionConfigs) {
      Set<String> metricUrns = DetectionConfigFormatter
          .extractMetricUrnsFromProperties(detectionConfig.getProperties());
      Set<String> datasets = new HashSet<>();
      for (String urn : metricUrns) {
        MetricEntity me = MetricEntity.fromURN(urn);
        if (!metricCache.containsKey(me.getId())) {
          datasets.addAll(ThirdEyeUtils.getDatasetConfigsFromMetricUrn(urn,
              datasetConfigManager,
              metricConfigManager,
              thirdEyeCacheRegistry)
              .stream().map(DatasetConfigDTO::getDataset).collect(Collectors.toList()));
          // cache the mapping in memory to avoid duplicate retrieval
          metricCache.put(me.getId(), datasets);
        } else {
          // retrieve dataset mapping from memory
          datasets.addAll(metricCache.get(me.getId()));
        }
      }
      if (datasets.isEmpty()) {
        LOG.error("No valid dataset is found for detection {}.", detectionConfig.getId());
        continue;
      }
      dataset2DetectionMap.put(detectionConfig, datasets);
      for (String dataset : datasets) {
        if (!datasetConfigMap.containsKey(dataset)) {
          DatasetConfigDTO datasetConfig = datasetConfigManager.findByDataset(dataset);
          datasetConfigMap.put(dataset, datasetConfig);
        }
      }
    }
  }

  private Map<Long, TaskDTO> retrieveRunningDetectionTasks() {
    List<TaskStatus> statusList = new ArrayList<>();
    statusList.add(TaskStatus.WAITING);
    statusList.add(TaskStatus.RUNNING);
    List<TaskDTO> tasks = taskManager
        .findByStatusesAndTypeWithinDays(statusList, TaskType.DETECTION,
            (int) TimeUnit.MILLISECONDS.toDays(CoreConstants.DETECTION_TASK_MAX_LOOKBACK_WINDOW));
    Map<Long, TaskDTO> res = new HashMap<>(tasks.size());
    for (TaskDTO task : tasks) {
      res.put(ThirdEyeUtils.getDetectionIdFromJobName(task.getJobName()), task);
    }
    return res;
  }

  private void loadLatestTaskCreateTime(AlertDTO detectionConfig) throws Exception {
    long detectionConfigId = detectionConfig.getId();
    List<TaskDTO> tasks = taskManager
        .findByNameOrderByCreateTime(TaskType.DETECTION +
            "_" + detectionConfigId, 1, false);
    if (tasks.size() == 0) {
      detectionIdToLastTaskEndTimeMap.put(detectionConfigId, detectionConfig.getLastTimestamp());
    } else {
      // Load the watermark
      DetectionPipelineTaskInfo taskInfo = (DetectionPipelineTaskInfo) TaskInfoFactory
          .get(TaskType.DETECTION, tasks.get(0).getTaskInfo());
      detectionIdToLastTaskEndTimeMap.put(detectionConfigId, taskInfo.getEnd());
    }
  }

  private boolean isAllDatasetUpdated(AlertDTO detectionConfig, Set<String> datasets,
      Map<String, DatasetConfigDTO> datasetConfigMap) {
    long lastTimestamp = detectionConfig.getLastTimestamp();
    long curr = System.currentTimeMillis();
    return datasets.stream()
        .allMatch(d -> datasetConfigMap.get(d).getLastRefreshTime() > lastTimestamp
            && curr - datasetConfigMap.get(d).getLastRefreshEventTime() >= TimeUnit.SECONDS
            .toMillis(scheduleDelayInSec));
  }

  /* check if the fallback cron need to be triggered if the detection has not been run for long time */
  private boolean needFallback(AlertDTO detectionConfig) throws Exception {
    long detectionConfigId = detectionConfig.getId();
    long notRunThreshold = ((detectionConfig.getTaskTriggerFallBackTimeInSec() == 0) ?
        fallBackTimeInSec : detectionConfig.getTaskTriggerFallBackTimeInSec()) * 1000;
    if (!detectionIdToLastTaskEndTimeMap.containsKey(detectionConfigId)) {
      loadLatestTaskCreateTime(detectionConfig);
    }
    long lastRunTime = detectionIdToLastTaskEndTimeMap.get(detectionConfigId);
    return (System.currentTimeMillis() - lastRunTime >= notRunThreshold);
  }

  /*
  check if the current time is within scheduling window to avoid repeating scheduling the same task if
  detection watermark is not moving forward */
  private boolean isWithinSchedulingWindow(Set<String> datasets,
      Map<String, DatasetConfigDTO> datasetConfigMap) {
    long maxEventTime = datasets.stream()
        .map(dataset -> datasetConfigMap.get(dataset).getLastRefreshEventTime())
        .max(Comparator.naturalOrder()).orElse(0L);
    return System.currentTimeMillis() <= maxEventTime + schedulingWindowInSec * 1000;
  }

  public void shutdown() {
    executorService.shutdown();
  }
}