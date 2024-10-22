/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.read.service;

import io.camunda.db.rdbms.sql.UserTaskMapper;
import io.camunda.db.rdbms.write.domain.UserTaskDbModel;
import io.camunda.db.rdbms.write.domain.VariableDbModel;

public class UserTaskReader {

  private final UserTaskMapper userTaskMapper;

  public UserTaskReader(final UserTaskMapper userTaskMapper) {
    this.userTaskMapper = userTaskMapper;
  }

  public UserTaskDbModel findOne(final Long key) {
    return userTaskMapper.findOne(key);
  }
}
