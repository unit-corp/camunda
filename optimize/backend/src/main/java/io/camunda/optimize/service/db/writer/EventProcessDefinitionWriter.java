/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.writer;

import io.camunda.optimize.dto.optimize.query.event.process.EventProcessDefinitionDto;
import java.util.Collection;
import java.util.List;

public interface EventProcessDefinitionWriter {

  void importEventProcessDefinitions(final List<EventProcessDefinitionDto> definitionOptimizeDtos);

  void deleteEventProcessDefinitions(final Collection<String> definitionIds);
}
