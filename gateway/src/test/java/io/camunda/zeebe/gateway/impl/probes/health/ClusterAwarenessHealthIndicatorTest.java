/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.gateway.impl.probes.health;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.gateway.impl.broker.cluster.BrokerClusterState;
import io.micronaut.health.HealthStatus;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.Test;
import reactor.core.publisher.Mono;

public class ClusterAwarenessHealthIndicatorTest {

  @Test
  public void shouldRejectNullInConstructor() {
    // when + then
    assertThatThrownBy(() -> new ClusterAwarenessHealthIndicator(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldReportDownIfSupplierReturnsEmpty() {
    // given
    final Supplier<Optional<BrokerClusterState>> stateSupplier = () -> Optional.empty();
    final var sutHealthIndicator = new ClusterAwarenessHealthIndicator(stateSupplier);

    // when
    final var actualHealth = sutHealthIndicator.getResult();
    final var healthResult = Mono.from(actualHealth).block(Duration.ofMillis(5000));

    // then
    assertThat(healthResult).isNotNull();
    assertThat(healthResult.getStatus()).isEqualTo(HealthStatus.DOWN);
  }

  @Test
  public void shouldReportUpIfListOfBrokersIsNotEmpty() {
    // given
    final BrokerClusterState mockClusterState = mock(BrokerClusterState.class);
    when(mockClusterState.getBrokers()).thenReturn(List.of(1));

    final Supplier<Optional<BrokerClusterState>> stateSupplier =
        () -> Optional.of(mockClusterState);
    final var sutHealthIndicator = new ClusterAwarenessHealthIndicator(stateSupplier);

    // when
    final var actualHealth = sutHealthIndicator.getResult();
    final var healthResult = Mono.from(actualHealth).block(Duration.ofMillis(5000));

    // then
    assertThat(healthResult).isNotNull();
    assertThat(healthResult.getStatus()).isEqualTo(HealthStatus.UP);
  }

  @Test
  public void shouldReportDownIfListOfBrokersIsEmpty() {
    // given
    final BrokerClusterState mockClusterState = mock(BrokerClusterState.class);
    when(mockClusterState.getBrokers()).thenReturn(emptyList());

    final Supplier<Optional<BrokerClusterState>> stateSupplier =
        () -> Optional.of(mockClusterState);
    final var sutHealthIndicator = new ClusterAwarenessHealthIndicator(stateSupplier);

    // when
    final var actualHealth = sutHealthIndicator.getResult();
    final var healthResult = Mono.from(actualHealth).block(Duration.ofMillis(5000));

    // then
    assertThat(healthResult).isNotNull();
    assertThat(healthResult.getStatus()).isEqualTo(HealthStatus.DOWN);
  }
}
