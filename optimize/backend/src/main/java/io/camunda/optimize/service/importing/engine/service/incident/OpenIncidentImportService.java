/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.importing.engine.service.incident;

import io.camunda.optimize.dto.optimize.persistence.incident.IncidentDto;
import io.camunda.optimize.rest.engine.EngineContext;
import io.camunda.optimize.service.db.DatabaseClient;
import io.camunda.optimize.service.db.writer.incident.OpenIncidentWriter;
import io.camunda.optimize.service.importing.DatabaseImportJob;
import io.camunda.optimize.service.importing.engine.service.definition.ProcessDefinitionResolverService;
import io.camunda.optimize.service.importing.job.OpenIncidentDatabaseImportJob;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenIncidentImportService extends AbstractEngineIncidentImportService {

  private final OpenIncidentWriter openIncidentWriter;

  public OpenIncidentImportService(
      final ConfigurationService configurationService,
      final OpenIncidentWriter openIncidentWriter,
      final EngineContext engineContext,
      final ProcessDefinitionResolverService processDefinitionResolverService,
      final DatabaseClient databaseClient) {
    super(configurationService, engineContext, processDefinitionResolverService, databaseClient);
    this.openIncidentWriter = openIncidentWriter;
  }

  @Override
  protected DatabaseImportJob<IncidentDto> createDatabaseImportJob(
      final List<IncidentDto> incidents, final Runnable callback) {
    final OpenIncidentDatabaseImportJob incidentImportJob =
        new OpenIncidentDatabaseImportJob(
            openIncidentWriter, configurationService, callback, databaseClient);
    incidentImportJob.setEntitiesToImport(incidents);
    return incidentImportJob;
  }
}
