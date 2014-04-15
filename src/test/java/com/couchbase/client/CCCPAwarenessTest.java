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

import com.couchbase.client.clustermanager.BucketType;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.TestConfig;
import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.VBucketAware;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for features added as part of the CCCP project.
 */
public class CCCPAwarenessTest {

  private static final Logger LOGGER =
    LoggerFactory.getLogger(CCCPAwarenessTest.class);

  private static boolean isCCCPAware;

  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
    + ":8091/pools";

  private static CouchbaseClient client;

  @BeforeClass
  public static void beforeTest() throws Exception {
    final List<URI> uris = Arrays.asList(URI.create("http://"
      + TestConfig.IPV4_ADDR + ":8091/pools"));

    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 1, true);

    BucketTool.FunctionCallback callback = new BucketTool.FunctionCallback() {
      @Override
      public void callback() throws Exception {
        client = new CouchbaseClient(new CouchbaseConnectionFactory(uris, "default", ""));
      }

      @Override
      public String success(long elapsedTime) {
        return "Client Initialization took " + elapsedTime + "ms";
      }
    };
    bucketTool.poll(callback);
    bucketTool.waitForWarmup(client);

    ArrayList<String> versions = new ArrayList<String>(
      client.getVersions().values());
    if (versions.size() > 0) {
      CbTestConfig.Version version = new CbTestConfig.Version(versions.get(0));
      isCCCPAware = version.isCarrierConfigAware();
    }

    client.shutdown();
  }

  @Test
  public void shouldGetUpdatedVBucketMap() throws Exception {
    if (!isCCCPAware) {
      LOGGER.info("Skipping Test because cluster is not CCCP aware.");
      return;
    }

    // Setup
    TestingCouchbaseConnectionFactory factory =
      new TestingCouchbaseConnectionFactory(Arrays.asList(
        new URI(SERVER_URI)), "default", ""
      );
    CouchbaseClient client = new CouchbaseClient(factory);
    TestingCouchbaseConnection connection = factory.getConnection();

    // Send crafted operation to wrong vbucket
    String key = "key";
    final CountDownLatch latch = new CountDownLatch(1);
    GetOperation op = factory.getOperationFactory().get(key, new GetOperation.Callback() {
      @Override
      public void gotData(String key, int flags, byte[] data) {
      }

      @Override
      public void receivedStatus(OperationStatus status) {
      }

      @Override
      public void complete() {
        latch.countDown();
      }
    });
    // Use a VBucket Id that will never be true (32k).
    ((VBucketAware) op).setVBucket(key, Short.MAX_VALUE);
    connection.addOperation(connection.getLocator().getPrimary(key), op);

    // Observe that new config arrives and gets applied
    assertTrue(latch.await(30, TimeUnit.SECONDS));
    assertTrue(connection.getNewConfigCount() >= 1);

    // Run operations to see that it still works and nothing is broken
    for (int i = 0; i < 100; i++) {
      assertTrue(client.set("key::" + i, 0, "value").get());
      assertEquals("value", client.get("key::" + i));
    }
  }

  /**
   * Mock factory to expose and override different methods for testing.
   */
  static class TestingCouchbaseConnectionFactory
    extends CouchbaseConnectionFactory {

    private TestingCouchbaseConnection connection;

    TestingCouchbaseConnectionFactory(List<URI> baseList, String bucketName,
      String password) throws IOException {
      super(baseList, bucketName, password);
    }

    @Override
    public MemcachedConnection createConnection(List<InetSocketAddress> addrs)
      throws IOException {
      connection = new TestingCouchbaseConnection(getReadBufSize(), this, addrs,
        getInitialObservers(), getFailureMode(), getOperationFactory());
      return connection;
    }

    public TestingCouchbaseConnection getConnection() {
      return connection;
    }
  }

  /**
   * Mock CouchbaseConnection to expose different methods for testing.
   */
  static class TestingCouchbaseConnection extends CouchbaseConnection {
    TestingCouchbaseConnection(int bufSize, CouchbaseConnectionFactory f,
      List<InetSocketAddress> a, Collection<ConnectionObserver> obs,
      FailureMode fm, OperationFactory opfactory) throws IOException {
      super(bufSize, f, a, obs, fm, opfactory);
    }

    private int newConfigCount;

    @Override
    public void addOperation(MemcachedNode node, Operation o) {
      super.addOperation(node, o);
    }

    @Override
    protected void handleRetryInformation(byte[] retryMessage) {
      super.handleRetryInformation(retryMessage);
      newConfigCount++;
    }

    public int getNewConfigCount() {
      return newConfigCount;
    }
  }

}
