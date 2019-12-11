/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.camunda.optimize.dto.optimize.OptimizeDto;

import java.util.Optional;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class EventSequenceCountDto implements OptimizeDto {

  public static final String ID_FIELD_SEPARATOR = ":";
  public static final String ID_EVENT_SEPARATOR = "%";

  String id;
  EventTypeDto sourceEvent;
  EventTypeDto targetEvent;
  Long count;

  public String getId() {
    if (id == null) {
      generateIdForEventSequenceCountDto();
    }
    return id;
  }

  public void generateIdForEventSequenceCountDto() {
    if (id == null) {
      id = generateIdForEventType(sourceEvent) + ID_EVENT_SEPARATOR + generateIdForEventType(targetEvent);
    }
  }

  private String generateIdForEventType(EventTypeDto eventTypeDto) {
    final Optional<EventTypeDto> eventType = Optional.ofNullable(eventTypeDto);
    return String.join(
      ID_FIELD_SEPARATOR,
      eventType.map(EventTypeDto::getGroup).orElse(null),
      eventType.map(EventTypeDto::getSource).orElse(null),
      eventType.map(EventTypeDto::getEventName).orElse(null)
    );
  }

}
