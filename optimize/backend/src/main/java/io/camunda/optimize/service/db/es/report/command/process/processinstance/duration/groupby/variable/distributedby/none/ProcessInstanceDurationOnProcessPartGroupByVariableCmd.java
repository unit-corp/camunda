/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.es.report.command.process.processinstance.duration.groupby.variable.distributedby.none;

import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import io.camunda.optimize.service.db.es.report.command.ProcessCmd;
import io.camunda.optimize.service.db.es.report.command.exec.ProcessReportCmdExecutionPlan;
import io.camunda.optimize.service.db.es.report.command.exec.builder.ReportCmdExecutionPlanBuilder;
import io.camunda.optimize.service.db.es.report.command.modules.distributed_by.process.ProcessDistributedByNone;
import io.camunda.optimize.service.db.es.report.command.modules.group_by.process.ProcessGroupByVariable;
import io.camunda.optimize.service.db.es.report.command.modules.view.process.duration.ProcessViewInstanceDurationOnProcessPart;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProcessInstanceDurationOnProcessPartGroupByVariableCmd
    extends ProcessCmd<List<MapResultEntryDto>> {

  public ProcessInstanceDurationOnProcessPartGroupByVariableCmd(
      final ReportCmdExecutionPlanBuilder builder) {
    super(builder);
  }

  @Override
  protected ProcessReportCmdExecutionPlan<List<MapResultEntryDto>> buildExecutionPlan(
      final ReportCmdExecutionPlanBuilder builder) {
    return builder
        .createExecutionPlan()
        .processCommand()
        .view(ProcessViewInstanceDurationOnProcessPart.class)
        .groupBy(ProcessGroupByVariable.class)
        .distributedBy(ProcessDistributedByNone.class)
        .resultAsMap()
        .build();
  }
}
