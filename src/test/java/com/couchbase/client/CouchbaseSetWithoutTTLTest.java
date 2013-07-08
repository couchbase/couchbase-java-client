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
import java.util.LinkedList;
import java.util.List;

import net.spy.memcached.TestConfig;
import net.spy.memcached.internal.OperationFuture;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the correct functionality when the {@link CouchbaseClient} is used
 * without TTL arguments.
 */
public class CouchbaseSetWithoutTTLTest {

  protected static TestingClient client = null;
  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
          + ":8091/pools";

  /**
   * Initialize the client connection.
   *
   * @throws Exception
   */
  protected static void initClient() throws Exception {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    client = new TestingClient(uris, "default", "");
  }

  @Before
  public void setUp() throws Exception {
    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 0, true);

    BucketTool.FunctionCallback callback = new BucketTool.FunctionCallback() {
      @Override
      public void callback() throws Exception {
        initClient();
      }

      @Override
      public String success(long elapsedTime) {
        return "Client Initialization took " + elapsedTime + "ms";
      }
    };
    bucketTool.poll(callback);
    bucketTool.waitForWarmup(client);
  }

  @Test
  public void testSimpleSetWithNoTTLNoDurability() throws Exception {
    String jsonValue = "{\"name\":\"This is a test with no TTL\"}";
    String jsonValue2 = "NewValue";
    String key001 = "key:001";

    // test simple values
    OperationFuture op = client.set(key001, jsonValue);
    assertTrue(op.getStatus().isSuccess());
    assertEquals(client.get(key001), jsonValue);
    client.delete(key001);

    // test replace that should fail
    op = client.replace(key001, jsonValue2);
    assertFalse(op.getStatus().isSuccess());

    // test add
    op = client.add(key001, jsonValue);
    assertTrue(op.getStatus().isSuccess());
    assertEquals(client.get(key001), jsonValue);

    // test add
    op = client.replace(key001, jsonValue2);
    assertTrue(op.getStatus().isSuccess());
    assertEquals(client.get(key001), jsonValue2);

    // test add
    op = client.add(key001, jsonValue);
    assertFalse(op.getStatus().isSuccess());

    client.delete(key001);
  }

}
