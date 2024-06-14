/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.service.db.writer;

import io.camunda.optimize.dto.optimize.importing.LastKpiEvaluationResultsDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessDigestDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessDigestRequestDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessOverviewDto;
import io.camunda.optimize.dto.optimize.query.processoverview.ProcessUpdateDto;
import io.camunda.optimize.service.db.repository.ProcessOverviewRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class ProcessOverviewWriter {
  private final ProcessOverviewRepository processOverviewRepository;

  public void updateProcessConfiguration(
      final String processDefinitionKey, final ProcessUpdateDto processUpdateDto) {
    final ProcessOverviewDto overviewDto =
        createNewProcessOverviewDto(processDefinitionKey, processUpdateDto);
    processOverviewRepository.updateProcessConfiguration(processDefinitionKey, overviewDto);
  }

  public void updateProcessDigestResults(
      final String processDefKey, final ProcessDigestDto processDigestDto) {
    processOverviewRepository.updateProcessDigestResults(processDefKey, processDigestDto);
  }

  public void updateProcessOwnerIfNotSet(final String processDefinitionKey, final String ownerId) {
    final ProcessUpdateDto processUpdateDto = new ProcessUpdateDto();
    processUpdateDto.setOwnerId(ownerId);
    final ProcessDigestRequestDto processDigestRequestDto = new ProcessDigestRequestDto();
    processUpdateDto.setProcessDigest(processDigestRequestDto);
    final ProcessOverviewDto processOverviewDto =
        createNewProcessOverviewDto(processDefinitionKey, processUpdateDto);
    processOverviewRepository.updateProcessOwnerIfNotSet(
        processDefinitionKey, ownerId, processOverviewDto);
  }

  public void updateKpisForProcessDefinitions(
      final Map<String, LastKpiEvaluationResultsDto> definitionKeyToKpis) {
    log.debug(
        "Updating KPI values for process definitions with keys: [{}]",
        definitionKeyToKpis.keySet());
    final List<ProcessOverviewDto> processOverviewDtos =
        definitionKeyToKpis.entrySet().stream()
            .map(
                entry -> {
                  Map<String, String> reportIdToValue = entry.getValue().getReportIdToValue();
                  ProcessOverviewDto processOverviewDto = new ProcessOverviewDto();
                  processOverviewDto.setProcessDefinitionKey(entry.getKey());
                  processOverviewDto.setDigest(new ProcessDigestDto(false, Collections.emptyMap()));
                  processOverviewDto.setLastKpiEvaluationResults(reportIdToValue);
                  return processOverviewDto;
                })
            .toList();
    processOverviewRepository.updateKpisForProcessDefinitions(processOverviewDtos);
  }

  public void deleteProcessOwnerEntry(final String processDefinitionKey) {
    log.info("Removing pending entry " + processDefinitionKey);
    processOverviewRepository.deleteProcessOwnerEntry(processDefinitionKey);
  }

  private ProcessOverviewDto createNewProcessOverviewDto(
      final String processDefinitionKey, final ProcessUpdateDto processUpdateDto) {
    final ProcessOverviewDto processOverviewDto = new ProcessOverviewDto();
    processOverviewDto.setProcessDefinitionKey(processDefinitionKey);
    processOverviewDto.setOwner(processUpdateDto.getOwnerId());
    processOverviewDto.setDigest(
        new ProcessDigestDto(
            processUpdateDto.getProcessDigest().isEnabled(), Collections.emptyMap()));
    processOverviewDto.setLastKpiEvaluationResults(Collections.emptyMap());
    return processOverviewDto;
  }
}
