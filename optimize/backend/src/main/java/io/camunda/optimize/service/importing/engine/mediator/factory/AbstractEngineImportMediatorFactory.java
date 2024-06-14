/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.importing.engine.mediator.factory;

import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.importing.ImportIndexHandlerRegistry;
import io.camunda.optimize.service.importing.ImportMediator;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.BeanFactory;

@AllArgsConstructor
public abstract class AbstractEngineImportMediatorFactory {
  protected final BeanFactory beanFactory;
  protected final ImportIndexHandlerRegistry importIndexHandlerRegistry;
  protected final ConfigurationService configurationService;
  protected final DatabaseClient databaseClient;

  public abstract List<ImportMediator> createMediators(EngineContext engineContext);
}
