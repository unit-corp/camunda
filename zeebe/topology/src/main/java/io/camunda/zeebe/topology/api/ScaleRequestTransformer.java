/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.topology.api;

import io.atomix.cluster.MemberId;
import io.camunda.zeebe.topology.changes.ConfigurationChangeCoordinator.TopologyChangeRequest;
import io.camunda.zeebe.topology.state.ClusterConfiguration;
import io.camunda.zeebe.topology.state.ClusterConfigurationChangeOperation;
import io.camunda.zeebe.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ScaleRequestTransformer implements TopologyChangeRequest {

  private final Set<MemberId> members;
  private final Optional<Integer> newReplicationFactor;
  private final ArrayList<ClusterConfigurationChangeOperation> generatedOperations = new ArrayList<>();

  public ScaleRequestTransformer(final Set<MemberId> members) {
    this(members, Optional.empty());
  }

  public ScaleRequestTransformer(
      final Set<MemberId> members, final Optional<Integer> newReplicationFactor) {
    this.members = members;
    this.newReplicationFactor = newReplicationFactor;
  }

  @Override
  public Either<Exception, List<ClusterConfigurationChangeOperation>> operations(
      final ClusterConfiguration currentTopology) {
    generatedOperations.clear();

    // First add new members
    return new AddMembersTransformer(members)
        .operations(currentTopology)
        .map(this::addToOperations)
        // then reassign partitions
        .flatMap(
            ignore ->
                new PartitionReassignRequestTransformer(members, newReplicationFactor)
                    .operations(currentTopology))
        .map(this::addToOperations)
        // then remove members that are not part of the new topology
        .flatMap(
            ignore -> {
              final var membersToRemove =
                  currentTopology.members().keySet().stream()
                      .filter(m -> !members.contains(m))
                      .collect(Collectors.toSet());
              return new RemoveMembersTransformer(membersToRemove).operations(currentTopology);
            })
        .map(this::addToOperations);
  }

  private ArrayList<ClusterConfigurationChangeOperation> addToOperations(
      final List<ClusterConfigurationChangeOperation> reassignOperations) {
    generatedOperations.addAll(reassignOperations);
    return generatedOperations;
  }
}
