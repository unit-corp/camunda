/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.authorization;

import static io.camunda.zeebe.it.util.AuthorizationsUtil.awaitUserExistsInElasticsearch;
import static io.camunda.zeebe.it.util.AuthorizationsUtil.createClientWithAuthorization;
import static io.camunda.zeebe.it.util.AuthorizationsUtil.createUserWithPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.application.Profile;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.ProblemException;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.protocol.rest.AuthorizationPatchRequest.ResourceTypeEnum;
import io.camunda.zeebe.client.protocol.rest.AuthorizationPatchRequestPermissionsInner.PermissionTypeEnum;
import io.camunda.zeebe.it.util.AuthorizationsUtil.Permissions;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.DeploymentIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.test.util.junit.AutoCloseResources;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.containers.BindMode;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@AutoCloseResources
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
public class MessageCorrelationCorrelateAuthorizationIT {

  public static final String INTERMEDIATE_MSG_NAME = "intermediateMsg";
  public static final String START_MSG_NAME = "startMsg";
  public static final String CORRELATION_KEY_VARIABLE = "correlationKey";
  private static final DockerImageName ELASTIC_IMAGE =
      DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch")
          .withTag(RestClient.class.getPackage().getImplementationVersion());

  @Container
  private static final ElasticsearchContainer CONTAINER =
      new ElasticsearchContainer(ELASTIC_IMAGE)
          // use JVM option files to avoid overwriting default options set by the ES container class
          .withClasspathResourceMapping(
              "elasticsearch-fast-startup.options",
              "/usr/share/elasticsearch/config/jvm.options.d/ elasticsearch-fast-startup.options",
              BindMode.READ_ONLY)
          // can be slow in CI
          .withStartupTimeout(Duration.ofMinutes(5))
          .withEnv("action.auto_create_index", "true")
          .withEnv("xpack.security.enabled", "false")
          .withEnv("xpack.watcher.enabled", "false")
          .withEnv("xpack.ml.enabled", "false")
          .withEnv("action.destructive_requires_name", "false");

  private static final String PROCESS_ID = "processId";
  @TestZeebe private TestStandaloneBroker zeebe;
  private ZeebeClient defaultUserClient;
  private ZeebeClient authorizedUserClient;
  private ZeebeClient unauthorizedUserClient;

  @BeforeAll
  void beforeAll() throws Exception {
    zeebe =
        new TestStandaloneBroker()
            .withRecordingExporter(true)
            .withBrokerConfig(
                b ->
                    b.getExperimental()
                        .getEngine()
                        .getAuthorizations()
                        .setEnableAuthorization(true))
            .withCamundaExporter("http://" + CONTAINER.getHttpHostAddress())
            .withAdditionalProfile(Profile.AUTH_BASIC);
    zeebe.start();
    defaultUserClient = createClientWithAuthorization(zeebe, "demo", "demo");
    awaitUserExistsInElasticsearch(CONTAINER.getHttpHostAddress(), "demo");
    defaultUserClient
        .newDeployResourceCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess(PROCESS_ID)
                .startEvent()
                .intermediateCatchEvent()
                .message(
                    m ->
                        m.name(INTERMEDIATE_MSG_NAME)
                            .zeebeCorrelationKeyExpression(CORRELATION_KEY_VARIABLE))
                .endEvent()
                .moveToProcess(PROCESS_ID)
                .startEvent()
                .message(m -> m.name(START_MSG_NAME))
                .done(),
            "process.xml")
        .send()
        .join();

    authorizedUserClient =
        createUserWithPermissions(
            zeebe,
            defaultUserClient,
            CONTAINER.getHttpHostAddress(),
            "foo",
            "password",
            new Permissions(
                ResourceTypeEnum.PROCESS_DEFINITION,
                PermissionTypeEnum.UPDATE,
                List.of(PROCESS_ID)),
            new Permissions(
                ResourceTypeEnum.PROCESS_DEFINITION,
                PermissionTypeEnum.CREATE,
                List.of(PROCESS_ID)));
    unauthorizedUserClient =
        createUserWithPermissions(
            zeebe, defaultUserClient, CONTAINER.getHttpHostAddress(), "bar", "password");
  }

  @Test
  void shouldBeAuthorizedToCorrelateMessageToIntermediateEventWithDefaultUser() {
    // given
    final var correlationKey = UUID.randomUUID().toString();
    final var processInstance = createProcessInstance(correlationKey);

    // when
    final var response =
        defaultUserClient
            .newCorrelateMessageCommand()
            .messageName(INTERMEDIATE_MSG_NAME)
            .correlationKey(correlationKey)
            .send()
            .join();

    // then
    assertThat(response.getProcessInstanceKey()).isEqualTo(processInstance.getProcessInstanceKey());
  }

  @Test
  void shouldBeAuthorizedToCorrelateMessageToIntermediateEventWithUser() {
    // given
    final var correlationKey = UUID.randomUUID().toString();
    final var processInstance = createProcessInstance(correlationKey);

    // when
    final var response =
        authorizedUserClient
            .newCorrelateMessageCommand()
            .messageName(INTERMEDIATE_MSG_NAME)
            .correlationKey(correlationKey)
            .send()
            .join();

    // then
    assertThat(response.getProcessInstanceKey()).isEqualTo(processInstance.getProcessInstanceKey());
  }

  @Test
  void shouldBeUnauthorizedToCorrelateMessageToIntermediateEventIfNoPermissions() {
    // given
    final var correlationKey = UUID.randomUUID().toString();
    createProcessInstance(correlationKey);

    // when
    final var response =
        unauthorizedUserClient
            .newCorrelateMessageCommand()
            .messageName(INTERMEDIATE_MSG_NAME)
            .correlationKey(correlationKey)
            .send();

    // then
    assertThatThrownBy(response::join)
        .isInstanceOf(ProblemException.class)
        .hasMessageContaining("title: UNAUTHORIZED")
        .hasMessageContaining("status: 401")
        .hasMessageContaining(
            "Unauthorized to perform operation 'UPDATE' on resource 'PROCESS_DEFINITION'");
  }

  @Test
  void shouldBeAuthorizedToCorrelateMessageToStartEventWithDefaultUser() {
    // when
    final var response =
        defaultUserClient
            .newCorrelateMessageCommand()
            .messageName(START_MSG_NAME)
            .withoutCorrelationKey()
            .send()
            .join();

    // then
    assertThat(response.getProcessInstanceKey()).isPositive();
  }

  @Test
  void shouldBeAuthorizedToCorrelateMessageToStartEventWithUser() {
    // when
    final var response =
        authorizedUserClient
            .newCorrelateMessageCommand()
            .messageName(START_MSG_NAME)
            .withoutCorrelationKey()
            .send()
            .join();

    // then
    assertThat(response.getProcessInstanceKey()).isPositive();
  }

  @Test
  void shouldBeUnauthorizedToCorrelateMessageToStartEventIfNoPermissions() {
    // when
    final var response =
        unauthorizedUserClient
            .newCorrelateMessageCommand()
            .messageName(START_MSG_NAME)
            .withoutCorrelationKey()
            .send();

    // then
    assertThatThrownBy(response::join)
        .isInstanceOf(ProblemException.class)
        .hasMessageContaining("title: UNAUTHORIZED")
        .hasMessageContaining("status: 401")
        .hasMessageContaining(
            "Unauthorized to perform operation 'CREATE' on resource 'PROCESS_DEFINITION'");
  }

  @Test
  void shouldNotCorrelateAnyMessageIfUnauthorizedForOne() {
    // given a process with a processId the user is not authorized for
    final var unauthorizedProcessId = "unauthorizedProcessId";
    final var resourceName = "unauthorizedProcess.xml";
    final var deploymentKey =
        defaultUserClient
            .newDeployResourceCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess(unauthorizedProcessId)
                    .startEvent()
                    .message(m -> m.name(START_MSG_NAME))
                    .endEvent()
                    .done(),
                resourceName)
            .send()
            .join()
            .getKey();

    // when
    final var response =
        authorizedUserClient
            .newCorrelateMessageCommand()
            .messageName(START_MSG_NAME)
            .withoutCorrelationKey()
            .send();

    // then
    assertThatThrownBy(response::join)
        .isInstanceOf(ProblemException.class)
        .hasMessageContaining("title: UNAUTHORIZED")
        .hasMessageContaining("status: 401")
        .hasMessageContaining(
            "Unauthorized to perform operation 'CREATE' on resource 'PROCESS_DEFINITION'");

    final var deploymentPosition =
        RecordingExporter.deploymentRecords(DeploymentIntent.CREATED)
            .withRecordKey(deploymentKey)
            .getFirst()
            .getPosition();
    assertThat(
            RecordingExporter.records()
                .after(deploymentPosition)
                .limit(r -> r.getRejectionType() == RejectionType.UNAUTHORIZED)
                .processInstanceRecords()
                .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATING)
                .withBpmnProcessId(unauthorizedProcessId)
                .exists())
        .isFalse();
  }

  private ProcessInstanceEvent createProcessInstance(final String correlationKey) {
    return defaultUserClient
        .newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_ID)
        .latestVersion()
        .variables(Map.of(CORRELATION_KEY_VARIABLE, correlationKey))
        .send()
        .join();
  }
}
