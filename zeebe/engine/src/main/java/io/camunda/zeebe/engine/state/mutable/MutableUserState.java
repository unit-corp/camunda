/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.mutable;

import io.camunda.zeebe.engine.state.immutable.UserState;
import io.camunda.zeebe.protocol.impl.record.value.user.UserRecord;

public interface MutableUserState extends UserState {

  void create(final UserRecord user);

  void addRole(final long userKey, final long roleKey);

  void removeRole(final long userKey, final long roleKey);

  void addTenantId(final long userKey, final String tenantId);

  void removeTenant(final long userKey, final String tenantId);
}
