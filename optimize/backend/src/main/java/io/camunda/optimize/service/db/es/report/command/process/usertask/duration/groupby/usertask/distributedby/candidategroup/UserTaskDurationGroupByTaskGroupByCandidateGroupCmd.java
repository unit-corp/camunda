/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.es.report.command.process.usertask.duration.groupby.usertask.distributedby.candidategroup;

import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import io.camunda.optimize.service.db.es.report.command.ProcessCmd;
import io.camunda.optimize.service.db.es.report.command.exec.ProcessReportCmdExecutionPlan;
import io.camunda.optimize.service.db.es.report.command.exec.builder.ReportCmdExecutionPlanBuilder;
import io.camunda.optimize.service.db.es.report.command.modules.distributed_by.process.identity.ProcessDistributedByCandidateGroup;
import io.camunda.optimize.service.db.es.report.command.modules.group_by.process.usertask.ProcessGroupByUserTask;
import io.camunda.optimize.service.db.es.report.command.modules.view.process.duration.ProcessViewUserTaskDuration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserTaskDurationGroupByTaskGroupByCandidateGroupCmd
    extends ProcessCmd<List<HyperMapResultEntryDto>> {

  public UserTaskDurationGroupByTaskGroupByCandidateGroupCmd(
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
        .groupBy(ProcessGroupByUserTask.class)
        .distributedBy(ProcessDistributedByCandidateGroup.class)
        .resultAsHyperMap()
        .build();
  }
}
