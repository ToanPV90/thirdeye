/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.notification.formatter.channels;

import static ai.startree.thirdeye.notification.commons.ThirdEyeJiraClient.PROP_ASSIGNEE;
import static ai.startree.thirdeye.notification.commons.ThirdEyeJiraClient.PROP_COMPONENTS;
import static ai.startree.thirdeye.notification.commons.ThirdEyeJiraClient.PROP_CUSTOM;
import static ai.startree.thirdeye.notification.commons.ThirdEyeJiraClient.PROP_ISSUE_TYPE;
import static ai.startree.thirdeye.notification.commons.ThirdEyeJiraClient.PROP_LABELS;
import static ai.startree.thirdeye.notification.commons.ThirdEyeJiraClient.PROP_MERGE_GAP;
import static ai.startree.thirdeye.notification.commons.ThirdEyeJiraClient.PROP_PROJECT;
import static ai.startree.thirdeye.spi.util.SpiUtils.optional;

import ai.startree.thirdeye.config.ThirdEyeServerConfiguration;
import ai.startree.thirdeye.config.UiConfiguration;
import ai.startree.thirdeye.notification.NotificationContext;
import ai.startree.thirdeye.notification.commons.JiraConfiguration;
import ai.startree.thirdeye.notification.commons.JiraEntity;
import ai.startree.thirdeye.notification.content.BaseNotificationContent;
import ai.startree.thirdeye.notification.content.NotificationContent;
import ai.startree.thirdeye.notification.content.templates.MetricAnomaliesContent;
import ai.startree.thirdeye.spi.Constants.SubjectType;
import ai.startree.thirdeye.spi.datalayer.dto.SubscriptionGroupDTO;
import ai.startree.thirdeye.spi.detection.AnomalyResult;
import ai.startree.thirdeye.spi.detection.ConfigUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class formats the content for jira alerts
 */
public class JiraContentFormatter {
  protected static final String PROP_SUBJECT_STYLE = "subject";


  private static final Logger LOG = LoggerFactory.getLogger(JiraContentFormatter.class);

  private final JiraConfiguration jiraAdminConfig;

  private static final String CHARSET = "UTF-8";
  static final String PROP_DEFAULT_LABEL = "thirdeye";

  public static final int MAX_JIRA_SUMMARY_LENGTH = 255;

  private static final Map<String, String> alertContentToTemplateMap;

  protected Properties alertClientConfig;
  protected SubscriptionGroupDTO subsConfig;
  protected ThirdEyeServerConfiguration teConfig;
  protected NotificationContent notificationContent;

  static {
    Map<String, String> aMap = new HashMap<>();
    aMap.put(MetricAnomaliesContent.class.getSimpleName(), "jira-metric-anomalies-template.ftl");
    alertContentToTemplateMap = Collections.unmodifiableMap(aMap);
  }

  public JiraContentFormatter(JiraConfiguration jiraAdminConfig, Properties jiraClientConfig,
      NotificationContent content, ThirdEyeServerConfiguration teConfig,
      SubscriptionGroupDTO subsConfig) {
    this.alertClientConfig = jiraClientConfig;
    this.teConfig = teConfig;
    notificationContent = content;
    this.subsConfig = subsConfig;
    notificationContent.init(new NotificationContext()
        .setProperties(jiraClientConfig)
        .setUiPublicUrl(optional(teConfig)
            .map(ThirdEyeServerConfiguration::getUiConfiguration)
            .map(UiConfiguration::getExternalUrl)
            .orElse("")));


    this.jiraAdminConfig = jiraAdminConfig;
    validateJiraConfigs(jiraAdminConfig);
  }

  /**
   * Plug the appropriate subject style based on configuration
   */
  SubjectType getSubjectType(final Properties alertSchemeClientConfigs) {
    final SubjectType subjectType;
    if (alertSchemeClientConfigs != null && alertSchemeClientConfigs
        .containsKey(PROP_SUBJECT_STYLE)) {
      subjectType = SubjectType
          .valueOf(alertSchemeClientConfigs.get(PROP_SUBJECT_STYLE).toString());
    } else {
      // To support the legacy email subject configuration
      subjectType = subsConfig.getSubjectType();
    }

    return subjectType;
  }
  /**
   * Make sure the base admin parameters are configured before proceeding
   */
  private void validateJiraConfigs(JiraConfiguration jiraAdminConfig) {
    Preconditions.checkNotNull(jiraAdminConfig.getUser());
    Preconditions.checkNotNull(jiraAdminConfig.getPassword());
    Preconditions.checkNotNull(jiraAdminConfig.getJiraHost());
  }

  /**
   * Format and construct a {@link JiraEntity} by rendering the anomalies and properties
   *
   * @param dimensionFilters dimensions configured in the multi-dimensions alerter
   * @param anomalies anomalies to be reported to recipients configured in (@link
   *     #jiraClientConfig}
   */
  public JiraEntity getJiraEntity(Multimap<String, String> dimensionFilters,
      Collection<AnomalyResult> anomalies) {
    Map<String, Object> templateData = notificationContent.format(anomalies, this.subsConfig);
    templateData.put("dashboardHost", teConfig.getUiConfiguration().getExternalUrl());
    return buildJiraEntity(alertContentToTemplateMap.get(notificationContent.getTemplate()),
        templateData,
        dimensionFilters);
  }

  private String buildSummary(Map<String, Object> templateValues,
      Multimap<String, String> dimensionFilters) {
    String issueSummary =
        BaseNotificationContent
            .makeSubject(getSubjectType(alertClientConfig), this.subsConfig, templateValues);

    // Append dimensional info to summary
    StringBuilder dimensions = new StringBuilder();
    for (Map.Entry<String, Collection<String>> dimFilter : dimensionFilters.asMap().entrySet()) {
      dimensions.append(", ").append(dimFilter.getKey()).append("=")
          .append(String.join(",", dimFilter.getValue()));
    }
    issueSummary = issueSummary + dimensions;

    // Truncate summary due to jira character limit
    return StringUtils.abbreviate(issueSummary, MAX_JIRA_SUMMARY_LENGTH);
  }

  private List<String> buildLabels(Multimap<String, String> dimensionFilters) {
    List<String> labels = ConfigUtils.getList(alertClientConfig.get(PROP_LABELS));
    labels.add(PROP_DEFAULT_LABEL);
    labels.add("subsId=" + this.subsConfig.getId().toString());
    dimensionFilters.asMap().forEach((k, v) -> labels.add(k + "=" + String.join(",", v)));
    return labels;
  }

  private String buildDescription(String jiraTemplate, Map<String, Object> templateValues) {
    String description;

    // Render the values in templateValues map to the jira ftl template file
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (Writer out = new OutputStreamWriter(baos, CHARSET)) {
      Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_21);
      freemarkerConfig
          .setClassForTemplateLoading(getClass(), "/ai/startree/thirdeye/detector");
      freemarkerConfig.setDefaultEncoding(CHARSET);
      freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      Template template = freemarkerConfig.getTemplate(jiraTemplate);
      template.process(templateValues, out);

      description = new String(baos.toByteArray(), CHARSET);
    } catch (Exception e) {
      description =
          "Found an exception while constructing the description content. Pls report & reach out"
              + " to the Thirdeye team. Exception = " + e.getMessage();
    }

    return description;
  }

  private File buildSnapshot() {
    File snapshotFile = null;
    try {
      snapshotFile = new File(this.notificationContent.getSnaphotPath());
    } catch (Exception e) {
      LOG.error("Exception while loading snapshot {}", this.notificationContent.getSnaphotPath(),
          e);
    }
    return snapshotFile;
  }

  /**
   * Apply the parameter map to given jira template, and format it as JiraEntity
   */
  private JiraEntity buildJiraEntity(String jiraTemplate, Map<String, Object> templateValues,
      Multimap<String, String> dimensionFilters) {
    String jiraProject = MapUtils.getString(alertClientConfig, PROP_PROJECT,
        this.jiraAdminConfig.getDefaultProject());
    Long jiraIssueTypeId = MapUtils
        .getLong(alertClientConfig, PROP_ISSUE_TYPE, this.jiraAdminConfig.getJiraIssueTypeId());

    JiraEntity jiraEntity = new JiraEntity(jiraProject, jiraIssueTypeId,
        buildSummary(templateValues, dimensionFilters));
    jiraEntity.setAssignee(
        MapUtils.getString(alertClientConfig, PROP_ASSIGNEE, "")); // Default - Unassigned
    jiraEntity.setMergeGap(
        MapUtils.getLong(alertClientConfig, PROP_MERGE_GAP, -1L)); // Default - Always merge
    jiraEntity.setLabels(buildLabels(dimensionFilters));
    jiraEntity.setDescription(buildDescription(jiraTemplate, templateValues));
    jiraEntity.setComponents(ConfigUtils.getList(alertClientConfig.get(PROP_COMPONENTS)));
    jiraEntity.setSnapshot(buildSnapshot());
    Map<String, Object> customFieldsMap = ConfigUtils.getMap(alertClientConfig.get(PROP_CUSTOM));
    jiraEntity.setCustomFieldsMap(customFieldsMap);

    return jiraEntity;
  }
}
