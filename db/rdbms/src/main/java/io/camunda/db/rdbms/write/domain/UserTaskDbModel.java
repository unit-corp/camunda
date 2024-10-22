/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.domain;

import java.time.OffsetDateTime;
import java.util.List;

public record UserTaskDbModel(
    Long key,
    String flowNodeBpmnId,
    String bpmnProcessId,
    OffsetDateTime creationTime,
    OffsetDateTime completionTime,
    String assignee,
    UserTaskState state,
    Long formKey,
    Long processDefinitionKey, // processDefinitionId in UserTaskEntity
    Long processInstanceKey, // processInstanceId in UserTaskEntity
    Long elementInstanceKey,// flowNodeInstanceId in UserTaskEntity
    String tenantId,
    String dueDate,
    String followUpDate,
    List<String> candidateGroups,
    List<String> candidateUsers,
    String externalFormReference,
    Integer processDefinitionVersion,
    String serializedCustomHeaders,
    Integer priority) {
  public enum UserTaskState {
    CREATED,
    COMPLETED,
    CANCELED,
    FAILED
  }
}
