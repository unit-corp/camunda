/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.importing.zeebe.handler;

import io.camunda.optimize.dto.optimize.datasource.ZeebeDataSourceDto;
import io.camunda.optimize.service.importing.PositionBasedImportIndexHandler;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ZeebeIncidentImportIndexHandler extends PositionBasedImportIndexHandler {

  private static final String ZEEBE_INCIDENT_IMPORT_INDEX_DOC_ID = "zeebeIncidentImportIndex";

  public ZeebeIncidentImportIndexHandler(final ZeebeDataSourceDto dataSourceDto) {
    this.dataSource = dataSourceDto;
  }

  @Override
  protected String getDatabaseDocID() {
    return ZEEBE_INCIDENT_IMPORT_INDEX_DOC_ID;
  }
}
