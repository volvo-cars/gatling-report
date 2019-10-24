/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.tools.gatling.report;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class App implements Runnable {
    protected static final String PROGRAM_NAME = "java -jar gatling-report.jar";

    private final static Logger log = Logger.getLogger(App.class);

    protected final Options options;

    protected List<SimulationContext> stats;

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
       (new Thread(new App(args))).start();
    }

    @Override
    public void run() {
        parseSimulationFiles();
        render();
    }

    protected void parseSimulationFiles() {
        stats = new ArrayList<>(options.simulations.size());
        options.simulations.forEach(simulation -> parseSimulationFile(new File(simulation)));
    }

    protected void parseSimulationFile(File file) {
        log.info("Parsing " + file.getAbsolutePath());
        try {
            SimulationParser parser = ParserFactory.getParser(file, options.apdexT);
            stats.add(parser.parse());
        } catch (IOException e) {
            log.error("Invalid file: " + file.getAbsolutePath(), e);
        }
    }

    protected void render() {
        if (options.outputDirectory == null) {
            renderAsCsv();
        } else {
            try {
                renderAsReport();
            } catch (IOException e) {
                log.error("Can not generate report", e);
            }
        }
    }

    protected void renderAsReport() throws IOException {
        File dir = new File(options.outputDirectory);
        if (!dir.mkdirs()) {
            if (!options.force) {
                log.error("Abort, report directory already exists, use -f to override.");
                System.exit(-2);
            }
            log.warn("Overriding existing report directory" + options.outputDirectory);
        }
        String reportPath = new Report(stats).setOutputDirectory(dir)
                                             .includeJs(options.includeJs)
                                             .setTemplate(options.template)
                                             .includeGraphite(options.graphiteUrl, options.user, options.password,
                                                     options.getZoneId())
                                             .yamlReport(options.yaml)
                                             .withMap(options.map)
                                             .setFilename(options.outputName)
                                             .create();
        log.info("Report generated: " + reportPath);
    }

    protected void renderAsCsv() {
        System.out.println(RequestStat.header());
        stats.forEach(System.out::println);
        createCSV();
        bulkUploadCSVToES(true);
    }

    protected void createCSV() {
        try {
            FileWriter csvWriter = new FileWriter("new.csv");
            csvWriter.append(RequestStat.header());
            csvWriter.append("\n");
            for (SimulationContext s : stats) {
                csvWriter.append(String.join(",", s.toString()));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        }catch(Exception e) {
            System.out.println("exception");
        }
    }

    protected void bulkUploadCSVToES(boolean isHeaderIncluded) {
        TransportClient client = null;

        String index = Utils.createESIndex();
        try {
            Settings settings = Settings.builder()
                    .put("client.transport.sniff", true)
                    .put("cluster.name", "elasticsearch_paggarwa")
                    .build();
            client = new PreBuiltTransportClient(settings).addTransportAddress(
                    new TransportAddress(InetAddress.getByName("localhost"), 9300));

            BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
            File file = new File("/Users/paggarwa/Documents/support-backend/perf-test/new.csv");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line = null;
            if (bufferedReader != null && isHeaderIncluded) {
                bufferedReader.readLine();
            }
            while ((line = bufferedReader.readLine()) != null) {
                if (line.trim().length() == 0)
                    continue;
                String[] data = line.split("\t");
                for(int i=0;i<data.length;i++)
                    System.out.println(data[i]);
                XContentBuilder xContentBuilder = jsonBuilder().startObject()
                        .field("scenario",data[1])
//                            .field("maxUsers",data[2])
//                            .field("request",data[3])
                        .field("start",data[4])
//                            .field("startDate",data[5])
                        .field("duration",data[6])
                        .field("end",data[7])
//                            .field("count",data[8])
//                            .field("successCount",data[9])
//                            .field("errorCount",data[10])
//                            .field("min",data[11])
//                            .field("p50",data[12])
//                            .field("p90",data[13])
//                            .field("p95",data[14])
//                            .field("p99",data[15])
//                            .field("max",data[16])
//                            .field("avg",data[17])
//                            .field("stddev",data[18])
//                            .field("rps",data[19])
//                            .field("apdex",data[20])
                        .field("rating",data[21])
                        .endObject();

                bulkRequestBuilder.add(client.prepareIndex(index,"gatling").setSource(xContentBuilder));
            }
            bufferedReader.close();
            BulkResponse bulkResponse = bulkRequestBuilder.execute().actionGet();
            if(bulkResponse.hasFailures()) {
                System.out.println("Upload to ES failed");
                System.out.println(bulkResponse.buildFailureMessage());
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

}
