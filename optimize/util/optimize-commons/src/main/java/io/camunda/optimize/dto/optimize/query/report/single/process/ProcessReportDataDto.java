/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.report.single.process;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import io.camunda.optimize.dto.optimize.query.report.single.ReportDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.ViewProperty;
import io.camunda.optimize.dto.optimize.query.report.single.configuration.ReportConfigurationDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.distributed.ProcessReportDistributedByDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import io.camunda.optimize.dto.optimize.query.report.single.process.validation.ProcessFiltersMustReferenceExistingDefinitionsConstraint;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewDto;
import io.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@SuperBuilder
@ProcessFiltersMustReferenceExistingDefinitionsConstraint
public class ProcessReportDataDto extends ReportDataDto {

  private static final String COMMAND_KEY_SEPARATOR = "_";
  private static final String MISSING_COMMAND_PART_PLACEHOLDER = "null";

  @Builder.Default @Valid protected List<ProcessFilterDto<?>> filter = new ArrayList<>();
  protected ProcessViewDto view;
  protected ProcessGroupByDto<?> groupBy;

  @Builder.Default
  protected ProcessReportDistributedByDto<?> distributedBy = new ProcessReportDistributedByDto<>();

  protected ProcessVisualization visualization;
  @Builder.Default protected boolean managementReport = false;
  @Builder.Default protected boolean instantPreviewReport = false;

  public String getProcessDefinitionKey() {
    return getDefinitionKey();
  }

  @JsonIgnore
  public void setProcessDefinitionKey(final String key) {
    final List<ReportDataDefinitionDto> definitions = getDefinitions();
    if (definitions.isEmpty()) {
      definitions.add(new ReportDataDefinitionDto());
    }
    definitions.get(0).setKey(key);
  }

  @JsonIgnore
  public void setProcessDefinitionName(final String name) {
    final List<ReportDataDefinitionDto> definitions = getDefinitions();
    if (definitions.isEmpty()) {
      definitions.add(new ReportDataDefinitionDto());
    }
    definitions.get(0).setName(name);
  }

  public List<String> getProcessDefinitionVersions() {
    return getDefinitionVersions();
  }

  @JsonIgnore
  public void setProcessDefinitionVersions(final List<String> versions) {
    final List<ReportDataDefinitionDto> definitions = getDefinitions();
    if (definitions.isEmpty()) {
      definitions.add(new ReportDataDefinitionDto());
    }
    definitions.get(0).setVersions(versions);
  }

  @JsonIgnore
  public void setProcessDefinitionVersion(final String version) {
    final List<ReportDataDefinitionDto> definitions = getDefinitions();
    if (definitions.isEmpty()) {
      definitions.add(new ReportDataDefinitionDto());
    }
    definitions.get(0).setVersion(version);
  }

  @Override
  public List<ViewProperty> getViewProperties() {
    return view.getProperties();
  }

  @Override
  public String createCommandKey() {
    return createCommandKeys().get(0);
  }

  @Override
  public List<String> createCommandKeys() {
    final String groupByCommandKey =
        groupBy == null ? MISSING_COMMAND_PART_PLACEHOLDER : groupBy.createCommandKey();
    final String distributedByCommandKey = createDistributedByCommandKey();
    final String configurationCommandKey =
        Optional.ofNullable(getConfiguration())
            .map(ReportConfigurationDto::createCommandKey)
            .orElse(MISSING_COMMAND_PART_PLACEHOLDER);
    return Optional.ofNullable(view)
        .map(ProcessViewDto::createCommandKeys)
        .orElse(Collections.singletonList(MISSING_COMMAND_PART_PLACEHOLDER))
        .stream()
        .map(
            viewKey ->
                String.join(
                    COMMAND_KEY_SEPARATOR,
                    viewKey,
                    groupByCommandKey,
                    distributedByCommandKey,
                    configurationCommandKey))
        .collect(Collectors.toList());
  }

  public String createDistributedByCommandKey() {
    if (distributedBy != null && (isModelElementCommand() || isInstanceCommand())) {
      return distributedBy.createCommandKey();
    }
    return null;
  }

  @JsonIgnore
  public List<ProcessFilterDto<?>> getAdditionalFiltersForReportType() {
    if (isGroupByEndDateReport()) {
      return ProcessFilterBuilder.filter().completedInstancesOnly().add().buildList();
    } else if (isUserTaskReport()) {
      return ProcessFilterBuilder.filter().userTaskFlowNodesOnly().add().buildList();
    }
    return Collections.emptyList();
  }

  public boolean isUserTaskReport() {
    return nonNull(view) && ProcessViewEntity.USER_TASK.equals(view.getEntity());
  }

  @JsonIgnore
  public Map<String, List<ProcessFilterDto<?>>> groupFiltersByDefinitionIdentifier() {
    final Map<String, List<ProcessFilterDto<?>>> filterByDefinition = new HashMap<>();
    getFilter()
        .forEach(
            filterDto ->
                filterDto
                    .getAppliedTo()
                    .forEach(
                        definitionIdentifier ->
                            filterByDefinition
                                .computeIfAbsent(definitionIdentifier, key -> new ArrayList<>())
                                .add(filterDto)));
    return filterByDefinition;
  }

  private boolean isGroupByEndDateReport() {
    return groupBy != null
        && ProcessViewEntity.PROCESS_INSTANCE.equals(view.getEntity())
        && ProcessGroupByType.END_DATE.equals(groupBy.getType());
  }

  private boolean isModelElementCommand() {
    return nonNull(view)
        && nonNull(view.getEntity())
        && (ProcessViewEntity.USER_TASK.equals(view.getEntity())
            || ProcessViewEntity.FLOW_NODE.equals(view.getEntity()));
  }

  private boolean isInstanceCommand() {
    return nonNull(view)
        && nonNull(view.getEntity())
        && ProcessViewEntity.PROCESS_INSTANCE.equals(view.getEntity());
  }

  public static final class Fields {

    public static final String filter = "filter";
    public static final String view = "view";
    public static final String groupBy = "groupBy";
    public static final String distributedBy = "distributedBy";
    public static final String visualization = "visualization";
    public static final String managementReport = "managementReport";
    public static final String instantPreviewReport = "instantPreviewReport";
  }
}
