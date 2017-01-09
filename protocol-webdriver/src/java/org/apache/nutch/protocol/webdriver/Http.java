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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.apache.commons.httpclient.Header;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.net.protocols.Response;
import org.apache.nutch.protocol.http.api.HttpBase;
import org.apache.nutch.protocol.ProtocolException;
import org.apache.nutch.util.NutchConfiguration;
import org.apache.nutch.storage.WebPage;

import org.apache.nutch.protocol.webdriver.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a protocol plugin that HTTP client for fetch headers. It
 * creates {@link HttpResponse} object and gets the content of the url from it.
 * Also, It allows to configure the proxy settings, but require the proxy server
 * as a cascading proxy server if using Basic, Digest and NTLM authentication
 * scheme .
 * 
 * @author Kiyonari Harigae
 */
public class Http extends HttpBase {

  public static final Logger LOG = LoggerFactory.getLogger(Http.class);

  private static final Collection<WebPage.Field> FIELDS = new HashSet<WebPage.Field>();

  private static MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

  // Since the Configuration has not yet been set,
  // then an unconfigured client is returned.
  private static HttpClient client = new HttpClient(connectionManager);

  private int maxThreadsTotal = 10;

  static {
    FIELDS.add(WebPage.Field.MODIFIED_TIME);
    FIELDS.add(WebPage.Field.HEADERS);
  }

  public Http() {
    super(LOG);
  }

  @Override
  public void setConf(Configuration conf) {
    super.setConf(conf);
    this.maxThreadsTotal = conf.getInt("fetcher.threads.fetch", 10);
    configureClient();
  }

  public static void main(String[] args) throws Exception {
    Http http = new Http();
    http.setConf(NutchConfiguration.create());
    main(http, args);
  }

  @Override
  protected Response getResponse(URL url, WebPage page, boolean redirect)
      throws ProtocolException, IOException {
    return new HttpResponse(this, url, page, getConf());
  }

  @Override
  public Collection<WebPage.Field> getFields() {
    return FIELDS;
  }

  static synchronized HttpClient getClient() {
    return client;
  }

  /**
   * Configures the HTTP client
   */
  private void configureClient() {

    // Set up an HTTPS socket factory that accepts self-signed certs.
    ProtocolSocketFactory factory = new SSLProtocolSocketFactory();
    Protocol https = new Protocol("https", factory, 443);
    Protocol.registerProtocol("https", https);

    HttpConnectionManagerParams params = connectionManager.getParams();
    params.setConnectionTimeout(timeout);
    params.setSoTimeout(timeout);
    params.setSendBufferSize(BUFFER_SIZE);
    params.setReceiveBufferSize(BUFFER_SIZE);
    params.setMaxTotalConnections(maxThreadsTotal);

    // Also set max connections per host to maxThreadsTotal since all threads
    // might be used to fetch from the same host - otherwise timeout errors can
    // occur
    params.setDefaultMaxConnectionsPerHost(maxThreadsTotal);

    // executeMethod(HttpMethod) seems to ignore the connection timeout on
    // the connection manager.
    // set it explicitly on the HttpClient.
    client.getParams().setConnectionManagerTimeout(timeout);

    HostConfiguration hostConf = client.getHostConfiguration();
    if (useProxy) {
      hostConf.setProxy(proxyHost, proxyPort);
    }
    ArrayList<Header> headers = new ArrayList<Header>();
    // prefer English
    headers.add(new Header("Accept-Language", "en-us,en-gb,en;q=0.7,*;q=0.3"));
    // prefer UTF-8
    headers.add(new Header("Accept-Charset", "utf-8,ISO-8859-1;q=0.7,*;q=0.7"));
    // prefer understandable formats
    headers
        .add(new Header(
            "Accept",
            "text/html,application/xml;q=0.9,application/xhtml+xml,text/xml;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5"));
    // accept gzipped content
    // headers.add(new Header("Accept-Encoding", "x-gzip, gzip, deflate"));
    hostConf.getParams().setParameter("http.default-headers", headers);
  }

}
