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
package ai.startree.thirdeye.plugins.bootstrap.opencore;

import ai.startree.thirdeye.spi.api.AlertTemplateApi;
import org.mapstruct.Mapper;
import org.mapstruct.control.DeepClone;
import org.mapstruct.factory.Mappers;

@Mapper(mappingControl = DeepClone.class)
public interface AlertTemplateMapper {

  AlertTemplateMapper INSTANCE = Mappers.getMapper(AlertTemplateMapper.class);

  AlertTemplateApi deepClone(AlertTemplateApi src);
}
