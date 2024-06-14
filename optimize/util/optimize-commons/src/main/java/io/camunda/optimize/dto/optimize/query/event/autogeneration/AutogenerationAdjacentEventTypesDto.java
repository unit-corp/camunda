/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.event.autogeneration;

import io.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AutogenerationAdjacentEventTypesDto {
  @Builder.Default private List<EventTypeDto> precedingEvents = new ArrayList<>();
  @Builder.Default private List<EventTypeDto> succeedingEvents = new ArrayList<>();
}
