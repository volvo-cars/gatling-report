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

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.settings.Settings;

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

public class Utils {

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

    public static String createESIndex(String url){
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(url,9200)));

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        String indexName = "gatlingreport".concat(timeStamp);
        CreateIndexResponse indexResponse = null;
        CreateIndexRequest request = new CreateIndexRequest(indexName);

        try {
            request.settings(Settings.builder()
                    .put("index.number_of_shards", 3)
                    .put("index.number_of_replicas", 0));

            indexResponse = client.indices().create(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("response id: "+indexResponse.index());
        return indexResponse.index();
    }
}
