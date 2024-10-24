/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.exporter.rdbms;

import static io.camunda.zeebe.protocol.record.intent.UserTaskIntent.*;

import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel.UserTaskState;
import io.camunda.db.rdbms.write.service.UserTaskWriter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.UserTaskIntent;
import io.camunda.zeebe.protocol.record.value.UserTaskRecordValue;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on UserTaskRecordToTaskEntityMapper
 */
public class UserTaskExportHandler implements RdbmsExportHandler<UserTaskRecordValue> {

  private static final Logger LOG = LoggerFactory.getLogger(UserTaskExportHandler.class);

  private final UserTaskWriter userTaskWriter;

  public UserTaskExportHandler(final UserTaskWriter userTaskWriter) {
    this.userTaskWriter = userTaskWriter;
  }

  @Override
  public boolean canExport(final Record<UserTaskRecordValue> record) {
    return record.getIntent() == UserTaskIntent.CREATED
        || record.getIntent() == UserTaskIntent.UPDATED
        || record.getIntent() == UserTaskIntent.CANCELED
        || record.getIntent() == UserTaskIntent.ASSIGNED
        || record.getIntent() == UserTaskIntent.COMPLETED
        || record.getIntent() == UserTaskIntent.MIGRATED;
  }

  @Override
  public void export(final Record<UserTaskRecordValue> record) {
    final UserTaskRecordValue value = record.getValue();
    switch (record.getIntent()) {
      case CREATED -> userTaskWriter.create(
          map(value, UserTaskState.CREATED, null)
      );
      case CANCELED -> userTaskWriter.update(
          map(value, UserTaskState.CANCELED, DateUtil.toOffsetDateTime(record.getTimestamp()))
      );
      case COMPLETED -> userTaskWriter.update(
          map(value, UserTaskState.COMPLETED, DateUtil.toOffsetDateTime(record.getTimestamp()))
      );
      case MIGRATED -> userTaskWriter.update(
          map(value, UserTaskState.CREATED, null)
      );
      default -> userTaskWriter.update(
          map(value, null, null) // We should not update the last state
      );
    }
  }

  private UserTaskDbModel map(
      final UserTaskRecordValue record,
      final UserTaskState state,
      final OffsetDateTime completionTime
  ) {
    return new UserTaskDbModel(
        record.getUserTaskKey(),
        record.getElementId(), // flowNodeBpmnId based on UserTaskRecordToTaskEntityMapper
        record.getBpmnProcessId(),
        DateUtil.toOffsetDateTime(record.getCreationTimestamp()),
        completionTime,
        record.getAssignee(),
        state,
        record.getFormKey(),
        record.getProcessDefinitionKey(),
        record.getProcessInstanceKey(),
        record.getElementInstanceKey(), // flowNodeInstanceId based on UserTaskRecordToTaskEntityMapper
        record.getTenantId(),
        record.getDueDate(),
        record.getFollowUpDate(),
        record.getCandidateGroupsList(),
        record.getCandidateUsersList(),
        record.getExternalFormReference(),
        record.getProcessDefinitionVersion(),
        record.getCustomHeaders(),
        record.getPriority()
    );
  }
}
