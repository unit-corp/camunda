/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.rest;

import java.util.List;
import lombok.Data;

@Data
public class FlowNodeIdsToNamesRequestDto {

  protected String processDefinitionKey;
  protected String processDefinitionVersion;
  protected String tenantId;
  protected List<String> nodeIds;
}
