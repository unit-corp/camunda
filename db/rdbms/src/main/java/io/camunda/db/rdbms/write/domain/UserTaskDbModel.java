/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.db.rdbms.read.domain.ProcessInstanceDbQuery;
import io.camunda.util.ObjectBuilder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

  static public class Builder implements ObjectBuilder<UserTaskDbModel> {

    private Long key;
    private String flowNodeBpmnId;
    private String bpmnProcessId;
    private OffsetDateTime creationTime;
    private OffsetDateTime completionTime;
    private String assignee;
    private UserTaskDbModel.UserTaskState state;
    private Long formKey;
    private Long processDefinitionKey;
    private Long processInstanceKey;
    private Long elementInstanceKey;
    private String tenantId;
    private String dueDate;
    private String followUpDate;
    private List<String> candidateGroups;
    private List<String> candidateUsers;
    private String externalFormReference;
    private Integer processDefinitionVersion;
    private String serializedCustomHeaders;
    private Integer priority;

    // Public constructor to initialize the builder
    public Builder() {}

    public static UserTaskDbModel of(
        final Function<UserTaskDbModel.Builder, ObjectBuilder<UserTaskDbModel>> fn) {
      return fn.apply(new UserTaskDbModel.Builder()).build();
    }

    // Builder methods for each field
    public Builder key(final Long key) {
      this.key = key;
      return this;
    }

    public Builder flowNodeBpmnId(final String flowNodeBpmnId) {
      this.flowNodeBpmnId = flowNodeBpmnId;
      return this;
    }

    public Builder bpmnProcessId(final String bpmnProcessId) {
      this.bpmnProcessId = bpmnProcessId;
      return this;
    }

    public Builder creationTime(final OffsetDateTime creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    public Builder completionTime(final OffsetDateTime completionTime) {
      this.completionTime = completionTime;
      return this;
    }

    public Builder assignee(final String assignee) {
      this.assignee = assignee;
      return this;
    }

    public Builder assignee(final UserTaskDbModel.UserTaskState state) {
      this.state = state;
      return this;
    }

    public Builder formKey(final Long formKey) {
      this.formKey = formKey;
      return this;
    }

    public Builder processDefinitionKey(final Long processDefinitionKey) {
      this.processDefinitionKey = processDefinitionKey;
      return this;
    }

    public Builder processInstanceKey(final Long processInstanceKey) {
      this.processInstanceKey = processInstanceKey;
      return this;
    }

    public Builder elementInstanceKey(final Long elementInstanceKey) {
      this.elementInstanceKey = elementInstanceKey;
      return this;
    }

    public Builder tenantId(final String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder dueDate(final String dueDate) {
      this.dueDate = dueDate;
      return this;
    }

    public Builder followUpDate(final String followUpDate) {
      this.followUpDate = followUpDate;
      return this;
    }

    public Builder candidateGroups(final List<String> candidateGroups) {
      this.candidateGroups = candidateGroups;
      return this;
    }

    public Builder candidateUsers(final List<String> candidateUsers) {
      this.candidateUsers = candidateUsers;
      return this;
    }

    public Builder externalFormReference(final String externalFormReference) {
      this.externalFormReference = externalFormReference;
      return this;
    }

    public Builder processDefinitionVersion(final int processDefinitionVersion) {
      this.processDefinitionVersion = processDefinitionVersion;
      return this;
    }

    public Builder customHeaders(final Map<String, String> customHeaders)
        throws JsonProcessingException {
      final ObjectMapper mapper = new ObjectMapper();
      serializedCustomHeaders =  mapper.writeValueAsString(customHeaders);
      return this;
    }

    public Builder serializedCustomHeaders(final String serializedCustomHeaders) {
      this.serializedCustomHeaders = serializedCustomHeaders;
      return this;
    }

    public Builder priority(final int priority) {
      this.priority = priority;
      return this;
    }

    // Build method to create the record
    @Override
    public UserTaskDbModel build() {
      return new UserTaskDbModel(
          key,
          flowNodeBpmnId,
          bpmnProcessId,
          creationTime,
          completionTime,
          assignee,
          state,
          formKey,
          processDefinitionKey,
          processInstanceKey,
          elementInstanceKey,
          tenantId,
          dueDate,
          followUpDate,
          candidateGroups,
          candidateUsers,
          externalFormReference,
          processDefinitionVersion,
          serializedCustomHeaders,
          priority
      );
    }
  }

  public enum UserTaskState {
    CREATED,
    COMPLETED,
    CANCELED,
    FAILED
  }
}
