/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.report.single.filter.data.variable;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.OperatorMultipleValuesFilterDataDto;
import io.camunda.optimize.dto.optimize.query.variable.VariableType;
import java.util.List;

public class ShortVariableFilterDataDto extends OperatorMultipleValuesVariableFilterDataDto {
  protected ShortVariableFilterDataDto() {
    this(null, null, null);
  }

  public ShortVariableFilterDataDto(
      final String name, final FilterOperator operator, final List<String> values) {
    super(name, VariableType.SHORT, new OperatorMultipleValuesFilterDataDto(operator, values));
  }
}
