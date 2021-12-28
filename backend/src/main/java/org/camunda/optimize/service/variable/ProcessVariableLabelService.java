/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.variable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.DefinitionOptimizeResponseDto;
import org.camunda.optimize.dto.optimize.DefinitionType;
import org.camunda.optimize.dto.optimize.query.variable.DefinitionLabelsDto;
import org.camunda.optimize.service.DefinitionService;
import org.camunda.optimize.service.es.writer.VariableLabelWriter;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProcessVariableLabelService {

  private final VariableLabelWriter variableLabelWriter;
  private final DefinitionService definitionService;

  public void storeVariableLabels(final DefinitionLabelsDto definitionLabelsDto) {
    definitionLabelsDto.getLabels()
      .stream()
      .collect(Collectors.groupingBy(
        label -> label.getVariableName() + label.getVariableType(),
        Collectors.counting()
      )).values().forEach(count -> {
        if (count > 1) {
          throw new BadRequestException("Each variable can only have a single label!");
        }
      });

    if (definitionService.definitionExists(DefinitionType.PROCESS, definitionLabelsDto.getDefinitionKey())) {
      variableLabelWriter.createVariableLabelUpsertRequest(definitionLabelsDto);
    } else {
        throw new NotFoundException(
          "The process definition with id " + definitionLabelsDto.getDefinitionKey() + " has not yet been imported to Optimize");
    }
  }

}
