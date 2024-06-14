/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.event.process.db;

import io.camunda.optimize.dto.optimize.OptimizeDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventMappingDto;
import io.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@EqualsAndHashCode
public class DbEventMappingDto implements OptimizeDto {

  String flowNodeId;
  EventTypeDto start;
  EventTypeDto end;

  public static DbEventMappingDto fromEventMappingDto(
      final String flowNodeId, final EventMappingDto eventMappingDto) {
    return DbEventMappingDto.builder()
        .flowNodeId(flowNodeId)
        .start(eventMappingDto.getStart())
        .end(eventMappingDto.getEnd())
        .build();
  }
}
