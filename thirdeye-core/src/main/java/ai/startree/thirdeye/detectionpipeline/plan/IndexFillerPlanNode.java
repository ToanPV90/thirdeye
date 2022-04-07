/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.detectionpipeline.plan;

import ai.startree.thirdeye.detectionpipeline.operator.TimeIndexFillerOperator;
import ai.startree.thirdeye.spi.detection.v2.Operator;
import ai.startree.thirdeye.spi.detection.v2.OperatorContext;

public class IndexFillerPlanNode extends DetectionPipelinePlanNode {

  @Override
  public String getType() {
    return "TimeIndexFiller";
  }

  @Override
  public Operator buildOperator() throws Exception {
    final TimeIndexFillerOperator timeIndexFillerOperator = new TimeIndexFillerOperator();
    timeIndexFillerOperator.init(new OperatorContext()
        .setDetectionInterval(this.detectionInterval)
        .setPlanNode(planNodeBean)
        .setInputsMap(inputsMap)
    );
    return timeIndexFillerOperator;
  }
}