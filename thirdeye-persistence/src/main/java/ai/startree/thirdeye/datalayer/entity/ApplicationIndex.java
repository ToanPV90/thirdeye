/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.datalayer.entity;

public class ApplicationIndex extends AbstractIndexEntity {

  String application;
  String recipients;

  public String getApplication() {
    return application;
  }

  public void setApplication(String application) {
    this.application = application;
  }

  public String getRecipients() {
    return recipients;
  }

  public void setRecipients(String recipients) {
    this.recipients = recipients;
  }
}
