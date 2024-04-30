/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.topology.api;

import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.camunda.zeebe.scheduler.testing.TestActorFuture;
import io.camunda.zeebe.topology.changes.ConfigurationChangeCoordinator;
import io.camunda.zeebe.topology.state.ClusterChangePlan;
import io.camunda.zeebe.topology.state.ClusterConfiguration;
import io.camunda.zeebe.topology.state.ClusterConfigurationChangeOperation;
import java.util.ArrayList;
import java.util.List;

final class RecordingChangeCoordinator implements ConfigurationChangeCoordinator {

  private ClusterConfiguration currentTopology = ClusterConfiguration.init();
  private final List<ClusterConfigurationChangeOperation> lastAppliedOperation = new ArrayList<>();

  public void setCurrentTopology(final ClusterConfiguration topology) {
    currentTopology = topology;
  }

  @Override
  public ActorFuture<ClusterConfiguration> getClusterConfiguration() {
    return TestActorFuture.completedFuture(currentTopology);
  }

  @Override
  public ActorFuture<TopologyChangeResult> applyOperations(final TopologyChangeRequest request) {
    final var operationsEither = request.operations(currentTopology);
    if (operationsEither.isLeft()) {
      return TestActorFuture.failedFuture(operationsEither.getLeft());
    }

    final var operations = operationsEither.get();
    lastAppliedOperation.clear();
    lastAppliedOperation.addAll(operations);
    final var newTopology =
        operations.isEmpty() ? currentTopology : currentTopology.startTopologyChange(operations);

    return TestActorFuture.completedFuture(
        new TopologyChangeResult(
            currentTopology,
            newTopology, // This is not correct, but enough for tests
            newTopology.pendingChanges().map(ClusterChangePlan::id).orElse(0L),
            operations));
  }

  @Override
  public ActorFuture<TopologyChangeResult> simulateOperations(
      final TopologyChangeRequest requestTransformer) {
    throw new UnsupportedOperationException("Simulating changes is not supported in tests");
  }

  @Override
  public ActorFuture<ClusterConfiguration> cancelChange(final long changeId) {
    return TestActorFuture.failedFuture(new UnsupportedOperationException());
  }

  public List<ClusterConfigurationChangeOperation> getLastAppliedOperation() {
    return lastAppliedOperation;
  }
}
