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

import com.couchbase.client.http.AsyncConnectionManager;
import com.couchbase.client.protocol.views.HttpOperation;

import com.couchbase.client.vbucket.ConfigurationException;
import com.couchbase.client.vbucket.ConfigurationProvider;
import com.couchbase.client.vbucket.ConfigurationProviderHTTP;
import com.couchbase.client.vbucket.VBucketNodeLocator;
import com.couchbase.client.vbucket.config.Config;
import com.couchbase.client.vbucket.config.ConfigType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

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

  private final ConfigurationProvider configurationProvider;
  private final String bucket;
  private final String pass;

  public CouchbaseConnectionFactory(final List<URI> baseList,
      final String bucketName, final String password)
    throws IOException {
    // ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder(cf);
    for (URI bu : baseList) {
      if (!bu.isAbsolute()) {
        throw new IllegalArgumentException("The base URI must be absolute");
      }
    }
    bucket = bucketName;
    pass = password;
    configurationProvider =
        new ConfigurationProviderHTTP(baseList, bucketName, password);
  }

  public ViewNode createViewNode(InetSocketAddress addr,
      AsyncConnectionManager connMgr) {
    return new ViewNode(addr, connMgr,
        new LinkedBlockingQueue<HttpOperation>(opQueueLen),
        getOpQueueMaxBlockTime(), getOperationTimeout(), bucket, pass);
  }

  @Override
  public MemcachedConnection createConnection(
      List<InetSocketAddress> addrs) throws IOException {
    return new CouchbaseConnection(this, addrs, getInitialObservers());
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
      return new KetamaNodeLocator(nodes, getHashAlg());
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
      return configurationProvider.getBucketConfiguration(bucket).getConfig();
    } catch (ConfigurationException e) {
      return null;
    }
  }

  public ConfigurationProvider getConfigurationProvider() {
    return configurationProvider;
  }
}
