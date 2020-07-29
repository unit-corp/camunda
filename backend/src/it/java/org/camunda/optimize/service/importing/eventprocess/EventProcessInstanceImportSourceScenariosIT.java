/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing.eventprocess;

import lombok.SneakyThrows;
import org.assertj.core.util.Maps;
import org.camunda.optimize.dto.optimize.importing.index.TimestampBasedImportIndexDto;
import org.camunda.optimize.dto.optimize.query.event.CamundaActivityEventDto;
import org.camunda.optimize.dto.optimize.query.event.EventMappingDto;
import org.camunda.optimize.dto.optimize.query.event.EventProcessInstanceDto;
import org.camunda.optimize.dto.optimize.query.event.EventSourceEntryDto;
import org.camunda.optimize.dto.optimize.query.event.EventTypeDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.es.reader.ElasticsearchReaderUtil;
import org.camunda.optimize.service.es.schema.index.events.CamundaActivityEventIndex;
import org.camunda.optimize.service.util.EsHelper;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.service.events.CamundaEventService.EVENT_SOURCE_CAMUNDA;
import static org.camunda.optimize.service.importing.engine.handler.RunningProcessInstanceImportIndexHandler.RUNNING_PROCESS_INSTANCE_IMPORT_INDEX_DOC_ID;
import static org.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaProcessInstanceEndEventSuffix;
import static org.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaProcessInstanceStartEventSuffix;
import static org.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaTaskEndEventSuffix;
import static org.camunda.optimize.service.util.EventDtoBuilderUtil.applyCamundaTaskStartEventSuffix;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.BUSINESS_KEY_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.TIMESTAMP_BASED_IMPORT_INDEX_NAME;
import static org.camunda.optimize.util.BpmnModels.getDoubleUserTaskDiagram;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;

public class EventProcessInstanceImportSourceScenariosIT extends AbstractEventProcessIT {

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    publishEventMappingUsingProcessInstanceCamundaEvents(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      )
    );
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          processInstanceEngineDto.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void multipleInstancesAreGeneratedFromCamundaEventImportSource_processStartEnd() {
    // given
    final ProcessInstanceEngineDto firstInstance = deployAndStartProcess();
    final String otherBusinessKey = "someOtherBusinessKey";
    engineIntegrationExtension.startProcessInstance(
      firstInstance.getDefinitionId(),
      Collections.emptyMap(),
      otherBusinessKey
    );
    publishEventMappingUsingProcessInstanceCamundaEvents(
      firstInstance,
      createMappingsForEventProcess(
        firstInstance,
        applyCamundaProcessInstanceStartEventSuffix(firstInstance.getProcessDefinitionKey()),
        applyCamundaProcessInstanceEndEventSuffix(firstInstance.getProcessDefinitionKey()),
        BPMN_END_EVENT_ID
      )
    );
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasSize(2)
      .anySatisfy(processInstance -> {
        assertProcessInstance(
          processInstance,
          firstInstance.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      })
      .anySatisfy(processInstance -> {
        assertProcessInstance(
          processInstance,
          otherBusinessKey,
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void multipleInstancesAreGeneratedFromCamundaEventImportSource() {
    // given
    final String otherBusinessKey = "someOtherBusinessKey";
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    engineIntegrationExtension.startProcessInstance(
      processInstanceEngineDto.getDefinitionId(),
      Collections.emptyMap(),
      otherBusinessKey
    );
    publishEventMappingUsingProcessInstanceCamundaEvents(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      )
    );
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasSize(2)
      .anySatisfy(processInstance -> {
        assertProcessInstance(
          processInstance,
          processInstanceEngineDto.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      })
      .anySatisfy(processInstance -> {
        assertProcessInstance(
          processInstance,
          otherBusinessKey,
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_allEvents_multipleBatches() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    publishEventMappingUsingProcessInstanceCamundaEvents(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      )
    );
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          processInstanceEngineDto.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE)
        );
      });

    // when
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> secondImportProcessInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(secondImportProcessInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          processInstanceEngineDto.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_processStartEndEvents() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    publishEventMappingUsingProcessInstanceCamundaEvents(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        applyCamundaProcessInstanceStartEventSuffix(processInstanceEngineDto.getProcessDefinitionKey()),
        USER_TASK_ID_ONE,
        applyCamundaProcessInstanceEndEventSuffix(processInstanceEngineDto.getProcessDefinitionKey())
      )
    );
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          processInstanceEngineDto.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void instancesAreGeneratedCorrectlyWithMappingsFromCamundaEventImportSourceWithMultipleSplitEvents() {
    // given
    final ProcessInstanceEngineDto firstInstance = deployAndStartTwoUserTasksProcess("processName");
    engineIntegrationExtension.finishAllRunningUserTasks(firstInstance.getId());
    publishEventMappingUsingProcessInstanceCamundaEvents(
      firstInstance,
      createMappingsForEventProcess(
        firstInstance,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        applyCamundaTaskEndEventSuffix(USER_TASK_ID_ONE),
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_TWO)
      )
    );
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          firstInstance.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_correlatedByVariable() {
    // given
    final String tracingVariable = "tracingVariable";
    final String variableValue = "someValue";
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcessWithVariables(Maps.newHashMap(
      tracingVariable,
      variableValue
    ));
    publishEventMappingUsingProcessInstanceCamundaEventsAndTraceVariable(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      ),
      tracingVariable
    );
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          variableValue,
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_correlatedByVariable_variableNotFoundAmongImportedVariables() {
    // given
    final String otherVariable = "variableForProcessInstance";
    final String otherVariableValue = "someValue";
    final String tracingVariable = "tracingVariableNotUsedIntoProcess";
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcessWithVariables(Maps.newHashMap(
      otherVariable,
      otherVariableValue
    ));
    publishEventMappingUsingProcessInstanceCamundaEventsAndTraceVariable(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      ),
      tracingVariable
    );
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances).isEmpty();
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_correlatedByVariable_noVariablesImported() {
    // given
    final String tracingVariable = "tracingVariableNotUsedIntoProcess";
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    publishEventMappingUsingProcessInstanceCamundaEventsAndTraceVariable(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      ),
      tracingVariable
    );
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances).isEmpty();
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_correlatedByBusinessKey_businessKeyNotFound() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    deleteBusinessKeyFromElasticsearchForProcessInstance(processInstanceEngineDto.getId());

    publishEventMappingUsingProcessInstanceCamundaEvents(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      )
    );

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances).isEmpty();
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_correlatedByBusinessKey_ignoreInstancesWithNullBusinessKey() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartInstanceWithBusinessKey(null);
    final ProcessInstanceEngineDto instanceWithBusinessKey = engineIntegrationExtension
      .startProcessInstance(processInstanceEngineDto.getDefinitionId());
    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    publishEventMappingUsingProcessInstanceCamundaEvents(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      )
    );

    // when
    executeImportCycle();

    // then only the instance with a business key present is saved
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          instanceWithBusinessKey.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void instancesAreGeneratedFromMultipleCamundaEventImportSources() {
    // given
    final ProcessInstanceEngineDto firstProcessInstanceEngineDto = deployAndStartProcess();
    final ProcessInstanceEngineDto secondProcessInstanceEngineDto = deployAndStartProcess();

    final Map<String, EventMappingDto> mappingsForEventProcess = createMappingsForEventProcess(
      firstProcessInstanceEngineDto,
      BPMN_START_EVENT_ID,
      applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
      BPMN_END_EVENT_ID
    );
    mappingsForEventProcess.put(
      BPMN_END_EVENT_ID,
      EventMappingDto.builder()
        .start(EventTypeDto.builder()
                 .eventName(BPMN_END_EVENT_ID)
                 .group(secondProcessInstanceEngineDto.getProcessDefinitionKey())
                 .source(EVENT_SOURCE_CAMUNDA)
                 .build()
        )
        .build()
    );

    List<EventSourceEntryDto> firstEventSource =
      createCamundaEventSourceEntryAsListForDeployedProcessTracedByBusinessKey(
        firstProcessInstanceEngineDto);
    List<EventSourceEntryDto> secondEventSource =
      createCamundaEventSourceEntryAsListForDeployedProcessTracedByBusinessKey(
        secondProcessInstanceEngineDto);

    createAndPublishEventMapping(mappingsForEventProcess, Stream.of(firstEventSource, secondEventSource).flatMap(
      Collection::stream).collect(Collectors.toList()));

    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          secondProcessInstanceEngineDto.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void instancesAreGeneratedFromExternalAndCamundaEventImportSources() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    ingestTestEvent(BPMN_END_EVENT_ID, processInstanceEngineDto.getBusinessKey());

    final Map<String, EventMappingDto> mappingsForEventProcess = createMappingsForEventProcess(
      processInstanceEngineDto,
      BPMN_START_EVENT_ID,
      applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
      BPMN_END_EVENT_ID
    );
    mappingsForEventProcess.put(
      BPMN_END_EVENT_ID,
      EventMappingDto.builder()
        .start(EventTypeDto.builder()
                 .eventName(BPMN_END_EVENT_ID)
                 .group(EXTERNAL_EVENT_GROUP)
                 .source(EXTERNAL_EVENT_SOURCE)
                 .build())
        .build()
    );

    List<EventSourceEntryDto> firstEventSource =
      createCamundaEventSourceEntryAsListForDeployedProcessTracedByBusinessKey(
        processInstanceEngineDto);
    List<EventSourceEntryDto> secondEventSource = createExternalEventSourceAsList();

    createAndPublishEventMapping(mappingsForEventProcess, Stream.of(firstEventSource, secondEventSource).flatMap(
      Collection::stream).collect(Collectors.toList()));

    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          processInstanceEngineDto.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE, BPMN_END_EVENT_ID)
        );
      });
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_ignoreEventsWithVersionNotMatching() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    final List<EventSourceEntryDto> eventSource =
      createCamundaEventSourceEntryAsListForDeployedProcessTracedByBusinessKey(
        processInstanceEngineDto,
        Collections.singletonList("versionNotSameAsInstance")
      );

    final Map<String, EventMappingDto> mappingsForEventProcess = createMappingsForEventProcess(
      processInstanceEngineDto,
      BPMN_START_EVENT_ID,
      applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
      BPMN_END_EVENT_ID
    );
    createAndPublishEventMapping(mappingsForEventProcess, eventSource);

    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances).isEmpty();
  }

  @Test
  public void instancesAreGeneratedFromCamundaEventImportSource_eventsNewerThanLastExecutionTimestampOfImportersNotIncluded() {
    // given
    final ProcessInstanceEngineDto processInstanceEngineDto = deployAndStartProcess();
    publishEventMappingUsingProcessInstanceCamundaEvents(
      processInstanceEngineDto,
      createMappingsForEventProcess(
        processInstanceEngineDto,
        BPMN_START_EVENT_ID,
        applyCamundaTaskStartEventSuffix(USER_TASK_ID_ONE),
        BPMN_END_EVENT_ID
      )
    );

    importEngineEntities();
    CamundaActivityEventDto lastImportedActivityForFirstImport =
      getLastImportedActivityForProcessDefinition(processInstanceEngineDto.getProcessDefinitionKey());

    engineIntegrationExtension.finishAllRunningUserTasks();
    importEngineEntities();

    // when
    updateImportIndexLastImportExecutionTimestamp(
      RUNNING_PROCESS_INSTANCE_IMPORT_INDEX_DOC_ID,
      lastImportedActivityForFirstImport.getTimestamp().plus(1, ChronoField.MILLI_OF_SECOND.getBaseUnit())
    );

    executeImportCycle();

    // then
    final List<EventProcessInstanceDto> processInstances = getEventProcessInstancesFromElasticsearch();
    assertThat(processInstances)
      .hasOnlyOneElementSatisfying(processInstanceDto -> {
        assertProcessInstance(
          processInstanceDto,
          processInstanceEngineDto.getBusinessKey(),
          Arrays.asList(BPMN_START_EVENT_ID, USER_TASK_ID_ONE)
        );
      });
  }

  private void updateImportIndexLastImportExecutionTimestamp(final String importType,
                                                             final OffsetDateTime timestampToSet) {
    final TimestampBasedImportIndexDto runningProcessImport = new ArrayList<>(ElasticsearchReaderUtil.mapHits(
      elasticSearchIntegrationTestExtension.getSearchResponseForAllDocumentsOfIndex(
        TIMESTAMP_BASED_IMPORT_INDEX_NAME).getHits(),
      TimestampBasedImportIndexDto.class,
      embeddedOptimizeExtension.getObjectMapper()
    )).stream().filter(index -> index.getEsTypeIndexRefersTo().equalsIgnoreCase(
      importType))
      .findFirst().get();
    runningProcessImport.setLastImportExecutionTimestamp(timestampToSet);

    elasticSearchIntegrationTestExtension.addEntryToElasticsearch(
      TIMESTAMP_BASED_IMPORT_INDEX_NAME,
      EsHelper.constructKey(
        runningProcessImport.getEsTypeIndexRefersTo(),
        runningProcessImport.getEngine()
      ),
      runningProcessImport
    );
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();
  }

  private CamundaActivityEventDto getLastImportedActivityForProcessDefinition(final String processDefinitionKey) {
    return ElasticsearchReaderUtil.mapHits(
      elasticSearchIntegrationTestExtension.getSearchResponseForAllDocumentsOfIndex(
        new CamundaActivityEventIndex(processDefinitionKey).getIndexName()
      ).getHits(),
      CamundaActivityEventDto.class,
      embeddedOptimizeExtension.getObjectMapper()
    ).stream().max(Comparator.comparing(CamundaActivityEventDto::getTimestamp)).get();
  }

  @SneakyThrows
  public void deleteBusinessKeyFromElasticsearchForProcessInstance(String processInstanceId) {
    DeleteRequest request =
      new DeleteRequest(BUSINESS_KEY_INDEX_NAME)
        .id(processInstanceId)
        .setRefreshPolicy(IMMEDIATE);
    elasticSearchIntegrationTestExtension.getOptimizeElasticClient().delete(request, RequestOptions.DEFAULT);
  }

  protected ProcessInstanceEngineDto deployAndStartTwoUserTasksProcess(String processName) {
    return engineIntegrationExtension.deployAndStartProcess(
      getDoubleUserTaskDiagram(
        processName,
        BPMN_START_EVENT_ID,
        BPMN_END_EVENT_ID,
        USER_TASK_ID_ONE,
        USER_TASK_ID_TWO
      ));
  }

}
