/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.definition;

import io.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Data;

@Data
public class ProcessDefinitionGroupOptimizeDto {

  private String key;
  private List<ProcessDefinitionOptimizeDto> versions = new ArrayList<>();

  public void sort() {
    try {
      versions.sort(
          Comparator.comparing(
                  ProcessDefinitionOptimizeDto::getVersion, Comparator.comparing(Long::valueOf))
              .reversed());
    } catch (final NumberFormatException exception) {
      throw new OptimizeRuntimeException(
          "Error while trying to parse version numbers for sorting process definition groups: "
              + versions);
    }
  }
}
