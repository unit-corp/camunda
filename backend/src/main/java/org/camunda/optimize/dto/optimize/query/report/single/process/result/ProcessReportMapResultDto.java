package org.camunda.optimize.dto.optimize.query.report.single.process.result;

import org.camunda.optimize.dto.optimize.query.report.single.result.ResultType;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProcessReportMapResultDto extends ProcessReportResultDto {

  private Map<String, Long> result = new LinkedHashMap<>();

  public Map<String, Long> getResult() {
    return result;
  }

  public void setResult(Map<String, Long> result) {
    this.result = result;
  }

  @Override
  public ResultType getResultType() {
    return ResultType.FREQUENCY_MAP;
  }
}
