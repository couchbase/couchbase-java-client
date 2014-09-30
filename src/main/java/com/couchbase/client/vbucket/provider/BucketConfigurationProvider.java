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

package com.couchbase.client.vbucket.provider;

import com.couchbase.client.CouchbaseConnection;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseProperties;
import com.couchbase.client.vbucket.ConfigurationException;
import com.couchbase.client.vbucket.ConfigurationProviderHTTP;
import com.couchbase.client.vbucket.CouchbaseNodeOrder;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.config.Bucket;
import com.couchbase.client.vbucket.config.Config;
import com.couchbase.client.vbucket.config.ConfigurationParser;
import com.couchbase.client.vbucket.config.ConfigurationParserJSON;
import net.spy.memcached.ArrayModNodeLocator;
import net.spy.memcached.BroadcastOpFactory;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.NodeLocator;
import net.spy.memcached.auth.AuthThreadMonitor;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This {@link ConfigurationProvider} provides the current bucket configuration
 * in a best-effort way, mixing both http and binary fetching techniques
 * (depending on the supported mechanisms on the cluster side).
 */
public class BucketConfigurationProvider extends SpyObject
  implements ConfigurationProvider, Reconfigurable {

  private static final int DEFAULT_BINARY_PORT = 11210;
  private static final String ANONYMOUS_BUCKET = "default";

  private final AtomicReference<Bucket> config;
  private final List<URI> seedNodes;
  private final List<Reconfigurable> observers;
  private final String bucket;
  private final String password;
  private final CouchbaseConnectionFactory connectionFactory;
  private final ConfigurationParser configurationParser;
  private final AtomicReference<ConfigurationProviderHTTP> httpProvider;
  private final AtomicBoolean refreshingHttp;
  private final AtomicBoolean pollingBinary;
  private final AtomicReference<CouchbaseConnection> binaryConnection;
  private final boolean disableCarrierBootstrap;
  private final boolean disableHttpBootstrap;
  private volatile BootstrapProviderType bootstrapProvider = BootstrapProviderType.NONE;
  private volatile long lastRevision;
  private volatile boolean shutdown;

  public BucketConfigurationProvider(final List<URI> seedNodes,
    final String bucket, final String password,
    final CouchbaseConnectionFactory connectionFactory) {
    config = new AtomicReference<Bucket>();
    configurationParser = new ConfigurationParserJSON();
    httpProvider = new AtomicReference<ConfigurationProviderHTTP>(
      new ConfigurationProviderHTTP(seedNodes, bucket, password)
    );
    refreshingHttp = new AtomicBoolean(false);
    pollingBinary = new AtomicBoolean(false);
    observers = Collections.synchronizedList(new ArrayList<Reconfigurable>());
    binaryConnection = new AtomicReference<CouchbaseConnection>();

    this.seedNodes = Collections.synchronizedList(new ArrayList<URI>(seedNodes));
    this.bucket = bucket;
    this.password = password;
    this.connectionFactory = connectionFactory;
    potentiallyRandomizeNodeList(seedNodes);

    disableCarrierBootstrap = Boolean.parseBoolean(
      CouchbaseProperties.getProperty("disableCarrierBootstrap", "false"));
    disableHttpBootstrap = Boolean.parseBoolean(
      CouchbaseProperties.getProperty("disableHttpBootstrap", "false"));
    shutdown = false;
  }

  @Override
  public Bucket bootstrap() {
    if(shutdown) {
      getLogger().debug("Omitting bootstrap since already shutdown.");
    }

    bootstrapProvider = BootstrapProviderType.NONE;
    if (!bootstrapBinary() && !bootstrapHttp()) {
      throw new ConfigurationException("Could not fetch a valid Bucket "
        + "configuration.");
    }

    if (bootstrapProvider.isCarrier()) {
      getLogger().info("Could bootstrap through carrier publication.");
    } else {
      getLogger().info("Carrier config not available, bootstrapped through "
        + "HTTP.");
    }

    return config.get();
  }

  /**
   * Helper method to initiate the binary bootstrap process.
   *
   * If no config is found (either because of an error or it is not supported
   * on the cluster side), false is returned.
   *
   * @return true if the binary bootstrap process was successful.
   */
  boolean bootstrapBinary() {
    if (disableCarrierBootstrap) {
      getLogger().info("Carrier bootstrap manually disabled, skipping.");
      return false;
    }

    List<InetSocketAddress> nodes =
      new ArrayList<InetSocketAddress>(seedNodes.size());
    for (URI seedNode : seedNodes) {
      nodes.add(new InetSocketAddress(seedNode.getHost(), DEFAULT_BINARY_PORT));
    }

    try {
      for (InetSocketAddress node : nodes) {
        if(tryBinaryBootstrapForNode(node)) {
          bootstrapProvider = BootstrapProviderType.CARRIER;
          return true;
        }
      }

      getLogger().debug("Not a single node returned a carrier publication "
        + "config.");
      return false;
    } catch(Exception ex) {
      getLogger().info("Could not fetch config from carrier publication seed "
        + "nodes.", ex);
      return false;
    }
  }

  /**
   * Try bootstrapping to a binary connection for the given node.
   *
   * @param node the node to connect.
   * @return true if successful, false otherwise.
   * @throws Exception if an error while bootstrapping occurs.
   */
  private boolean tryBinaryBootstrapForNode(InetSocketAddress node)
    throws Exception {
    if (binaryConnection.get() != null) {
        return true;
    }
    ConfigurationConnectionFactory fact =
      new ConfigurationConnectionFactory(seedNodes, bucket, password);
    CouchbaseConnectionFactory cf = connectionFactory;
    CouchbaseConnection connection = null;

    List<ConnectionObserver> initialObservers = new ArrayList<ConnectionObserver>();
    final CountDownLatch latch = new CountDownLatch(1);
    initialObservers.add(new ConnectionObserver() {
      @Override
      public void connectionEstablished(SocketAddress socketAddress, int i) {
        latch.countDown();
      }

      @Override
      public void connectionLost(SocketAddress socketAddress) {
        // not needed
      }
    });

    try {
       connection = new CouchbaseConfigConnection(
        cf.getReadBufSize(), fact, Collections.singletonList(node),
        initialObservers, cf.getFailureMode(),
        cf.getOperationFactory()
      );

      boolean result = latch.await(5, TimeUnit.SECONDS);
      if (!result) {
        throw new IOException("Connection could not be established to carrier"
          + " port in the given time interval.");
      }
    } catch (Exception ex) {
      if (connection != null) {
        connection.shutdown();
      }
      getLogger().debug("(Carrier Publication) Could not load config from "
        + node.getHostName() + ", trying next node.", ex);
      return false;
    }

    if (!bucket.equals(ANONYMOUS_BUCKET)) {
      AuthThreadMonitor monitor = new AuthThreadMonitor();
      List<MemcachedNode> connectedNodes = new ArrayList<MemcachedNode>(
        connection.getLocator().getAll());
      for (MemcachedNode connectedNode : connectedNodes) {
        if (connectedNode.getSocketAddress().equals(node)) {
          monitor.authConnection(connection, cf.getOperationFactory(),
            cf.getAuthDescriptor(), connectedNode);
        }
      }
    }

    List<String> configs = getConfigsFromBinaryConnection(connection);

    if (configs.isEmpty()) {
      getLogger().debug("(Carrier Publication) Could not load config from "
        + node.getHostName() + ", trying next node.");
      connection.shutdown();
      return false;
    }

    String appliedConfig = connection.replaceConfigWildcards(
      configs.get(0));
    try {
        Bucket config = configurationParser.parseBucket(appliedConfig);
        setConfig(config);
    } catch(Exception ex) {
        getLogger().warn("Could not parse config, retrying bootstrap.", ex);
        connection.shutdown();
        return false;
    }

    connection.addObserver(new ConnectionObserver() {
      @Override
      public void connectionEstablished(SocketAddress sa, int reconnectCount) {
        getLogger().debug("Carrier Config Connection established to " + sa);
      }

      @Override
      public void connectionLost(SocketAddress sa) {
        getLogger().debug("Carrier Config Connection lost from " + sa);
        CouchbaseConnection conn = binaryConnection.getAndSet(null);
        try {
          conn.shutdown();

        } catch (IOException e) {
          getLogger().debug("Could not shut down Carrier Config Connection", e);
        }
        signalOutdated();
      }
    });

    CouchbaseConnection old = binaryConnection.get();
    if (old != null) {
        old.shutdown();
    }
    binaryConnection.set(connection);
    return true;
  }

  /**
   * Load the configs from a binary connection through a broadcast op.
   *
   * Note that this operation is blocking, so run in a thread pool if needed.
   *
   * @param connection the connection to execute against.
   * @return a list of configs (potentially empty, but not null)
   */
  private List<String> getConfigsFromBinaryConnection(
    final CouchbaseConnection connection) throws Exception {
    final List<String> configs = Collections.synchronizedList(
      new ArrayList<String>());

    CountDownLatch blatch = connection.broadcastOperation(
      new BroadcastOpFactory() {
        @Override
        public Operation newOp(MemcachedNode n, final CountDownLatch latch) {
          return new GetConfigOperationImpl(new OperationCallback() {
            @Override
            public void receivedStatus(OperationStatus status) {
              if (status.isSuccess()) {
                configs.add(status.getMessage());
              }
            }

            @Override
            public void complete() {
              latch.countDown();
            }
          });
        }
      }
    );

    blatch.await(connectionFactory.getOperationTimeout(),
      TimeUnit.MILLISECONDS);
    return configs;
  }

  /**
   * Helper method to initiate the http bootstrap process.
   *
   * If no config is found (because of an error), false is returned. For now,
   * this is delegated to the old HTTP provider, but no monitor is attached
   * for a subsequent streaming connection.
   *
   * @return true if the http bootstrap process was successful.
   */
  boolean bootstrapHttp() {
    if (disableHttpBootstrap) {
      getLogger().info("Http bootstrap manually disabled, skipping.");
      return false;
    }

    try {
      httpProvider.get().clearBuckets();
      Bucket config = httpProvider.get().getBucketConfiguration(bucket);
      setConfig(config);
      monitorBucket();
      bootstrapProvider = BootstrapProviderType.HTTP;
      return true;
    } catch(Exception ex) {
      getLogger().info("Could not fetch config from http seed nodes.", ex);
      return false;
    }
  }

  /**
   * Start to monitor the bucket configuration, depending on the provider
   * used.
   */
  private void monitorBucket() {
    if (!shutdown && bootstrapProvider.isHttp()) {
        httpProvider.get().subscribe(bucket, this);
    }
  }

  @Override
  public void reconfigure(final Bucket bucket) {
    setConfig(bucket);
  }

  @Override
  public Bucket getConfig() {
    if (config.get() == null) {
      bootstrap();
    }
    return config.get();
  }

  @Override
  public void setConfig(final Bucket config) {
    if (config.isNotUpdating()) {
      signalOutdated();
      return;
    }
    long configRevision = config.getRevision();
    if (configRevision > 0) {
      if (configRevision > lastRevision) {
        lastRevision = configRevision;
      } else {
        return;
      }
    }
    getLogger().debug("Applying new bucket config for bucket \"" + bucket
      + "\" (carrier publication: " + bootstrapProvider.isCarrier() + "): " + config);

    this.config.set(config);
    httpProvider.get().updateBucket(config.getName(), config);
    updateSeedNodes();
    notifyObservers();

    manageTaintedConfig(config.getConfig());
  }

  /**
   * Orchestrating method to start/stop background config fetcher for binary
   * configs if tainted/not tainted anymore.
   *
   * @param config the config to check.
   */
  private void manageTaintedConfig(final Config config) {
    if (bootstrapProvider != BootstrapProviderType.CARRIER) {
      return;
    }

    if (config.isTainted() && pollingBinary.compareAndSet(false, true)) {
      getLogger().debug("Found tainted configuration, starting carrier "
        + "poller.");
      Thread thread = new Thread(new BinaryConfigPoller());
      thread.setName("couchbase - carrier config poller");
      thread.start();
    }
  }

  /**
   * Updates the current list of seed nodes with a current one from the stored
   * configuration.
   */
  private void updateSeedNodes() {
    Config config = this.config.get().getConfig();

    List<String> clusterNodes = config.getRestEndpoints();
    if (!clusterNodes.isEmpty()) {
      List<URI> newNodes = new ArrayList<URI>();
      for (String clusterNode : clusterNodes) {
        try {
          newNodes.add(new URI(clusterNode));
        } catch(URISyntaxException ex) {
          getLogger().warn("Could not add node to updated bucket list because "
            + "of a parsing exception.");
          getLogger().debug("Could not parse list because: " + ex);
        }
      }

      if (seedNodesAreDifferent(seedNodes, newNodes)) {
        potentiallyRandomizeNodeList(newNodes);
        synchronized (seedNodes) {
          seedNodes.clear();
          seedNodes.addAll(newNodes);
        }
        httpProvider.get().updateBaseListFromConfig(seedNodes);
      }
    }
  }

  /**
   * Randomize the given node list if set in the factory.
   *
   * @param list the list to shuffle.
   */
  private void potentiallyRandomizeNodeList(List<URI> list) {
    if (connectionFactory.getStreamingNodeOrder()
      == CouchbaseNodeOrder.ORDERED) {
      return;
    }
    Collections.shuffle(list);
  }

  /**
   * Checks if two given lists are different.
   *
   * @param left one list to check.
   * @param right the other list to check against.
   * @return true if they are different, false if they are the same.
   */
  private static boolean seedNodesAreDifferent(List<URI> left,
    List<URI> right) {
    if (left.size() != right.size()) {
      return true;
    }

    for (URI uri : left) {
      if (!right.contains(uri)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void signalOutdated() {
    if (shutdown) {
      getLogger().debug("Omitting signalOutdated since already shutdown.");
      return;
    }

    if (bootstrapProvider.isCarrier()) {
      if (binaryConnection.get() == null) {
        bootstrap();
      } else {
        try {
          List<String> configs = getConfigsFromBinaryConnection(binaryConnection.get());
          if (configs.isEmpty()) {
            bootstrap();
            return;
          }
          String appliedConfig = binaryConnection.get().replaceConfigWildcards(
            configs.get(0));
          Bucket config = configurationParser.parseBucket(appliedConfig);
          setConfig(config);
        } catch(Exception ex) {
          getLogger().info("Could not load config from existing "
            + "connection, rerunning bootstrap.", ex);
          bootstrap();
        }
      }
    } else {
      if (disableHttpBootstrap) {
        getLogger().info("Http bootstrap manually disabled, skipping.");
        return;
      }
      if (refreshingHttp.compareAndSet(false, true)) {
        Thread refresherThread = new Thread(new HttpProviderRefresher());
        refresherThread.setName("HttpConfigurationProvider Reloader");
        refresherThread.start();
      } else {
        getLogger().debug("Suppressing duplicate refreshing attempt.");
      }
    }
  }

  @Override
  public void reloadConfig() {
    if (bootstrapProvider.isCarrier() && !shutdown) {
      signalOutdated();
    }
  }

  @Override
  public void shutdown() {
    observers.clear();
    shutdown = true;
    if (httpProvider.get() != null) {
      httpProvider.get().shutdown();
    }
    if (binaryConnection.get() != null) {
      try {
        binaryConnection.get().shutdown();
      } catch (IOException e) {
        getLogger().warn("Could not shutdown carrier publication config "
          + "connection.", e);
      }
    }
  }

  @Override
  public String getAnonymousAuthBucket() {
    return ANONYMOUS_BUCKET;
  }

  @Override
  public void setConfig(final String config) {
    try {
      setConfig(configurationParser.parseBucket(config));
    } catch (Exception ex) {
      getLogger().warn("Got new config to update, but could not decode it. "
        + "Staying with old one.", ex);
    }
  }

  @Override
  public void subscribe(Reconfigurable rec) {
    observers.add(rec);
  }

  @Override
  public void unsubscribe(Reconfigurable rec) {
    observers.remove(rec);
  }

  /**
   * Notify all observers of a new configuration.
   */
  private void notifyObservers() {
    synchronized (observers) {
      for (Reconfigurable rec : observers) {
        getLogger().debug("Notifying Observer of new configuration: "
          + rec.getClass().getSimpleName());
        rec.reconfigure(getConfig());
      }
    }
  }

  class HttpProviderRefresher implements Runnable {

    @Override
    public void run() {
      try {

        long reconnectAttempt = 0;
        long backoffTime = 1000;
        long maxWaitTime = 10000;
        while(true) {
          try {
            long waitTime = reconnectAttempt++ * backoffTime;
            if(reconnectAttempt >= 10) {
              waitTime = maxWaitTime;
            }
            getLogger().info("Reconnect attempt " + reconnectAttempt
              + ", waiting " + waitTime + "ms");
            Thread.sleep(waitTime);

            ConfigurationProviderHTTP oldProvider = httpProvider.get();
            ConfigurationProviderHTTP newProvider =
              new ConfigurationProviderHTTP(seedNodes, bucket, password);
            httpProvider.set(newProvider);
            monitorBucket();
            oldProvider.shutdown();
            return;
          } catch(Exception ex) {
            getLogger().debug("Got exception while trying to reconnect the " +
              "configuration provider.", ex);
            continue;
          }
        }
      } finally {
        refreshingHttp.set(false);
      }
    }
  }

  /**
   * A config poller for carrier configurations.
   */
  class BinaryConfigPoller implements Runnable {

    /**
     * The time to wait between config poll intervals in ms.
     */
    private static final int waitPeriod = 1000;

    /**
     * Counter to log polling attempts for this poller.
     */
    private int attempt;

    /**
     * Calling {@link #signalOutdated()} against a running binary configuration
     * will trigger a config refresh.
     */
    @Override
    public void run() {
      try {
        while (bootstrapProvider.isCarrier() && getConfig().getConfig().isTainted()) {
          getLogger().debug("Polling for new carrier configuration and " +
            "waiting " + waitPeriod + "ms (Attempt " + ++attempt + ").");
          signalOutdated();
          try {
            Thread.sleep(waitPeriod);
          } catch (InterruptedException e) {
            getLogger().warn("Got interrupted while trying to poll for new " +
              "carrier config.", e);
            break;
          }
        }
      } finally {
        getLogger().debug("Finished polling for new carrier configuration.");
        pollingBinary.set(false);
      }
    }
  }

  static class ConfigurationConnectionFactory
    extends CouchbaseConnectionFactory {
    ConfigurationConnectionFactory(List<URI> baseList, String bucketName,
      String password) throws IOException {
      super(baseList, bucketName, password);
    }

    @Override
    public NodeLocator createLocator(List<MemcachedNode> nodes) {
      return new ArrayModNodeLocator(nodes, getHashAlg());
    }
  }

}
