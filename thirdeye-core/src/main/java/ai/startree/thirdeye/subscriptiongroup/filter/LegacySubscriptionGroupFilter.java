/*
 * Copyright 2023 StarTree Inc
 *
 * Licensed under the StarTree Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.startree.ai/legal/startree-community-license
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
package ai.startree.thirdeye.subscriptiongroup.filter;

import static ai.startree.thirdeye.spi.util.AnomalyUtils.isIgnore;
import static ai.startree.thirdeye.spi.util.SpiUtils.optional;
import static java.util.stream.Collectors.toMap;

import ai.startree.thirdeye.spi.Constants;
import ai.startree.thirdeye.spi.datalayer.bao.AlertManager;
import ai.startree.thirdeye.spi.datalayer.bao.MergedAnomalyResultManager;
import ai.startree.thirdeye.spi.datalayer.dto.AbstractDTO;
import ai.startree.thirdeye.spi.datalayer.dto.AlertAssociationDto;
import ai.startree.thirdeye.spi.datalayer.dto.AlertDTO;
import ai.startree.thirdeye.spi.datalayer.dto.MergedAnomalyResultDTO;
import ai.startree.thirdeye.spi.datalayer.dto.SubscriptionGroupDTO;
import ai.startree.thirdeye.spi.detection.AnomalyResultSource;
import ai.startree.thirdeye.spi.detection.ConfigUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The detection alert filter that sends the anomaly email to all recipients
 */
@Singleton
public class LegacySubscriptionGroupFilter {

  private static final String PROP_DETECTION_CONFIG_IDS = "detectionConfigIds";

  private final MergedAnomalyResultManager mergedAnomalyResultManager;
  private final AlertManager alertManager;

  @Inject
  public LegacySubscriptionGroupFilter(final MergedAnomalyResultManager mergedAnomalyResultManager,
      final AlertManager alertManager) {
    this.mergedAnomalyResultManager = mergedAnomalyResultManager;
    this.alertManager = alertManager;
  }

  /**
   * Helper to determine presence of user-feedback for an anomaly
   *
   * @param anomaly anomaly
   * @return {@code true} if feedback exists and is TRUE or FALSE, {@code false} otherwise
   */
  private static boolean hasFeedback(final MergedAnomalyResultDTO anomaly) {
    return anomaly.getFeedback() != null
        && !anomaly.getFeedback().getFeedbackType().isUnresolved();
  }

  private static boolean shouldFilter(final long finalStartTime,
      final MergedAnomalyResultDTO anomaly) {
    return anomaly != null && !anomaly.isChild()
        && !hasFeedback(anomaly)
        && anomaly.getCreatedTime() > finalStartTime
        && !isIgnore(anomaly)
        && (anomaly.getAnomalyResultSource().equals(AnomalyResultSource.DEFAULT_ANOMALY_DETECTION)
        || anomaly.getAnomalyResultSource().equals(AnomalyResultSource.ANOMALY_REPLAY));
  }

  private static long findStartTime(final Map<Long, Long> vectorClocks, final long endTime,
      final Long alertId) {
    long startTime = vectorClocks.get(alertId);

    // No point in fetching anomalies older than MAX_ANOMALY_NOTIFICATION_LOOKBACK
    if (startTime < endTime - Constants.ANOMALY_NOTIFICATION_LOOKBACK_TIME) {
      startTime = endTime - Constants.ANOMALY_NOTIFICATION_LOOKBACK_TIME;
    }
    return startTime;
  }

  private static Map<Long, Long> newVectorClocks(final List<AlertAssociationDto> alertAssociations,
      final Map<Long, Long> vectorClocks) {
    final Map<Long, Long> vc = optional(vectorClocks).orElse(Map.of());
    return alertAssociations.stream()
        .map(AlertAssociationDto::getAlert)
        .map(AbstractDTO::getId)
        .collect(toMap(
            id -> id,
            id -> vc.getOrDefault(id, 0L),
            (a, b) -> b)
        );
  }

  private static AlertDTO fromId(final Long id) {
    final AlertDTO alert = new AlertDTO();
    alert.setId(id);
    return alert;
  }

  /**
   * Generate List of Alert Association objects from existing subscription group
   *
   * @return List of Alert Association objects
   */
  private List<AlertAssociationDto> generate(final SubscriptionGroupDTO subscriptionGroup) {
    final List<Long> alertIds = ConfigUtils.getLongs(subscriptionGroup.getProperties()
        .get(PROP_DETECTION_CONFIG_IDS));

    return alertIds.stream()
        .map(LegacySubscriptionGroupFilter::fromId)
        .map(alert -> new AlertAssociationDto().setAlert(alert))
        .collect(Collectors.toList());
  }

  public DetectionAlertFilterResult filter(final SubscriptionGroupDTO sg,
      final long endTime) {
    final List<AlertAssociationDto> alertAssociations = optional(sg.getAlertAssociations())
        .orElseGet(() -> generate(sg));

    // Fetch all the anomalies to be notified to the recipients
    final Map<Long, Long> vectorClocks = newVectorClocks(alertAssociations, sg.getVectorClocks());
    final Set<MergedAnomalyResultDTO> anomalies = findAnomalies(alertAssociations, vectorClocks, endTime);

    final DetectionAlertFilterResult result = new DetectionAlertFilterResult();
    return result.addMapping(new DetectionAlertFilterNotification(sg), anomalies);
  }

  private Set<MergedAnomalyResultDTO> findAnomalies(
      final List<AlertAssociationDto> alertAssociations, final Map<Long, Long> vectorClocks,
      final long endTime) {
    // retrieve all candidate anomalies
    final Set<MergedAnomalyResultDTO> allAnomalies = new HashSet<>();
    for (final AlertAssociationDto aa : alertAssociations) {
      final long alertId = aa.getAlert().getId();

      // Ignore disabled detections
      final AlertDTO detection = alertManager.findById(alertId);

      if (detection != null && detection.isActive()) {
        final long startTime = findStartTime(vectorClocks, endTime, alertId);

        final Collection<MergedAnomalyResultDTO> candidates = mergedAnomalyResultManager
            .findByCreatedTimeInRangeAndDetectionConfigId(startTime + 1, endTime, alertId);

        final Collection<MergedAnomalyResultDTO> anomalies = candidates.stream()
            .filter(anomaly -> shouldFilter(startTime, anomaly))
            .collect(Collectors.toSet());

        allAnomalies.addAll(anomalies);
      }
    }
    return allAnomalies;
  }
}
