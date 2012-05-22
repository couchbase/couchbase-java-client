/**
 * Copyright (C) 2009-2011 Couchbase, Inc.
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

  private volatile ConfigurationProvider configurationProvider;
  private final String bucket;
  private final String pass;
  private final List<URI> storedBaseList;
  private static final Logger LOGGER =
    Logger.getLogger(CouchbaseConnectionFactory.class.getName());
  private boolean needsReconnect;
  private volatile long thresholdLastCheck = System.nanoTime();
  private volatile int configThresholdCount = 0;
  private final int maxConfigCheck = 10; //maximum checks in 10 sec interval

  public CouchbaseConnectionFactory(final List<URI> baseList,
      final String bucketName, final String password)
    throws IOException {
    storedBaseList = new ArrayList<URI>();
    for (URI bu : baseList) {
      if (!bu.isAbsolute()) {
        throw new IllegalArgumentException("The base URI must be absolute");
      }
      storedBaseList.add(bu);
    }
    bucket = bucketName;
    pass = password;
    configurationProvider =
        new ConfigurationProviderHTTP(baseList, bucketName, password);
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
        configurationProvider = new ConfigurationProviderHTTP(storedBaseList,
            bucket, pass);
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

  void checkConfigUpdate() {
    if (needsReconnect || pastReconnThreshold()) {
      LOGGER.log(Level.INFO,
                 "Attempting to resubscribe for cluster config updates.");
      configurationProvider =
        new ConfigurationProviderHTTP(storedBaseList, bucket, pass);
      configurationProvider.finishResubscribe();
    } else {
      LOGGER.log(Level.WARNING, "No reconnect required, though check requested."
              + " Current config check is {0} out of a threshold of {1}.",
              new Object[]{configThresholdCount, maxConfigCheck});
    }
  }

  private boolean pastReconnThreshold() {
    long currentTime = System.nanoTime();
    if (currentTime - thresholdLastCheck > 100000000) { //if longer than 10 sec
      configThresholdCount = 0;
    }
    configThresholdCount++;
    thresholdLastCheck = currentTime;

    if (configThresholdCount >= maxConfigCheck) {
      return true;
    }
    return false;
  }

}
