/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db;

import io.camunda.optimize.dto.optimize.DecisionDefinitionOptimizeDto;
import io.camunda.optimize.dto.optimize.DefinitionType;
import io.camunda.optimize.service.db.reader.DecisionDefinitionReader;
import io.camunda.optimize.service.db.reader.DefinitionReader;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class DecisionDefinitionReaderImpl implements DecisionDefinitionReader {

  private final DefinitionReader definitionReader;

  @Override
  public Optional<DecisionDefinitionOptimizeDto> getDecisionDefinition(
      final String decisionDefinitionKey,
      final List<String> decisionDefinitionVersions,
      final List<String> tenantIds) {
    return definitionReader.getFirstFullyImportedDefinitionFromTenantsIfAvailable(
        DefinitionType.DECISION, decisionDefinitionKey, decisionDefinitionVersions, tenantIds);
  }

  @Override
  public List<DecisionDefinitionOptimizeDto> getAllDecisionDefinitions() {
    return definitionReader.getDefinitions(DefinitionType.DECISION, false, false, true);
  }

  @Override
  public String getLatestVersionToKey(String key) {
    return definitionReader.getLatestVersionToKey(DefinitionType.DECISION, key);
  }
}
