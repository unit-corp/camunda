package org.camunda.optimize.service.es.report.process.processinstance.duration.groupby.none;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.optimize.query.IdDto;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.SingleReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.VariableFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.group.ProcessGroupByType;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ProcessReportNumberResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewOperation;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewProperty;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.camunda.optimize.test.it.rule.ElasticSearchIntegrationTestRule;
import org.camunda.optimize.test.it.rule.EmbeddedOptimizeRule;
import org.camunda.optimize.test.it.rule.EngineDatabaseRule;
import org.camunda.optimize.test.it.rule.EngineIntegrationRule;
import org.camunda.optimize.test.util.ProcessReportDataBuilder;
import org.camunda.optimize.test.util.ProcessReportDataType;
import org.camunda.optimize.upgrade.es.ElasticsearchConstants;
import org.elasticsearch.index.reindex.UpdateByQueryAction;
import org.elasticsearch.index.reindex.UpdateByQueryRequestBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
import static org.camunda.optimize.service.es.schema.OptimizeIndexNameHelper.getOptimizeIndexAliasForType;
import static org.camunda.optimize.test.util.ProcessVariableFilterUtilHelper.createBooleanVariableFilter;
import static org.elasticsearch.script.Script.DEFAULT_SCRIPT_LANG;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(JUnitParamsRunner.class)
public class ProcessInstanceDurationByNoneWithProcessPartReportEvaluationIT {

  private static final String END_EVENT = "endEvent";
  private static final String START_EVENT = "startEvent";
  private static final String START_LOOP = "mergeExclusiveGateway";
  private static final String END_LOOP = "splittingGateway";
  public EngineIntegrationRule engineRule = new EngineIntegrationRule();
  public ElasticSearchIntegrationTestRule elasticSearchRule = new ElasticSearchIntegrationTestRule();
  public EmbeddedOptimizeRule embeddedOptimizeRule = new EmbeddedOptimizeRule();
  public EngineDatabaseRule engineDatabaseRule = new EngineDatabaseRule();

  private static final String TEST_ACTIVITY = "testActivity";

  @Rule
  public RuleChain chain = RuleChain
    .outerRule(elasticSearchRule)
      .around(engineRule)
      .around(embeddedOptimizeRule)
      .around(engineDatabaseRule);

  @Test
  @Parameters
  public void reportEvaluationForOneProcess(ProcessReportDataType reportDataType, ProcessViewOperation operation) throws Exception {

    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    OffsetDateTime endDate = startDate.plusSeconds(1);
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseRule.changeActivityInstanceStartDateForProcessDefinition(
      processInstanceDto.getDefinitionId(),
      startDate
    );
    engineDatabaseRule.changeActivityInstanceEndDateForProcessDefinition(processInstanceDto.getDefinitionId(), endDate);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    ProcessReportDataDto resultReportDataDto = result.getData();
    assertThat(result.getProcessInstanceCount(), is(1L));
    assertThat(resultReportDataDto.getProcessDefinitionKey(), is(processInstanceDto.getProcessDefinitionKey()));
    assertThat(resultReportDataDto.getProcessDefinitionVersion(), is(processInstanceDto.getProcessDefinitionVersion()));
    assertThat(resultReportDataDto.getView(), is(notNullValue()));
    assertThat(resultReportDataDto.getView().getOperation(), is(operation));
    assertThat(resultReportDataDto.getView().getEntity(), is(ProcessViewEntity.PROCESS_INSTANCE));
    assertThat(resultReportDataDto.getView().getProperty(), is(ProcessViewProperty.DURATION));
    assertThat(resultReportDataDto.getGroupBy().getType(), is(ProcessGroupByType.NONE));
    assertThat(resultReportDataDto.getParameters().getProcessPart(), is(notNullValue()));
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(1000L));
  }

  private Object[] parametersForReportEvaluationForOneProcess() {
    return new Object[]{
      new Object[]{ProcessReportDataType.AVG_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, ProcessViewOperation.AVG},
      new Object[]{ProcessReportDataType.MIN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, ProcessViewOperation.MIN},
      new Object[]{ProcessReportDataType.MAX_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, ProcessViewOperation.MAX},
      new Object[]{ProcessReportDataType.MEDIAN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, ProcessViewOperation.MEDIAN}
    };
  }

  @Test
  @Parameters
  public void reportEvaluationById(ProcessReportDataType reportDataType, ProcessViewOperation operation) throws Exception {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    OffsetDateTime endDate = startDate.plusSeconds(1);
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseRule.changeActivityInstanceStartDateForProcessDefinition(
      processInstanceDto.getDefinitionId(),
      startDate
    );
    engineDatabaseRule.changeActivityInstanceEndDateForProcessDefinition(processInstanceDto.getDefinitionId(), endDate);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();
    ProcessReportDataDto reportDataDto = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    String reportId = createAndStoreDefaultReportDefinition(reportDataDto);

    // when
    ProcessReportNumberResultDto result = evaluateReportById(reportId);

    // then
    ProcessReportDataDto resultReportDataDto = result.getData();
    assertThat(resultReportDataDto.getProcessDefinitionKey(), is(processInstanceDto.getProcessDefinitionKey()));
    assertThat(resultReportDataDto.getProcessDefinitionVersion(), is(processInstanceDto.getProcessDefinitionVersion()));

    assertThat(resultReportDataDto.getView(), is(notNullValue()));
    assertThat(resultReportDataDto.getView().getOperation(), is(operation));
    assertThat(resultReportDataDto.getView().getEntity(), is(ProcessViewEntity.PROCESS_INSTANCE));
    assertThat(resultReportDataDto.getView().getProperty(), is(ProcessViewProperty.DURATION));
    assertThat(resultReportDataDto.getGroupBy().getType(), is(ProcessGroupByType.NONE));
    assertThat(resultReportDataDto.getParameters().getProcessPart(), is(notNullValue()));
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(1000L));
  }

  private Object[] parametersForReportEvaluationById() {
    return new Object[]{
      new Object[]{ProcessReportDataType.AVG_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, ProcessViewOperation.AVG},
      new Object[]{ProcessReportDataType.MIN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, ProcessViewOperation.MIN},
      new Object[]{ProcessReportDataType.MAX_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, ProcessViewOperation.MAX},
      new Object[]{ProcessReportDataType.MEDIAN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, ProcessViewOperation.MEDIAN}
    };
  }

  @Test
  @Parameters
  public void evaluateReportForMultipleEvents(ProcessReportDataType reportDataType, Long expectedDuration) throws Exception {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    ProcessInstanceEngineDto processInstanceDto2 =
      engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    ProcessInstanceEngineDto processInstanceDto3 =
      engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    Map<String, OffsetDateTime> startDatesToUpdate = new HashMap<>();
    startDatesToUpdate.put(processInstanceDto.getId(), startDate);
    startDatesToUpdate.put(processInstanceDto2.getId(), startDate);
    startDatesToUpdate.put(processInstanceDto3.getId(), startDate);
    engineDatabaseRule.updateActivityInstanceStartDates(startDatesToUpdate);
    Map<String, OffsetDateTime> endDatesToUpdate = new HashMap<>();
    endDatesToUpdate.put(processInstanceDto.getId(), startDate.plusSeconds(1));
    endDatesToUpdate.put(processInstanceDto2.getId(), startDate.plusSeconds(2));
    endDatesToUpdate.put(processInstanceDto3.getId(), startDate.plusSeconds(9));
    engineDatabaseRule.updateActivityInstanceEndDates(endDatesToUpdate);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();
    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(expectedDuration));
  }

  private Object[] parametersForEvaluateReportForMultipleEvents() {
    return new Object[]{
      new Object[]{ProcessReportDataType.AVG_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 4000L},
      new Object[]{ProcessReportDataType.MIN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 1000L},
      new Object[]{ProcessReportDataType.MAX_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 9000L},
      new Object[]{ProcessReportDataType.MEDIAN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 2000L}
    };
  }

  @Test
  @Parameters(source = ReportDataBuilderProvider.class)
  public void takeCorrectActivityOccurrences(ProcessReportDataType reportDataType) throws Exception {
    // given
    OffsetDateTime startDate = OffsetDateTime.now().minusHours(1);
    ProcessInstanceEngineDto processInstanceDto = deployAndStartLoopingProcess();
    engineDatabaseRule.changeFirstActivityInstanceStartDate(START_LOOP, startDate);
    engineDatabaseRule.changeFirstActivityInstanceEndDate(END_LOOP, startDate.plusSeconds(2));
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId(START_LOOP)
            .setEndFlowNodeId(END_LOOP)
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(2000L));
  }

  /**
   * When migrating from Optimize 2.1 to 2.2 then all the activity instances
   * that were imported in 2.1 don't have a start and end date. This test
   * ensures that Optimize can cope with that.
   */
  @Test
  @Parameters(source = ReportDataBuilderProvider.class)
  public void activityHasNullDates(ProcessReportDataType reportDataType) {
    // given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();
    setActivityStartDatesToNull();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(0L));
  }

  private void setActivityStartDatesToNull() {
    UpdateByQueryRequestBuilder updateByQuery =
      UpdateByQueryAction.INSTANCE.newRequestBuilder(elasticSearchRule.getClient());
    ConfigurationService configurationService = embeddedOptimizeRule.getConfigurationService();
    String processInstanceIndex = getOptimizeIndexAliasForType(ElasticsearchConstants.PROC_INSTANCE_TYPE);
    Script setActivityStartDatesToNull = new Script(
      ScriptType.INLINE,
      DEFAULT_SCRIPT_LANG,
      "for (event in ctx._source.events) { event.startDate = null }",
      Collections.emptyMap()
    );
    updateByQuery.source(processInstanceIndex)
    .abortOnVersionConflict(false)
    .script(setActivityStartDatesToNull);
    updateByQuery.refresh(true).get();
  }

  @Test
  @Parameters(source = ReportDataBuilderProvider.class)
  public void firstOccurrenceOfEndDateIsBeforeFirstOccurrenceOfStartDate(ProcessReportDataType reportDataType) throws
                                                                                                              Exception {
    // given
    OffsetDateTime startDate = OffsetDateTime.now().minusHours(1);
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseRule.changeFirstActivityInstanceStartDate(START_EVENT, startDate);
    engineDatabaseRule.changeFirstActivityInstanceEndDate(END_EVENT, startDate.minusSeconds(2));
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(0L));
  }

  @Test
  @Parameters(source = ReportDataBuilderProvider.class)
  public void unknownStartReturnsZero(ProcessReportDataType reportDataType) throws SQLException {
    // given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseRule.changeActivityInstanceEndDateForProcessDefinition(
      processInstanceDto.getDefinitionId(),
      OffsetDateTime.now().plusHours(1)
    );
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId("FOoO")
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(0L));
  }

  @Test
  @Parameters(source = ReportDataBuilderProvider.class)
  public void unknownEndReturnsZero(ProcessReportDataType reportDataType) throws SQLException {
    // given
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseRule.changeActivityInstanceStartDateForProcessDefinition(
      processInstanceDto.getDefinitionId(),
      OffsetDateTime.now().minusHours(1)
    );
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId("FOO")
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(0L));
  }

  @Test
  @Parameters(source = ReportDataBuilderProvider.class)
  public void noAvailableProcessInstancesReturnsZero(ProcessReportDataType reportDataType) {
    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey("FOOPROCDEF")
            .setProcessDefinitionVersion("1")
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(0L));
  }

  @Test
  @Parameters
  public void reportAcrossAllVersions(ProcessReportDataType reportDataType, Long expectedDuration) throws Exception {
    //given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();

    engineDatabaseRule.changeActivityInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeActivityInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(1));
    processInstanceDto = engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    engineDatabaseRule.changeActivityInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeActivityInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(9));
    processInstanceDto = deployAndStartSimpleServiceTaskProcess();
    engineDatabaseRule.changeActivityInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeActivityInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(2));
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(ALL_VERSIONS)
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(expectedDuration));
  }

  private Object[] parametersForReportAcrossAllVersions() {
    return new Object[]{
      new Object[]{ProcessReportDataType.AVG_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 4000L},
      new Object[]{ProcessReportDataType.MIN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 1000L},
      new Object[]{ProcessReportDataType.MAX_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 9000L},
      new Object[]{ProcessReportDataType.MEDIAN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 2000L}
    };
  }

  @Test
  @Parameters
  public void otherProcessDefinitionsDoNoAffectResult(ProcessReportDataType reportDataType, Long expectedDuration) throws Exception {
    // given
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcess();

    String processDefinitionKey = processInstanceDto.getProcessDefinitionKey();
    String processDefinitionVersion = processInstanceDto.getProcessDefinitionVersion();

    engineDatabaseRule.changeActivityInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeActivityInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(1));
    processInstanceDto = engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    engineDatabaseRule.changeActivityInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeActivityInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(9));
    processInstanceDto = engineRule.startProcessInstance(processInstanceDto.getDefinitionId());
    engineDatabaseRule.changeActivityInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeActivityInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(2));
    deployAndStartSimpleServiceTaskProcess();
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processDefinitionKey)
            .setProcessDefinitionVersion(processDefinitionVersion)
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(notNullValue()));
    assertThat(result.getResult(), is(expectedDuration));
  }

  private Object[] parametersForOtherProcessDefinitionsDoNoAffectResult() {
    return new Object[]{
      new Object[]{ProcessReportDataType.AVG_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 4000L},
      new Object[]{ProcessReportDataType.MIN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 1000L},
      new Object[]{ProcessReportDataType.MAX_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 9000L},
      new Object[]{ProcessReportDataType.MEDIAN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART, 2000L}
    };
  }

  @Test
  @Parameters(source = ReportDataBuilderProvider.class)
  public void filterInReportWorks(ProcessReportDataType reportDataType) throws Exception {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("var", true);
    OffsetDateTime startDate = OffsetDateTime.now();
    ProcessInstanceEngineDto processInstanceDto = deployAndStartSimpleServiceTaskProcessWithVariables(variables);
    engineDatabaseRule.changeActivityInstanceStartDate(processInstanceDto.getId(), startDate);
    engineDatabaseRule.changeActivityInstanceEndDate(processInstanceDto.getId(), startDate.plusSeconds(1));
    String processDefinitionId = processInstanceDto.getDefinitionId();
    engineRule.startProcessInstance(processDefinitionId);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshOptimizeIndexInElasticsearch();

    // when
    ProcessReportDataDto reportData = ProcessReportDataBuilder
            .createReportData()
            .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
            .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
            .setStartFlowNodeId(START_EVENT)
            .setEndFlowNodeId(END_EVENT)
            .setReportDataType(reportDataType)
            .build();

    reportData.setFilter(createVariableFilter("true"));
    ProcessReportNumberResultDto result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(1000L));

    // when
    reportData.setFilter(createVariableFilter("false"));
    result = evaluateReport(reportData);

    // then
    assertThat(result.getResult(), is(0L));
  }

  private List<ProcessFilterDto> createVariableFilter(String value) {
    VariableFilterDto variableFilterDto = createBooleanVariableFilter("var", value);
    return Collections.singletonList(variableFilterDto);
  }


  private ProcessInstanceEngineDto deployAndStartSimpleServiceTaskProcess() {
    BpmnModelInstance processModel = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent(START_EVENT)
      .serviceTask()
        .camundaExpression("${true}")
      .endEvent(END_EVENT)
      .done();
    return engineRule.deployAndStartProcess(processModel);
  }

  private ProcessInstanceEngineDto deployAndStartLoopingProcess() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess()
    .startEvent("startEvent")
    .exclusiveGateway(START_LOOP)
      .serviceTask()
        .camundaExpression("${true}")
      .exclusiveGateway(END_LOOP)
        .condition("Take another round", "${!anotherRound}")
      .endEvent("endEvent")
    .moveToLastGateway()
      .condition("End process", "${anotherRound}")
      .serviceTask("serviceTask")
        .camundaExpression("${true}")
        .camundaInputParameter("anotherRound", "${anotherRound}")
        .camundaOutputParameter("anotherRound", "${!anotherRound}")
      .scriptTask("scriptTask")
        .scriptFormat("groovy")
        .scriptText("sleep(10)")
      .connectTo("mergeExclusiveGateway")
    .done();
    Map<String, Object> variables = new HashMap<>();
    variables.put("anotherRound", true);
    return engineRule.deployAndStartProcessWithVariables(modelInstance, variables);
  }

  private ProcessInstanceEngineDto deployAndStartSimpleServiceTaskProcessWithVariables(Map<String, Object> variables) {
    BpmnModelInstance processModel = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent(START_EVENT)
      .serviceTask(TEST_ACTIVITY)
        .camundaExpression("${true}")
      .endEvent(END_EVENT)
      .done();
    return engineRule.deployAndStartProcessWithVariables(processModel, variables);
  }

  private ProcessReportNumberResultDto evaluateReport(ProcessReportDataDto reportData) {
    Response response = evaluateReportAndReturnResponse(reportData);
    assertThat(response.getStatus(), is(200));

    return response.readEntity(ProcessReportNumberResultDto.class);
  }

  private Response evaluateReportAndReturnResponse(ProcessReportDataDto reportData) {
    return embeddedOptimizeRule
            .getRequestExecutor()
            .buildEvaluateSingleUnsavedReportRequest(reportData)
            .execute();
  }

  private String createAndStoreDefaultReportDefinition(ProcessReportDataDto reportData) {
    String id = createNewReport();

    SingleReportDefinitionDto<ProcessReportDataDto> report = new SingleReportDefinitionDto<>();
    report.setData(reportData);
    report.setId(id);
    report.setLastModifier("something");
    report.setName("something");
    report.setCreated(OffsetDateTime.now());
    report.setLastModified(OffsetDateTime.now());
    report.setOwner("something");
    updateReport(id, report);
    return id;
  }

  private void updateReport(String id, ReportDefinitionDto updatedReport) {
    Response response = embeddedOptimizeRule
            .getRequestExecutor()
            .buildUpdateReportRequest(id, updatedReport)
            .execute();

    assertThat(response.getStatus(), is(204));
  }

  private String createNewReport() {
    return embeddedOptimizeRule
            .getRequestExecutor()
            .buildCreateSingleProcessReportRequest()
            .execute(IdDto.class, 200)
            .getId();
  }

  private ProcessReportNumberResultDto evaluateReportById(String reportId) {
    return embeddedOptimizeRule
            .getRequestExecutor()
            .buildEvaluateSavedReportRequest(reportId)
            .execute(ProcessReportNumberResultDto.class, 200);
  }

  public static class ReportDataBuilderProvider {
    public static Object[] provideReportDataCreator() {
      return new Object[]{
        new Object[]{ProcessReportDataType.AVG_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART},
        new Object[]{ProcessReportDataType.MIN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART},
        new Object[]{ProcessReportDataType.MAX_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART},
        new Object[]{ProcessReportDataType.MEDIAN_PROC_INST_DUR_GROUP_BY_NONE_WITH_PART}
      };
    }
  }

}
