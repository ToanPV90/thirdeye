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
package ai.startree.thirdeye.plugins.datasource;

import ai.startree.thirdeye.plugins.datasource.csv.CsvThirdEyeDataSourceFactory;
import ai.startree.thirdeye.plugins.datasource.mock.MockThirdEyeDataSourceFactory;
import ai.startree.thirdeye.plugins.datasource.sql.SqlThirdEyeDataSourceFactory;
import ai.startree.thirdeye.spi.Plugin;
import ai.startree.thirdeye.spi.datasource.ThirdEyeDataSourceFactory;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;

@AutoService(Plugin.class)
public class DefaultDataSourcesPlugin implements Plugin {

  @Override
  public Iterable<ThirdEyeDataSourceFactory> getDataSourceFactories() {
    return ImmutableList.of(
        new SqlThirdEyeDataSourceFactory(),
        new CsvThirdEyeDataSourceFactory(),
        new MockThirdEyeDataSourceFactory()
    );
  }
}
