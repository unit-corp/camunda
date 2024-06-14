/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process.filter.util;

import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_END_DATE;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_START_DATE;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RelativeDateFilterStartDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.flownode.FlowNodeDateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.flownode.RelativeFlowNodeDateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FilterApplicationLevel;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FlowNodeEndDateFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.FlowNodeStartDateFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import java.util.List;

public class RelativeFlowNodeDateFilterBuilder {
  private final ProcessFilterBuilder filterBuilder;
  private List<String> flowNodeIds;
  private RelativeDateFilterStartDto start;
  private String type;
  private FilterApplicationLevel filterLevel = FilterApplicationLevel.VIEW;

  private RelativeFlowNodeDateFilterBuilder(ProcessFilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  public static RelativeFlowNodeDateFilterBuilder startDate(ProcessFilterBuilder filterBuilder) {
    RelativeFlowNodeDateFilterBuilder builder =
        new RelativeFlowNodeDateFilterBuilder(filterBuilder);
    builder.type = FLOW_NODE_START_DATE;
    return builder;
  }

  public static RelativeFlowNodeDateFilterBuilder endDate(ProcessFilterBuilder filterBuilder) {
    RelativeFlowNodeDateFilterBuilder builder =
        new RelativeFlowNodeDateFilterBuilder(filterBuilder);
    builder.type = FLOW_NODE_END_DATE;
    return builder;
  }

  public RelativeFlowNodeDateFilterBuilder start(Long value, DateUnit unit) {
    this.start = new RelativeDateFilterStartDto(value, unit);
    return this;
  }

  public RelativeFlowNodeDateFilterBuilder flowNodeIds(final List<String> flowNodeIds) {
    this.flowNodeIds = flowNodeIds;
    return this;
  }

  public RelativeFlowNodeDateFilterBuilder filterLevel(final FilterApplicationLevel filterLevel) {
    this.filterLevel = filterLevel;
    return this;
  }

  public ProcessFilterBuilder add() {
    ProcessFilterDto<FlowNodeDateFilterDataDto<?>> filterDto;
    filterDto =
        type.equals(FLOW_NODE_START_DATE)
            ? new FlowNodeStartDateFilterDto()
            : new FlowNodeEndDateFilterDto();
    filterDto.setData(new RelativeFlowNodeDateFilterDataDto(flowNodeIds, start));
    filterDto.setFilterLevel(filterLevel);
    filterBuilder.addFilter(filterDto);
    return filterBuilder;
  }
}
