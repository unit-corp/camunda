/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.rest;

import lombok.AllArgsConstructor;
import org.camunda.optimize.dto.optimize.query.IdResponseDto;
import org.camunda.optimize.dto.optimize.rest.export.SingleProcessReportDefinitionExportDto;
import org.camunda.optimize.rest.providers.Secured;
import org.camunda.optimize.service.entities.EntityImportService;
import org.camunda.optimize.service.security.SessionService;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import static org.camunda.optimize.rest.queryparam.QueryParamUtil.normalizeNullStringValue;

@AllArgsConstructor
@Path("/import")
@Secured
@Component
public class ImportRestService {

  private final SessionService sessionService;
  private final EntityImportService entityImportService;

  @POST
  @Path("report/process")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public IdResponseDto importReport(@Context final ContainerRequestContext requestContext,
                                    @QueryParam("collectionId") String collectionId,
                                    @Valid final SingleProcessReportDefinitionExportDto exportedDto) {
    final String userId = sessionService.getRequestUserOrFailNotAuthorized(requestContext);
    collectionId = normalizeNullStringValue(collectionId);
    return entityImportService.importProcessReportIntoCollection(
      userId,
      collectionId,
      exportedDto
    );
  }
}
