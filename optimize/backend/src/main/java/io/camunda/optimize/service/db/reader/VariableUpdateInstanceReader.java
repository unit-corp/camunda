/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.reader;

import io.camunda.optimize.dto.optimize.query.variable.VariableUpdateInstanceDto;
import io.camunda.optimize.service.db.repository.VariableRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class VariableUpdateInstanceReader {

  private final VariableRepository variableRepository;

  public List<VariableUpdateInstanceDto> getVariableInstanceUpdatesForProcessInstanceIds(
      final Set<String> processInstanceIds) {
    log.debug(
        "Fetching variable instance updates for [{}] process instances", processInstanceIds.size());

    if (processInstanceIds.isEmpty()) {
      return Collections.emptyList();
    }
    return variableRepository.getVariableInstanceUpdatesForProcessInstanceIds(processInstanceIds);
  }
}
