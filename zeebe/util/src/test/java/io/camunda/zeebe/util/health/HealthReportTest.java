/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.util.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

public class HealthReportTest {

  @Test
  public void reportFromChildrenIsCorrect() {
    final var children =
        ImmutableMap.of(
            "a",
            new HealthReport("a", HealthStatus.HEALTHY, null, ImmutableMap.of()),
            "b",
            new HealthReport(
                "b",
                HealthStatus.UNHEALTHY,
                new HealthIssue("storage is full", null, null),
                ImmutableMap.of()));
    final var root = HealthReport.fromChildrenStatus("root", children);

    final var expected =
        new HealthReport(
            "root",
            HealthStatus.UNHEALTHY,
            new HealthIssue("storage is full", null, null),
            children);

    assertThat(root).isPresent();
    assertThat(root.get()).isEqualTo(expected);
  }
}
