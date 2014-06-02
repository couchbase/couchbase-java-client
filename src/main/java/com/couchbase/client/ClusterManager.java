/**
 * Copyright (C) 2009-2013 Couchbase, Inc.
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
import net.spy.memcached.compat.SpyObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A client to perform cluster-wide operations over the HTTP REST API.
 */
public class ClusterManager extends SpyObject {

  /**
   * The default connection timeout in milliseconds.
   */
  public static final int DEFAULT_CONN_TIMEOUT =
    (int) TimeUnit.MINUTES.toMillis(2);

  /**
   * The default socket timeout in milliseconds.
   */
  public static final int DEFAULT_SOCKET_TIMEOUT =
    (int) TimeUnit.MINUTES.toMillis(2);

  /**
   * By default, enable tcp nodelay.
   */
  public static final boolean DEFAULT_TCP_NODELAY = true;

  /**
   * The default number of IO (worker) threads to use.
   */
  public static final int DEFAULT_IO_THREADS = 1;

  /**
   *  The default number fo max. connections per node to keep open.
   */
  public static final int DEFAULT_CONNS_PER_NODE = 5;

  /**
   * The relative path to the buckets
   */
  private static final String BUCKETS = "/pools/default/buckets/";

  /**
   * The list of cluster nodes to communicate with.
   */
  private final List<HttpHost> clusterNodes;

  /**
   * The asynchronous IO reactor which executes the requests.
   */
  private final ConnectingIOReactor ioReactor;

  /**
   * The connection pool manager for multiplexing connections.
   */
  private final BasicNIOConnPool pool;

  /**
   * A requester that helps with asynchronous request/response flow.
   */
  private final HttpAsyncRequester requester;

  /**
   * The REST API (admin) username.
   */
  private final String username;

  /**
   * The REST API (admin) password.
   */
  private final String password;

  /**
   * The thread where the {@link #ioReactor} executes in.
   */
  private volatile Thread reactorThread;

  /**
   * If the connection is running or shut down.
   */
  private volatile boolean running;

  /**
   * Create a new {@link ClusterManager} instance.
   *
   * Not all nodes in the cluster need to be provided, a subset is enough so
   * that the {@link ClusterManager} can connect to at least one of them, even
   * in the case of a node failure.
   *
   * @param nodes the list of nodes in the cluster to connect to.
   * @param username the admin username.
   * @param password the admin password.
   */
  public ClusterManager(final List<URI> nodes, final String username,
    final String password) {
    this(nodes, username, password, DEFAULT_CONN_TIMEOUT,
      DEFAULT_SOCKET_TIMEOUT, DEFAULT_TCP_NODELAY, DEFAULT_IO_THREADS,
      DEFAULT_CONNS_PER_NODE);
  }

  /**
   * Create a new {@link ClusterManager} instance.
   *
   * Not all nodes in the cluster need to be provided, a subset is enough so
   * that the {@link ClusterManager} can connect to at least one of them, even
   * in the case of a node failure.
   *
   * @param nodes the list of nodes in the cluster to connect to.
   * @param username the admin username.
   * @param password the admin password.
   * @param connectionTimeout the timeout of the connection once established.
   * @param socketTimeout the socket timeout of the connection.
   * @param tcpNoDelay if nagle should be used or not.
   * @param ioThreadCount the number of IO threads to use.
   * @param connectionsPerNode the number of connections per node to establish.
   */
  public ClusterManager(List<URI> nodes, String username, String password,
    int connectionTimeout, int socketTimeout, boolean tcpNoDelay,
    int ioThreadCount, int connectionsPerNode) {
    if (nodes == null || nodes.isEmpty()) {
      throw new IllegalArgumentException("List of nodes is null or empty");
    }
    if (username == null || username.isEmpty()) {
      throw new IllegalArgumentException("Username is null or empty");
    }
    if (password == null) {
      throw new IllegalArgumentException("Password is null");
    }

    this.username = username;
    this.password = password;

    clusterNodes = Collections.synchronizedList(new ArrayList<HttpHost>());
    for (URI node : nodes) {
      clusterNodes.add(new HttpHost(node.getHost(), node.getPort()));
    }

    HttpProcessor httpProc = HttpProcessorBuilder.create()
      .add(new RequestContent())
      .add(new RequestTargetHost())
      .add(new RequestConnControl())
      .add(new RequestUserAgent("JCBC/1.2"))
      .add(new RequestExpectContinue(true)).build();

    requester = new HttpAsyncRequester(httpProc);

    try {
      ioReactor = new DefaultConnectingIOReactor(IOReactorConfig.custom()
        .setConnectTimeout(connectionTimeout)
        .setSoTimeout(socketTimeout)
        .setTcpNoDelay(tcpNoDelay)
        .setIoThreadCount(ioThreadCount)
        .build());
    } catch (IOReactorException ex) {
      throw new IllegalStateException("Could not create IO reactor");
    }

    pool = new BasicNIOConnPool(ioReactor, ConnectionConfig.DEFAULT);
    pool.setDefaultMaxPerRoute(connectionsPerNode);
    initializeReactorThread();
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
    createBucket(type, "default", memorySizeMB, AuthType.NONE, replicas, 11212,
      "", flushEnabled);
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
    int memorySizeMB, int replicas, String authPassword, boolean flushEnabled) {
    createBucket(type, name, memorySizeMB, AuthType.SASL, replicas, 11212,
      authPassword, flushEnabled);
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
  public void createPortBucket(BucketType type, String name, int memorySizeMB,
    int replicas, int port, boolean flush) {
    createBucket(type, name, memorySizeMB, AuthType.NONE, replicas, port, "",
      flush);
  }

  /**
   * Deletes a bucket.
   *
   * @param name The name of the bucket to delete.
   */
  public void deleteBucket(final String name) {
    BasicHttpRequest request = new BasicHttpRequest("DELETE", BUCKETS + name);
    checkForErrorCode(200, sendRequest(request));
  }

  /**
   * Lists all buckets in a Couchbase cluster.
   */
  public List<String> listBuckets() {
    BasicHttpRequest request = new BasicHttpRequest("GET", BUCKETS);
    HttpResult result = sendRequest(request);
    checkForErrorCode(200, result);

    String json = result.getBody();
    List<String> names = new ArrayList<String>();
    if (json != null && !json.isEmpty()) {
      try {
        JSONArray base = new JSONArray(json);
        for (int i = 0; i < base.length(); i++) {
          JSONObject bucket = base.getJSONObject(i);
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
  public FlushResponse flushBucket(final String name) {
    String url = BUCKETS + name + "/controller/doFlush";
    BasicHttpRequest request = new BasicHttpRequest("POST", url);
    HttpResult result = sendRequest(request);

    if (result.getErrorCode() == 200) {
      return FlushResponse.OK;
    } else if (result.getErrorCode() == 400) {
      return FlushResponse.NOT_ENABLED;
    } else {
      throw new RuntimeException("Http Error: " + result.getErrorCode()
        + " Reason: " + result.getErrorPhrase() + " Details: "
        + result.getReason());
    }

  }

  /**
   * Update a bucket with the new settings.
   *
   * @param name The name of the bucket.
   * @param memorySizeMB The amount of memory to allocate to this bucket.
   * @param authType the authentication type to use.
   * @param replicas The number of replicas for this bucket.
   * @param port The port for this bucket to listen on.
   * @param authpassword the authentication password.
   * @param flushEnabled whether flush is enabled.
   */
  public void updateBucket(final String name, final int memorySizeMB,
    final AuthType authType, final int replicas, final int port,
    final String authpassword, final boolean flushEnabled) {

    List<String> buckets = listBuckets();
    if (buckets.contains(name)) {
      HttpRequest request = prepareRequest(BUCKETS + name, null, name,
        memorySizeMB, authType, replicas, port, authpassword, flushEnabled);
      checkForErrorCode(200, sendRequest(request));
    } else {
      throw new RuntimeException("Bucket with given name already does not "
        + "exist");
    }
  }

  /**
   * Helper method to create a new bucket.
   *
   * @param type the type of the bucket.
   * @param name The name of the bucket.
   * @param memorySizeMB The amount of memory to allocate to this bucket.
   * @param authType the authentication type to use.
   * @param replicas The number of replicas for this bucket.
   * @param port The port for this bucket to listen on.
   * @param authpassword the authentication password.
   * @param flushEnabled whether flush is enabled.
   */
  private void createBucket(final BucketType type, final String name,
    final int memorySizeMB, final AuthType authType, final int replicas,
    final int port, final String authpassword, final boolean flushEnabled) {

    List<String> buckets = listBuckets();
    if(buckets.contains(name)){
      throw new RuntimeException("Bucket with given name already exists");
    } else {
      HttpRequest request = prepareRequest(BUCKETS, type, name, memorySizeMB,
        authType, replicas, port, authpassword, flushEnabled);
      checkForErrorCode(202, sendRequest(request));
    }
  }

  /**
   * Helper method to prepare the request with infos.
   *
   * @param type the type of the bucket.
   * @param name The name of the bucket.
   * @param memorySizeMB The amount of memory to allocate to this bucket.
   * @param authType the authentication type to use.
   * @param replicas The number of replicas for this bucket.
   * @param port The port for this bucket to listen on.
   * @param authpassword the authentication password.
   * @param flushEnabled whether flush is enabled.
   *
   * @return the prepared {@link HttpRequest}.
   */
  private HttpRequest prepareRequest(final String path, final BucketType type,
    final String name, final int memorySizeMB, final AuthType authType,
    final int replicas, final int port, final String authpassword,
    final boolean flushEnabled) {
    BasicHttpEntityEnclosingRequest request =
      new BasicHttpEntityEnclosingRequest("POST", path);

    StringBuilder sb = new StringBuilder();
    sb.append("name=").append(name)
      .append("&ramQuotaMB=")
      .append(memorySizeMB)
      .append("&authType=")
      .append(authType.getAuthType())
      .append("&replicaNumber=")
      .append(replicas)
      .append("&proxyPort=")
      .append(port);
    if (type != null) {
      sb.append("&bucketType=")
        .append(type.getBucketType());
    }
    if (authType == AuthType.SASL) {
      sb.append("&saslPassword=")
        .append(authpassword);
    }
    if(flushEnabled) {
      sb.append("&flushEnabled=1");
    }

    try {
      request.setEntity(new StringEntity(sb.toString()));
      return request;
    } catch (UnsupportedEncodingException e) {
      getLogger().error("Error creating request. Bad arguments");
      throw new RuntimeException(e);
    }
  }

  /**
   * Find a node and send the request.
   *
   * @param request the request to send.
   * @return a result from the sent request.
   */
  private HttpResult sendRequest(final HttpRequest request) {
    if (!running) {
      throw new IllegalStateException("Not connected to one of the nodes.");
    }

    HttpCoreContext coreContext = HttpCoreContext.create();

    request.addHeader("Authorization", "Basic "
      + Base64.encodeBase64String((username + ':' + password).getBytes()));
    request.addHeader("Accept", "*/*");
    request.addHeader("Content-Type", "application/x-www-form-urlencoded");

    for (HttpHost node : clusterNodes) {
      try {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicReference<HttpResponse> response =
          new AtomicReference<HttpResponse>();

        requester.execute(
          new BasicAsyncRequestProducer(node, request),
          new BasicAsyncResponseConsumer(),
          pool,
          coreContext,
          new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse result) {
              success.set(true);
              response.set(result);
              latch.countDown();
            }

            @Override
            public void failed(Exception ex) {
              getLogger().warn("Cluster Response failed with: ", ex);
              latch.countDown();
            }

            @Override
            public void cancelled() {
              getLogger().warn("Cluster Response was cancelled.");
              latch.countDown();
            }
          }
        );

        latch.await();
        if (!success.get()) {
          getLogger().info("Could not finish request execution");
          continue;
        }

        int code = response.get().getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.get().getEntity());
        String reason = parseError(body);
        String phrase = response.get().getStatusLine().getReasonPhrase();
        return new HttpResult(body, code, phrase, reason);
      } catch (InterruptedException ex) {
        getLogger().debug("Got interrupted while waiting for the response.");
      } catch (IOException e) {
        getLogger().debug("Unable to connect to: " + node
          + ". Trying another server");
      }
    }
    throw new RuntimeException("Unable to connect to cluster");
  }

  /**
   * Parse the error string (if there is one).
   *
   * @param json the json of the error message.
   *
   * @return a stringified representation of the error.
   */
  private static String parseError(final String json) {
    if (json != null && !json.isEmpty()) {
      try {
        JSONObject base = new JSONObject(json);
        if (base.has("errors")) {
          return base.getJSONObject("errors").toString();
        }
      } catch (JSONException e) {
        return "Client error parsing error response";
      }
    }

    return "No reason given";
  }

  /**
   * Helper method to check if an error is found in the result.
   *
   * @param expectedCode the code to expect.
   * @param result the result to check against.
   */
  private static void checkForErrorCode(int expectedCode, HttpResult result) {
    if (result.getErrorCode() != expectedCode) {
      throw new RuntimeException("Http Error: " + result.getErrorCode()
        + " Reason: " + result.getErrorPhrase() + " Details: "
        + result.getReason());
    }
  }

  /**
   * Shutdown the {@link ClusterManager}.
   *
   * @return if success, false if not running or already shutting down.
   */
  public boolean shutdown() {
    if (!running) {
      getLogger().info("Suppressing duplicate attempt to shut down");
      return false;
    }
    running = false;

    try {
      ioReactor.shutdown();
    } catch (IOException e) {
      getLogger().info("Got exception while shutting down", e);
    }
    try {
      reactorThread.join(0);
    } catch (InterruptedException ex) {
      getLogger().error("Interrupt " + ex + " received while waiting for "
        + "view thread to shut down.");
    }
    return true;
  }

  /**
   * Initialize the reactor IO thread.
   */
  private void initializeReactorThread() {
    final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(
      new HttpAsyncRequestExecutor(),
      ConnectionConfig.DEFAULT
    );

    reactorThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          ioReactor.execute(ioEventDispatch);
        } catch (InterruptedIOException ex) {
          getLogger().error("I/O reactor Interrupted", ex);
        } catch (IOException e) {
          getLogger().error("I/O error: " + e.getMessage(), e);
        }
        getLogger().debug("I/O reactor terminated");
      }
    }, "Couchbase ClusterManager Thread");
    reactorThread.start();

    running = true;
  }

  /**
   * Value Object to aggregate a raw response message.
   */
  public final static class HttpResult {

    /**
     * The body of the message.
     */
    private final String body;

    /**
     * The error code if there is one.
     */
    private final int errorCode;

    /**
     * The error phrase.
     */
    private final String errorPhrase;

    /**
     * The reason of the error.
     */
    private final String errorReason;

    /**
     * Create a new {@link HttpResult}.
     *
     * @param entity the underlying entity.
     * @param code the response code.
     * @param phrase the error phrase.
     * @param reason the error reason.
     */
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
