/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.service;

import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.VariableDbModel;
import io.camunda.db.rdbms.write.queue.ContextType;
import io.camunda.db.rdbms.write.queue.ExecutionQueue;
import io.camunda.db.rdbms.write.queue.QueueItem;

public class UserTaskWriter {

  private final ExecutionQueue executionQueue;

  public UserTaskWriter(final ExecutionQueue executionQueue) {
    this.executionQueue = executionQueue;
  }

  public void create(final UserTaskDbModel userTaskDbModel) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.USER_TASK,
            userTaskDbModel.key(),
            "io.camunda.db.rdbms.sql.VariableMapper.insert",
            userTaskDbModel));
  }

  public void update(final UserTaskDbModel userTaskDbModel) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.USER_TASK,
            userTaskDbModel.key(),
            "io.camunda.db.rdbms.sql.VariableMapper.update",
            userTaskDbModel));
  }
}
