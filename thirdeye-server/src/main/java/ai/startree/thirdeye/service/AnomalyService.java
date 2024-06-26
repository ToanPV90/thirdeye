/*
 * Copyright 2024 StarTree Inc
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
package ai.startree.thirdeye.service;

import static ai.startree.thirdeye.RequestCache.buildCache;
import static ai.startree.thirdeye.spi.util.SpiUtils.optional;

import ai.startree.thirdeye.RequestCache;
import ai.startree.thirdeye.auth.AuthorizationManager;
import ai.startree.thirdeye.auth.ThirdEyeServerPrincipal;
import ai.startree.thirdeye.mapper.ApiBeanMapper;
import ai.startree.thirdeye.spi.api.AnomalyApi;
import ai.startree.thirdeye.spi.api.AnomalyFeedbackApi;
import ai.startree.thirdeye.spi.api.AnomalyStatsApi;
import ai.startree.thirdeye.spi.api.AuthorizationConfigurationApi;
import ai.startree.thirdeye.spi.auth.ResourceIdentifier;
import ai.startree.thirdeye.spi.auth.ThirdEyePrincipal;
import ai.startree.thirdeye.spi.datalayer.AnomalyFilter;
import ai.startree.thirdeye.spi.datalayer.bao.AlertManager;
import ai.startree.thirdeye.spi.datalayer.bao.AnomalyManager;
import ai.startree.thirdeye.spi.datalayer.dto.AnomalyDTO;
import ai.startree.thirdeye.spi.datalayer.dto.AnomalyFeedbackDTO;
import ai.startree.thirdeye.spi.datalayer.dto.AuthorizationConfigurationDTO;
import com.google.common.collect.ImmutableMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;

@Singleton
public class AnomalyService extends CrudService<AnomalyApi, AnomalyDTO> {

  public static final ImmutableMap<String, String> API_TO_INDEX_FILTER_MAP = ImmutableMap.<String, String>builder()
      .put("alert.id", "detectionConfigId")
      .put("startTime", "startTime")
      .put("endTime", "endTime")
      .put("isChild", "child")
      .put("metadata.metric.name", "metric")
      .put("metadata.dataset.name", "collection")
      .put("enumerationItem.id", "enumerationItemId")
      .put("feedback.id", "anomalyFeedbackId")
      .put("anomalyLabels.ignore", "ignored")
      .build();

  private final AnomalyManager anomalyManager;
  private final AlertManager alertManager;

  @Inject
  public AnomalyService(
      final AnomalyManager anomalyManager,
      final AlertManager alertManager,
      final AuthorizationManager authorizationManager) {
    super(authorizationManager, anomalyManager, API_TO_INDEX_FILTER_MAP);
    this.anomalyManager = anomalyManager;
    this.alertManager = alertManager;
  }

  @Override
  protected RequestCache createRequestCache() {
    return super.createRequestCache()
        .setAlerts(buildCache(alertManager::findById));
  }

  @Override
  protected AnomalyDTO toDto(final AnomalyApi api) {
    final AnomalyDTO dto = ApiBeanMapper.toDto(api);
    // todo authz - once namespace resolver is removed - simply inherit from the alert id
    final ResourceIdentifier authId = authorizationManager.resourceId(dto);
    dto.setAuth(new AuthorizationConfigurationDTO().setNamespace(authId.getNamespace()));
    return dto;
  }

  @Override
  protected AnomalyApi toApi(final AnomalyDTO dto, final RequestCache cache) {
    final AnomalyApi anomalyApi = ApiBeanMapper.toApi(dto);
    optional(anomalyApi.getAlert())
        .filter(alertApi -> alertApi.getId() != null)
        .ifPresent(alertApi -> alertApi.setName(cache.getAlerts()
            .getUnchecked(alertApi.getId())
            .getName()));
    // fixme cyril authz - implement migration to written namespace - see how it's done for enumeration items - at bootstrap time
    anomalyApi.setAuth(
        new AuthorizationConfigurationApi().setNamespace(authorizationManager.resourceId(
            dto).getNamespace()));
    return anomalyApi;
  }

  public void setFeedback(final ThirdEyeServerPrincipal principal, final Long id,
      final AnomalyFeedbackApi api) {
    final AnomalyDTO dto = getDto(id);
    // todo cyril review authz - only require read right to add a feedback to an anomaly - to avoid feedback frictions for the moment
    authorizationManager.ensureCanRead(principal, dto);
    final AnomalyFeedbackDTO feedbackDTO = ApiBeanMapper.toAnomalyFeedbackDTO(api);
    feedbackDTO.setUpdatedBy(principal.getName());
    dto.setFeedback(feedbackDTO);
    anomalyManager.updateAnomalyFeedback(dto);

    if (dto.isChild()) {
      optional(anomalyManager.findParent(dto))
          .ifPresent(p -> {
            p.setFeedback(feedbackDTO);
            anomalyManager.updateAnomalyFeedback(p);
          });
    }
  }

  public AnomalyStatsApi stats(final ThirdEyePrincipal principal, final @Nullable Long startTime,
      final @Nullable Long endTime) {
    final AnomalyFilter filter = new AnomalyFilter()
        .setStartTimeIsGte(startTime)
        .setEndTimeIsLte(endTime);
    final @Nullable String namespace = authorizationManager.currentNamespace(principal);
    // todo cyril authz usage of dummy entity for access check - avoid this
    authorizationManager.ensureCanRead(principal, new AnomalyDTO().setAuth(new AuthorizationConfigurationDTO().setNamespace(namespace)));
    return anomalyManager.anomalyStats(namespace, filter);
  }

  // fixme cyril authz implement validate and ensure alert id is set - anomalies can be created manually
}
