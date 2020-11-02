/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.pinot.thirdeye.anomalydetection.performanceEvaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pinot.thirdeye.common.dimension.DimensionMap;
import org.apache.pinot.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import org.apache.pinot.thirdeye.util.IntervalUtils;
import org.joda.time.Interval;

/**
 * The precision of the cloned function with regarding to the labeled anomalies in original
 * function.
 * The calculation is based on the time overlapped with the labeled anomalies.
 * precision = (the overlapped time duration between detected anomalies and labelled anomalies) /
 * (the time length of the detected anomalies)
 */
public class PrecisionByTimePerformanceEvaluation extends BasePerformanceEvaluate {

  private Map<DimensionMap, List<Interval>> knownAnomalyIntervals;      // The merged anomaly intervals which are labeled by user
  private List<MergedAnomalyResultDTO> detectedAnomalies;        // The merged anomalies which are generated by anomaly function

  public PrecisionByTimePerformanceEvaluation(List<MergedAnomalyResultDTO> knownAnomalies,
      List<MergedAnomalyResultDTO> detectedAnomalies) {
    this.knownAnomalyIntervals = mergedAnomalyResultsToIntervalMap(knownAnomalies);
    this.detectedAnomalies = detectedAnomalies;
  }

  @Override
  public double evaluate() {
    if (knownAnomalyIntervals == null || knownAnomalyIntervals.size() == 0) {
      return 0;
    }
    Map<DimensionMap, List<Interval>> anomalyIntervals = mergedAnomalyResultsToIntervalMap(
        detectedAnomalies);
    IntervalUtils.mergeIntervals(anomalyIntervals);
    Map<DimensionMap, Long> dimensionToDetectedAnomalyTimeLength = new HashMap<>();
    Map<DimensionMap, Long> dimensionToOverlapTimeLength = new HashMap<>();

    for (MergedAnomalyResultDTO detectedAnomaly : detectedAnomalies) {
      Interval anomalyInterval = new Interval(detectedAnomaly.getStartTime(),
          detectedAnomaly.getEndTime());
      DimensionMap dimensions = detectedAnomaly.getDimensions();
      for (Interval knownAnomalyInterval : knownAnomalyIntervals.get(dimensions)) {
        if (!dimensionToDetectedAnomalyTimeLength.containsKey(dimensions)) {
          dimensionToDetectedAnomalyTimeLength.put(dimensions, 0L);
          dimensionToOverlapTimeLength.put(dimensions, 0L);
        }
        Interval overlapInterval = knownAnomalyInterval.overlap(anomalyInterval);
        if (overlapInterval != null) {
          dimensionToOverlapTimeLength.put(dimensions,
              dimensionToOverlapTimeLength.get(dimensions) + overlapInterval.toDurationMillis());
        }
        dimensionToDetectedAnomalyTimeLength.put(dimensions,
            dimensionToDetectedAnomalyTimeLength.get(dimensions) + anomalyInterval
                .toDurationMillis());
      }
    }

    // take average of all the precisions
    long totalDetectedAnomalyTimeLength = 0;
    long totalDimensionOverlapTimeLength = 0;
    for (DimensionMap dimensions : dimensionToOverlapTimeLength.keySet()) {
      totalDetectedAnomalyTimeLength += dimensionToDetectedAnomalyTimeLength.get(dimensions);
      totalDimensionOverlapTimeLength += dimensionToOverlapTimeLength.get(dimensions);
    }
    if (totalDetectedAnomalyTimeLength == 0) {
      return Double.NaN;
    }
    return (double) totalDimensionOverlapTimeLength / totalDetectedAnomalyTimeLength;
  }
}
