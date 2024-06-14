/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.importing;

import io.camunda.optimize.dto.optimize.datasource.ZeebeDataSourceDto;
import io.camunda.optimize.service.importing.page.ImportPage;

public interface ZeebeImportIndexHandler<PAGE extends ImportPage, INDEX_DTO>
    extends ImportIndexHandler<PAGE, INDEX_DTO> {

  ZeebeDataSourceDto getDataSource();
}
