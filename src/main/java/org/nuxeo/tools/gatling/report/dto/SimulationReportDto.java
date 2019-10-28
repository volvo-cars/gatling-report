package org.nuxeo.tools.gatling.report.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SimulationReportDto {

  private final String scenario;
  private final Long successCount;
  private final Long errorCount;
  private final Long start;
  private final Long end;
  private final double duration;
  private final Long min, max, p50, p90, p95, p99;

  @JsonCreator
  public SimulationReportDto(String scenario, Long successCount, Long errorCount, Long start, Long end,
                             double duration, Long min, Long max, Long p50, Long p90, Long p95, Long p99)
  {
    this.scenario = scenario;
    this.successCount = successCount;
    this.errorCount = errorCount;
    this.start = start;
    this.end = end;
    this.duration = duration;
    this.min = min;
    this.max = max;
    this.p50 = p50;
    this.p90 = p90;
    this.p95 = p95;
    this.p99 = p99;
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

  public Long getStart() {
    return start;
  }

  public Long getEnd() {
    return end;
  }

  public double getDuration() {
    return duration;
  }

  public Long getMin() {
    return min;
  }

  public Long getMax() {
    return max;
  }

  public Long getP50() {
    return p50;
  }

  public Long getP90() {
    return p90;
  }

  public Long getP95() {
    return p95;
  }

  public Long getP99() {
    return p99;
  }
}
