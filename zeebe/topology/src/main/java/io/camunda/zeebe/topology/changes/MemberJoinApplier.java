/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.topology.changes;

import io.atomix.cluster.MemberId;
import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.camunda.zeebe.scheduler.future.CompletableActorFuture;
import io.camunda.zeebe.topology.changes.ConfigurationChangeAppliers.MemberOperationApplier;
import io.camunda.zeebe.topology.state.ClusterConfiguration;
import io.camunda.zeebe.topology.state.MemberState;
import io.camunda.zeebe.topology.state.MemberState.State;
import io.camunda.zeebe.util.Either;
import java.util.function.UnaryOperator;

/** A Member join operation is applied when the member is not already part of the topology. */
final class MemberJoinApplier implements MemberOperationApplier {

  private final MemberId memberId;
  private final ClusterMembershipChangeExecutor clusterMembershipChangeExecutor;

  MemberJoinApplier(
      final MemberId memberId,
      final ClusterMembershipChangeExecutor clusterMembershipChangeExecutor) {
    this.memberId = memberId;
    this.clusterMembershipChangeExecutor = clusterMembershipChangeExecutor;
  }

  @Override
  public MemberId memberId() {
    return memberId;
  }

  @Override
  public Either<Exception, UnaryOperator<MemberState>> initMemberState(
      final ClusterConfiguration currentClusterConfiguration) {
    if (currentClusterConfiguration.hasMember(memberId)
        && !currentClusterConfiguration.getMember(memberId).state().equals(State.JOINING)) {
      return Either.left(
          new IllegalStateException(
              String.format(
                  "Expected to join member %s, but the member is already part of the topology",
                  memberId)));
    }

    if (currentClusterConfiguration.hasMember(memberId)
        && currentClusterConfiguration.getMember(memberId).state().equals(State.JOINING)) {
      // If member is already joining, then we don't need to set it again. This can happen if the
      // node restarted while applying the join operation. To ensure that the topology change can
      // make progress, we do not treat this as an error.
      return Either.right(UnaryOperator.identity());
    }

    return Either.right(
        ignore ->
            // Member doesn't exist, so create a new state
            MemberState.uninitialized().toJoining());
  }

  @Override
  public ActorFuture<UnaryOperator<MemberState>> applyOperation() {
    final var future = new CompletableActorFuture<UnaryOperator<MemberState>>();
    clusterMembershipChangeExecutor
        .addBroker(memberId)
        .onComplete(
            (ignore, error) -> {
              if (error == null) {
                future.complete(MemberState::toActive);
              } else {
                future.completeExceptionally(error);
              }
            });
    return future;
  }
}
