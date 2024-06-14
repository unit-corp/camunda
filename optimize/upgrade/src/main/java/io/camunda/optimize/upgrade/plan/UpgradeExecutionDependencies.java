/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.upgrade.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.service.db.es.OptimizeElasticsearchClient;
import io.camunda.optimize.service.db.es.schema.ElasticSearchMetadataService;
import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import io.camunda.optimize.service.util.configuration.ConfigurationService;

public record UpgradeExecutionDependencies(
    ConfigurationService configurationService,
    OptimizeIndexNameService indexNameService,
    OptimizeElasticsearchClient esClient,
    ObjectMapper objectMapper,
    ElasticSearchMetadataService metadataService) {}
