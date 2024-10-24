/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.rdbms.db.fixtures;

import io.camunda.db.rdbms.RdbmsService;
import io.camunda.db.rdbms.write.RdbmsWriter;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel.Builder;
import java.util.List;
import java.util.function.Function;

public final class UserTaskFixtures extends CommonFixtures {

  private UserTaskFixtures() {}

  public static UserTaskDbModel createRandomized() {
    return createRandomized(b -> b);
  }

  public static UserTaskDbModel createRandomized(
      final Function<Builder, Builder> builderFunction) {
    final var builder =
        new Builder()
            .key(nextKey())
            .processInstanceKey(nextKey())
            .scopeKey(nextKey())
            .name("variable-name-" + RANDOM.nextInt(1000))
            .value("variable-value-" + RANDOM.nextInt(1000))
            .tenantId("tenant-" + RANDOM.nextInt(1000));

    return builderFunction.apply(builder).build();
  }

  public static void createAndSaveRandomUserTasks(final RdbmsService rdbmsService) {
    createAndSaveRandomUserTasks(rdbmsService, nextKey());
  }

  public static void createAndSaveRandomUserTasks(
      final RdbmsService rdbmsService,
      final Function<Builder, Builder> builderFunction) {
    final RdbmsWriter rdbmsWriter = rdbmsService.createWriter(0L);
    for (int i = 0; i < 20; i++) {
      rdbmsWriter.getUserTaskWriter().create(UserTaskFixtures.createRandomized(builderFunction));
    }

    rdbmsWriter.flush();
  }

  public static void createAndSaveRandomUserTasks(
      final RdbmsService rdbmsService, final Long scopeKey) {
    createAndSaveRandomUserTasks(rdbmsService, b -> b.scopeKey(scopeKey));
  }

  public static void createAndSaveUserTask(
      final RdbmsService rdbmsService, final UserTaskDbModel processInstance) {
    createAndSaveUserTasks(rdbmsService, List.of(processInstance));
  }

  public static void createAndSaveUserTasks(
      final RdbmsService rdbmsService, final List<UserTaskDbModel> processInstanceList) {
    final RdbmsWriter rdbmsWriter = rdbmsService.createWriter(0L);
    for (final UserTaskDbModel processInstance : processInstanceList) {
      rdbmsWriter.getUserTaskWriter().create(processInstance);
    }
    rdbmsWriter.flush();
  }
}
