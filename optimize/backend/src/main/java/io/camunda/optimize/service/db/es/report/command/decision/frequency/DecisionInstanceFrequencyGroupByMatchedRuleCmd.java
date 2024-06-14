/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.es.report.command.decision.frequency;

import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.MapResultEntryDto;
import io.camunda.optimize.service.db.es.report.command.DecisionCmd;
import io.camunda.optimize.service.db.es.report.command.exec.DecisionReportCmdExecutionPlan;
import io.camunda.optimize.service.db.es.report.command.exec.builder.ReportCmdExecutionPlanBuilder;
import io.camunda.optimize.service.db.es.report.command.modules.distributed_by.decision.DecisionDistributedByNone;
import io.camunda.optimize.service.db.es.report.command.modules.group_by.decision.DecisionGroupByMatchedRule;
import io.camunda.optimize.service.db.es.report.command.modules.view.decision.DecisionViewInstanceFrequency;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DecisionInstanceFrequencyGroupByMatchedRuleCmd
    extends DecisionCmd<List<MapResultEntryDto>> {

  public DecisionInstanceFrequencyGroupByMatchedRuleCmd(
      final ReportCmdExecutionPlanBuilder builder) {
    super(builder);
  }

  @Override
  protected DecisionReportCmdExecutionPlan<List<MapResultEntryDto>> buildExecutionPlan(
      final ReportCmdExecutionPlanBuilder builder) {
    return builder
        .createExecutionPlan()
        .decisionCommand()
        .view(DecisionViewInstanceFrequency.class)
        .groupBy(DecisionGroupByMatchedRule.class)
        .distributedBy(DecisionDistributedByNone.class)
        .resultAsMap()
        .build();
  }
}
