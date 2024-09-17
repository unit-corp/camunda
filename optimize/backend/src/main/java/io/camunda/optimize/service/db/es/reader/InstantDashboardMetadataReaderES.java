/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.reader;

import static io.camunda.optimize.service.db.DatabaseConstants.INSTANT_DASHBOARD_INDEX_NAME;

import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.query.dashboard.InstantDashboardDataDto;
import io.camunda.optimize.service.db.es.OptimizeElasticsearchClient;
import io.camunda.optimize.service.db.es.builders.OptimizeGetRequestBuilderES;
import io.camunda.optimize.service.db.reader.InstantDashboardMetadataReader;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.configuration.condition.ElasticSearchCondition;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@Conditional(ElasticSearchCondition.class)
public class InstantDashboardMetadataReaderES implements InstantDashboardMetadataReader {

  private final OptimizeElasticsearchClient esClient;
  private final ObjectMapper objectMapper;

  @Override
  public Optional<String> getInstantDashboardIdFor(String processDefinitionKey, String template)
      throws OptimizeRuntimeException {
    log.debug(
        "Fetching Instant preview dashboard ID for [{}] with template [{}] ",
        processDefinitionKey,
        template);
    InstantDashboardDataDto dashboardDataDto = new InstantDashboardDataDto();
    dashboardDataDto.setTemplateName(template);
    dashboardDataDto.setProcessDefinitionKey(processDefinitionKey);

    final String instantDashboardKey = dashboardDataDto.getInstantDashboardId();
    GetRequest getRequest =
        OptimizeGetRequestBuilderES.of(
            b -> b.optimizeIndex(esClient, INSTANT_DASHBOARD_INDEX_NAME).id(instantDashboardKey));

    GetResponse<InstantDashboardDataDto> getResponse;
    try {
      getResponse = esClient.get(getRequest, InstantDashboardDataDto.class);
    } catch (IOException e) {
      String reason =
          String.format(
              "Could not fetch Instant preview dashboard with key [%s]", instantDashboardKey);
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }

    if (getResponse.found()) {
      final InstantDashboardDataDto dashboardData = getResponse.source();
      return Optional.of(dashboardData.getDashboardId());
    } else {
      String reason =
          "Could not find dashboard data for key [" + instantDashboardKey + "] in Elasticsearch.";
      log.error(reason);
      return Optional.empty();
    }
  }
}
