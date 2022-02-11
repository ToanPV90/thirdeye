/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.notification.content.templates;

import static ai.startree.thirdeye.spi.util.SpiUtils.optional;

import ai.startree.thirdeye.detection.anomaly.alert.util.AlertScreenshotHelper;
import ai.startree.thirdeye.notification.NotificationContext;
import ai.startree.thirdeye.notification.content.AnomalyReportEntity;
import ai.startree.thirdeye.notification.content.BaseNotificationContent;
import ai.startree.thirdeye.spi.datalayer.bao.AlertManager;
import ai.startree.thirdeye.spi.datalayer.bao.EventManager;
import ai.startree.thirdeye.spi.datalayer.bao.MergedAnomalyResultManager;
import ai.startree.thirdeye.spi.datalayer.bao.MetricConfigManager;
import ai.startree.thirdeye.spi.datalayer.dto.AlertDTO;
import ai.startree.thirdeye.spi.datalayer.dto.EventDTO;
import ai.startree.thirdeye.spi.datalayer.dto.MergedAnomalyResultDTO;
import ai.startree.thirdeye.spi.datalayer.dto.SubscriptionGroupDTO;
import ai.startree.thirdeye.spi.detection.AnomalyFeedback;
import ai.startree.thirdeye.spi.detection.AnomalyResult;
import ai.startree.thirdeye.spi.util.SpiUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This email formatter lists the anomalies by their functions or metric.
 */
@Singleton
public class MetricAnomaliesContent extends BaseNotificationContent {

  private static final Logger LOG = LoggerFactory.getLogger(MetricAnomaliesContent.class);

  private final AlertManager alertManager;

  @Inject
  public MetricAnomaliesContent(final MetricConfigManager metricConfigManager,
      final EventManager eventManager,
      final MergedAnomalyResultManager mergedAnomalyResultManager,
      final AlertManager detectionConfigManager) {
    super(metricConfigManager, eventManager, mergedAnomalyResultManager);
    alertManager = detectionConfigManager;
  }

  @Override
  public void init(final NotificationContext context) {
    super.init(context);

    // TODO spyne Investigate if rcaClient is required. Else remove.
//    if (this.rcaClient == null) {
//      final ThirdEyePrincipal principal = new ThirdEyePrincipal(
//          this.thirdEyeAnomalyConfig.getTeRestConfig().getAdminUser(),
//          this.thirdEyeAnomalyConfig.getTeRestConfig().getSessionKey()
//      );
//      this.rcaClient = new ThirdEyeRcaRestClient(principal,
//          this.thirdEyeAnomalyConfig.getUiConfiguration().getExternalUrl());
//    }
  }

  @Override
  public String getTemplate() {
    return MetricAnomaliesContent.class.getSimpleName();
  }

  @Override
  public Map<String, Object> format(final Collection<AnomalyResult> anomalies,
      final SubscriptionGroupDTO subsConfig) {
    final Map<String, Object> templateData = super.getTemplateData(subsConfig, anomalies);
    enrichMetricInfo(templateData, anomalies);

    DateTime windowStart = DateTime.now();
    DateTime windowEnd = new DateTime(0);

    final Map<String, Long> functionToId = new HashMap<>();
    final Multimap<String, String> anomalyDimensions = ArrayListMultimap.create();
    final Multimap<String, AnomalyReportEntity> functionAnomalyReports = ArrayListMultimap.create();
    final Multimap<String, AnomalyReportEntity> metricAnomalyReports = ArrayListMultimap.create();
    final List<AnomalyReportEntity> anomalyDetails = new ArrayList<>();
    final List<String> anomalyIds = new ArrayList<>();

    final List<AnomalyResult> sortedAnomalies = new ArrayList<>(anomalies);
    sortedAnomalies.sort(Comparator.comparingDouble(AnomalyResult::getWeight));

    for (final AnomalyResult anomalyResult : anomalies) {
      if (!(anomalyResult instanceof MergedAnomalyResultDTO)) {
        LOG.warn("Anomaly result {} isn't an instance of MergedAnomalyResultDTO. Skip from alert.",
            anomalyResult);
        continue;
      }
      final MergedAnomalyResultDTO anomaly = (MergedAnomalyResultDTO) anomalyResult;

      final DateTime anomalyStartTime = new DateTime(anomaly.getStartTime(), dateTimeZone);
      final DateTime anomalyEndTime = new DateTime(anomaly.getEndTime(), dateTimeZone);

      if (anomalyStartTime.isBefore(windowStart)) {
        windowStart = anomalyStartTime;
      }
      if (anomalyEndTime.isAfter(windowEnd)) {
        windowEnd = anomalyEndTime;
      }

      final AnomalyFeedback feedback = anomaly.getFeedback();

      final String feedbackVal = getFeedbackValue(feedback);

      String functionName = "Alerts";
      String funcDescription = "";
      Long id = -1L;

      if (anomaly.getDetectionConfigId() != null) {
        final AlertDTO config = alertManager.findById(anomaly.getDetectionConfigId());
        Preconditions.checkNotNull(config,
            String.format("Cannot find detection config %d", anomaly.getDetectionConfigId()));
        functionName = config.getName();
        funcDescription = config.getDescription() == null ? "" : config.getDescription();
        id = config.getId();
      }

      final AnomalyReportEntity anomalyReport = buildAnomalyReportEntity(
          anomaly,
          feedbackVal,
          functionName,
          funcDescription);

      // dimension filters / values
      for (final Map.Entry<String, String> entry : anomaly.getDimensions().entrySet()) {
        anomalyDimensions.put(entry.getKey(), entry.getValue());
      }

      // include notified alerts only in the email
      if (!includeSentAnomaliesOnly || anomaly.isNotified()) {
        anomalyDetails.add(anomalyReport);
        anomalyIds.add(anomalyReport.getAnomalyId());
        functionAnomalyReports.put(functionName, anomalyReport);
        metricAnomalyReports.put(optional(anomaly.getMetric()).orElse("UNKNOWN"), anomalyReport);
        functionToId.put(functionName, id);
      }
    }

    // holidays
    final DateTime eventStart = windowStart.minus(preEventCrawlOffset);
    final DateTime eventEnd = windowEnd.plus(postEventCrawlOffset);
    final Map<String, List<String>> targetDimensions = new HashMap<>();

    // TODO spyne understand and restore HolidayContriesWhiteList feature
//    if (thirdEyeAnomalyConfig.getHolidayCountriesWhitelist() != null) {
//      targetDimensions
//          .put(EVENT_FILTER_COUNTRY, thirdEyeAnomalyConfig.getHolidayCountriesWhitelist());
//    }
    final List<EventDTO> holidays = getHolidayEvents(eventStart, eventEnd, targetDimensions);
    holidays.sort(Comparator.comparingLong(EventDTO::getStartTime));

    // Insert anomaly snapshot image
    if (anomalyDetails.size() == 1) {
      final AnomalyReportEntity singleAnomaly = anomalyDetails.get(0);
      try {
        imgPath = AlertScreenshotHelper
            .takeGraphScreenShot(singleAnomaly.getAnomalyId(),
                context.getUiPublicUrl());
      } catch (final Exception e) {
        LOG.error("Exception while embedding screenshot for anomaly {}",
            singleAnomaly.getAnomalyId(), e);
      }
    }

    templateData.put("anomalyDetails", anomalyDetails);
    templateData.put("anomalyIds", Joiner.on(",").join(anomalyIds));
    templateData.put("holidays", holidays);
    templateData.put("detectionToAnomalyDetailsMap", functionAnomalyReports.asMap());
    templateData.put("metricToAnomalyDetailsMap", metricAnomalyReports.asMap());
    templateData.put("functionToId", functionToId);

    // Display RCA highlights in email only if report contains anomalies belonging to a single metric.
    // Note: Once we have a sophisticated rca highlight support and users start seeing value, we'll
    // enable it for all the metrics.
    //TODO: the API's for RCA had changed. Need to migrate
/*    if (this.rcaClient != null && metricAnomalyReports.keySet().size() == 1) {
      String anomalyId = metricAnomalyReports.values().iterator().next().getAnomalyId();
      try {
        Map<String, Object> rcaHighlights = this.rcaClient
            .getRootCauseHighlights(Long.parseLong(anomalyId));
        templateData.put("cubeDimensions",
            ConfigUtils.getMap(rcaHighlights.get("cubeResults")).get("dimensions"));
        templateData.put("cubeResponseRows",
            ConfigUtils.getMap(rcaHighlights.get("cubeResults")).get("responseRows"));
      } catch (Exception e) {
        // alert notification shouldn't fail if rca insights are not available
        LOG.error("Skip Embedding RCA in email. Failed to retrieve the RCA Highlights for anomaly "
            + anomalyId, e);
      }
    }*/

    return templateData;
  }

  private AnomalyReportEntity buildAnomalyReportEntity(final MergedAnomalyResultDTO anomaly,
      final String feedbackVal, final String functionName, final String funcDescription) {
    final Properties props = new Properties();
    props.putAll(anomaly.getProperties());
    final double lift = BaseNotificationContent
        .getLift(anomaly.getAvgCurrentVal(), anomaly.getAvgBaselineVal());
    final AnomalyReportEntity anomalyReport = new AnomalyReportEntity(String.valueOf(anomaly.getId()),
        getAnomalyURL(anomaly, context.getUiPublicUrl()),
        getPredictedValue(anomaly),
        getCurrentValue(anomaly),
        getFormattedLiftValue(anomaly, lift),
        getLiftDirection(lift),
        0d,
        getDimensionsList(anomaly.getDimensionMap()),
        getTimeDiffInHours(anomaly.getStartTime(), anomaly.getEndTime()), // duration
        feedbackVal,
        functionName,
        funcDescription,
        anomaly.getMetric(),
        getDateString(anomaly.getStartTime(), dateTimeZone),
        getDateString(anomaly.getEndTime(), dateTimeZone),
        getTimezoneString(dateTimeZone),
        getIssueType(anomaly),
        anomaly.getType().getLabel(),
        SpiUtils.encodeCompactedProperties(props),
        anomaly.getMetricUrn()
    );
    return anomalyReport;
  }
}
