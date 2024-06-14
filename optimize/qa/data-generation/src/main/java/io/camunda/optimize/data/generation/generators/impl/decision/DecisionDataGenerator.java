/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.data.generation.generators.impl.decision;

import io.camunda.optimize.data.generation.generators.DataGenerator;
import io.camunda.optimize.test.util.client.SimpleEngineClient;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;

public abstract class DecisionDataGenerator extends DataGenerator<DmnModelInstance> {

  public DecisionDataGenerator(final SimpleEngineClient engineClient, final Integer nVersions) {
    super(engineClient, nVersions, null);
  }

  @Override
  protected void startInstance(final String definitionId, final Map<String, Object> variables) {
    engineClient.startDecisionInstance(definitionId, variables);
  }

  @Override
  protected List<String> deployDiagrams(final DmnModelInstance instance) {
    return engineClient.deployDecisions(instance, nVersions, tenants);
  }

  protected DmnModelInstance readDecisionDiagram(final String dmnPath) {
    final InputStream inputStream = DecisionDataGenerator.class.getResourceAsStream(dmnPath);
    return Dmn.readModelFromStream(inputStream);
  }
}
