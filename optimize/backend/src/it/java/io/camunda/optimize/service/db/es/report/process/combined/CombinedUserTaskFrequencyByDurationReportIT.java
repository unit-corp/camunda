/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.es.report.process.combined;

import static io.camunda.optimize.service.util.ProcessReportDataType.USER_TASK_FREQ_GROUP_BY_USER_TASK_DURATION;

import io.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import io.camunda.optimize.service.util.ProcessReportDataType;

public class CombinedUserTaskFrequencyByDurationReportIT extends AbstractCombinedDurationReportIT {

  @Override
  protected void startInstanceAndModifyRelevantDurations(
      final String definitionId, final int durationInMillis) {
    final ProcessInstanceEngineDto processInstance =
        engineIntegrationExtension.startProcessInstance(definitionId);
    engineIntegrationExtension.finishAllRunningUserTasks(processInstance.getId());
    engineDatabaseExtension.changeAllFlowNodeTotalDurations(
        processInstance.getId(), durationInMillis);
  }

  @Override
  protected ProcessReportDataType getReportDataType() {
    return USER_TASK_FREQ_GROUP_BY_USER_TASK_DURATION;
  }
}
