/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.engine;

import java.util.Optional;
import lombok.Data;

@Data
public class DecisionDefinitionXmlEngineDto implements TenantSpecificEngineDto {
  private String id;
  private String dmnXml;
  private String tenantId;

  @Override
  public Optional<String> getTenantId() {
    return Optional.ofNullable(tenantId);
  }
}
