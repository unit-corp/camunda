/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.externalcode.client.sync;

import io.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.ComponentTemplate;
import org.opensearch.client.opensearch.cluster.ComponentTemplateNode;
import org.opensearch.client.opensearch.cluster.PutComponentTemplateRequest;
import org.opensearch.client.opensearch.indices.PutTemplateRequest;

public class OpenSearchTemplateOperations extends OpenSearchRetryOperation {
  public OpenSearchTemplateOperations(
      final OpenSearchClient openSearchClient, final OptimizeIndexNameService indexNameService) {
    super(openSearchClient, indexNameService);
  }

  private boolean templatesExist(final String templatePattern) throws IOException {
    return openSearchClient.indices().existsIndexTemplate(it -> it.name(templatePattern)).value();
  }

  public boolean createTemplateWithRetries(final PutTemplateRequest request) {
    return executeWithRetries(
        "CreateTemplate " + request.name(),
        () -> {
          if (!templatesExist(request.name())) {
            return openSearchClient.indices().putTemplate(request).acknowledged();
          }
          return true;
        });
  }

  public boolean deleteTemplatesWithRetries(final String templateNamePattern) {
    return executeWithRetries(
        "DeleteTemplate " + templateNamePattern,
        () -> {
          if (templatesExist(templateNamePattern)) {
            return openSearchClient
                .indices()
                .deleteIndexTemplate(it -> it.name(templateNamePattern))
                .acknowledged();
          }
          return true;
        });
  }

  public boolean createComponentTemplateWithRetries(final PutComponentTemplateRequest request) {
    return executeWithRetries(
        "CreateComponentTemplate " + request.name(),
        () -> {
          if (!templatesExist(request.name())) {
            return openSearchClient.cluster().putComponentTemplate(request).acknowledged();
          }
          return false;
        });
  }

  public Map<String, ComponentTemplateNode> getComponentTemplate() {
    return safe(
        () ->
            openSearchClient.cluster().getComponentTemplate().componentTemplates().stream()
                .collect(
                    Collectors.toMap(
                        ComponentTemplate::name, ComponentTemplate::componentTemplate)),
        e -> "Failed to get component template from opensearch!");
  }
}
