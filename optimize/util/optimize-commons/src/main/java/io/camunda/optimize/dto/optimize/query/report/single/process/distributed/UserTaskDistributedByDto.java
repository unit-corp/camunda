/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.distributed;

import io.camunda.optimize.dto.optimize.query.report.single.configuration.DistributedByType;
import io.camunda.optimize.dto.optimize.query.report.single.process.distributed.value.UserTaskDistributedByValueDto;

public class UserTaskDistributedByDto
    extends ProcessReportDistributedByDto<UserTaskDistributedByValueDto> {

  public UserTaskDistributedByDto() {
    this.type = DistributedByType.USER_TASK;
  }
}
