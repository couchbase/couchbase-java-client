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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.spy.memcached.TestConfig;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the correct behavior of the Couchbase Server 2.0 HTTP-based
 * flush capabilities.
 */
public class FlushTest {

  private static String noflushBucket = "noflush";
  private static String memcachedBucket = "cache";
  private static CouchbaseClient defaultClient;
  private static CouchbaseClient saslClient;
  private static CouchbaseClient memcachedClient;

  /**
   * Setup the cluster and buckets to have a reliable and reproducible
   * environment to test against.
   */
  @BeforeClass
  public static void prepareBuckets() throws Exception {
    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 128, 0, true);
    bucketTool.createSaslBucket(noflushBucket, BucketType.COUCHBASE, 128, 0,
      false);
    bucketTool.createSaslBucket(memcachedBucket, BucketType.MEMCACHED, 128, 0, true);

    BucketTool.FunctionCallback callback = new BucketTool.FunctionCallback() {
      @Override
      public void callback() throws Exception {
        List<URI> uris = new ArrayList<URI>();
        uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
        defaultClient = new CouchbaseClient(uris, "default", "");
        saslClient = new CouchbaseClient(uris,
          noflushBucket, noflushBucket);
        memcachedClient = new CouchbaseClient(uris, memcachedBucket, memcachedBucket);
      }

      @Override
      public String success(long elapsedTime) {
        return "Client Initialization took " + elapsedTime + "ms";
      }
    };
    bucketTool.poll(callback);
    bucketTool.waitForWarmup(defaultClient);
    bucketTool.waitForWarmup(saslClient);
  }

  /**
   * Test the flush command against a bucket when enabled and check success.
   *
   * @pre Use the server configuration for connection. Set sample documents
   * to the database in a loop. Flush the data and check its return value.
   * @post Shutdown the client.
   * @throws Exception
   */
  @Test
  public void testFlushWhenEnabled() throws Exception {
    List<URI> uris = new ArrayList<URI>();
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    CouchbaseClient client = new CouchbaseClient(uris, "default", "");

    for(int i = 0; i <= 10; i++) {
      client.set("doc:"+ i, 0, "sampledocument").get();
    }

    for(int i = 0; i <= 10; i++) {
      assertEquals("sampledocument", (String) client.get("doc:" + i));
    }

    Boolean response = client.flush().get();
    assertTrue(response);

    for(int i = 0; i <= 10; i++) {
      assertNull(client.get("doc:" + i));
    }

    client.shutdown();
  }

  /**
   * Test the flush command against a bucket when disabled and check errors.
   *
   * @pre Use the server configuration for connection. Set sample documents
   * to the database in a loop. Flush the data and check its return value.
   * @post Shutdown the client.
   * @throws Exception
   */
  @Test
  public void testFlushWhenDisabled() throws Exception {
    List<URI> uris = new ArrayList<URI>();
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    CouchbaseClient client = new CouchbaseClient(
      uris, noflushBucket, noflushBucket);

    for(int i = 0; i <= 10; i++) {
      client.set("doc:"+ i, 0, "sampledocument").get();
    }

    for(int i = 0; i <= 10; i++) {
      assertEquals("sampledocument", (String) client.get("doc:" + i));
    }

    Boolean response = client.flush().get();
    assertFalse(response);

    for(int i = 0; i <= 10; i++) {
      assertEquals("sampledocument", (String) client.get("doc:" + i));
    }

    client.shutdown();
  }

  /**
   * Make sure that flush() works on memcached type buckets.
   *
   * Note that this is a regression test for JCBC-349.
   *
   * @throws Exception
   */
  @Test
  public void testFlushOnMemcachedBucket() throws Exception {
    List<URI> uris = new ArrayList<URI>();
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    CouchbaseClient client = new CouchbaseClient(uris, memcachedBucket, memcachedBucket);

    assertTrue(client.flush().get());

    client.shutdown();
  }

}
