package org.camunda.optimize.service.importing.job.impl;

import org.camunda.optimize.dto.optimize.VariableDto;
import org.camunda.optimize.service.es.writer.VariableWriter;
import org.camunda.optimize.service.exceptions.OptimizeException;
import org.camunda.optimize.service.importing.job.ImportJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariableImportJob extends ImportJob<VariableDto> {

  private VariableWriter variableWriter;
  private Logger logger = LoggerFactory.getLogger(VariableImportJob.class);

  public VariableImportJob(VariableWriter variableWriter) {
    this.variableWriter = variableWriter;
  }

  @Override
  protected void getAbsentAggregateInformation() throws OptimizeException {
    // nothing to do here
  }

  @Override
  protected void executeImport() {
    try {
      variableWriter.importVariables(newOptimizeEntities);
    } catch (Exception e) {
      logger.error("error while writing variables to elasticsearch", e);
    }
  }
}
