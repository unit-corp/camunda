/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.read.service.UserTaskReader;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.it.rdbms.db.util.CamundaRdbmsInvocationContextProviderExtension;
import io.camunda.it.rdbms.db.util.CamundaRdbmsTestApplication;
import io.camunda.search.filter.UserTaskFilter;
import io.camunda.search.page.SearchQueryPage;
import io.camunda.search.sort.UserTaskSort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("rdbms")
@ExtendWith(CamundaRdbmsInvocationContextProviderExtension.class)
public class UserTaskIT {

  @TestTemplate
  public void shouldSaveAndFindUserTaskByKey(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var instance = rdbmsService.getUserTaskReader().findOne(randomizedUserTask.key());

    assertThat(instance).isNotNull();
    assertThat(instance).usingRecursiveComparison().isEqualTo(randomizedUserTask);
  }

  @TestTemplate
  public void shouldFindUserTaskByProcessInstanceKey(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final UserTaskDbModel randomizedUserTask = UserTaskFixtures.createRandomized();
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder()
                        .processInstanceKeys(randomizedUserTask.processInstanceKey())
                        .build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(10))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);

    final var instance = searchResult.hits().getFirst();

    assertThat(instance).isNotNull();
    assertThat(instance)
        .usingRecursiveComparison()
        .ignoringFields("isPreview", "fullValue")
        .isEqualTo(randomizedUserTask);
    assertThat(instance.fullValue()).isEqualTo(randomizedUserTask.value());
    assertThat(instance.isPreview()).isFalse();
  }

  @TestTemplate
  public void shouldFindAllUserTasksPaged(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final Long scopeKey = UserTaskFixtures.nextKey();
    createAndSaveRandomUserTasks(rdbmsService, scopeKey);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder().scopeKeys(scopeKey).build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(0).size(5))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(20);
    assertThat(searchResult.hits()).hasSize(5);
  }

  @TestTemplate
  public void shouldFindAllUserTasksPageValuesAreNull(
      final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();

    final Long scopeKey = UserTaskFixtures.nextKey();
    createAndSaveRandomUserTasks(rdbmsService, scopeKey);

    final var searchResult =
        rdbmsService
            .getUserTaskReader()
            .search(
                new UserTaskDbQuery(
                    new UserTaskFilter.Builder().scopeKeys(scopeKey).build(),
                    UserTaskSort.of(b -> b),
                    SearchQueryPage.of(b -> b.from(null).size(null))));

    assertThat(searchResult).isNotNull();
    assertThat(searchResult.total()).isEqualTo(20);
    assertThat(searchResult.hits()).hasSize(20);
  }

  @TestTemplate
  public void shouldFindUserTaskWithFullFilter(final CamundaRdbmsTestApplication testApplication) {
    final RdbmsService rdbmsService = testApplication.getRdbmsService();
    final UserTaskReader processInstanceReader = rdbmsService.getUserTaskReader();

    final Long scopeKey = UserTaskFixtures.nextKey();
    createAndSaveRandomUserTasks(rdbmsService, scopeKey);
    final UserTaskDbModel randomizedUserTask =
        UserTaskFixtures.createRandomized(b -> b.scopeKey(scopeKey));
    createAndSaveUserTask(rdbmsService, randomizedUserTask);

    final var searchResult =
        processInstanceReader.search(
            new UserTaskDbQuery(
                new UserTaskFilter.Builder()
                    .variableKeys(randomizedUserTask.key())
                    .processInstanceKeys(randomizedUserTask.processInstanceKey())
                    .scopeKeys(scopeKey)
                    .tenantIds(randomizedUserTask.tenantId())
                    .build(),
                UserTaskSort.of(b -> b),
                SearchQueryPage.of(b -> b.from(0).size(5))));

    assertThat(searchResult.total()).isEqualTo(1);
    assertThat(searchResult.hits()).hasSize(1);
    assertThat(searchResult.hits().getFirst().key()).isEqualTo(randomizedUserTask.key());
  }
}
