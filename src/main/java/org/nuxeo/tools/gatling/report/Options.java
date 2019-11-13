package org.nuxeo.tools.gatling.report;

import com.beust.jcommander.Parameter;

public class Options {

    @Parameter(names = { "--folder", "-f" }, description = "path to the SIMULATION.LOG...")
    public String simulationsPath;

    @Parameter(names = { "--cluster_name", "-c" }, description = "Elastic search cluster name")
    public String clusterName;

    @Parameter(names = { "--hostname", "-h" }, description = "Elastic search hostname")
    public String hostname;

    @Parameter(names = { "--port", "-p" }, description = "Elastic search port.")
    public Integer port;

    @Parameter(names = { "--url", "-u" }, description = "Elastic search url.")
    public String url;

    @Parameter(names = { "--help"}, description = "Display this message.")
    public boolean help = false;

    @Parameter(names = { "-d", "--delete_index" }, description = "Delete the daily index before creating one.")
    public boolean deleteIndex = false;
}
