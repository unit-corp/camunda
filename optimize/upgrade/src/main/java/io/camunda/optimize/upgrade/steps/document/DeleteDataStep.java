/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.upgrade.steps.document;

import io.camunda.optimize.service.db.schema.IndexMappingCreator;
import io.camunda.optimize.upgrade.es.SchemaUpgradeClient;
import io.camunda.optimize.upgrade.steps.UpgradeStep;
import io.camunda.optimize.upgrade.steps.UpgradeStepType;
import lombok.EqualsAndHashCode;
import org.elasticsearch.index.query.QueryBuilder;

@EqualsAndHashCode(callSuper = true)
public class DeleteDataStep extends UpgradeStep {
  private final QueryBuilder query;

  public DeleteDataStep(final IndexMappingCreator index, final QueryBuilder query) {
    super(index);
    this.query = query;
  }

  @Override
  public UpgradeStepType getType() {
    return UpgradeStepType.DATA_DELETE;
  }

  @Override
  public void execute(final SchemaUpgradeClient schemaUpgradeClient) {
    schemaUpgradeClient.deleteDataByIndexName(index, query);
  }
}
