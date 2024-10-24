/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.service;

import io.camunda.db.rdbms.sql.FlowNodeInstanceMapper.EndFlowNodeDto;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel;
import io.camunda.db.rdbms.write.domain.FlowNodeInstanceDbModel.FlowNodeInstanceDbModelBuilder;
import io.camunda.db.rdbms.write.queue.ContextType;
import io.camunda.db.rdbms.write.queue.ExecutionQueue;
import io.camunda.db.rdbms.write.queue.QueueItem;
import io.camunda.db.rdbms.write.queue.QueueItemMerger;
import io.camunda.search.entities.FlowNodeInstanceEntity.FlowNodeState;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowNodeInstanceWriter {

  private static final Logger LOG = LoggerFactory.getLogger(FlowNodeInstanceWriter.class);

  private final ExecutionQueue executionQueue;

  public FlowNodeInstanceWriter(final ExecutionQueue executionQueue) {
    this.executionQueue = executionQueue;
  }

  public void create(final FlowNodeInstanceDbModel flowNode) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.FLOW_NODE,
            flowNode.flowNodeInstanceKey(),
            "io.camunda.db.rdbms.sql.FlowNodeInstanceMapper.insert",
            flowNode));
  }

  public void update(final FlowNodeInstanceDbModel flowNode) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.FLOW_NODE,
            flowNode.flowNodeInstanceKey(),
            "io.camunda.db.rdbms.sql.FlowNodeInstanceMapper.update",
            flowNode));
  }

  public void update(
      final long flowNodeKey, final FlowNodeState state, final OffsetDateTime endDate) {
    final var dto = new EndFlowNodeDto(flowNodeKey, state, endDate);
    final boolean wasMerged =
        executionQueue.tryMergeWithExistingQueueItem(new EndFlowNodeToInsertMerger(dto));

    if (!wasMerged) {
      executionQueue.executeInQueue(
          new QueueItem(
              ContextType.FLOW_NODE,
              flowNodeKey,
              "io.camunda.db.rdbms.sql.FlowNodeInstanceMapper.updateStateAndEndDate",
              dto));
    }
  }

  public static class EndFlowNodeToInsertMerger implements QueueItemMerger {

    private final EndFlowNodeDto dto;

    public EndFlowNodeToInsertMerger(final EndFlowNodeDto dto) {
      this.dto = dto;
    }

    @Override
    public boolean canBeMerged(final QueueItem queueItem) {
      return queueItem.contextType() == ContextType.FLOW_NODE
          && queueItem.id().equals(dto.flowNodeKey())
          && queueItem.parameter() instanceof FlowNodeInstanceDbModel;
    }

    @Override
    public QueueItem merge(final QueueItem originalItem) {
      final var newParameter =
          FlowNodeInstanceDbModelBuilder.of((FlowNodeInstanceDbModel) originalItem.parameter())
              .state(dto.state())
              .endDate(dto.endDate())
              .build();

      return new QueueItem(
          originalItem.contextType(), originalItem.id(), originalItem.statementId(), newParameter);
    }
  }
}
