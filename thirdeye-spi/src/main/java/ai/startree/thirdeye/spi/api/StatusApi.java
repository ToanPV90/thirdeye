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

import ai.startree.thirdeye.spi.ThirdEyeStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class StatusApi implements ThirdEyeApi {

  private ThirdEyeStatus code;
  private String msg;
  private ExceptionApi exception;

  public ThirdEyeStatus getCode() {
    return code;
  }

  public StatusApi setCode(final ThirdEyeStatus code) {
    this.code = code;
    return this;
  }

  public String getMsg() {
    return msg;
  }

  public StatusApi setMsg(final String msg) {
    this.msg = msg;
    return this;
  }

  public ExceptionApi getException() {
    return exception;
  }

  public StatusApi setException(final ExceptionApi exception) {
    this.exception = exception;
    return this;
  }
}
