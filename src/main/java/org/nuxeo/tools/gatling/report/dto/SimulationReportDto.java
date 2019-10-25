package org.nuxeo.tools.gatling.report.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SimulationReportDto {

  private final String scenario;
  private final Long successCount;
  private final Long errorCount;

  @JsonCreator
  public SimulationReportDto(String scenario, Long successCount, Long errorCount) {
    this.scenario = scenario;
    this.successCount = successCount;
    this.errorCount = errorCount;
  }

  public String getScenario() {
    return scenario;
  }

  public Long getSuccessCount() {
    return successCount;
  }

  public Long getErrorCount() {
    return errorCount;
  }

}
