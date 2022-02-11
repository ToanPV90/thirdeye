/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.detection.alert.scheme;

import ai.startree.thirdeye.detection.alert.DetectionAlertFilterResult;
import ai.startree.thirdeye.notification.NotificationSchemeContext;
import ai.startree.thirdeye.notification.content.NotificationContent;
import ai.startree.thirdeye.spi.datalayer.dto.SubscriptionGroupDTO;
import ai.startree.thirdeye.spi.detection.AnomalyResult;
import com.codahale.metrics.MetricRegistry;
import java.util.Comparator;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NotificationScheme {

  public static final String PROP_TEMPLATE = "template";
  private static final Logger LOG = LoggerFactory.getLogger(NotificationScheme.class);
  protected static final Comparator<AnomalyResult> COMPARATOR_DESC =
      (o1, o2) -> -1 * Long.compare(o1.getStartTime(), o2.getStartTime());

  protected NotificationSchemeContext context;
  protected MetricRegistry metricRegistry;
  private NotificationContent metricAnomaliesContent;
  private NotificationContent entityGroupKeyContent;

  public void init(NotificationSchemeContext context) {
    this.context = context;
    this.metricAnomaliesContent = context.getMetricAnomaliesContent();
    this.entityGroupKeyContent = context.getEntityGroupKeyContent();
    this.metricRegistry = context.getMetricRegistry();
  }

  public abstract void run(
      final SubscriptionGroupDTO subscriptionGroup,
      final DetectionAlertFilterResult result) throws Exception;

  public void destroy() {
    // do nothing
  }

  private EmailTemplateType getTemplate(final Properties properties) {
    if (properties != null && properties.containsKey(PROP_TEMPLATE)) {
      return EmailTemplateType.valueOf(properties.get(PROP_TEMPLATE).toString());
    }
    return EmailTemplateType.DEFAULT_EMAIL;
  }

  /**
   * Fail the alert task if unable to notify owner. However, in case of dimensions recipient
   * alerter,
   * do not fail the alert if a subset of recipients are invalid.
   */
  void handleAlertFailure(final Exception e) {
    LOG.error("Skipping! Found illegal arguments while sending alert. ", e);
  }

  protected NotificationContent getNotificationContent(
      final Properties properties) {
    final EmailTemplateType template = getTemplate(properties);
    switch (template) {
      case DEFAULT_EMAIL:
        return metricAnomaliesContent;
      case ENTITY_GROUPBY_REPORT:
        return entityGroupKeyContent;
      default:
        throw new IllegalArgumentException(String.format("Unknown email template '%s'", template));
    }
  }

  public enum EmailTemplateType {
    DEFAULT_EMAIL,
    ENTITY_GROUPBY_REPORT
  }
}
