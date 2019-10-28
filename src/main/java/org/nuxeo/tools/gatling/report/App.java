package org.nuxeo.tools.gatling.report;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.nuxeo.tools.gatling.report.dto.SimulationReportDto;

public class App implements Runnable {

  protected static final String PROGRAM_NAME = "java -jar gatling-report.jar";

  private final static Logger LOGGER = Logger.getLogger(App.class);

  protected final Options options;

  public App(String[] args) {
    options = new Options();
    JCommander command = new JCommander(options, args);
    command.setProgramName(PROGRAM_NAME);
    if(null==options.help){System.out.println(args.toString());}
    else {
      command.usage();
      System.exit(0);
    }
  }

//  public static void main(String[] args) {new App(args);}
public static void main(String args[]) {
  (new Thread(new App(args))).start();
}

  @Override
  public void run() {
    List<SimulationContext> statistics = parseSimulationFiles();
    generateReport(statistics);
  }

  protected List<SimulationContext> parseSimulationFiles() {
    List<SimulationContext> stats = options
        .simulations
        .stream()
        .map(simulation -> parseSimulationFile(new File(simulation))).collect(
            Collectors.toList());

    return stats;
  }

  protected SimulationContext parseSimulationFile(File file) {
    LOGGER.info("Parsing " + file.getAbsolutePath());
    try {
      SimulationParser parser = ParserFactory.getParser(file, 1.5f);
      return parser.parse();
    } catch (IOException e) {
      throw new IllegalStateException("ERROR");
    }
  }

  private List<SimulationReportDto> generateSimulationReports(List<SimulationContext> statistics) {
    //TODO: is there a need for multiple scenarios?
    return statistics
        .stream()
        .flatMap(stat -> stat.reqStats.entrySet().stream().map(this::toDto))
        .collect(Collectors.toList());
  }

  protected void generateReport(List<SimulationContext> statistics) {
    List<SimulationReportDto> reports = generateSimulationReports(statistics);
    uploadReports(reports);
  }

  private void uploadReports(List<SimulationReportDto> reports) {
    String index = Utils.createESIndex(options.url);

    Settings settings = Settings.builder()
        .put("client.transport.sniff", true)
        .put("cluster.name", options.clusterName)
        .build();

    Client client = new PreBuiltTransportClient(settings).addTransportAddress(
        new TransportAddress(getHostByName(), options.port));

    reports.forEach(report -> uploadSimulation(index, client, report));
  }

  private InetAddress getHostByName() {
    try {
      return InetAddress.getByName(options.hostname);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  private void uploadSimulation(String index, Client client, SimulationReportDto report) {
    try {
      String json = new ObjectMapper().writeValueAsString(report);
      String type = "Simulation";
      IndexResponse response = client.prepareIndex(index, type)
          .setSource(json, XContentType.JSON).get();
      LOGGER.debug(response.toString());
    } catch (JsonProcessingException e) {
      LOGGER.error("Error while processing document", e);
    }
  }

  private SimulationReportDto toDto(Map.Entry<String, RequestStat> entry) {
    return new SimulationReportDto(entry.getKey(), entry.getValue().successCount, entry.getValue().errorCount,
            entry.getValue().start,entry.getValue().end,entry.getValue().duration,entry.getValue().min,
            entry.getValue().max, entry.getValue().p50, entry.getValue().p90, entry.getValue().p95, entry.getValue().p99);
  }

}
