/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.es.report.command;

import io.camunda.optimize.dto.optimize.query.report.CommandEvaluationResult;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.service.db.es.report.MinMaxStatDto;
import io.camunda.optimize.service.db.es.report.ReportEvaluationContext;
import io.camunda.optimize.service.exceptions.OptimizeException;
import java.util.Optional;

public interface Command<T, R extends ReportDefinitionDto<?>> {

  CommandEvaluationResult<T> evaluate(ReportEvaluationContext<R> reportEvaluationContext)
      throws OptimizeException;

  String createCommandKey();

  /**
   * This method is used for *combined* grouped by commands to calculate the total data range. This
   * allows to calculate the same bucket interval for each single report in the combined report. By
   * default it's assumed that there is no range to be calculated, this needs to be implemented in
   * corresponding commands.
   *
   * @param reportEvaluationContext the command context to perform the min max retrieval with
   * @return the min max stats for the commands groupBy, empty if not available or not implemented
   */
  default Optional<MinMaxStatDto> getGroupByMinMaxStats(
      final ReportEvaluationContext<R> reportEvaluationContext) {
    return Optional.empty();
  }

  default boolean isAssigneeReport() {
    return false;
  }
}
