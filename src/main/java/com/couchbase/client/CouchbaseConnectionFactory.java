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

import com.couchbase.client.vbucket.CouchbaseNodeOrder;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.VBucketNodeLocator;
import com.couchbase.client.vbucket.config.Config;
import com.couchbase.client.vbucket.config.ConfigType;
import com.couchbase.client.vbucket.provider.BucketConfigurationProvider;
import com.couchbase.client.vbucket.provider.ConfigurationProvider;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.DefaultHashAlgorithm;
import net.spy.memcached.FailureMode;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.KetamaNodeLocator;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.NodeLocator;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Couchbase implementation of ConnectionFactory.
 *
 * <p>
 * This implementation creates connections where the operation queue is an
 * ArrayBlockingQueue and the read and write queues are unbounded
 * LinkedBlockingQueues. The <code>Retry</code> FailureMode and <code>
 * KetamaHash</code> VBucket hashing mechanism are always used. If other
 * configurations are needed, look at the ConnectionFactoryBuilder.
 *
 * </p>
 */
public class CouchbaseConnectionFactory extends BinaryConnectionFactory {

  /**
   * Default failure mode.
   */
  public static final FailureMode DEFAULT_FAILURE_MODE =
    FailureMode.Redistribute;

  /**
   * Default hash algorithm.
   */
  public static final HashAlgorithm DEFAULT_HASH =
    DefaultHashAlgorithm.NATIVE_HASH;

  /**
   * Maximum length of the operation queue returned by this connection factory.
   */
  public static final int DEFAULT_OP_QUEUE_LEN = 16384;
  /**
   * Specify a default minimum reconnect interval of 1.1s.
   *
   * This means that if a reconnect is needed, it won't try to reconnect
   * more frequently than 1.1s between tries.  The initial HTTP connections
   * under us take up to 500ms per request.
   */
  public static final long DEFAULT_MIN_RECONNECT_INTERVAL = 1100;

  /**
   * Default View request timeout in ms.
   */
  public static final int DEFAULT_VIEW_TIMEOUT = 75000;

  /**
   * Default size of view io worker threads.
   */
  public static final int DEFAULT_VIEW_WORKER_SIZE = 1;

  /**
   * Default amount of max connections per node.
   */
  public static final int DEFAULT_VIEW_CONNS_PER_NODE = 10;

  /**
   * Default Timeout when persistence/replication constraints are used (in ms).
   */
  public static final long DEFAULT_OBS_TIMEOUT = 5000;

  /**
   * Default Observe poll interval in ms.
   */
  public static final long DEFAULT_OBS_POLL_INTERVAL = 10;

  /**
   * Default maximum amount of poll cycles before failure.
   *
   * See {@link #DEFAULT_OBS_TIMEOUT} for correct use. The number of polls is
   * now calculated automatically based on the {@link #DEFAULT_OBS_TIMEOUT} and
   * {@link #DEFAULT_OBS_POLL_INTERVAL}.
   */
  @Deprecated
  public static final int DEFAULT_OBS_POLL_MAX = 500;

  /**
   * Default auth wait time.
   */
  public static final long DEFAULT_AUTH_WAIT_TIME = 2500;

  /**
   * Default Node ordering to use for streaming connection.
   */
  public static final CouchbaseNodeOrder DEFAULT_STREAMING_NODE_ORDER =
    CouchbaseNodeOrder.RANDOM;

  protected volatile ConfigurationProvider configurationProvider;
  private volatile String bucket;
  private volatile String pass;
  private volatile List<URI> storedBaseList;
  private static final Logger LOGGER =
    Logger.getLogger(CouchbaseConnectionFactory.class.getName());
  private volatile boolean needsReconnect;
  private final int maxConfigCheck = 10; //maximum allowed checks before we
                                         // reconnect in a 10 sec interval
  private volatile long configProviderLastUpdateTimestamp;
  private long minReconnectInterval = DEFAULT_MIN_RECONNECT_INTERVAL;
  private final ExecutorService resubExec = Executors.newSingleThreadExecutor();
  private final CouchbaseNodeOrder nodeOrder = DEFAULT_STREAMING_NODE_ORDER;
  private ClusterManager clusterManager;

  /**
   * Create a new {@link CouchbaseConnectionFactory} and load the required
   * connection information from system properties.
   *
   * <p>The following properties need to be set in order to bootstrap:
   *  - cbclient.nodes: ;-separated list of URIs
   *  - cbclient.bucket: name of the bucket
   *  - cbclient.password: password of the bucket
   * </p>
   */
  public CouchbaseConnectionFactory() {
    String nodes = CouchbaseProperties.getProperty("nodes");
    String bucket =  CouchbaseProperties.getProperty("bucket");
    String password = CouchbaseProperties.getProperty("password");

    if (nodes == null) {
      throw new IllegalArgumentException("System property cbclient.nodes "
        + "not set or empty");
    }
    if (bucket == null) {
      throw new IllegalArgumentException("System property cbclient.bucket "
        + "not set or empty");
    }
    if (password == null) {
      throw new IllegalArgumentException("System property cbclient.password "
        + "not set or empty");
    }

    List<URI> baseList = new ArrayList<URI>();
    String[] nodeList = nodes.split(";");
    for (String node : nodeList) {
      try {
        baseList.add(new URI(node));
      } catch (Exception e) {
        throw new IllegalArgumentException("Could not parse node list into "
          + " URI format.");
      }
    }

    initialize(baseList, bucket, password);
  }

  public CouchbaseConnectionFactory(final List<URI> baseList,
    final String bucketName, String password) throws IOException {
    initialize(baseList, bucketName, password);
  }

  private void initialize(List<URI> baseList, String bucket, String password) {
    storedBaseList = new ArrayList<URI>();
    for (URI bu : baseList) {
      if (!bu.isAbsolute()) {
        throw new IllegalArgumentException("The base URI must be absolute");
      }
      storedBaseList.add(bu);
    }

    if (bucket == null || bucket.isEmpty()) {
      throw new IllegalArgumentException("The bucket name must not be null "
        + "or empty.");
    }
    if (password == null) {
      throw new IllegalArgumentException("The bucket password must not be "
        + " null.");
    }

    this.bucket = bucket;
    pass = password;
    configurationProvider =
      new BucketConfigurationProvider(baseList, bucket, password, this);
  }

  @Override
  public MemcachedConnection createConnection(List<InetSocketAddress> addrs)
    throws IOException {
    Config config = getVBucketConfig();
    if (config.getConfigType() == ConfigType.MEMCACHE) {
      return new CouchbaseMemcachedConnection(getReadBufSize(), this, addrs,
        getInitialObservers(), getFailureMode(), getOperationFactory());
    } else if (config.getConfigType() == ConfigType.COUCHBASE) {
      return new CouchbaseConnection(getReadBufSize(), this, addrs,
        getInitialObservers(), getFailureMode(), getOperationFactory());
    }
    throw new IOException("No ConnectionFactory for bucket type "
      + config.getConfigType());
  }


  public ViewConnection createViewConnection(
      List<InetSocketAddress> addrs) throws IOException {
    return new ViewConnection(this, addrs, bucket, pass);
  }

  @Override
  public NodeLocator createLocator(List<MemcachedNode> nodes) {
    Config config = getVBucketConfig();

    if (config == null) {
      throw new IllegalStateException("Couldn't get config");
    }

    if (config.getConfigType() == ConfigType.MEMCACHE) {
      return new KetamaNodeLocator(nodes, DefaultHashAlgorithm.KETAMA_HASH);
    } else if (config.getConfigType() == ConfigType.COUCHBASE) {
      return new VBucketNodeLocator(nodes, getVBucketConfig());
    } else {
      throw new IllegalStateException("Unhandled locator type: "
          + config.getConfigType());
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.ConnectionFactory#shouldOptimize()
   */
  @Override
  public boolean shouldOptimize() {
    return false;
  }

  @Override
  public AuthDescriptor getAuthDescriptor() {
    if (!configurationProvider.getAnonymousAuthBucket().equals(bucket)
        && bucket != null) {
      return new AuthDescriptor(new String[] {},
        new PlainCallbackHandler(bucket, pass));
    } else {
      return null;
    }
  }

  public String getBucketName() {
    return bucket;
  }

  public int getViewTimeout() {
    return DEFAULT_VIEW_TIMEOUT;
  }

  public int getViewWorkerSize() {
    return DEFAULT_VIEW_WORKER_SIZE;
  }

  public int getViewConnsPerNode() {
    return DEFAULT_VIEW_CONNS_PER_NODE;
  }

  public CouchbaseNodeOrder getStreamingNodeOrder() {
    return nodeOrder;
  }

  public Config getVBucketConfig() {
    return configurationProvider.getConfig().getConfig();
  }

  public synchronized ConfigurationProvider getConfigurationProvider() {
    return configurationProvider;
  }

  protected void requestConfigReconnect(String bucketName, Reconfigurable rec) {
    configurationProvider.signalOutdated();
    needsReconnect = true;
  }

  synchronized void setConfigurationProvider(
    ConfigurationProvider configProvider) {
    this.configProviderLastUpdateTimestamp = System.currentTimeMillis();
    this.configurationProvider = configProvider;
  }

  void setMinReconnectInterval(long reconnIntervalMsecs) {
    this.minReconnectInterval = reconnIntervalMsecs;
  }

  /**
   * Check for a new configuration update.
   *
   * In many different cases, it is possible that for a new configuration should be checked
   * proactively. This internally performs a {@link ConfigurationProvider#signalOutdated()}
   * call under the covers to work with the configuration provider on a new configuration.
   *
   * To not ask too often, only if more time than specified through
   * {@link #getMinReconnectInterval()} passed, the {@link ConfigurationProvider#signalOutdated()}
   * will be executed.
   */
  void checkConfigUpdate() {
      long now = System.currentTimeMillis();
      long intervalWaited = now - this.configProviderLastUpdateTimestamp;
      if (intervalWaited < this.getMinReconnectInterval()) {
        LOGGER.log(Level.FINE, "Ignoring config update check. Only {0}ms out"
                + " of a threshold of {1}ms since last update.",
                new Object[]{intervalWaited, this.getMinReconnectInterval()});
        return;
      }

      getConfigurationProvider().signalOutdated();
      configProviderLastUpdateTimestamp = System.currentTimeMillis();
  }

  /**
   * The minimum reconnect interval in milliseconds.
   *
   * @return the minReconnectInterval
   */
  public long getMinReconnectInterval() {
    return minReconnectInterval;
  }

  /**
   * The observe poll interval in milliseconds.
   *
   * @return the observe poll interval.
   */
  public long getObsPollInterval() {
    return DEFAULT_OBS_POLL_INTERVAL;
  }

  /**
   * The observe timeout in milliseconds.
   *
   * @return the observe timeout.
   */
  public long getObsTimeout() {
    return DEFAULT_OBS_TIMEOUT;
  }

  @Override
  public long getAuthWaitTime() {
    return DEFAULT_AUTH_WAIT_TIME;
  }

  /**
   * The number of observe polls to execute before giving up.
   *
   * It is calculated out of the observe timeout and the observe interval,
   * rounded to the next largest integer value.
   *
   * @return the number of polls.
   */
  public int getObsPollMax() {
    return new Double(
      Math.ceil((double) getObsTimeout() / getObsPollInterval())
    ).intValue();
  }

  /**
   * Returns the amount of how many config checks in a given time period
   * (currently 10 seconds) are allowed before a reconfiguration is triggered.
   *
   * @return the number of config checks allowed.
   */
  int getMaxConfigCheck() {
    return maxConfigCheck;
  }

  /**
   * Returns a ClusterManager and initializes one if it does not exist.
   * @return Returns an instance of a ClusterManager.
   */
  public ClusterManager getClusterManager() {
    if(clusterManager == null) {
      clusterManager = new ClusterManager(storedBaseList, bucket, pass);
    }
    return clusterManager;
  }

  /**
   * Returns the current base list.
   *
   * @return the base list.
   */
  List<URI> getStoredBaseList() {
    return storedBaseList;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CouchbaseConnectionFactory{");
    sb.append("bucket='").append(getBucketName()).append('\'');
    sb.append(", nodes=").append(getStoredBaseList());
    sb.append(", order=").append(getStreamingNodeOrder());
    sb.append(", opTimeout=").append(getOperationTimeout());
    sb.append(", opQueue=").append(getOpQueueLen());
    sb.append(", opQueueBlockTime=").append(getOpQueueMaxBlockTime());
    sb.append(", obsPollInt=").append(getObsPollInterval());
    sb.append(", obsPollMax=").append(getObsPollMax());
    sb.append(", obsTimeout=").append(getObsTimeout());
    sb.append(", viewConns=").append(getViewConnsPerNode());
    sb.append(", viewTimeout=").append(getViewTimeout());
    sb.append(", viewWorkers=").append(getViewWorkerSize());
    sb.append(", configCheck=").append(getMaxConfigCheck());
    sb.append(", reconnectInt=").append(getMinReconnectInterval());
    sb.append(", failureMode=").append(getFailureMode());
    sb.append(", hashAlgo=").append(getHashAlg());
    sb.append(", authWaitTime=").append(getAuthWaitTime());
    sb.append('}');
    return sb.toString();
  }
}
