/**
 * Copyright (C) 2009-2012 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package com.couchbase.client;

import com.couchbase.client.clustermanager.AuthType;
import com.couchbase.client.clustermanager.BucketType;
import com.couchbase.client.clustermanager.FlushResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import net.spy.memcached.compat.SpyObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * A client for the Couchbase REST API.
 */
public class ClusterManager extends SpyObject {

  private DefaultHttpClientConnection conn;
  private HttpContext context;
  private HttpHost host;
  private HttpRequestExecutor httpexecutor;
  private HttpProcessor httpproc;

  private final List<URI> addrs;
  private final String user;
  private final String pass;

  /**
   * Creates a connection to the Couchbase REST interface.
   *
   * @param uris A list of servers in the cluster.
   * @param username The cluster admin user name.
   * @param password The cluster admin password.
   */
  public ClusterManager(List<URI> uris, String username, String password) {
    addrs = uris;
    user = username;
    pass = password;

    httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
      new RequestContent(), new RequestTargetHost(),
      new RequestConnControl(), new RequestUserAgent(),
      new RequestExpectContinue()});

    httpexecutor = new HttpRequestExecutor();
    context = new BasicHttpContext(null);
    conn = new DefaultHttpClientConnection();
  }

  /**
   * Connects to a given server if a connection has not been made to at least
   * one of the servers in the server list already.
   * @param uri
   * @return
   */
  private boolean connect(URI uri) {
    host = new HttpHost(uri.getHost(), uri.getPort());
    context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
    context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
    try {
      if (!conn.isOpen()) {
        Socket socket = new Socket(host.getHostName(), host.getPort());
        conn.bind(socket, new SyncBasicHttpParams());
      }
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Creates the default bucket.
   *
   * @param type The bucket type to create.
   * @param memorySizeMB The amount of memory to allocate to this bucket.
   * @param replicas The number of replicas for this bucket.
   * @param flushEnabled If flush should be enabled on this bucket.
   */
  public void createDefaultBucket(BucketType type, int memorySizeMB,
      int replicas, boolean flushEnabled) {
    createBucket(type, "default", memorySizeMB, AuthType.NONE, replicas,
        11212, "", flushEnabled);
  }

  /**
   * Creates a named bucket with a given password for SASL authentication.
   *
   * @param type The bucket type to create.
   * @param name The name of the bucket.
   * @param memorySizeMB The amount of memory to allocate to this bucket.
   * @param replicas The number of replicas for this bucket.
   * @param authPassword The password for this bucket.
   * @param flushEnabled If flush should be enabled on this bucket.
   */
  public void createNamedBucket(BucketType type, String name,
      int memorySizeMB, int replicas, String authPassword,
      boolean flushEnabled) {
    createBucket(type, name, memorySizeMB, AuthType.SASL, replicas,
        11212, authPassword, flushEnabled);
  }

  /**
   * Creates the a sasl bucket.
   *
   * @param type The bucket type to create.
   * @param name The name of the bucket.
   * @param memorySizeMB The amount of memory to allocate to this bucket.
   * @param replicas The number of replicas for this bucket.
   * @param port The port for this bucket to listen on.
   */
  public void createPortBucket(BucketType type, String name,
      int memorySizeMB, int replicas, int port, boolean flush) {
    createBucket(type, name, memorySizeMB, AuthType.NONE, replicas,
        port, "", flush);
  }

  /**
   * Deletes a bucket.
   *
   * @param name The name of the bucket to delete.
   */
  public void deleteBucket(String name) {
    String url = "/pools/default/buckets/" + name;
    BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest("DELETE", url);

    checkError(200, sendRequest(request));
  }

  /**
   * Lists all buckets in a Couchbase cluster.
   */
  public List<String> listBuckets() {
    String url = "/pools/default/buckets/";
    BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest("GET", url);

    HttpResult result = sendRequest(request);
    checkError(200, result);

    String json = result.getBody();
    List<String> names = new LinkedList<String>();
    if (json != null && !json.equals("")) {
      try {
        JSONArray base = new JSONArray(json);
        for (int i = 0; i < base.length(); i++) {
          JSONObject bucket = (JSONObject) base.get(i);
          if (bucket.has("name")) {
            names.add(bucket.getString("name"));
          }
        }
      } catch (JSONException e) {
        getLogger().error("Unable to interpret list buckets response.");
        throw new RuntimeException(e);
      }
    }
    return names;
  }

  /**
   * Deletes all data in a bucket.
   *
   * @param name The bucket to flush.
   */
  public FlushResponse flushBucket(String name) {
    String url = "/pools/default/buckets/" + name + "/controller/doFlush";
    BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest("POST", url);

    HttpResult result = sendRequest(request);
    if(result.getErrorCode() == 200) {
      return FlushResponse.OK;
    } else if(result.getErrorCode() == 400) {
      return FlushResponse.NOT_ENABLED;
    } else {
      throw new RuntimeException("Http Error: " + result.getErrorCode()
          + " Reason: " + result.getErrorPhrase() + " Details: "
          + result.getReason());
    }

  }

  private  void createBucket(BucketType type, String name,
      int memorySizeMB, AuthType authType, int replicas, int port,
      String authpassword, boolean flushEnabled) {
    BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest("POST", "/pools/default/buckets");

    StringBuilder sb = new StringBuilder();
    sb.append("name=").append(name);
    sb.append("&ramQuotaMB=").append(memorySizeMB);
    sb.append("&authType=").append(authType.getAuthType());
    sb.append("&replicaNumber=").append(replicas);
    sb.append("&bucketType=").append(type.getBucketType());
    sb.append("&proxyPort=").append(port);
    if (authType == AuthType.SASL) {
      sb.append("&saslPassword=").append(authpassword);
    }
    if(flushEnabled) {
      sb.append("&flushEnabled=1");
    }

    try {
      request.setEntity(new StringEntity(sb.toString()));
    } catch (UnsupportedEncodingException e) {
      getLogger().error("Error creating request. Bad arguments");
      throw new RuntimeException(e);
    }

    checkError(202, sendRequest(request));
  }

  private HttpResult sendRequest(HttpRequest request) {
    HttpParams params = new SyncBasicHttpParams();
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    HttpProtocolParams.setUserAgent(params, "Couchbase Java Client/1.1");
    HttpProtocolParams.setUseExpectContinue(params, true);

    request.addHeader("Authorization", "Basic "
        + Base64.encodeBase64String((user + ":" + pass).getBytes()));
    request.addHeader("Accept", "*/*");
    request.addHeader("Content-Type", "application/x-www-form-urlencoded");

    for (int i = 0; i < addrs.size(); i++) {
      try {
        if (!connect(addrs.get(i))) {
          continue;
        }
        httpexecutor.preProcess(request, httpproc, context);
        HttpResponse response = httpexecutor.execute(request, conn, context);
        httpexecutor.postProcess(response, httpproc, context);

        int code = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity());
        String reason = parseError(body);
        String phrase = response.getStatusLine().getReasonPhrase();
        return new HttpResult(body, code, phrase, reason);
      } catch (HttpException e) {
        getLogger().debug("Error processing http request: " + e.getMessage());
        throw new RuntimeException(e);
      } catch (IOException e) {
        getLogger().debug("Unable to connect to: " + addrs.get(i)
            + ". Trying another server");
      }
    }
    throw new RuntimeException("Unable to connect to cluster");
  }

  private String parseError(String json) {
    if (json != null && !json.equals("")) {
      try {
        JSONObject base = new JSONObject(json);
        if (base.has("errors")) {
          JSONObject errors = (JSONObject) base.get("errors");
          return errors.toString();
        }
      } catch (JSONException e) {
        return "Client error parsing error response";
      }
    }
    return "No reason given";
  }

  private void checkError(int expectedCode, HttpResult result)  {
    if (result.getErrorCode() != expectedCode) {
      throw new RuntimeException("Http Error: " + result.getErrorCode()
          + " Reason: " + result.getErrorPhrase() + " Details: "
          + result.getReason());
    }
  }

  public boolean shutdown() {
    try {
      conn.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private final class HttpResult {
    private final String body;
    private final int errorCode;
    private final String errorPhrase;
    private final String errorReason;

    public HttpResult(String entity, int code, String phrase, String reason) {
      body = entity;
      errorCode = code;
      errorPhrase = phrase;
      errorReason = reason;
    }

    public String getBody() {
      return body;
    }

    public int getErrorCode() {
      return errorCode;
    }

    public String getErrorPhrase() {
      return errorPhrase;
    }

    public String getReason() {
      return errorReason;
    }
  }
}
