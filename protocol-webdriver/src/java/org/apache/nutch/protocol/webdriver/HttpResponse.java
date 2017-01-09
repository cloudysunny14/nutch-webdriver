/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.protocol.webdriver;

// JDK imports
import java.io.IOException;
import java.net.URL;

// HTTP Client imports
import org.apache.avro.util.Utf8;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.hadoop.conf.Configuration;
import org.apache.commons.httpclient.HttpClient;

// Nutch imports
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.metadata.SpellCheckedMetadata;
import org.apache.nutch.net.protocols.HttpDateFormat;
import org.apache.nutch.net.protocols.Response;
import org.apache.nutch.storage.WebPage;

/**
 * An HTTP response that fetches from given url.
 * 
 * @author Kiyonari Harigae
 *
 */
public class HttpResponse implements Response {

  private URL url;
  private byte[] content;
  private int code;
  private Metadata headers = new SpellCheckedMetadata();
  private Configuration conf;

  /**
   * Fetches the given <code>url</code> and prepares HTTP response. Fetch the
   * content using WebDriver to extract HTML from Ajax site, other responses are
   * fetches using HTTPClient.
   * 
   * @param http
   *          An instance of the implementation class of this plugin
   * @param url
   *          URL to be fetched
   * @param page
   *          WebPage
   * @param followRedirects
   *          Whether to follow redirects; follows redirect if and only if this
   *          is true
   * @return HTTP response
   * @throws IOException
   *           When an error occurs
   */
  HttpResponse(Http http, URL url, WebPage page, Configuration conf)
      throws IOException {

    // Prepare GET method for HTTP request
    this.url = url;
    this.conf = conf;
    GetMethod get = new GetMethod(url.toString());
    get.setFollowRedirects(false);
    get.setDoAuthentication(true);
    if (page.getModifiedTime() > 0) {
      get.setRequestHeader("If-Modified-Since",
          HttpDateFormat.toString(page.getModifiedTime()));
    }

    // Set HTTP parameters
    HttpMethodParams params = get.getParams();
    if (http.getUseHttp11()) {
      params.setVersion(HttpVersion.HTTP_1_1);
    } else {
      params.setVersion(HttpVersion.HTTP_1_0);
    }
    params.makeLenient();
    params.setContentCharset("UTF-8");

    try {
      HttpClient client = Http.getClient();
      client.getParams().setParameter("http.useragent", http.getUserAgent()); // NUTCH-1941
      code = client.executeMethod(get);

      Header[] heads = get.getResponseHeaders();

      for (int i = 0; i < heads.length; i++) {
        headers.set(heads[i].getName(), heads[i].getValue());
      }

      readPlainContent(url);

      StringBuilder fetchTrace = null;
      if (Http.LOG.isTraceEnabled()) {
        // Trace message
        fetchTrace = new StringBuilder("url: " + url + "; status code: " + code
            + "; bytes received: " + content.length);
        if (getHeader(Response.CONTENT_LENGTH) != null)
          fetchTrace.append("; Content-Length: "
              + getHeader(Response.CONTENT_LENGTH));
        if (getHeader(Response.LOCATION) != null)
          fetchTrace.append("; Location: " + getHeader(Response.LOCATION));
      }
      // add headers in metadata to row
      if (page.getHeaders() != null) {
        page.getHeaders().clear();
      }
      for (String key : headers.names()) {
        page.getHeaders().put(new Utf8(key), new Utf8(headers.get(key)));
      }

      // Logger trace message
      if (Http.LOG.isTraceEnabled()) {
        Http.LOG.trace(fetchTrace.toString());
      }
    } finally {
      get.releaseConnection();
    }
  }

  private void readPlainContent(URL url) throws IOException {
    String page = HttpWebClient.getHtmlPage(url.toString(), conf);
    content = page.getBytes("UTF-8");
  }

  /*
   * ------------------------- * <implementation:Response> *
   * -------------------------
   */

  public URL getUrl() {
    return url;
  }

  public int getCode() {
    return code;
  }

  public String getHeader(String name) {
    return headers.get(name);
  }

  public Metadata getHeaders() {
    return headers;
  }

  public byte[] getContent() {
    return content;
  }
}
