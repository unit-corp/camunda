/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.decision.group;

import static io.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_EVALUATION_DATE_TYPE;
import static io.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_INPUT_VARIABLE_TYPE;
import static io.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_MATCHED_RULE_TYPE;
import static io.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_NONE_TYPE;
import static io.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_OUTPUT_VARIABLE_TYPE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.camunda.optimize.dto.optimize.query.report.single.decision.group.value.DecisionGroupByValueDto;
import lombok.Data;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DecisionGroupByNoneDto.class, name = GROUP_BY_NONE_TYPE),
  @JsonSubTypes.Type(
      value = DecisionGroupByEvaluationDateTimeDto.class,
      name = GROUP_BY_EVALUATION_DATE_TYPE),
  @JsonSubTypes.Type(
      value = DecisionGroupByInputVariableDto.class,
      name = GROUP_BY_INPUT_VARIABLE_TYPE),
  @JsonSubTypes.Type(
      value = DecisionGroupByOutputVariableDto.class,
      name = GROUP_BY_OUTPUT_VARIABLE_TYPE),
  @JsonSubTypes.Type(value = DecisionGroupByMatchedRuleDto.class, name = GROUP_BY_MATCHED_RULE_TYPE)
})
@Data
public abstract class DecisionGroupByDto<VALUE extends DecisionGroupByValueDto> {

  @JsonProperty protected DecisionGroupByType type;
  protected VALUE value;

  @Override
  public String toString() {
    return type.getId();
  }

  @JsonIgnore
  public String createCommandKey() {
    return type.getId();
  }
}
