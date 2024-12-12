/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.service;

import io.camunda.search.clients.UsageMetricsSearchClient;
import io.camunda.search.entities.UsageMetricsCount;
import io.camunda.search.query.UsageMetricsQuery;
import io.camunda.security.auth.Authentication;
import io.camunda.service.security.SecurityContextProvider;
import io.camunda.zeebe.broker.client.api.BrokerClient;

public class UsageMetricsServices extends ApiServices<UsageMetricsServices> {

  private final UsageMetricsSearchClient usageMetricsSearchClient;

  public UsageMetricsServices(
      final BrokerClient brokerClient,
      final SecurityContextProvider securityContextProvider,
      final UsageMetricsSearchClient usageMetricsSearchClient,
      final Authentication authentication) {
    super(brokerClient, securityContextProvider, authentication);
    this.usageMetricsSearchClient = usageMetricsSearchClient;
  }

  public UsageMetricsCount search(final UsageMetricsQuery query) {
    final var assignees = usageMetricsSearchClient.countAssignees(query);
    final var processInstances = usageMetricsSearchClient.countProcessInstances(query);
    final var decisionInstances = usageMetricsSearchClient.countDecisionInstances(query);
    return new UsageMetricsCount(assignees, processInstances, decisionInstances);
  }

  @Override
  public UsageMetricsServices withAuthentication(final Authentication authentication) {
    return new UsageMetricsServices(
        brokerClient, securityContextProvider, usageMetricsSearchClient, authentication);
  }
}
