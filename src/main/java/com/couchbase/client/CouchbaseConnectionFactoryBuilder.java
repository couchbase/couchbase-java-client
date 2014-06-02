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
import com.couchbase.client.vbucket.config.Config;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.FailureMode;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.metrics.MetricCollector;
import net.spy.memcached.metrics.MetricType;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.transcoders.Transcoder;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@link CouchbaseConnectionFactoryBuilder} enables the customization of
 * {@link CouchbaseConnectionFactory} settings.
 *
 * <p>Some of the provides setter methods are for internal usage only, see the
 * individual documentation for setter methods on this class for their impact,
 * usage and defaults.</p>
 *
 * <p>The builder should be used like this:</p>
 *
 * <pre>{@code
 * // Create the builder
 * CouchbaseConnectionFactoryBuilder builder =
 * new CouchbaseConnectionFactoryBuilder();
 *
 * // Change a setting
 * builder.setOpTimeout(3000);
 *
 * // Build the factory and use it
 * List<URI> nodes = Arrays.asList(URI.create("..."));
 * CouchbaseClient client = new CouchbaseClient(
 *   builder.buildCouchbaseConnection(nodes, "bucket", "password")
 * );
 * }</pre>
 */
public class CouchbaseConnectionFactoryBuilder
  extends ConnectionFactoryBuilder {

  /**
   * The logger used for the builder.
   */
  private static final Logger LOGGER = Logger.getLogger(
    CouchbaseConnectionFactoryBuilder.class.getName()
  );

  private Config vBucketConfig;
  private long reconnThresholdTimeMsecs =
    CouchbaseConnectionFactory.DEFAULT_MIN_RECONNECT_INTERVAL;
  private long obsPollInterval =
    CouchbaseConnectionFactory.DEFAULT_OBS_POLL_INTERVAL;
  private int obsPollMax = CouchbaseConnectionFactory.DEFAULT_OBS_POLL_MAX;
  private long obsTimeout = CouchbaseConnectionFactory.DEFAULT_OBS_TIMEOUT;

  private int viewTimeout = CouchbaseConnectionFactory.DEFAULT_VIEW_TIMEOUT;
  private int viewWorkers = CouchbaseConnectionFactory.DEFAULT_VIEW_WORKER_SIZE;
  private int viewConns =
    CouchbaseConnectionFactory.DEFAULT_VIEW_CONNS_PER_NODE;

  private CouchbaseNodeOrder nodeOrder
    = CouchbaseConnectionFactory.DEFAULT_STREAMING_NODE_ORDER;

  protected MetricType metricType;
  protected MetricCollector collector;
  protected ExecutorService executorService;
  protected long authWaitTime = -1;

  /**
   * Sets a custom {@link Config} to use.
   *
   * <p>Note that this method is only used for internal purposes and should not
   * be used by regular applications. Use with care!</p>
   *
   * @param config the configuration to use.
   * @return the builder instance for dsl-like chaining functionality.
   */
  public CouchbaseConnectionFactoryBuilder setVBucketConfig(final Config config) {
    vBucketConfig = config;
    return this;
  }

  /**
   * Sets a custom reconnect threshold.
   *
   * <p>If operations are failing for a period of time, the attached streaming
   * connection for configuration updates is considered invalid and the client
   * tries to establish a new connection by reconnecting. This threshold sets
   * the time between those reconnect attempts to not overwhelm the server if
   * it is already in a potentially unstable state.</p>
   *
   * <p>Defaults to:
   * {@link CouchbaseConnectionFactory#DEFAULT_MIN_RECONNECT_INTERVAL}</p>
   *
   * @param time the time for the threshold.
   * @param unit the unit for the time.
   * @return the builder instance for dsl-like chaining functionality.
   */
  public CouchbaseConnectionFactoryBuilder setReconnectThresholdTime(
    final long time, final TimeUnit unit) {
    reconnThresholdTimeMsecs = TimeUnit.MILLISECONDS.convert(time, unit);
    return this;
  }

  /**
   * Sets a custom interval between observe poll cycles.
   *
   * <p>Every time a mutation operation with constraints (like
   * {@link CouchbaseClient#set(String, Object, net.spy.memcached.PersistTo,
   * net.spy.memcached.ReplicateTo)}) is used, internally a poller observes the
   * server state until the required constraints can be met. This configuration
   * setting tunes this poll interval. If you expect faster replication or
   * persistence timings on the server than provided by the default interval,
   * set it to a lower value. If this value gets adjusted, it might make sense
   * to also adjust {@link #setObsTimeout(long)}.</p>
   *
   * <p>Defaults to:
   * {@link CouchbaseConnectionFactory#DEFAULT_OBS_POLL_INTERVAL}</p>
   *
   * @param interval the interval in milliseconds.
   * @return the builder instance for dsl-like chaining functionality.
   */
  public CouchbaseConnectionFactoryBuilder setObsPollInterval(
    final long interval) {
    obsPollInterval = interval;
    return this;
  }

  /**
   * Sets the absolute per-operation timeout for observe calls.
   *
   * <p>This is the total timeout that the observe poller spends polling for
   * the desired constraint specified through a call like
   * {@link CouchbaseClient#set(String, Object, net.spy.memcached.PersistTo,
   * net.spy.memcached.ReplicateTo)}. Together with
   * {@link #setObsPollInterval(long)}, both settings provide the tuning knobs
   * for observe-related behavior.</p>
   *
   * <p>Defaults to:
   * {@link CouchbaseConnectionFactory#DEFAULT_OBS_TIMEOUT}.</p>
   *
   * @param timeout the timeout in milliseconds.
   * @return the builder instance for dsl-like chaining functionality.
   */
  public CouchbaseConnectionFactoryBuilder setObsTimeout(final long timeout) {
    obsTimeout = timeout;
    return this;
  }

  /**
   * Sets the maximum number of observe poll cycles for observe calls.
   *
   * <p>This method has been deprecated. Do not use this method directly, but
   * instead use a combination of {@link #setObsPollInterval(long)} and
   * {@link #setObsTimeout(long)} to achieve the same effect. This setting
   * is ignored and has no impact on client behavior.</p>
   *
   * @param maxPoll the maximum number of polls to run before giving up.
   * @return the builder instance for dsl-like chaining functionality.
   */
  @Deprecated
  public CouchbaseConnectionFactoryBuilder setObsPollMax(final int maxPoll) {
    obsPollMax = maxPoll;
    return this;
  }

  /**
   * Sets a custom timeout for view operations.
   *
   * <p>A custom timeout in miliseconds can be specified to overrule the default
   * timeout. If tuning this value significantly lower, it should be taken into
   * consideration that view results take eventually longer to return than their
   * key-based counterparts.</p>
   *
   * <p>If a timeout lower than 500ms is applied, it will be capped to 500ms as
   * a precaution and a warning will be issued. If it is lower than 2.5 seconds,
   * a warning is also issued to notify a potential configuration issue.</p>
   *
   * <p>Defaults to:
   * {@link CouchbaseConnectionFactory#DEFAULT_VIEW_TIMEOUT}.</p>
   *
   * @param timeout the timeout in miliseconds.
   * @return the builder instance for dsl-like chaining functionality.
   */
  public CouchbaseConnectionFactoryBuilder setViewTimeout(int timeout) {
    if(timeout < 500) {
      timeout = 500;
      LOGGER.log(Level.WARNING, "ViewTimeout is too short. Overriding "
        + "viewTimeout with threshold of 500ms.");
    } else if(timeout < 2500) {
      LOGGER.log(Level.WARNING, "ViewTimeout is very short, should be "
        + "more than 2500ms.");
    }
    viewTimeout = timeout;
    return this;
  }

  /**
   * Sets a custom worker count for view operations.
   *
   * <p>View requests are dispatched to asynchronous workers. This setting
   * normally only needs to be tuned if there is a very high view workload
   * and there is a suspicion that the default worker size is the bottleneck
   * and cannot handle the needed traffic. If increased, also consider
   * increasing the maximum connections per node.</p>
   *
   * <p>Defaults to
   * {@link CouchbaseConnectionFactory#DEFAULT_VIEW_WORKER_SIZE}.</p>
   *
   * @param workers the number of workers.
   * @return the builder instance for dsl-like chaining functionality.
   */
  public CouchbaseConnectionFactoryBuilder setViewWorkerSize(int workers) {
    if (workers < 1) {
      throw new IllegalArgumentException("The View worker size needs to be "
        + "greater than zero.");
    }

    viewWorkers = workers;
    return this;
  }

  /**
   * Changes the maximum parallel open connections per node for view operations.
   *
   * <p>View connections are openend on demand to each node, so with increasing
   * traffic more and more parallel connections are opened to satisfy the
   * workload needs. This setting allows to change the upper limit of open
   * connections per node.</p>
   *
   * <p>Defaults to
   * {@link CouchbaseConnectionFactory#DEFAULT_VIEW_CONNS_PER_NODE}.</p>
   *
   * @param conns maximum parallel open connections per node.
   * @return the builder instance for dsl-like chaining functionality.
   */
  public CouchbaseConnectionFactoryBuilder setViewConnsPerNode(int conns) {
    if (conns < 1) {
      throw new IllegalArgumentException("The View connections per node need "
        + "to be greater than zero");
    }
    viewConns = conns;
    return this;
  }

  /**
   * Changes the node ordering for streaming connections.
   *
   * <p>To prevent the case where all streaming connections are opened on the
   * first node in the bootstrap list (and potentially overwhelm the server),
   * the default ordering mixes the nodes before attaching the connection. This
   * provides better distribution of the streaming connection. If old behavior
   * should be forced or there is a specific reason to fallback to the same
   * ordering, the setting can be changed.</p>
   *
   * <p>Defaults to
   * {@link CouchbaseConnectionFactory#DEFAULT_STREAMING_NODE_ORDER}.</p>
   *
   * @param order the ordering to use.
   * @return the builder instance for dsl-like chaining functionality.
   */
  public CouchbaseConnectionFactoryBuilder setStreamingNodeOrder(
    final CouchbaseNodeOrder order) {
    nodeOrder = order;
    return this;
  }

  /**
   * Enable the collection of runtime metrics.
   *
   * <p>Runtime metrics collection can be enabled to provide better insight
   * to what is happening inside the client library. Since the collection of
   * metrics comes with a performance penalty, it is disable by default and can
   * be enabled to a variety of different metric collection types. The debug
   * mode subsumes the performance mode and provides the deepest insight.</p>
   *
   * <p>Defaults to {@link CouchbaseConnectionFactory#DEFAULT_METRIC_TYPE}.</p>
   *
   * @param type the metric type to use (or disable).
   * @return the builder instance for dsl-like chaining functionality.
   */
  @Override
  public ConnectionFactoryBuilder setEnableMetrics(final MetricType type) {
    metricType = type;
    return this;
  }

  /**
   * Allows to provide a custom {@link MetricCollector}.
   *
   * <p>If metrics should be collected in a different way, a custom collector
   * can be passed in which will then receive the metrics exposed by the client
   * library. This is considered advanced functionality and should normally
   * not be overriden.</p>
   *
   * @param collector the metric collector to use.
   * @return the builder instance for dsl-like chaining functionality.
   */
  @Override
  public ConnectionFactoryBuilder setMetricCollector(
    final MetricCollector collector) {
    this.collector = collector;
    return this;
  }

  /**
   * Set a custom {@link ExecutorService} to execute the listener callbacks and
   * other callback-type operations.
   *
   * <p>If the executor is created inside the client library, it is also shut
   * down automatically. If a custom one gets passed in, it is the
   * responsibility of the caller to shut it down since the client library does
   * not know who else is depending on the executor.</p>
   *
   * <p>Passing in a custom executor service is handy if it should be shared
   * across many {@link CouchbaseClient} instances or if the application already
   * uses a thread pool for long running tasks and it can be shared with the
   * client library.</p>
   *
   * <p>Defaults to a thread pool executor which can scale up to the number of
   * configured CPUs as exposed by the {@link Runtime#availableProcessors()}
   * setting.</p>
   *
   * @param executorService the ExecutorService to use.
   * @return the builder instance for dsl-like chaining functionality.
   */
  @Override
  public ConnectionFactoryBuilder setListenerExecutorService(
    final ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  /**
   * Sets a custom waiting time until authentication completes.
   *
   * <p>This setting does not need to be changed normally, but if the client
   * logs indicate that authentication to server nodes takes longer than
   * expected, it might pay off to increase the wait time. That said, in general
   * this indicates some odd behavior and should be looked into separately.</p>
   *
   * <p>Defaults to
   * {@link CouchbaseConnectionFactory#DEFAULT_AUTH_WAIT_TIME}.</p>
   *
   * @param authWaitTime the wait time in miliseconds.
   * @return the builder instance for dsl-like chaining functionality.
   */
  @Override
  public ConnectionFactoryBuilder setAuthWaitTime(final long authWaitTime) {
    this.authWaitTime = authWaitTime;
    return this;
  }

  /**
   * Build the {@link CouchbaseConnectionFactory} set up with the provided
   * settings.
   *
   * @param baseList list of seed nodes.
   * @param bucketName the name of the bucket.
   * @param pwd the password of the bucket.
   * @return a {@link CouchbaseConnectionFactory}.
   * @throws IOException if the connection could not be established.
   */
  public CouchbaseConnectionFactory buildCouchbaseConnection(
    final List<URI> baseList, final String bucketName, final String pwd)
    throws IOException {
    return new CouchbaseConnectionFactory(baseList, bucketName, pwd) {

      @Override
      public BlockingQueue<Operation> createOperationQueue() {
        return opQueueFactory == null ? super.createOperationQueue()
          : opQueueFactory.create();
      }

      @Override
      public BlockingQueue<Operation> createReadOperationQueue() {
        return readQueueFactory == null ? super.createReadOperationQueue()
          : readQueueFactory.create();
      }

      @Override
      public BlockingQueue<Operation> createWriteOperationQueue() {
        return writeQueueFactory == null ? super.createReadOperationQueue()
          : writeQueueFactory.create();
      }

      @Override
      public Transcoder<Object> getDefaultTranscoder() {
        return transcoder == null ? super.getDefaultTranscoder() : transcoder;
      }

      @Override
      public FailureMode getFailureMode() {
        return failureMode == null ? DEFAULT_FAILURE_MODE : failureMode;
      }

      @Override
      public HashAlgorithm getHashAlg() {
        return hashAlg == null ? DEFAULT_HASH : hashAlg;
      }

      @Override
      public Collection<ConnectionObserver> getInitialObservers() {
        return initialObservers;
      }

      @Override
      public OperationFactory getOperationFactory() {
        return opFact == null ? super.getOperationFactory() : opFact;
      }

      @Override
      public long getOperationTimeout() {
        return opTimeout == -1 ? super.getOperationTimeout() : opTimeout;
      }

      @Override
      public long getAuthWaitTime() {
        return authWaitTime == -1 ? super.getAuthWaitTime() : authWaitTime;
      }

      @Override
      public int getReadBufSize() {
        return readBufSize == -1 ? super.getReadBufSize() : readBufSize;
      }

      @Override
      public boolean isDaemon() {
        return isDaemon;
      }

      @Override
      public boolean shouldOptimize() {
        return false;
      }

      @Override
      public boolean useNagleAlgorithm() {
        return useNagle;
      }

      @Override
      public long getMaxReconnectDelay() {
        return maxReconnectDelay;
      }

      @Override
      public long getOpQueueMaxBlockTime() {
        return opQueueMaxBlockTime > -1 ? opQueueMaxBlockTime
          : super.getOpQueueMaxBlockTime();
      }

      @Override
      public int getTimeoutExceptionThreshold() {
        return timeoutExceptionThreshold;
      }

      @Override
      public long getMinReconnectInterval() {
        return reconnThresholdTimeMsecs;
      }

      @Override
      public long getObsPollInterval() {
        return obsPollInterval;
      }

      @Override
      public long getObsTimeout() {
        return obsTimeout;
      }

      @Override
      public int getViewTimeout() {
        return viewTimeout;
      }

      @Override
      public int getViewWorkerSize() {
        return viewWorkers;
      }

      @Override
      public int getViewConnsPerNode() {
        return viewConns;
      }

      @Override
      public CouchbaseNodeOrder getStreamingNodeOrder() {
        return nodeOrder;
      }

      @Override
      public MetricType enableMetrics() {
        return metricType == null ? super.enableMetrics() : metricType;
      }

      @Override
      public MetricCollector getMetricCollector() {
        return collector == null ? super.getMetricCollector() : collector;
      }

      @Override
      public ExecutorService getListenerExecutorService() {
        return executorService == null ? super.getListenerExecutorService() : executorService;
      }

      @Override
      public boolean isDefaultExecutorService() {
        return executorService == null;
      }

      @Override
      public AuthDescriptor getAuthDescriptor() {
        return authDescriptor == null ? super.getAuthDescriptor() : authDescriptor;
      }
    };  }


  /**
   * Build the {@link CouchbaseConnectionFactory} set up with the provided
   * settings.
   *
   * <p>This method is deprecated since the username does not need to be
   * provided - only the name of the bucket. Use the
   * {@link #buildCouchbaseConnection(java.util.List, String, String)} method
   * instead.</p>
   *
   * @param baseList list of seed nodes.
   * @param bucketName the name of the bucket.
   * @param usr the username.
   * @param pwd the password of the bucket.
   * @return a {@link CouchbaseConnectionFactory}.
   * @throws IOException if the connection could not be established.
   */
  @Deprecated
  public CouchbaseConnectionFactory buildCouchbaseConnection(
      final List<URI> baseList, final String bucketName, final String usr,
      final String pwd) throws IOException {
    return buildCouchbaseConnection(baseList, bucketName, pwd);
  }

  /**
   * Build the {@link CouchbaseConnectionFactory} set up with the provided
   * settings.
   *
   * <p>If this method is used, the seed nodes, bucket name and password are
   * picked from system properties instead of the actual method params. They
   * are:</p>
   *
   * <p>The following properties need to be set in order to bootstrap:
   *  - cbclient.nodes: ;-separated list of URIs
   *  - cbclient.bucket: name of the bucket
   *  - cbclient.password: password of the bucket
   * </p>
   *
   * @return a {@link CouchbaseConnectionFactory}.
   * @throws IOException if the connection could not be established.
   */
  public CouchbaseConnectionFactory buildCouchbaseConnection() throws IOException {
    return new CouchbaseConnectionFactory() {

      @Override
      public BlockingQueue<Operation> createOperationQueue() {
        return opQueueFactory == null ? super.createOperationQueue()
          : opQueueFactory.create();
      }

      @Override
      public BlockingQueue<Operation> createReadOperationQueue() {
        return readQueueFactory == null ? super.createReadOperationQueue()
          : readQueueFactory.create();
      }

      @Override
      public BlockingQueue<Operation> createWriteOperationQueue() {
        return writeQueueFactory == null ? super.createReadOperationQueue()
          : writeQueueFactory.create();
      }

      @Override
      public Transcoder<Object> getDefaultTranscoder() {
        return transcoder == null ? super.getDefaultTranscoder() : transcoder;
      }

      @Override
      public FailureMode getFailureMode() {
        return failureMode == null ? DEFAULT_FAILURE_MODE : failureMode;
      }

      @Override
      public HashAlgorithm getHashAlg() {
        return hashAlg == null ? DEFAULT_HASH : hashAlg;
      }

      @Override
      public Collection<ConnectionObserver> getInitialObservers() {
        return initialObservers;
      }

      @Override
      public OperationFactory getOperationFactory() {
        return opFact == null ? super.getOperationFactory() : opFact;
      }

      @Override
      public long getOperationTimeout() {
        return opTimeout == -1 ? super.getOperationTimeout() : opTimeout;
      }

      @Override
      public long getAuthWaitTime() {
        return authWaitTime == -1 ? super.getAuthWaitTime() : authWaitTime;
      }

      @Override
      public int getReadBufSize() {
        return readBufSize == -1 ? super.getReadBufSize() : readBufSize;
      }

      @Override
      public boolean isDaemon() {
        return isDaemon;
      }

      @Override
      public boolean shouldOptimize() {
        return false;
      }

      @Override
      public boolean useNagleAlgorithm() {
        return useNagle;
      }

      @Override
      public long getMaxReconnectDelay() {
        return maxReconnectDelay;
      }

      @Override
      public long getOpQueueMaxBlockTime() {
        return opQueueMaxBlockTime > -1 ? opQueueMaxBlockTime
          : super.getOpQueueMaxBlockTime();
      }

      @Override
      public int getTimeoutExceptionThreshold() {
        return timeoutExceptionThreshold;
      }

      @Override
      public long getMinReconnectInterval() {
        return reconnThresholdTimeMsecs;
      }

      @Override
      public CouchbaseNodeOrder getStreamingNodeOrder() {
        return nodeOrder;
      }

      @Override
      public long getObsPollInterval() {
        return obsPollInterval;
      }

      @Override
      public long getObsTimeout() {
        return obsTimeout;
      }

      @Override
      public int getViewTimeout() {
        return viewTimeout;
      }

      @Override
      public int getViewWorkerSize() {
        return viewWorkers;
      }

      @Override
      public int getViewConnsPerNode() {
        return viewConns;
      }

      @Override
      public MetricType enableMetrics() {
        return metricType == null ? super.enableMetrics() : metricType;
      }

      @Override
      public MetricCollector getMetricCollector() {
        return collector == null ? super.getMetricCollector() : collector;
      }

      @Override
      public ExecutorService getListenerExecutorService() {
        return executorService == null ? super.getListenerExecutorService() : executorService;
      }

      @Override
      public boolean isDefaultExecutorService() {
        return executorService == null;
      }

      @Override
      public AuthDescriptor getAuthDescriptor() {
        return authDescriptor == null ? super.getAuthDescriptor() : authDescriptor;
      }
    };
  }

  /**
   * Returns the currently set observe poll interval.
   *
   * @return the observe poll interval.
   */
  public long getObsPollInterval() {
    return obsPollInterval;
  }

  /**
   * Returns the currently set maximum observe poll cycles.
   *
   * @return the poll cycles.
   */
  @Deprecated
  public int getObsPollMax() {
    return obsPollMax;
  }

  /**
   * Returns the currently set observe timeout.
   *
   * @return the timeout setting.
   */
  public long getObsTimeout() {
    return obsTimeout;
  }

  /**
   * Returns the currently set view timeout.
   *
   * @return the view timeout.
   */
  public int getViewTimeout() {
    return viewTimeout;
  }

  /**
   * The currently configured number of view workers.
   *
   * @return view worker count.
   */
  public int getViewWorkerSize() {
    return viewWorkers;
  }

  /**
   * The currently configured number of maximum open view connections per node.
   *
   * @return the number of view connections.
   */
  public int getViewConnsPerNode() {
    return viewConns;
  }

  /**
   * Returns the currently configured authentication wait time.
   *
   * @return the auth wait time.
   */
  public long getAuthWaitTime() {
    return authWaitTime;
  }

  /**
   * Returns a potentially set configuration.
   *
   * <p>For internal use only.</p>
   * @return the vbucket config.
   */
  public Config getVBucketConfig() {
    return vBucketConfig;
  }

  /**
   * Returns the currently configured streaming node order.
   *
   * @return the streaming node order.
   */
  public CouchbaseNodeOrder getStreamingNodeOrder() {
    return nodeOrder;
  }
}
