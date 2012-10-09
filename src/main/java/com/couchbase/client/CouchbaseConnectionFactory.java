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

import com.couchbase.client.http.AsyncConnectionManager;

import com.couchbase.client.vbucket.ConfigurationException;
import com.couchbase.client.vbucket.ConfigurationProvider;
import com.couchbase.client.vbucket.ConfigurationProviderHTTP;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.VBucketNodeLocator;
import com.couchbase.client.vbucket.config.Config;
import com.couchbase.client.vbucket.config.ConfigType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  public static final FailureMode DEFAULT_FAILURE_MODE = FailureMode.Retry;

  /**
   * Default hash algorithm.
   */
  public static final HashAlgorithm DEFAULT_HASH =
    DefaultHashAlgorithm.KETAMA_HASH;

  /**
   * Maximum length of the operation queue returned by this connection factory.
   */
  public static final int DEFAULT_OP_QUEUE_LEN = 16384;
  /**
   * Specify a default minimum reconnect interval of 1.1s.
   * This means that if a reconnect is needed, it won't try to reconnect
   * more frequently than 1.1s between tries.  The initial HTTP connections
   * under us take up to 500ms per request.
   */
  public static final long DEFAULT_MIN_RECONNECT_INTERVAL = 1100;

  protected volatile ConfigurationProvider configurationProvider;
  private String bucket;
  private String pass;
  private List<URI> storedBaseList;
  private static final Logger LOGGER =
    Logger.getLogger(CouchbaseConnectionFactory.class.getName());
  private volatile boolean needsReconnect;
  private AtomicBoolean doingResubscribe = new AtomicBoolean(false);
  private volatile long thresholdLastCheck = System.nanoTime();
  private AtomicInteger configThresholdCount = new AtomicInteger(0);
  private final int maxConfigCheck = 10; //maximum allowed checks before we
                                         // reconnect in a 10 sec interval
  private volatile long configProviderLastUpdateTimestamp;
  private long minReconnectInterval = DEFAULT_MIN_RECONNECT_INTERVAL;
  private ExecutorService resubExec = Executors.newSingleThreadExecutor();
  private long obsPollInterval = 100;
  private int obsPollMax = 400;

  public CouchbaseConnectionFactory(final List<URI> baseList,
      final String bucketName, String password)
    throws IOException {
    storedBaseList = new ArrayList<URI>();
    for (URI bu : baseList) {
      if (!bu.isAbsolute()) {
        throw new IllegalArgumentException("The base URI must be absolute");
      }
      storedBaseList.add(bu);
    }
    bucket = bucketName;
    if(password == null) {
      password = "";
    }
    pass = password;
    configurationProvider =
        new ConfigurationProviderHTTP(baseList, bucketName, password);
  }

  public ViewNode createViewNode(InetSocketAddress addr,
      AsyncConnectionManager connMgr) {
    return new ViewNode(addr, connMgr, opQueueLen,
        getOpQueueMaxBlockTime(), getOperationTimeout(), bucket, pass);
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
    return new ViewConnection(this, addrs, getInitialObservers());
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
      return new AuthDescriptor(new String[] { "PLAIN" },
              new PlainCallbackHandler(bucket, pass));
    } else {
      return null;
    }
  }

  public String getBucketName() {
    return bucket;
  }

  public Config getVBucketConfig() {
    try {
      // If we find the config provider associated with this bucket is
      // disconnected and thus stale, we simply replace the configuration
      // provider
      if (configurationProvider.getBucketConfiguration(bucket)
           .isNotUpdating()) {
        LOGGER.warning("Noticed bucket configuration to be disconnected, "
            + "will attempt to reconnect");
        setConfigurationProvider(new ConfigurationProviderHTTP(storedBaseList,
          bucket, pass));
      }
      return configurationProvider.getBucketConfiguration(bucket).getConfig();
    } catch (ConfigurationException e) {
      return null;
    }
  }

  public ConfigurationProvider getConfigurationProvider() {
    return configurationProvider;
  }

  protected void requestConfigReconnect(String bucketName, Reconfigurable rec) {
    configurationProvider.markForResubscribe(bucketName, rec);
    needsReconnect = true;
  }

  void setConfigurationProvider(ConfigurationProvider configProvider) {
    this.configProviderLastUpdateTimestamp = System.currentTimeMillis();
    this.configurationProvider = configProvider;
  }

  void setMinReconnectInterval(long reconnIntervalMsecs) {
    this.minReconnectInterval = reconnIntervalMsecs;
  }


  void checkConfigUpdate() {
    if (needsReconnect || pastReconnThreshold()) {

      // past the threshold, but now we need to give the reconnect attempt
      // a bit of time to complete setting itself up
      long now = System.currentTimeMillis();
      long intervalWaited = now - this.configProviderLastUpdateTimestamp;
      if (intervalWaited < this.getMinReconnectInterval()) {
        LOGGER.log(Level.FINE, "Ignoring config update check. Only {0}ms out"
                + " of a threshold of {1}ms since last update.",
                new Object[]{intervalWaited, this.getMinReconnectInterval()});
        return;
      }
      if (doingResubscribe.compareAndSet(false, true)) {
        resubConfigUpdate();
      } else {
        LOGGER.log(Level.CONFIG, "Duplicate resubscribe for config updates"
          + " suppressed.");
      }

    } else {
      LOGGER.log(Level.FINE, "No reconnect required, though check requested."
              + " Current config check is {0} out of a threshold of {1}.",
              new Object[]{configThresholdCount, maxConfigCheck});
    }
  }

  private synchronized void resubConfigUpdate() {
    // synchronized shouldn't be needed here, just being defensive
    LOGGER.log(Level.INFO, "Attempting to resubscribe for cluster config"
      + " updates.");
    resubExec.execute(new Resubscriber());


  }

  /**
   * Check to see if we've gone beyond 10 secs since last reconnection
   * attempt.
   *
   * @return true if it's been more than 10 seconds since we last tried
   *         to connect
   */
  private boolean pastReconnThreshold() {
    long currentTime = System.nanoTime();
    if (currentTime - thresholdLastCheck > 100000000) { //if longer than 10 sec
      configThresholdCount.set(0); // it's been more than 10 sec since last
                                // tried, so don't try again just yet.
    }
    configThresholdCount.incrementAndGet();
    thresholdLastCheck = currentTime;

    if (configThresholdCount.get() >= maxConfigCheck) {
      return true;
    }
    return false;
  }

  /**
   * Will return the minimum reconnect interval in milliseconds.
   *
   * @return the minReconnectInterval
   */
  public long getMinReconnectInterval() {
    return minReconnectInterval;
  }

  long getObsPollInterval() {
    return obsPollInterval;
  }

  int getObsPollMax() {
    return obsPollMax;
  }

  private class Resubscriber implements Runnable {

    public void run() {
      String threadNameBase = "couchbase cluster resubscriber - ";
      Thread.currentThread().setName(threadNameBase + "running");
      LOGGER.log(Level.CONFIG, "Starting resubscription for bucket {0}",
        bucket);
      LOGGER.log(Level.CONFIG, "Resubscribing for {0} using base list {1}",
        new Object[]{bucket, storedBaseList});
      ConfigurationProvider oldConfigProvider = configurationProvider;
      setConfigurationProvider(new ConfigurationProviderHTTP(storedBaseList,
        bucket, pass));
      configurationProvider.finishResubscribe();
    // cleanup the old config provider
      if (null != oldConfigProvider) {
        oldConfigProvider.shutdown();
      }

      if (!doingResubscribe.compareAndSet(true, false)) {
        assert false : "Could not reset from doing a resubscribe.";
      }
      Thread.currentThread().setName(threadNameBase + "complete");
    }
  }

}
