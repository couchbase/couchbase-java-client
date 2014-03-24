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

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.FailureMode;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.metrics.MetricCollector;
import net.spy.memcached.metrics.MetricType;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.transcoders.Transcoder;

/**
 * CouchbaseConnectionFactoryBuilder.
 *
 */

public class CouchbaseConnectionFactoryBuilder extends ConnectionFactoryBuilder {

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
  private static final Logger LOGGER =
    Logger.getLogger(CouchbaseConnectionFactoryBuilder.class.getName());
  protected MetricType metricType = null;
  protected MetricCollector collector = null;
  protected ExecutorService executorService = null;
  protected long authWaitTime = -1;

  public Config getVBucketConfig() {
    return vBucketConfig;
  }

  public void setVBucketConfig(Config config) {
    this.vBucketConfig = config;
  }

  public void setReconnectThresholdTime(long time, TimeUnit unit) {
    reconnThresholdTimeMsecs = TimeUnit.MILLISECONDS.convert(time, unit);
  }

  /**
   * Set the interval between observe polls in milliseconds.
   *
   * @param interval the interval in milliseconds.
   * @return the builder for proper chaining.
   */
  public CouchbaseConnectionFactoryBuilder setObsPollInterval(long interval) {
    obsPollInterval = interval;
    return this;
  }

  /**
   * Set the timeout for observe-based operations in milliseconds.
   *
   * This timeout is always used when PersistTo or ReplicateTo overloaded
   * methods are used, instead of the default operation timeout.
   *
   * @param timeout the timeout in milliseconds.
   * @return the builder for proper chaining.
   */
  public CouchbaseConnectionFactoryBuilder setObsTimeout(long timeout) {
    obsTimeout = timeout;
    return this;
  }

  /**
   * Sets the maximum number of observe polls.
   *
   * Do not use this method directly, but instead use a combination of
   * {@link #setObsPollInterval(long)} and {@link #setObsTimeout(long)}.
   *
   * @param maxPoll the maximum number of polls to run before giving up.
   * @return the builder for proper chaining.
   */
  @Deprecated
  public CouchbaseConnectionFactoryBuilder setObsPollMax(int maxPoll) {
    obsPollMax = maxPoll;
    return this;
  }

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

  public CouchbaseConnectionFactoryBuilder setViewWorkerSize(int workers) {
    if (workers < 1) {
      throw new IllegalArgumentException("The View worker size needs to be "
        + "greater than zero.");
    }

    viewWorkers = workers;
    return this;
  }

  public CouchbaseConnectionFactoryBuilder setViewConnsPerNode(int conns) {
    if (conns < 1) {
      throw new IllegalArgumentException("The View connections per node need "
        + "to be greater than zero");
    }
    viewConns = conns;
    return this;
  }

  /**
   * Set the streaming connection node ordering.
   *
   * @param order the ordering to use.
   * @return the builder for chaining.
   */
  public CouchbaseConnectionFactoryBuilder setStreamingNodeOrder(CouchbaseNodeOrder order) {
    nodeOrder = order;
    return this;
  }

  /**
   * Enable or disable metric collection.
   *
   * @param type the metric type to use (or disable).
   */
  @Override
  public ConnectionFactoryBuilder setEnableMetrics(MetricType type) {
    metricType = type;
    return this;
  }

  /**
   * Set a custom {@link MetricCollector}.
   *
   * @param collector the metric collector to use.
   */
  @Override
  public ConnectionFactoryBuilder setMetricCollector(MetricCollector collector) {
    this.collector = collector;
    return this;
  }

  /**
   * Set a custom {@link ExecutorService} to execute the listener callbacks.
   *
   * @param executorService the ExecutorService to use.
   */
  @Override
  public ConnectionFactoryBuilder setListenerExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  @Override
  public ConnectionFactoryBuilder setAuthWaitTime(long authWaitTime) {
    this.authWaitTime = authWaitTime;
    return this;
  }

  /**
   * Get the CouchbaseConnectionFactory set up with the provided parameters.
   * Note that a CouchbaseConnectionFactory requires the failure mode is set
   * to retry, and the locator type is discovered dynamically based on the
   * cluster you are connecting to. As a result, these values will be
   * overridden upon calling this function.
   *
   * @param baseList a list of URI's that will be used to connect to the cluster
   * @param bucketName the name of the bucket to connect to, also used for
   * username
   * @param pwd the password for the bucket
   * @return a CouchbaseConnectionFactory object
   * @throws IOException
   */
  public CouchbaseConnectionFactory buildCouchbaseConnection(
      final List<URI> baseList, final String bucketName, final String pwd)
    throws IOException {
    return this.buildCouchbaseConnection(baseList, bucketName, bucketName, pwd);
  }


  /**
   * Get the CouchbaseConnectionFactory set up with the provided parameters.
   * Note that a CouchbaseConnectionFactory requires the failure mode is set
   * to retry, and the locator type is discovered dynamically based on the
   * cluster you are connecting to. As a result, these values will be
   * overridden upon calling this function.
   *
   * @param baseList a list of URI's that will be used to connect to the cluster
   * @param bucketName the name of the bucket to connect to
   * @param usr the username for the bucket
   * @param pwd the password for the bucket
   * @return a CouchbaseConnectionFactory object
   * @throws IOException
   */
  public CouchbaseConnectionFactory buildCouchbaseConnection(
      final List<URI> baseList, final String bucketName, final String usr,
      final String pwd) throws IOException {
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

    };
  }

  /**
   * Get the CouchbaseConnectionFactory set up with parameters provided by
   * system properties.
   *
   * @return the created factory.
   * @throws IOException
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
    };
  }

  /**
   * @return the obsPollInterval
   */
  public long getObsPollInterval() {
    return obsPollInterval;
  }

  /**
   * @return the obsPollMax
   */
  public int getObsPollMax() {
    return obsPollMax;
  }

  /**
   * @return the observe timeout
   */
  public long getObsTimeout() {
    return obsTimeout;
  }

  /**
   * @return the viewTimeout
   */
  public int getViewTimeout() {
    return viewTimeout;
  }

  public int getViewWorkerSize() {
    return viewWorkers;
  }

  public int getViewConnsPerNode() {
    return viewConns;
  }

  public long getAuthWaitTime() {
    return authWaitTime;
  }
}
