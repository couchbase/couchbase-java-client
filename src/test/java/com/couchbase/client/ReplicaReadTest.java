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
import com.couchbase.client.internal.ReplicaGetCompletionListener;
import com.couchbase.client.internal.ReplicaGetFuture;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.CASValue;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ReplicateTo;
import net.spy.memcached.TestConfig;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the correct functionality when reading from replicas.
 */
public class ReplicaReadTest {

  private static CouchbaseClient client;

  @Before
  public void beforeTest() throws Exception {
    final List<URI> uris = Arrays.asList(URI.create("http://"
      + TestConfig.IPV4_ADDR + ":8091/pools"));

    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 1, true);

    BucketTool.FunctionCallback callback = new BucketTool.FunctionCallback() {
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

  /**
   * Creating a new instance of the couchbase client object.
   */
  protected void initClient(ConnectionFactory cf) throws Exception {
    client = new CouchbaseClient((CouchbaseConnectionFactory) cf);
  }

  @Test
  public void testGetFromReplica() throws Exception {
    if (client.getAvailableServers().size() < 2) {
      return;
    }

    assertTrue(client.set("fookey", 0, "foovalue", ReplicateTo.ONE).get());
    ReplicaGetFuture<Object> future = client.asyncGetFromReplica("fookey");
    String result = (String) future.get();
    assertEquals(result, "foovalue");
  }

  @Test
  public void testGetFromReplicaListener() throws Exception {
    if (client.getAvailableServers().size() < 2) {
      return;
    }

    assertTrue(client.set("asyncKey", "value", ReplicateTo.ONE).get());
    ReplicaGetFuture<Object> future = client.asyncGetFromReplica("asyncKey");

    final CountDownLatch latch = new CountDownLatch(1);
    future.addListener(new ReplicaGetCompletionListener() {
      @Override
      public void onComplete(ReplicaGetFuture<?> f) throws Exception {
        String found = (String) f.get();
        if(found.equals("value")) {
          latch.countDown();
        }
      }
    });

    assertTrue("Async latch was not counted down",
      latch.await(5, TimeUnit.SECONDS));
  }

  @Test
  public void testGetsFromReplica() throws Exception {
    if (client.getAvailableServers().size() < 2) {
      return;
    }

    assertTrue(client.set("getskey", 0, "foovalue", ReplicateTo.ONE).get());
    ReplicaGetFuture<CASValue<Object>> future = client.asyncGetsFromReplica("getskey");
    CASValue<Object> result = future.get();
    assertTrue(result.getCas() != 0);
    assertEquals(result.getValue(), "foovalue");
  }

  @Test
  public void testGetsFromReplicaListener() throws Exception {
    if (client.getAvailableServers().size() < 2) {
      return;
    }

    assertTrue(client.set("asyncKeyGets", "value", ReplicateTo.ONE).get());
    ReplicaGetFuture<CASValue<Object>> future =
      client.asyncGetsFromReplica("asyncKeyGets");

    final CountDownLatch latch = new CountDownLatch(1);
    future.addListener(new ReplicaGetCompletionListener() {
      @Override
      public void onComplete(ReplicaGetFuture<?> f) throws Exception {
        CASValue<Object> found = (CASValue<Object>) f.get();
        if(found.getValue().equals("value") && found.getCas() != 0) {
          latch.countDown();
        }
      }
    });

    assertTrue("Async latch was not counted down",
      latch.await(5, TimeUnit.SECONDS));
  }

}
