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
package ai.startree.thirdeye.detectionpipeline;

import java.time.Duration;

public class ForkJoinConfiguration {

  private Integer parallelism = 5;
  private Duration timeout = Duration.ofHours(1);

  public Integer getParallelism() {
    return parallelism;
  }

  public ForkJoinConfiguration setParallelism(final Integer parallelism) {
    this.parallelism = parallelism;
    return this;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public ForkJoinConfiguration setTimeout(final Duration timeout) {
    this.timeout = timeout;
    return this;
  }
}
