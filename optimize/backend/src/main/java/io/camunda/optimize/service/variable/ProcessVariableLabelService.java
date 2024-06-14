/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.variable;

import io.camunda.optimize.dto.optimize.DefinitionType;
import io.camunda.optimize.dto.optimize.query.variable.DefinitionVariableLabelsDto;
import io.camunda.optimize.service.DefinitionService;
import io.camunda.optimize.service.db.writer.VariableLabelWriter;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProcessVariableLabelService {

  private final VariableLabelWriter variableLabelWriter;
  private final DefinitionService definitionService;

  public void storeVariableLabels(final DefinitionVariableLabelsDto definitionVariableLabelsDto) {
    definitionVariableLabelsDto.getLabels().stream()
        .collect(
            Collectors.groupingBy(
                label -> label.getVariableName() + label.getVariableType(), Collectors.counting()))
        .values()
        .forEach(
            count -> {
              if (count > 1) {
                throw new BadRequestException("Each variable can only have a single label!");
              }
            });

    if (definitionService.definitionExists(
        DefinitionType.PROCESS, definitionVariableLabelsDto.getDefinitionKey())) {
      variableLabelWriter.createVariableLabelUpsertRequest(definitionVariableLabelsDto);
    } else {
      throw new NotFoundException(
          "The process definition with id "
              + definitionVariableLabelsDto.getDefinitionKey()
              + " has not yet been "
              + "imported to Optimize");
    }
  }

  public void deleteVariableLabelsForDefinition(final String processDefinitionKey) {
    variableLabelWriter.deleteVariableLabelsForDefinition(processDefinitionKey);
  }
}
