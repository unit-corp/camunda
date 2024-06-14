/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.report.single.configuration.target_value;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class SingleReportTargetValueDto {

  private SingleReportCountChartDto countChart = new SingleReportCountChartDto();
  private DurationProgressDto durationProgress = new DurationProgressDto();
  private Boolean active = false;
  private CountProgressDto countProgress = new CountProgressDto();
  private SingleReportDurationChartDto durationChart = new SingleReportDurationChartDto();
  private Boolean isKpi;
}
