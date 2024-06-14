/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.report;

import io.camunda.optimize.dto.optimize.AuthorizedEntityDto;
import io.camunda.optimize.dto.optimize.RoleType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuthorizedReportEvaluationResult extends AuthorizedEntityDto {
  private ReportEvaluationResult evaluationResult;

  public AuthorizedReportEvaluationResult(
      final ReportEvaluationResult evaluationResult, final RoleType currentUserRole) {
    super(currentUserRole);
    this.evaluationResult = evaluationResult;
  }
}
