/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.client.command;

import static io.camunda.zeebe.protocol.record.Assertions.assertThat;
import static io.camunda.zeebe.test.util.TestUtil.waitUntil;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.it.util.RecordingJobHandler;
import io.camunda.zeebe.it.util.ZeebeAssertHelper;
import io.camunda.zeebe.it.util.ZeebeResourcesHelper;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.test.util.junit.AutoCloseResources;
import io.camunda.zeebe.test.util.junit.AutoCloseResources.AutoCloseResource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ZeebeIntegration
@AutoCloseResources
public class UserTaskListenersTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskListenersTest.class);

  @TestZeebe
  private final TestStandaloneBroker zeebe = new TestStandaloneBroker().withRecordingExporter(true);

  @AutoCloseResource private ZeebeClient client;
  private List<JobWorker> workers;

  private ZeebeResourcesHelper resourcesHelper;

  @BeforeEach
  void init() {
    client = zeebe.newClientBuilder().defaultRequestTimeout(Duration.ofSeconds(15)).build();
    workers = new ArrayList<>();
    resourcesHelper = new ZeebeResourcesHelper(client);
  }

  @AfterEach
  void tearDown() {
    workers.forEach(JobWorker::close);
    workers.clear();
  }

  @Test
  void shouldCompleteUserTaskWithCompleteTaskListener() {
    // given
    final var userTaskKey =
        resourcesHelper.createSingleUserTask(
            t -> t.zeebeTaskListener(l -> l.complete().type("my_listener")));

    // open worker for `my_listener` task listener job
    final RecordingJobHandler jbHandler = new RecordingJobHandler();
    workers.add(client.newWorker().jobType("my_listener").handler(jbHandler).open());

    // when: invoke complete user task command
    final var completeUserTaskFuture =
        client
            .newUserTaskCompleteCommand(userTaskKey)
            .send()
            .thenAccept(
                ok -> LOGGER.info("User task with key: '{}' completed successfully.", userTaskKey))
            .exceptionally(
                throwable -> {
                  fail(
                      "Failed to complete user task with key: '%d' due to error:"
                          .formatted(userTaskKey),
                      throwable);
                  return null;
                })
            .toCompletableFuture();

    waitUntil(() -> !jbHandler.getHandledJobs().isEmpty());

    final var listenerJob = jbHandler.getHandledJob("my_listener");

    // complete task listener job
    client.newCompleteCommand(listenerJob).send().join();

    // wait for `complete` user task command completion
    completeUserTaskFuture.join();

    // then
    ZeebeAssertHelper.assertUserTaskCompleted(
        userTaskKey,
        (userTask) -> {
          assertThat(userTask.getVariables()).isEmpty();
          assertThat(userTask.getAction()).isEqualTo("complete");
        });
  }

  @Test
  void shouldRejectCompleteTaskListenerJobCompletionWhenVariablesAreSetAndCreateIncident() {
    // given
    final int jobRetries = 2;
    final var listenerType = "complete_with_variables";
    final var userTaskKey =
        resourcesHelper.createSingleUserTask(
            task ->
                task.zeebeTaskListener(
                    listener ->
                        listener
                            .complete()
                            .type(listenerType)
                            .retries(String.valueOf(jobRetries))));

    final JobHandler completeJobWithVariableHandler =
        (jobClient, job) ->
            jobClient.newCompleteCommand(job.getKey()).variable("my_variable", 123).send().join();

    final var recordingHandler = new RecordingJobHandler(completeJobWithVariableHandler);
    workers.add(client.newWorker().jobType(listenerType).handler(recordingHandler).open());

    // when
    client.newUserTaskCompleteCommand(userTaskKey).send();
    await("until all retries are exhausted")
        .untilAsserted(
            () ->
                assertThat(recordingHandler.getHandledJobs())
                    .describedAs(
                        "TL job should retry until reaching the final attempt with retries set to 1")
                    .last()
                    .extracting(ActivatedJob::getRetries)
                    .isEqualTo(1));

    // then
    final var handledJobs = recordingHandler.getHandledJobs();
    assertThat(handledJobs)
        .hasSize(jobRetries)
        .allSatisfy(job -> assertThat(job.getType()).isEqualTo(listenerType));

    assertThat(handledJobs.getFirst())
        .describedAs("Job attempts should have same field values except 'retries' and 'deadline'")
        .usingRecursiveComparison()
        .ignoringFields("retries", "deadline")
        .isEqualTo(handledJobs.getLast());

    // assert that an incident was created due to the rejection of TL job completion with variables
    final var expectedErrorMessageWithRejectionReason =
        "Command 'COMPLETE' rejected with code 'INVALID_ARGUMENT': Task Listener job completion with variables is not supported";
    ZeebeAssertHelper.assertIncidentCreated(
        incident ->
            assertThat(incident)
                .hasJobKey(handledJobs.getLast().getKey())
                .hasErrorType(ErrorType.JOB_NO_RETRIES)
                .extracting(
                    IncidentRecordValue::getErrorMessage, as(InstanceOfAssertFactories.STRING))
                .startsWith("io.camunda.zeebe.client.api.command.ClientStatusException:")
                .contains(expectedErrorMessageWithRejectionReason));
  }
}
