/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.es.report.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.query.report.CommandEvaluationResult;
import io.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import io.camunda.optimize.service.db.es.report.ReportEvaluationContext;
import io.camunda.optimize.service.exceptions.OptimizeValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class NotSupportedCommand implements Command<Object, ReportDefinitionDto<?>> {

  private final ObjectMapper objectMapper;

  @Override
  public CommandEvaluationResult<Object> evaluate(
      final ReportEvaluationContext<ReportDefinitionDto<?>> reportEvaluationContext) {
    // Error should contain the report Name
    try {
      log.warn(
          "The following settings combination of the report data is not supported in Optimize: \n"
              + "{} \n "
              + "Therefore returning error result.",
          objectMapper.writeValueAsString(reportEvaluationContext.getReportDefinition()));
    } catch (JsonProcessingException e) {
      log.error("can't serialize report data", e);
    }
    throw new OptimizeValidationException(
        "This combination of the settings of the report builder is not supported!");
  }

  @Override
  public String createCommandKey() {
    // could be anything, we don't care
    return "not_supported";
  }
}
