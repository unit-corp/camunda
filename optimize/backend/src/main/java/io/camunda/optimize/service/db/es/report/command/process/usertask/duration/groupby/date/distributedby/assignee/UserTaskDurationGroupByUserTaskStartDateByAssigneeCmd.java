/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.es.report.command.process.usertask.duration.groupby.date.distributedby.assignee;

import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import io.camunda.optimize.service.db.es.report.command.ProcessCmd;
import io.camunda.optimize.service.db.es.report.command.exec.ProcessReportCmdExecutionPlan;
import io.camunda.optimize.service.db.es.report.command.exec.builder.ReportCmdExecutionPlanBuilder;
import io.camunda.optimize.service.db.es.report.command.modules.distributed_by.process.identity.ProcessDistributedByAssignee;
import io.camunda.optimize.service.db.es.report.command.modules.group_by.process.date.ProcessGroupByUserTaskStartDate;
import io.camunda.optimize.service.db.es.report.command.modules.view.process.duration.ProcessViewUserTaskDuration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserTaskDurationGroupByUserTaskStartDateByAssigneeCmd
    extends ProcessCmd<List<HyperMapResultEntryDto>> {

  public UserTaskDurationGroupByUserTaskStartDateByAssigneeCmd(
      final ReportCmdExecutionPlanBuilder builder) {
    super(builder);
  }

  @Override
  protected ProcessReportCmdExecutionPlan<List<HyperMapResultEntryDto>> buildExecutionPlan(
      final ReportCmdExecutionPlanBuilder builder) {
    return builder
        .createExecutionPlan()
        .processCommand()
        .view(ProcessViewUserTaskDuration.class)
        .groupBy(ProcessGroupByUserTaskStartDate.class)
        .distributedBy(ProcessDistributedByAssignee.class)
        .resultAsHyperMap()
        .build();
  }

  @Override
  public boolean isAssigneeReport() {
    return true;
  }
}
