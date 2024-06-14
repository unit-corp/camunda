/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.data.generation.generators.impl.process;

import io.camunda.optimize.data.generation.UserAndGroupProvider;
import io.camunda.optimize.test.util.client.SimpleEngineClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class AuthorizationArrangementDataGenerator extends ProcessDataGenerator {

  private static final String DIAGRAM = "/diagrams/process/authorization-arrangement.bpmn";

  public AuthorizationArrangementDataGenerator(
      final SimpleEngineClient engineClient,
      final Integer nVersions,
      final UserAndGroupProvider userAndGroupProvider) {
    super(engineClient, nVersions, userAndGroupProvider);
  }

  @Override
  protected BpmnModelInstance retrieveDiagram() {
    return readProcessDiagramAsInstance(DIAGRAM);
  }

  @Override
  protected Map<String, Object> createVariables() {
    final Map<String, Object> variables = new HashMap<>();
    variables.put("shipmentFilePrepared", ThreadLocalRandom.current().nextDouble());
    variables.put(
        "shippingAuthorizationRequiredConsignee", ThreadLocalRandom.current().nextDouble());
    variables.put("shippingAuthorizationRequiredCountry", ThreadLocalRandom.current().nextDouble());
    variables.put(
        "shipmentAuthorizationRequiredCountryGateway", ThreadLocalRandom.current().nextDouble());
    return variables;
  }
}
