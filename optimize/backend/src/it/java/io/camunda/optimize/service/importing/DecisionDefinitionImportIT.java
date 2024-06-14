/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.importing;

import static io.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static io.camunda.optimize.service.db.DatabaseConstants.DECISION_DEFINITION_INDEX_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import io.camunda.optimize.dto.engine.definition.DecisionDefinitionEngineDto;
import jakarta.ws.rs.core.Response;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

@Tag(OPENSEARCH_PASSING)
public class DecisionDefinitionImportIT extends AbstractImportIT {

  @Test
  public void decisionDefinitionImportBatchesThatRequirePartitioningCanBeImported() {
    // given
    // more definitions than the max ES boolQuery clause limit (1024)
    final int definitionsToDeploy = 1100;
    IntStream.range(0, definitionsToDeploy)
        .forEach(defCount -> engineIntegrationExtension.deployDecisionDefinition());

    // when
    importAllEngineEntitiesFromScratch();

    // then
    assertThat(databaseIntegrationTestExtension.getDocumentCountOf(DECISION_DEFINITION_INDEX_NAME))
        .isEqualTo(definitionsToDeploy);
  }

  @Test
  public void deletedDefinitionsAreMarkedAsDeletedIfXmlIsUnavailable() {
    // given
    final DecisionDefinitionEngineDto deployedDefinition =
        engineIntegrationExtension.deployDecisionDefinition();
    final ClientAndServer engineMockServer = useAndGetEngineMockServer();
    engineMockServer
        .when(
            request()
                .withPath(
                    engineIntegrationExtension.getEnginePath()
                        + "/decision-definition/"
                        + deployedDefinition.getId()
                        + "/xml"),
            Times.once())
        .respond(
            HttpResponse.response()
                .withStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                .withContentType(MediaType.APPLICATION_JSON_UTF_8));

    // when
    importAllEngineEntitiesFromScratch();

    // then
    assertThat(databaseIntegrationTestExtension.getAllDecisionDefinitions())
        .singleElement()
        .satisfies(definition -> assertThat(definition.isDeleted()).isTrue());
  }
}
