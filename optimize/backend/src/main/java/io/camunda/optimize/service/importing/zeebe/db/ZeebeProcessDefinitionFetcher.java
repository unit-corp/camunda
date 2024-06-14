/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.importing.zeebe.db;

import io.camunda.optimize.dto.zeebe.definition.ZeebeProcessDefinitionRecordDto;
import io.camunda.optimize.service.importing.page.PositionBasedImportPage;
import java.util.List;

public interface ZeebeProcessDefinitionFetcher extends ZeebeFetcher {
  List<ZeebeProcessDefinitionRecordDto> getZeebeRecordsForPrefixAndPartitionFrom(
      PositionBasedImportPage positionBasedImportPage);
}
