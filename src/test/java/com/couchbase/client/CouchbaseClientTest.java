/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

import com.couchbase.client.BucketTool.FunctionCallback;
import com.couchbase.client.clustermanager.BucketType;

import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.BinaryClientTest;
import net.spy.memcached.CASValue;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import net.spy.memcached.TestConfig;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;

import org.junit.Ignore;
import static org.junit.Assume.assumeTrue;

/**
 * A CouchbaseClientTest.
 */
public class CouchbaseClientTest extends BinaryClientTest {

  // Constant for empty string
  private static final String EMPTY = "";

  /**
   * Initialises the client, deletes all the buckets
   * and creates a new default bucket.
   */
  @Override
  protected void initClient() throws Exception {
    final List<URI> uris = Arrays.asList(URI.create("http://"
        + TestConfig.IPV4_ADDR + ":8091/pools"));

    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 1, true);

    BucketTool.FunctionCallback callback = new FunctionCallback() {
      @Override
      public void callback() throws Exception {
        initClient(new CouchbaseConnectionFactory(uris, "default", EMPTY));
      }

      @Override
      public String success(long elapsedTime) {
        return "Client Initialization took " + elapsedTime + "ms";
      }
    };
    bucketTool.poll(callback);
    bucketTool.waitForWarmup(client);
  }

  /**
   * Gets the server IP address to connect to.
   */
  @Override
  protected String getExpectedVersionSource() {
    if (TestConfig.IPV4_ADDR.equals("127.0.0.1")) {
      return "/127.0.0.1:11210";
    }
    return TestConfig.IPV4_ADDR + ":11210";
  }

  /**
   * Creating a new instance of the couchbase client object.
   */
  @Override
  protected void initClient(ConnectionFactory cf) throws Exception {
    client = new CouchbaseClient((CouchbaseConnectionFactory) cf);
  }

  /**
   * Check the available servers, a client is able to connect to.
   *
   * @pre First let the client warm up and sleeps for 10ms and then
   * deletes this bucket. Another default bucket is then created,
   * this time of the type - Memcached. This bucket is also deleted,
   * after a delay of 1000ms of its creation.
   * @post Creation and deletion of the default buckets succeeds
   * without exception.
   * @throws Exception
   */
  @Override
  public void testAvailableServers() {
    final Collection<SocketAddress> servers = client.getAvailableServers();
    assertTrue("There are no servers available.", servers.size() > 0);
  }

  /**
   * Attempt to add a key which already exists.
   * @pre Here, first the key is set and then added.
   * @post Upon calling add, the transaction doesn't
   * succeed.
   *
   * @throws Exception
   */
  public void testAddKeyExists() throws Exception {
    assertNull(client.get("test1"));
    assertFalse(client.asyncGet("test1").getStatus().isSuccess());
    assertTrue(client.set("test1", 5, "test1value").get());
    assertEquals("test1value", client.get("test1"));
    assertFalse(client.add("test1", 5, "ignoredvalue").get());
    assertFalse(client.add("test1", 5, "ignoredvalue").getStatus().isSuccess());
    // Should return the original value
    assertEquals("test1value", client.get("test1"));
  }

  /**
   * Attempt to replace a key which doesn't exist.
   * @post The transaction doesn't succeed and returns
   * 'Not Found'
   *
   * @throws Exception
   */
  public void testReplaceKeyNoExist() throws Exception {
    OperationFuture<Boolean> replace = client.replace("t1", 5, "replacevalue");
    OperationStatus status = replace.getStatus();
    assertFalse(status.isSuccess());
    assertEquals("Not found", status.getMessage());
  }

  /**
   * Attempt to set a key which is blank.
   * @post The transaction doesn't succeed
   * and returns an error that the key
   * must not be empty.
   *
   * @throws Exception
   */
  public void testSetInvalidKeyBlank() {
    try {
      client.set(EMPTY, 5, EMPTY);
    } catch (IllegalArgumentException e) {
      assertEquals("Key must contain at least one character.", e.getMessage());
    }
  }

  /**
   * Attempt to set a value which is blank.
   * @pre The blank value is successfully set.
   * @post The blank value is retrieved successfully.
   *
   * @throws Exception
   */
  public void testGetValBlank() throws Exception {
    assertTrue(client.set("blankValue", 0, EMPTY).get());
    assertEquals(EMPTY, client.get("blankValue"));
  }

  /**
   * Check the graceful shutdown of the client.
   *
   * @pre Once the client shuts down, wait for 5 seconds
   * and then initialise it again without recreating the bucket.
   * Add keys to the default bucket and then try to retrieve the
   * keys in bulk from the server.
   * @post Test succeeds to prove that the server had shut
   * down properly without tampering its state and the buckets.
   * @throws Exception
   */
  @Override
  public void testGracefulShutdown() throws Exception {
    for (int i = 0; i < 1000; i++) {
      client.set("t" + i, 10, i);
    }
    assertTrue("Couldn't shut down within five seconds",
        client.shutdown(5, TimeUnit.SECONDS));
    Thread.sleep(5000);
    // Initialize without recreating a bucket
    initClient(new CouchbaseConnectionFactory(
          Arrays.asList(URI.create("http://"
          + TestConfig.IPV4_ADDR + ":8091/pools")), "default", EMPTY));
    Collection<String> keys = new ArrayList<String>();
    for (int i = 0; i < 1000; i++) {
      keys.add("t" + i);
    }
    Map<String, Object> m = client.getBulk(keys);
    assertEquals(1000, m.size());
    for (int i = 0; i < 1000; i++) {
      assertEquals(i, m.get("t" + i));
    }
  }

  /**
   * Check the number of vbuckets using the couchbase client.
   *
   * @pre Using the client instance, get the number of vbuckets.
   * @post Test verifies and asserts that the number of vbuckets are
   * in a power of two.
   * @throws Exception
   */
  public void testNumVBuckets() throws Exception {
    // Check for power of two values
    int num = ((CouchbaseClient)client).getNumVBuckets();
    assertTrue("NumVBuckets has to be a power of two", (num & -num)== num);
  }

  /**
   * Check the versions of the client using the couchbase client instance.
   *
   * @pre Using the client instance, get the versions of the client.
   * @post Test asserts the size of available servers and
   * the size of versions inside the server.
   */
  @Override
  public void testGetVersions() {
    Map<SocketAddress, String> vs = ((CouchbaseClient)client).getVersions();
    System.out.println(vs);
    assertEquals(client.getAvailableServers().size(), vs.size());
  }

  /**
   * Check the server statistics.
   *
   * @pre Using the client instance, get the stats of the client.
   * @post Test asserts the size of available servers and
   * the size of stats inside the server and asserts
   * true if the curr_items exits in the server statistics.
   */
  @Override
  public void testGetStats() throws Exception {
    Map<SocketAddress, Map<String, String>> stats =
        ((CouchbaseClient)client).getStats();
    assertEquals(client.getAvailableServers().size(), stats.size());
    Map<String, String> oneStat = stats.values().iterator().next();
    assertTrue(oneStat.containsKey("curr_items"));
  }

  /**
   * Get a value and update the expiration time for a given key.
   *
   * @pre Call the simple get and touch operation for the same key
   * and update the expiry time. Wait for 1.3 seconds and
   * then try to get the value of that key. Wait for another
   * 2 seconds and then call get and touch operation on the same key.
   * @post Test succeeds to call the get and touch operation for the first
   * time but by the second time key has expired.
   *
   * @throws Exception
   */
  public void testGATTimeout() throws Exception {
    assertNull(client.get("gatkey"));
    assert client.set("gatkey", 1, "gatvalue").get().booleanValue();
    assert client.getAndTouch("gatkey", 2).getValue().equals("gatvalue");
    Thread.sleep(1300);
    assert client.get("gatkey").equals("gatvalue");
    Thread.sleep(2000);
    assertNull(client.getAndTouch("gatkey", 3));
  }

  /**
   * Update the expire time of an item in different time intervals.
   *
   * @pre Call the simple set and touch operations for a key
   * and update the expiry time. Wait for 1.3 seconds and
   * then try to get the value of that key. Wait for another
   * 2 seconds and then call touch operation on the same key.
   * @post Test succeeds to call the touch operation for the first
   * time but the second time a boolean false is returned
   * as the key has expired by then.
   *
   * @throws Exception
   */
  public void testTouchTimeout() throws Exception {
    assertNull(client.get("touchkey"));
    assert client.set("touchkey", 1, "touchvalue").get().booleanValue();
    assert client.touch("touchkey", 2).get().booleanValue();
    Thread.sleep(1300);
    assert client.get("touchkey").equals("touchvalue");
    Thread.sleep(2000);
    assertFalse(client.touch("touchkey", 3).get().booleanValue());
  }

  /**
   * Get the value for a key, lock the key from changes.
   *
   * @pre Call the simple set operation for a key value pair
   * and then call the getAndLock operation to prevent any changes.
   * Wait for 2 seconds and then try to set a new value of that key.
   * Wait for another 2 seconds and then again call the set operation.
   * @post Test succeeds to return an assert that the key has been locked.
   * @throws Exception
   */
  public void testSimpleGetl() throws Exception {
    assertNull(client.get("getltest"));
    client.set("getltest", 0, "value");
    ((CouchbaseClient)client).getAndLock("getltest", 3);
    Thread.sleep(2000);
    assert !client.set("getltest", 1, "newvalue").get().booleanValue()
      : "Key wasn't locked for the right amount of time";
    Thread.sleep(2000);
    assert client.set("getltest", 1, "newvalue").get().booleanValue()
      : "Key was locked for too long";
  }

  /**
   * Get the value for a key, lock the key from changes and then unlock.
   *
   * @pre Call the simple set operation for a key value pair
   * and then call the getAndLock operation to prevent any changes.
   * Unlock the key and then again try to set the value.
   * @post Test succeeds to return an assert that the key has been unlocked.
   * @throws Exception
   */
  public void testSimpleUnlock() throws Exception {
    assertNull(client.get("getunltest"));
    client.set("getunltest", 0, "value");
    CASValue<Object> casv =
        ((CouchbaseClient)client).getAndLock("getunltest", 6);
    assert !client.set("getunltest", 1, "newvalue").get().booleanValue()
      : "Key wasn't locked for the right amount of time";
    ((CouchbaseClient)client).unlock("getunltest", casv.getCas());
    assert client.set("getunltest", 1, "newvalue").get().booleanValue()
      : "Key was locked for too long";
  }

  /**
   * Test observe functionality.
   *
   * @pre The replication and persisting of key value pair for the
   * set, replace, delete add(both non persistent and the persistent)
   * operations.
   * @post Test succeeds to return an assert that the key was persisted
   * to master or not. In case of the add operation it also returns
   * whether the key has been replicated to other nodes.
   *
   * @throws Exception
   */
  public void testObserve() throws Exception {
    assertNull(client.get("observetest"));
    OperationFuture<Boolean> setOp =
            (((CouchbaseClient)client).set("observetest", 0, "value",
                PersistTo.MASTER));
    assertTrue("Key set was not persisted to master: "
      + setOp.getStatus().getMessage(), setOp.get());

    OperationFuture<Boolean> replaceOp =
            (((CouchbaseClient)client).replace("observetest", 0, "value",
                PersistTo.MASTER));
    assertTrue("Key replace was not persisted to master: "
      + replaceOp.getStatus().getMessage(), replaceOp.get());

    OperationFuture<Boolean> deleteOp =
            (((CouchbaseClient)client).delete("observetest",
                PersistTo.MASTER));
    assertTrue("Key was not deleted on master: "
      + deleteOp.getStatus().getMessage(), deleteOp.get());

    OperationFuture<Boolean> addOp =
            (((CouchbaseClient)client).add("observetest", 0, "value",
                PersistTo.MASTER, ReplicateTo.ZERO));
    assertTrue("Key add was not persisted to master : "
      + addOp.getStatus().getMessage(), addOp.get());

    // Don't run this test when only one server is in the cluster, as it will
    // fail. Those tests can be found below.
    if(client.getAvailableServers().size() < 2) {
      return;
    }

    OperationFuture<Boolean> noPersistOp =
            (((CouchbaseClient)client).add("nopersisttest", 0, "value",
              ReplicateTo.ONE));
    assertTrue("Key add was not correctly replicated: "
      + noPersistOp.getStatus().getMessage(), noPersistOp.get());
  }

  /**
   * Get the key status from the client in the operation future object.
   *
   * @pre set a key value pair to db using the client. On the client object
   * call getKeyStats. This will return a future object which has a status
   * associated with it.
   * @post Test asserts true if the status is success. Also it asserts
   * true if all the parameters related to the key are available.
   *
   * <p>Note that since Couchbase Server 2.1, two stats are not exported
   * anymore, this is why the test cases branches and checks for those
   * two states individually.</p>
   *
   * @throws Exception
   */
  public void testStatsKey() throws Exception {
    OperationFuture<Boolean> f = client.set("key", 0, "value");
    System.err.println(f.getStatus().getMessage());

    OperationFuture<Map<String, String>> future =
        ((CouchbaseClient)client).getKeyStats("key");
    assertTrue(future.getStatus().isSuccess());
    Map<String, String> stats = future.get();
    assertTrue(stats.size() == 5 || stats.size() == 7);
    assertTrue(stats.containsKey("key_vb_state"));
    assertTrue(stats.containsKey("key_flags"));
    assertTrue(stats.containsKey("key_is_dirty"));
    assertTrue(stats.containsKey("key_cas"));
    assertTrue(stats.containsKey("key_exptime"));
    if(stats.size() == 7) {
      assertTrue(stats.containsKey("key_data_age"));
      assertTrue(stats.containsKey("key_last_modification_time"));
    }

    future = ((CouchbaseClient)client).getKeyStats("key1");
    assertFalse(future.getStatus().isSuccess());
  }

  /**
   * Tests if passing NULL for ReplicateTo and/or PersistTo
   * correctly works and does not throw an exception.
   *
   * @pre Call add, set and replace operations on the client.
   * @post Test succeeds and success = true is returned
   *
   * @throws Exception
   */
  public void testNullObserve() throws Exception {
    boolean success;
    try {
      success = true;
      OperationFuture<Boolean> nullcheckOp =
        (((CouchbaseClient)client).add("nullcheck", 0, "value", null, null));
      nullcheckOp.get();
      nullcheckOp =
        (((CouchbaseClient)client).set("nullcheck", 0, "value1", null, null));
      nullcheckOp.get();
      nullcheckOp = (((CouchbaseClient)client).replace("nullcheck", 0,
          "value1", null, null));
      nullcheckOp.get();
    } catch(NullPointerException ex) {
      success = false;
    }
    assertTrue(success);
  }

  /**
   * Tests observe and persist to more nodes than configured.
   *
   * @pre Call set operations on the client object
   * with persist nodes more than the server size.
   * @post Asserts false.
   *
   * @throws Exception
   */
  public void testPersistToMoreThanConf() throws Exception {
    CouchbaseClient cb = (CouchbaseClient) client;
    assumeTrue(client.getAvailableServers().size() <= 3);

    OperationFuture<Boolean> invalid = cb.set("something", 0,
      "to_store", PersistTo.FOUR);
    assertFalse(invalid.get());
    assertTrue(invalid.getStatus().getMessage().matches(
      "Requested persistence to 4 node\\(s\\), but only \\d are available\\."));
  }

  /**
   * Tests observe and replicate to more nodes than configured.
   *
   * @pre Call set operations on the client object
   * with replicate nodes more than the server size.
   * @post Asserts false.
   *
   * @throws Exception
   */
  public void testReplicateToMoreThanConf() throws Exception {
    CouchbaseClient cb = (CouchbaseClient) client;
    assumeTrue(client.getAvailableServers().size() <= 3);

    OperationFuture<Boolean> invalid = cb.set("something", 0,
      "to_store", ReplicateTo.THREE);
    assertFalse(invalid.get());
    assertTrue(invalid.getStatus().getMessage().matches(
      "Requested replication to 3 node\\(s\\), but only \\d are available\\."));
  }

  /**
   * Tests observe and persist with zero nodes.
   * @pre Call set operations on the client object
   * with persist nodes as 0.
   * @post Asserts true.
   *
   * @throws Exception
   */
  public void testPersistZeroNodes() throws Exception {
    CouchbaseClient cb = (CouchbaseClient) client;
    OperationFuture<Boolean> success = cb.set("something", 0,
      "to_store", PersistTo.ZERO);
    assertTrue(success.get());
    String expected = "OK";
    assertEquals(expected, success.getStatus().getMessage());
    assertNotNull(cb.get("something"));
  }

  /**
   * Tests observe and replicate with zero nodes.
   * @pre Call set operations on the client object
   * with replicate nodes as 0.
   * @post Asserts true.
   *
   * @throws Exception
   */
  public void testReplicateZeroNodes() throws Exception {
    CouchbaseClient cb = (CouchbaseClient) client;
    OperationFuture<Boolean> success = cb.set("something", 0,
      "to_store", ReplicateTo.ZERO);
    assertTrue(success.get());
    String expected = "OK";
    assertEquals(expected, success.getStatus().getMessage());
    assertNotNull(cb.get("something"));
  }

  /**
   * Test observe in case of deletion where key exists.
   *
   * @pre Sets the key.
   * @post Deletes the key and
   * asserts the status of operation.
   *
   * @throws Exception
   */
  public void testObsDeleteKeyExists() throws Exception {
    client.set("observetest", 0, "something");
    OperationFuture<Boolean> delOp =
            (((CouchbaseClient)client).delete("observetest",
                PersistTo.MASTER));
    assert delOp.get();
    assertNotNull(delOp.getStatus().getMessage());
  }

  @Override
  public void testGetStatsSlabs() throws Exception {
    // Empty
  }

  @Override
  public void testGetStatsSizes() throws Exception {
    // Empty
  }

  public void testGetStatsCacheDump() throws Exception {
    // Empty
  }

  @Override
  public void testStupidlyLargeSetAndSizeOverride() throws Exception {
    // Empty
  }

  @Override
  protected void syncGetTimeoutsInitClient() throws Exception {
    initClient(new CouchbaseConnectionFactory(Arrays.asList(URI
        .create("http://" + TestConfig.IPV4_ADDR + ":8091/pools")),
        "default", EMPTY) {
      @Override
      public long getOperationTimeout() {
        return 2;
      }

      @Override
      public int getTimeoutExceptionThreshold() {
        return 1000000;
      }
    });
  }

  @Ignore
  @Override
  public void testDelayedFlush() throws Exception {
    // TODO: re-add after RESTful flush added: Couchbase MB-5170
  }

  @Ignore
  @Override
  public void testFlush() throws Exception {
    // TODO: re-add after RESTful flush added: Couchbase MB-5170
  }

  /**
   * Shutdown the client and return the client object to the garbage collector.
   */
  @Override
  protected void tearDown() throws Exception {
    // Can't use our CouchbaseClientBaseCase easily owing to inheritance.

    // Shut down, null things out. Error tests have
    // unpredictable timing issues.  See test from Spymemcached
    // net.spy.memcached.ClientBaseCase
    client.shutdown(200, TimeUnit.MILLISECONDS);
    client = null;
    System.gc();
  }
}
