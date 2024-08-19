/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.writer;

import static io.camunda.optimize.service.db.DatabaseConstants.DASHBOARD_SHARE_INDEX_NAME;
import static io.camunda.optimize.service.db.DatabaseConstants.REPORT_SHARE_INDEX_NAME;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.optimize.dto.optimize.query.sharing.DashboardShareRestDto;
import io.camunda.optimize.dto.optimize.query.sharing.ReportShareRestDto;
import io.camunda.optimize.service.db.es.OptimizeElasticsearchClient;
import io.camunda.optimize.service.db.writer.SharingWriter;
import io.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import io.camunda.optimize.service.util.IdGenerator;
import io.camunda.optimize.service.util.configuration.condition.ElasticSearchCondition;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(ElasticSearchCondition.class)
public class SharingWriterES implements SharingWriter {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(SharingWriterES.class);
  private final OptimizeElasticsearchClient esClient;
  private final ObjectMapper objectMapper;

  public SharingWriterES(
      final OptimizeElasticsearchClient esClient, final ObjectMapper objectMapper) {
    this.esClient = esClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public ReportShareRestDto saveReportShare(final ReportShareRestDto createSharingDto) {
    log.debug("Writing new report share to Elasticsearch");
    final String id = IdGenerator.getNextId();
    createSharingDto.setId(id);
    try {
      final IndexRequest request =
          new IndexRequest(REPORT_SHARE_INDEX_NAME)
              .id(id)
              .source(objectMapper.writeValueAsString(createSharingDto), XContentType.JSON)
              .setRefreshPolicy(IMMEDIATE);

      final IndexResponse indexResponse = esClient.index(request);

      if (!indexResponse.getResult().equals(DocWriteResponse.Result.CREATED)) {
        final String message =
            "Could not write report share to Elasticsearch. "
                + "Maybe the connection to Elasticsearch got lost?";
        log.error(message);
        throw new OptimizeRuntimeException(message);
      }
    } catch (final IOException e) {
      final String errorMessage = "Could not create report share.";
      log.error(errorMessage, e);
      throw new OptimizeRuntimeException(errorMessage, e);
    }

    log.debug(
        "report share with id [{}] for resource [{}] has been created",
        id,
        createSharingDto.getReportId());
    return createSharingDto;
  }

  @Override
  public DashboardShareRestDto saveDashboardShare(final DashboardShareRestDto createSharingDto) {
    log.debug("Writing new dashboard share to Elasticsearch");
    final String id = IdGenerator.getNextId();
    createSharingDto.setId(id);
    try {
      final IndexRequest request =
          new IndexRequest(DASHBOARD_SHARE_INDEX_NAME)
              .id(id)
              .source(objectMapper.writeValueAsString(createSharingDto), XContentType.JSON)
              .setRefreshPolicy(IMMEDIATE);

      final IndexResponse indexResponse = esClient.index(request);

      if (!indexResponse.getResult().equals(DocWriteResponse.Result.CREATED)) {
        final String message =
            "Could not write dashboard share to Elasticsearch. "
                + "Maybe the connection to Elasticsearch got lost?";
        log.error(message);
        throw new OptimizeRuntimeException(message);
      }
    } catch (final IOException e) {
      final String errorMessage = "Could not create dashboard share.";
      log.error(errorMessage, e);
      throw new OptimizeRuntimeException(errorMessage, e);
    }

    log.debug(
        "dashboard share with id [{}] for resource [{}] has been created",
        id,
        createSharingDto.getDashboardId());
    return createSharingDto;
  }

  @Override
  public void updateDashboardShare(final DashboardShareRestDto updatedShare) {
    final String id = updatedShare.getId();
    try {
      final IndexRequest request =
          new IndexRequest(DASHBOARD_SHARE_INDEX_NAME)
              .id(id)
              .source(objectMapper.writeValueAsString(updatedShare), XContentType.JSON)
              .setRefreshPolicy(IMMEDIATE);

      final IndexResponse indexResponse = esClient.index(request);

      if (!indexResponse.getResult().equals(DocWriteResponse.Result.CREATED)
          && !indexResponse.getResult().equals(DocWriteResponse.Result.UPDATED)) {
        final String message = "Could not write dashboard share to Elasticsearch.";
        log.error(message);
        throw new OptimizeRuntimeException(message);
      }
    } catch (final IOException e) {
      final String errorMessage =
          String.format(
              "Was not able to update dashboard share with id [%s] for resource [%s].",
              id, updatedShare.getDashboardId());
      log.error(errorMessage, e);
      throw new OptimizeRuntimeException(errorMessage, e);
    }

    log.debug(
        "dashboard share with id [{}] for resource [{}] has been updated",
        id,
        updatedShare.getDashboardId());
  }

  @Override
  public void deleteReportShare(final String shareId) {
    log.debug("Deleting report share with id [{}]", shareId);
    final DeleteRequest request =
        new DeleteRequest(REPORT_SHARE_INDEX_NAME).id(shareId).setRefreshPolicy(IMMEDIATE);

    final DeleteResponse deleteResponse;
    try {
      deleteResponse = esClient.delete(request);
    } catch (final IOException e) {
      final String reason = String.format("Could not delete report share with id [%s].", shareId);
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }

    if (!deleteResponse.getResult().equals(DocWriteResponse.Result.DELETED)) {
      final String message =
          String.format(
              "Could not delete report share with id [%s]. Report share does not exist. "
                  + "Maybe it was already deleted by someone else?",
              shareId);
      log.error(message);
      throw new NotFoundException(message);
    }
  }

  @Override
  public void deleteDashboardShare(final String shareId) {
    log.debug("Deleting dashboard share with id [{}]", shareId);
    final DeleteRequest request =
        new DeleteRequest(DASHBOARD_SHARE_INDEX_NAME).id(shareId).setRefreshPolicy(IMMEDIATE);

    final DeleteResponse deleteResponse;
    try {
      deleteResponse = esClient.delete(request);
    } catch (final IOException e) {
      final String reason =
          String.format("Could not delete dashboard share with id [%s].", shareId);
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }

    if (!deleteResponse.getResult().equals(DocWriteResponse.Result.DELETED)) {
      final String message =
          String.format(
              "Could not delete dashboard share with id [%s]. Dashboard share does not exist. "
                  + "Maybe it was already deleted by someone else?",
              shareId);
      log.error(message);
      throw new NotFoundException(message);
    }
  }
}
