/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db.fixtures;

import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import java.time.OffsetDateTime;
import java.util.List;

public class UserTaskDbModelBuilder {

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
  public UserTaskDbModelBuilder() {}

  // Builder methods for each field
  public UserTaskDbModelBuilder key(final Long key) {
    this.key = key;
    return this;
  }

  public UserTaskDbModelBuilder flowNodeBpmnId(final String flowNodeBpmnId) {
    this.flowNodeBpmnId = flowNodeBpmnId;
    return this;
  }

  public UserTaskDbModelBuilder bpmnProcessId(final String bpmnProcessId) {
    this.bpmnProcessId = bpmnProcessId;
    return this;
  }

  public UserTaskDbModelBuilder creationTime(final OffsetDateTime creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  public UserTaskDbModelBuilder completionTime(final OffsetDateTime completionTime) {
    this.completionTime = completionTime;
    return this;
  }

  public UserTaskDbModelBuilder assignee(final String assignee) {
    this.assignee = assignee;
    return this;
  }

  public UserTaskDbModelBuilder assignee(final UserTaskDbModel.UserTaskState state) {
    this.state = state;
    return this;
  }

  public UserTaskDbModelBuilder formKey(final Long formKey) {
    this.formKey = formKey;
    return this;
  }

  public UserTaskDbModelBuilder processDefinitionKey(final Long processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  public UserTaskDbModelBuilder processInstanceKey(final Long processInstanceKey) {
    this.processInstanceKey = processInstanceKey;
    return this;
  }

  public UserTaskDbModelBuilder elementInstanceKey(final Long elementInstanceKey) {
    this.elementInstanceKey = elementInstanceKey;
    return this;
  }

  public UserTaskDbModelBuilder tenantId(final String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  public UserTaskDbModelBuilder dueDate(final String dueDate) {
    this.dueDate = dueDate;
    return this;
  }

  public UserTaskDbModelBuilder followUpDate(final String followUpDate) {
    this.followUpDate = followUpDate;
    return this;
  }

  public UserTaskDbModelBuilder candidateGroups(final List<String> candidateGroups) {
    this.candidateGroups = candidateGroups;
    return this;
  }

  public UserTaskDbModelBuilder candidateUsers(final List<String> candidateUsers) {
    this.candidateUsers = candidateUsers;
    return this;
  }

  public UserTaskDbModelBuilder externalFormReference(final String externalFormReference) {
    this.externalFormReference = externalFormReference;
    return this;
  }

  public UserTaskDbModelBuilder processDefinitionVersion(final int processDefinitionVersion) {
    this.processDefinitionVersion = processDefinitionVersion;
    return this;
  }

  public UserTaskDbModelBuilder serializedCustomHeaders(final String serializedCustomHeaders) {
    this.serializedCustomHeaders = serializedCustomHeaders;
    return this;
  }

  public UserTaskDbModelBuilder priority(final int priority) {
    this.priority = priority;
    return this;
  }

  // Build method to create the record
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
