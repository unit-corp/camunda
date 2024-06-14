/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package io.camunda.optimize.rest.providers;

import io.camunda.optimize.dto.optimize.rest.ErrorResponseDto;
import io.camunda.optimize.service.LocalizationService;
import io.camunda.optimize.service.exceptions.license.OptimizeLicenseException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class LicenseExceptionMapper implements ExceptionMapper<OptimizeLicenseException> {

  private final LocalizationService localizationService;

  public LicenseExceptionMapper(@Context final LocalizationService localizationService) {
    this.localizationService = localizationService;
  }

  @Override
  public Response toResponse(OptimizeLicenseException e) {
    log.debug("Mapping OptimizeLicenseException.");
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(getErrorResponseDto(e))
        .build();
  }

  private ErrorResponseDto getErrorResponseDto(OptimizeLicenseException exception) {
    String errorCode = exception.getErrorCode();
    String errorMessage = localizationService.getDefaultLocaleMessageForApiErrorCode(errorCode);
    String detailedErrorMessage = exception.getMessage();

    return new ErrorResponseDto(errorCode, errorMessage, detailedErrorMessage);
  }
}
