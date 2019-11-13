/* (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

import java.io.*;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import static java.lang.String.*;

public class Utils {

    private final static Logger LOGGER = Logger.getLogger(Utils.class);

    protected static final String GZ = "gz";

    public static void setBasicAuth(String user, String password) {
        if (user == null) {
            return;
        }
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password.toCharArray());
            }
        });
    }

    public static String getBaseUrl(String url) {
        URL targetUrl;
        try {
            targetUrl = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }
        return targetUrl.getProtocol() + "://" + targetUrl.getHost();
    }

    public static void download(URL src, File dest) throws IOException {
        URLConnection conn = src.openConnection();
        byte[] buffer = new byte[8 * 1024];
        try (InputStream input = conn.getInputStream()) {
            try (OutputStream output = new FileOutputStream(dest)) {
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public static String getContent(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        StringBuilder ret = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null)
                ret.append(line);
        }
        return ret.toString();
    }

    public static String getIdentifier(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (Character.isLetter(str.charAt(i)))
                sb.append(str.charAt(i));
        }
        return sb.toString().toLowerCase();
    }

    public static Reader getReaderFor(File file) throws IOException {
        if (GZ.equals(getFileExtension(file))) {
            InputStream fileStream = new FileInputStream(file);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            return new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
        }
        return new FileReader(file);
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    public static String createESIndex(RestHighLevelClient client){

        String indexName = generateDailyIndexName();
        CreateIndexRequest request = new CreateIndexRequest(indexName);

        try {
            request.settings(Settings.builder()
                    .put("index.number_of_shards", 3)
                    .put("index.number_of_replicas", 0));

            request.mapping(
                    "{\n" +
                            "  \"_doc\": {\n" +
                                "  \"properties\": {\n" +
                                "    \"@timestamp\": {\n" +
                                "      \"type\": \"date\"\n" +
                                "    },\n" +
                                "    \"start\": {\n" +
                                "      \"type\": \"date\",\n" +
                                "      \"format\": \"epoch_millis\"\n" +
                                "    },\n" +
                                "    \"end\": {\n" +
                                "      \"type\": \"date\",\n" +
                                "      \"format\": \"epoch_millis\"\n" +
                                "    }\n" +
                                "  }\n" +
                            "  }\n" +
                            "}",
                    XContentType.JSON);

            CreateIndexResponse indexResponse = client
                .indices()
                .create(request, RequestOptions.DEFAULT);

            return indexResponse.index();
        } catch (ElasticsearchStatusException | IOException e) {
            if (e instanceof ElasticsearchStatusException){
                LOGGER.info("Index already exists");
                return indexName;
            } else {
                throw new RuntimeException(format("Could not create index: %s",indexName));
            }
        }

    }

    private static String generateDailyIndexName() {
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
        return "gatling-report-".concat(timeStamp);
    }

    public static void deleteESIndex(RestHighLevelClient client) {
        String indexName = generateDailyIndexName();
        try {
            DeleteIndexRequest request = new DeleteIndexRequest(indexName);
            client.indices().delete(request, RequestOptions.DEFAULT);
        } catch(ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
                LOGGER.info(String.format("Index not found, %s",indexName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
