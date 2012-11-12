/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

import org.junit.Ignore;

/**
 * A CouchbaseClientTest.
 */
public class CouchbaseClientTest extends BinaryClientTest {

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
        initClient(new CouchbaseConnectionFactory(uris, "default", ""));
      }

      @Override
      public String success(long elapsedTime) {
        return "Client Initialization took " + elapsedTime + "ms";
      }
    };
    bucketTool.poll(callback);
    bucketTool.waitForWarmup(client);
  }

  @Override
  protected String getExpectedVersionSource() {
    if (TestConfig.IPV4_ADDR.equals("127.0.0.1")) {
      return "/127.0.0.1:11210";
    }
    return TestConfig.IPV4_ADDR + ":11210";
  }

  @Override
  protected void initClient(ConnectionFactory cf) throws Exception {
    client = new CouchbaseClient((CouchbaseConnectionFactory) cf);
  }

  @Override
  public void testAvailableServers() {
    // CouchbaseClient tracks hostname and ip address of servers need to
    // make sure the available server list is 2 * (num servers)
    try {
      Thread.sleep(10); // Let the client warm up
    } catch (InterruptedException e) {
      fail("Interrupted while client was warming up");
    }

    StringBuilder availableServers = new StringBuilder();
    for(SocketAddress sa : client.getAvailableServers()) {
      if (availableServers.length() > 0) {
        availableServers.append(";");
      }
      availableServers.append(sa.toString());
    }

    assert (client.getAvailableServers().size() % 2) ==  0 : "Num servers "
      + client.getAvailableServers().size() + ".  They are: "
      + availableServers;
  }

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
          + TestConfig.IPV4_ADDR + ":8091/pools")), "default", ""));
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

  public void testNumVBuckets() throws Exception {
    // Check for power of two values
    int num = ((CouchbaseClient)client).getNumVBuckets();
    assertTrue("NumVBuckets has to be a power of two", (num & -num)== num);
  }

  public void testGetVersions() {
    Map<SocketAddress, String> vs = ((CouchbaseClient)client).getVersions();
    System.out.println(vs);
    assertEquals(client.getAvailableServers().size(), vs.size());
  }

  public void testGetStats() throws Exception {
    Map<SocketAddress, Map<String, String>> stats =
        ((CouchbaseClient)client).getStats();
    assertEquals(client.getAvailableServers().size(), stats.size());
    Map<String, String> oneStat = stats.values().iterator().next();
    assertTrue(oneStat.containsKey("curr_items"));
  }


  public void testGATTimeout() throws Exception {
    assertNull(client.get("gatkey"));
    assert client.set("gatkey", 1, "gatvalue").get().booleanValue();
    assert client.getAndTouch("gatkey", 2).getValue().equals("gatvalue");
    Thread.sleep(1300);
    assert client.get("gatkey").equals("gatvalue");
    Thread.sleep(2000);
    assertNull(client.getAndTouch("gatkey", 3));
  }

  public void testTouchTimeout() throws Exception {
    assertNull(client.get("touchkey"));
    assert client.set("touchkey", 1, "touchvalue").get().booleanValue();
    assert client.touch("touchkey", 2).get().booleanValue();
    Thread.sleep(1300);
    assert client.get("touchkey").equals("touchvalue");
    Thread.sleep(2000);
    assertFalse(client.touch("touchkey", 3).get().booleanValue());
  }

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

  public void testObserve() throws Exception {
    assertNull(client.get("observetest"));
    OperationFuture<Boolean> setOp =
            (((CouchbaseClient)client).set("observetest", 0, "value",
                PersistTo.MASTER));
    assert setOp.get()
            : "Key set was not persisted to master : "
            + setOp.getStatus().getMessage();
    OperationFuture<Boolean> replaceOp =
            (((CouchbaseClient)client).replace("observetest", 0, "value",
                PersistTo.MASTER));
    assert replaceOp.get()
            : "Key replace was not persisted to master : "
            + replaceOp.getStatus().getMessage();
    OperationFuture<Boolean> deleteOp =
            (((CouchbaseClient)client).delete("observetest",
                PersistTo.MASTER));
    assert deleteOp.get()
            : "Key was not deleted on master : "
            + deleteOp.getStatus().getMessage();
    OperationFuture<Boolean> addOp =
            (((CouchbaseClient)client).add("observetest", 0, "value",
                PersistTo.MASTER, ReplicateTo.ZERO));
    assert addOp.get()
            : "Key add was not persisted to master : "
            + addOp.getStatus().getMessage();

    OperationFuture<Boolean> noPersistOp =
            (((CouchbaseClient)client).add("nopersisttest", 0, "value",
              ReplicateTo.ONE));
    assert noPersistOp.get()
            : "Key add was not correctly replicated: "
            + noPersistOp.getStatus().getMessage();
  }

  public void testStatsKey() throws Exception {
    OperationFuture<Boolean> f = client.set("key", 0, "value");
    System.err.println(f.getStatus().getMessage());

    OperationFuture<Map<String, String>> future =
        ((CouchbaseClient)client).getKeyStats("key");
    System.err.println(future);
    System.err.println(future.getStatus());
    assertTrue(future.getStatus().isSuccess());
    Map<String, String> stats = future.get();
    assertTrue(stats.size() == 7);
    assertTrue(stats.containsKey("key_vb_state"));
    assertTrue(stats.containsKey("key_flags"));
    assertTrue(stats.containsKey("key_is_dirty"));
    assertTrue(stats.containsKey("key_cas"));
    assertTrue(stats.containsKey("key_data_age"));
    assertTrue(stats.containsKey("key_exptime"));
    assertTrue(stats.containsKey("key_last_modification_time"));

    future = ((CouchbaseClient)client).getKeyStats("key1");
    assertFalse(future.getStatus().isSuccess());
  }

  /**
   * Tests if passing NULL for ReplicateTo and/or
   * PersistTo correctly works and does not throw
   * an exception.
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

  public void testObservePersistWithTooFewNodes() throws Exception {
    CouchbaseClient cb = (CouchbaseClient) client;
    OperationFuture<Boolean> invalid = cb.set("something", 0,
      "to_store", PersistTo.FOUR);
    assertFalse(invalid.get());
    String expected = "Currently, there are less nodes in the cluster than "
      + "required to satisfy the persistence constraint.";
    assertEquals(expected, invalid.getStatus().getMessage());
  }

  public void testObserveReplicateWithTooFewNodes() throws Exception {
    CouchbaseClient cb = (CouchbaseClient) client;
    OperationFuture<Boolean> invalid = cb.set("something", 0,
      "to_store", ReplicateTo.THREE);
    assertFalse(invalid.get());
    String expected = "Currently, there are less nodes in the cluster than "
      + "required to satisfy the replication constraint.";
    assertEquals(expected, invalid.getStatus().getMessage());
  }

  public void testGetStatsSlabs() throws Exception {
    // Empty
  }

  public void testGetStatsSizes() throws Exception {
    // Empty
  }

  public void testGetStatsCacheDump() throws Exception {
    // Empty
  }

  public void testStupidlyLargeSetAndSizeOverride() throws Exception {
    // Empty
  }

  protected void syncGetTimeoutsInitClient() throws Exception {
    initClient(new CouchbaseConnectionFactory(Arrays.asList(URI
        .create("http://" + TestConfig.IPV4_ADDR + ":8091/pools")),
        "default", "") {
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
