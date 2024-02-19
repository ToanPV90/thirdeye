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
package ai.startree.thirdeye.spi.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Date;

@JsonInclude(Include.NON_NULL)
public class AlertAssociationApi {

  private AlertApi alert;
  private EnumerationItemApi enumerationItem;
  private Date created;
  private Date anomalyCompletionWatermark;

  public AlertApi getAlert() {
    return alert;
  }

  public AlertAssociationApi setAlert(final AlertApi alert) {
    this.alert = alert;
    return this;
  }

  public EnumerationItemApi getEnumerationItem() {
    return enumerationItem;
  }

  public AlertAssociationApi setEnumerationItem(
      final EnumerationItemApi enumerationItem) {
    this.enumerationItem = enumerationItem;
    return this;
  }

  public Date getCreated() {
    return created;
  }

  public AlertAssociationApi setCreated(final Date created) {
    this.created = created;
    return this;
  }

  public Date getAnomalyCompletionWatermark() {
    return anomalyCompletionWatermark;
  }

  public AlertAssociationApi setAnomalyCompletionWatermark(
      final Date anomalyCompletionWatermark) {
    this.anomalyCompletionWatermark = anomalyCompletionWatermark;
    return this;
  }
}
