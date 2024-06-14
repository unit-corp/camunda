/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.group;

import io.camunda.optimize.dto.optimize.query.report.single.process.group.value.NoneGroupByValueDto;

public class NoneGroupByDto extends ProcessGroupByDto<NoneGroupByValueDto> {

  public NoneGroupByDto() {
    this.type = ProcessGroupByType.NONE;
  }
}
