package com.couchbase.client;

import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.TestConfig;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.VBucketAware;
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

  private static boolean isCCCPAware;

  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
    + ":8091/pools";

  /**
   * Set a flag to see if the target cluster is CCCP ready and it makes sense
   * to run the tests.
   */
  @BeforeClass
  public static void checkCCCPAwareness() throws Exception {
    CouchbaseClient client = new CouchbaseClient(
      Arrays.asList(new URI(SERVER_URI)),
      "default",
      ""
    );

    ArrayList<String> versions = new ArrayList<String>(
      client.getVersions().values());
    if (versions.size() > 0) {
      Version version = new Version(versions.get(0));
      isCCCPAware = version.greaterOrEqualThan(2, 5, 0);
    }

    client.shutdown();
  }

  @Test
  public void shouldGetUpdatedVBucketMap() throws Exception {
    if (!isCCCPAware) {
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
    assertEquals(1, connection.getNewConfigCount());

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

  /**
   * Simple helper class to detect and compare the cluster node version.
   */
  static class Version {
    private final int major;
    private final int minor;
    private final int bugfix;

    public Version(String raw) {
      String[] tokens = raw.replaceAll("_.*$", "").split("\\.");
      major = Integer.parseInt(tokens[0]);
      minor = Integer.parseInt(tokens[1]);
      bugfix = Integer.parseInt(tokens[2]);
    }

    public boolean greaterOrEqualThan(int major, int minor, int bugfix) {
      return this.major >= major
        && this.minor >= minor
        && this.bugfix >= bugfix;
    }
  }

}
