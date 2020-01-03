/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing.user_task;

import com.google.common.collect.ImmutableSet;
import org.camunda.optimize.dto.optimize.ProcessInstanceDto;
import org.camunda.optimize.dto.optimize.UserTaskInstanceDto;
import org.camunda.optimize.dto.optimize.persistence.AssigneeOperationDto;
import org.camunda.optimize.dto.optimize.persistence.CandidateGroupOperationDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.IDENTITY_LINK_OPERATION_ADD;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.IDENTITY_LINK_OPERATION_DELETE;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_PASSWORD;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.PROCESS_INSTANCE_INDEX_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;


public class UserTaskIdentityLinkImportIT extends AbstractUserTaskImportIT {

  @Test
  public void importOfUserTaskWorkerDataCanBeDisabled() throws IOException {
    // given
    embeddedOptimizeExtension.getConfigurationService().setImportUserTaskWorkerDataEnabled(false);
    embeddedOptimizeExtension.reloadConfiguration();
    deployAndStartOneUserTaskProcess();
    String defaultCandidateGroup = "defaultCandidateGroupId";
    engineIntegrationExtension.createGroup(defaultCandidateGroup);
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(defaultCandidateGroup);
    engineIntegrationExtension.finishAllRunningUserTasks();

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto persistedProcessInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      persistedProcessInstanceDto.getUserTasks().forEach(task -> {
        assertThat(task.getAssignee(), nullValue());
        assertThat(task.getCandidateGroups().size(), is(0));
        assertThat(task.getCandidateGroupOperations().size(), is(0));
        assertThat(task.getAssigneeOperations().size(), is(0));
      });
    }
  }

  @Test
  public void identityLinksLogsAreImported() throws Exception {
    // given
    deployAndStartOneUserTaskProcess();
    String defaultCandidateGroup = "defaultCandidateGroupId";
    engineIntegrationExtension.createGroup(defaultCandidateGroup);
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(defaultCandidateGroup);
    engineIntegrationExtension.finishAllRunningUserTasks();

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto persistedProcessInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      persistedProcessInstanceDto.getUserTasks()
        .forEach(userTask -> {
          assertThat(userTask.getAssignee(), is(DEFAULT_USERNAME));
          assertThat(userTask.getCandidateGroups(), contains(defaultCandidateGroup));
          assertThat(userTask.getAssigneeOperations().size(), is(1));
          userTask.getAssigneeOperations().forEach(assigneeOperationDto -> {
            assertThat(assigneeOperationDto.getId(), is(notNullValue()));
            assertThat(assigneeOperationDto.getUserId(), is(DEFAULT_USERNAME));
            assertThat(assigneeOperationDto.getTimestamp(), is(notNullValue()));
            assertThat(assigneeOperationDto.getOperationType(), is(IDENTITY_LINK_OPERATION_ADD));
          });
          assertThat(userTask.getCandidateGroupOperations().size(), is(1));
          userTask.getCandidateGroupOperations().forEach(candidateGroupOperationDto -> {
            assertThat(candidateGroupOperationDto.getId(), is(notNullValue()));
            assertThat(candidateGroupOperationDto.getGroupId(), is(defaultCandidateGroup));
            assertThat(candidateGroupOperationDto.getTimestamp(), is(notNullValue()));
            assertThat(candidateGroupOperationDto.getOperationType(), is(IDENTITY_LINK_OPERATION_ADD));
          });
        });
    }
  }

  @Test
  public void changingAssigneeWithIdenticalLinkLogTimestampsResolvesCorrectAssignee() throws Exception {
    // background: changing the assignee in tasklist results in those two assignee operations, which will have the
    // exact same timestamp. We need to make sure that the add operation always wins over the delete if the timestamp
    // is identical.

    // given
    engineIntegrationExtension.addUser("kermit", "foo");
    engineIntegrationExtension.grantAllAuthorizations("kermit");
    ProcessInstanceEngineDto instanceDto = deployAndStartOneUserTaskProcess();
    engineIntegrationExtension.claimAllRunningUserTasks();
    engineIntegrationExtension.unclaimAllRunningUserTasks();
    engineIntegrationExtension.claimAllRunningUserTasks("kermit", "foo", instanceDto.getId());
    // we need to make sure that the new timestamp is after the first claim, since
    // otherwise the ordering of the assignee operations won't be correct.
    OffsetDateTime timestamp = OffsetDateTime.now().plusHours(1);
    engineDatabaseExtension.changeLinkLogTimestampForLastTwoAssigneeOperations(timestamp);

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    final SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto persistedProcessInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      persistedProcessInstanceDto.getUserTasks()
        .forEach(userTask -> assertThat(userTask.getAssignee(), is("kermit")));
    }
  }

  @Test
  public void assigneeIsCorrectlyDeterminedForMultipleUserTasks() throws Exception {
    // given
    deployAndStartTwoUserTasksProcess();
    engineIntegrationExtension.addUser("secondUser", "fooPassword");
    engineIntegrationExtension.grantAllAuthorizations("secondUser");
    engineIntegrationExtension.finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
    engineIntegrationExtension.finishAllRunningUserTasks("secondUser", "fooPassword");

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      List<UserTaskInstanceDto> userTasks = processInstanceDto.getUserTasks();
      assertThat(userTasks.size(), is(2));
      Set<String> expectedAssignees = ImmutableSet.of(DEFAULT_USERNAME, "secondUser");
      Set<String> actualAssignees = userTasks.stream()
        .map(UserTaskInstanceDto::getAssignee)
        .collect(Collectors.toSet());
      assertThat(actualAssignees, is(expectedAssignees));
    }
  }

  @Test
  public void candidateGroupIsCorrectlyDeterminedForMultipleUserTasks() throws Exception {
    // given
    deployAndStartTwoUserTasksProcess();
    engineIntegrationExtension.createGroup("firstGroup");
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("firstGroup");
    engineIntegrationExtension.finishAllRunningUserTasks();
    engineIntegrationExtension.createGroup("secondGroup");
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("secondGroup");
    engineIntegrationExtension.finishAllRunningUserTasks();

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      List<UserTaskInstanceDto> userTasks = processInstanceDto.getUserTasks();
      assertThat(userTasks.size(), is(2));
      Set<String> expectedCandidateGroups = ImmutableSet.of("firstGroup", "secondGroup");
      Set<String> actualCandidateGroups = userTasks.stream()
        .map(userTask -> {
          assertThat(userTask.getCandidateGroups().size(), is(1));
          return userTask.getCandidateGroups().get(0);
        })
        .collect(Collectors.toSet());
      assertThat(actualCandidateGroups, is(expectedCandidateGroups));
    }
  }

  @Test
  public void severalAssigneeOperationsLeadToCorrectResult() throws Exception {
    // given
    deployAndStartOneUserTaskProcess();
    engineIntegrationExtension.claimAllRunningUserTasks();
    engineIntegrationExtension.unclaimAllRunningUserTasks();
    engineIntegrationExtension.addUser("secondUser", "secondPassword");
    engineIntegrationExtension.grantAllAuthorizations("secondUser");
    engineIntegrationExtension.finishAllRunningUserTasks("secondUser", "secondPassword");

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      List<UserTaskInstanceDto> userTasks = processInstanceDto.getUserTasks();
      assertThat(userTasks.size(), is(1));
      List<AssigneeOperationDto> assigneeOperations = userTasks.get(0).getAssigneeOperations();
      assertThat(assigneeOperations.get(0).getOperationType(), is(IDENTITY_LINK_OPERATION_ADD));
      assertThat(assigneeOperations.get(1).getOperationType(), is(IDENTITY_LINK_OPERATION_DELETE));
      assertThat(assigneeOperations.get(2).getOperationType(), is(IDENTITY_LINK_OPERATION_ADD));
      assertThat(userTasks.get(0).getAssignee(), is("secondUser"));
    }
  }

  @Test
  public void assigneeWithoutClaimIsNull() throws Exception {
    // given
    ProcessInstanceEngineDto engineDto = deployAndStartOneUserTaskProcess();
    engineIntegrationExtension.completeUserTaskWithoutClaim(engineDto.getId());

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      List<UserTaskInstanceDto> userTasks = processInstanceDto.getUserTasks();
      assertThat(userTasks.size(), is(1));
      assertThat(userTasks.get(0).getAssignee(), nullValue());
    }
  }

  @Test
  public void assigneeCanBeDeterminedForStillRunningUserTasks() throws Exception {
    // given
    deployAndStartOneUserTaskProcess();
    engineIntegrationExtension.claimAllRunningUserTasks();

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      List<UserTaskInstanceDto> userTasks = processInstanceDto.getUserTasks();
      assertThat(userTasks.size(), is(1));
      assertThat(userTasks.get(0).getAssignee(), is(DEFAULT_USERNAME));
    }
  }

  @Test
  public void severalCandidateGroupOperationsLeadToCorrectResult() throws Exception {
    // given
    deployAndStartOneUserTaskProcess();
    engineIntegrationExtension.createGroup("firstGroup");
    engineIntegrationExtension.createGroup("secondGroup");
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("firstGroup");
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("secondGroup");
    engineIntegrationExtension.deleteCandidateGroupForAllRunningUserTasks("firstGroup");
    engineIntegrationExtension.finishAllRunningUserTasks();

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      List<UserTaskInstanceDto> userTasks = processInstanceDto.getUserTasks();
      assertThat(userTasks.size(), is(1));
      List<CandidateGroupOperationDto> candidateGroupOperations = userTasks.get(0).getCandidateGroupOperations();
      assertThat(candidateGroupOperations.get(0).getOperationType(), is(IDENTITY_LINK_OPERATION_ADD));
      assertThat(candidateGroupOperations.get(1).getOperationType(), is(IDENTITY_LINK_OPERATION_ADD));
      assertThat(candidateGroupOperations.get(2).getOperationType(), is(IDENTITY_LINK_OPERATION_DELETE));
      assertThat(userTasks.get(0).getCandidateGroups(), contains("secondGroup"));
    }
  }

  @Test
  public void deleteAssigneeAndDeleteCandidateGroupAsLastOperations() throws Exception {
    // given
    ProcessInstanceEngineDto engineDto = deployAndStartOneUserTaskProcess();
    engineIntegrationExtension.createGroup("firstGroup");
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks("firstGroup");
    engineIntegrationExtension.deleteCandidateGroupForAllRunningUserTasks("firstGroup");
    engineIntegrationExtension.claimAllRunningUserTasks();
    engineIntegrationExtension.unclaimAllRunningUserTasks();
    engineIntegrationExtension.completeUserTaskWithoutClaim(engineDto.getId());

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      List<UserTaskInstanceDto> userTasks = processInstanceDto.getUserTasks();
      assertThat(userTasks.size(), is(1));
      assertThat(userTasks.get(0).getAssignee(), nullValue());
      assertThat(userTasks.get(0).getCandidateGroups().size(), is(0));
    }
  }

  @Test
  public void importIsNotAffectedByPagination() throws Exception {
    // given
    ProcessInstanceEngineDto engineDto = deployAndStartOneUserTaskProcess();
    engineIntegrationExtension.claimAllRunningUserTasks();
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    engineIntegrationExtension.unclaimAllRunningUserTasks();
    engineIntegrationExtension.addUser("secondUser", "aPassword");
    engineIntegrationExtension.grantAllAuthorizations("secondUser");
    engineIntegrationExtension.finishAllRunningUserTasks("secondUser", "aPassword");
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      List<UserTaskInstanceDto> userTasks = processInstanceDto.getUserTasks();
      assertThat(userTasks.size(), is(1));
      assertThat(userTasks.get(0).getAssignee(), is("secondUser"));
    }
  }

  @Test
  public void onlyUserAssigneeOperationLogsRelatedToProcessInstancesAreImported() throws IOException {
    // given
    deployAndStartOneUserTaskProcess();
    engineIntegrationExtension.createIndependentUserTask();
    engineIntegrationExtension.finishAllRunningUserTasks();

    // when
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfIndex(PROCESS_INSTANCE_INDEX_NAME);
    assertThat(idsResp.getHits().getTotalHits().value, is(1L));
    for (SearchHit searchHitFields : idsResp.getHits()) {
      final ProcessInstanceDto processInstanceDto = objectMapper.readValue(
        searchHitFields.getSourceAsString(), ProcessInstanceDto.class
      );
      assertThat(processInstanceDto.getUserTasks().size(), is(1));
      processInstanceDto.getUserTasks()
        .forEach(userTask -> assertThat(userTask.getAssigneeOperations().size(), is(1)));
    }
  }


}
