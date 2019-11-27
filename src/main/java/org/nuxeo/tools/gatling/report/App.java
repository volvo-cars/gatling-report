package org.nuxeo.tools.gatling.report;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.nuxeo.tools.gatling.report.dto.SimulationReportDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.*;

public class App {

  protected static final String PROGRAM_NAME = "java -jar gatling-report.jar";

  private final static Logger LOGGER = Logger.getLogger(App.class);

  protected static Options options = null;

  public App(String[] args) {
    options = new Options();
    JCommander command = new JCommander(options, args);
    command.setProgramName(PROGRAM_NAME);
    if (options.help) {
      command.usage();
      System.exit(0);
    }
  }

  public static void main(String args[]) {
    new App(args);
    parseSimulationFiles().forEach(App::generateReport);
    System.exit(0);
  }


  protected static List<SimulationContext> parseSimulationFiles() {
    Map<String, List<File>> filesPerPath = getSimulationPath()
        .collect(groupingBy(App::getPathRoot, toList()));

    List<SimulationContext> stats = filesPerPath.values()
        .stream()
        .map(l -> l.stream().max(Comparator.comparingLong(File::lastModified)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(App::parseSimulationFile)
        .collect(toList());

    if (stats.isEmpty()) {
      throw new IllegalArgumentException(format("No simulation files found from path %s", options.simulationsPath));
    } else {
      return stats;
    }

  }

  private static String getPathRoot(File file) {
    String absolutePath = file.getAbsolutePath();
    return absolutePath.substring(0, absolutePath.lastIndexOf('-'));
  }

  private static Stream<File> getSimulationPath() {
    try {
      Path path = Paths.get(options.simulationsPath);
      return Files.walk(path)
          .filter(Files::isRegularFile)
          .map(Path::toFile)
          .filter(f -> f.getName().endsWith("log"));

    } catch (IOException e) {
      throw new IllegalArgumentException(format("No simulation files found from path %s", options.simulationsPath));
    }
  }

  protected static SimulationContext parseSimulationFile(File file) {
    LOGGER.info("Parsing " + file.getAbsolutePath());
    try {
      SimulationParser parser = ParserFactory.getParser(file, 1.5f);
      return parser.parse();
    } catch (IOException e) {
      throw new IllegalStateException("ERROR", e);
    }
  }

  private static List<SimulationReportDto> generateSimulationReports(SimulationContext statistics) {
    return statistics
        .reqStats
        .entrySet()
        .stream()
        .map(App::toDto).collect(toList());
  }

  protected static void generateReport(SimulationContext statistics) {
    List<SimulationReportDto> reports = generateSimulationReports(statistics);
    uploadReports(reports);
  }

  private static void uploadReports(List<SimulationReportDto> reports) {
    RestHighLevelClient client = createHttpsRestClient(options.url, options.port);

    if (options.deleteIndex)
      Utils.deleteESIndex(client);

    String index = Utils.createESIndex(client);

    reports.forEach(report -> uploadSimulation(index, client, report));
    LOGGER.info("Upload completed");
  }

  private static RestHighLevelClient createHttpsRestClient(String url, Integer port) {
    HttpHost httpsHost = new HttpHost(url, port, "https");
    RestClientBuilder lowLevelClientBuilder = RestClient
        .builder(httpsHost)
        .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setSSLHostnameVerifier((hostname, session) -> true));

    return new RestHighLevelClient(lowLevelClientBuilder);
  }

  private static void uploadSimulation(String index, RestHighLevelClient client, SimulationReportDto report) {

    try {
      String json = new ObjectMapper().writeValueAsString(report);
      IndexRequest request = new IndexRequest(index);
      request.id(report.getId());
      request.source(json, XContentType.JSON);

      IndexResponse response = client.index(request, RequestOptions.DEFAULT);
      LOGGER.info(response.toString());
    } catch (IOException e) {
      LOGGER.error("Error while processing document", e);
    }
  }

  private static SimulationReportDto toDto(Map.Entry<String, RequestStat> entry) {
    return new SimulationReportDto(entry.getKey(), entry.getValue().successCount, entry.getValue().errorCount,
        entry.getValue().start, entry.getValue().end, entry.getValue().duration, entry.getValue().min,
        entry.getValue().max, entry.getValue().p50, entry.getValue().p90, entry.getValue().p95, entry.getValue().p99,
            entry.getValue().apdex.getRating()
    );
  }

}
