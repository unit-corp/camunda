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
import io.camunda.zeebe.topology.state.ClusterConfigurationChangeOperation.MemberJoinOperation;
import io.camunda.zeebe.util.Either;
import java.util.List;
import java.util.Set;

public class AddMembersTransformer implements TopologyChangeRequest {

  final Set<MemberId> members;

  public AddMembersTransformer(final Set<MemberId> members) {
    this.members = members;
  }

  @Override
  public Either<Exception, List<ClusterConfigurationChangeOperation>> operations(
      final ClusterConfiguration currentTopology) {
    final var operations =
        members.stream()
            // only add members that are not already part of the topology
            .filter(memberId -> !currentTopology.hasMember(memberId))
            .map(MemberJoinOperation::new)
            .map(ClusterConfigurationChangeOperation.class::cast)
            .toList();
    return Either.right(operations);
  }
}
