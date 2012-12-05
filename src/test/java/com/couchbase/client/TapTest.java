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

import com.couchbase.client.BucketTool.FunctionCallback;
import com.couchbase.client.clustermanager.BucketType;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.TestConfig;
import net.spy.memcached.tapmessage.ResponseMessage;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

// TBD - Uncomment this line when the TAP tests are complete
// import net.spy.memcached.tapmessage.ResponseMessage;

/**
 * A TapTest.
 */
public class TapTest {

  private static final long TAP_DUMP_TIMEOUT = 2000;

  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools";

  protected static TestingClient client = null;

  protected static void initClient() throws Exception {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    client = new TestingClient(uris, "default", "");
  }

  @BeforeClass
  public static void before() throws Exception {
    final List<URI> uris = Arrays.asList(URI.create("http://"
        + TestConfig.IPV4_ADDR + ":8091/pools"));

    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 0);

    BucketTool.FunctionCallback callback = new FunctionCallback() {
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
  public void testBackfill() throws Exception {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    TapClient tc = new TapClient(uris, "default", "");
    tc.tapBackfill(null, 5, TimeUnit.SECONDS);
    HashMap<String, Boolean> items = new HashMap<String, Boolean>();
    for (int i = 0; i < 25; i++) {
      client.set("key" + i, 0, "value" + i);
      items.put("key" + i + ",value" + i, new Boolean(false));
    }

    while (tc.hasMoreMessages()) {
      ResponseMessage m;
      if ((m = tc.getNextMessage()) != null) {
        String key = m.getKey() + "," + new String(m.getValue());
        if (items.containsKey(key)) {
          items.put(key, new Boolean(true));
        } else {
          assertTrue(false);
        }
      }
    }
    checkTapKeys(items);
    // assertTrue(client.flush().get().booleanValue());
    tc.shutdown();
  }

  @Test
  public void testTapDump() throws Exception {
    HashMap<String, Boolean> items = new HashMap<String, Boolean>();
    for (int i = 0; i < 25; i++) {
      assert(client.set("key" + i, 0, "value" + i).get());
      items.put("key" + i + ",value" + i, new Boolean(false));
    }
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    TapClient tapClient = new TapClient(uris, "default", "");
    tapClient.tapDump(null);

    long st = System.currentTimeMillis();
    while (tapClient.hasMoreMessages()) {
      if ((System.currentTimeMillis() - st) > TAP_DUMP_TIMEOUT) {
        assertTrue("Tap dump took too long", false);
      }
      ResponseMessage m;
      if ((m = tapClient.getNextMessage()) != null) {
        String key = m.getKey() + "," + new String(m.getValue());
        if (items.containsKey(key)) {
          items.put(key, new Boolean(true));
        } else {
          assertTrue("Received key not found in the items", false);
        }
      }
    }
    checkTapKeys(items);
    // assertTrue(client.flush().get().booleanValue());
    tapClient.shutdown();
  }

  @Test
  public void testTapBucketDoesNotExist() throws Exception {
    TapClient tapClient = null;
    tapClient = new TapClient(Arrays.asList(new URI("http://"
      + TestConfig.IPV4_ADDR + ":8091/pools")), "abucket", "apassword");
    try {
      tapClient.tapBackfill(null, 5, TimeUnit.SECONDS);
      assertTrue("TAP started with a misconfiguration on " + tapClient, false);
    } catch (RuntimeException e) {
      // expected
      System.err.println("Expected tap of non existent bucket "
        + "failure:\n" + e.getMessage());
      return;
    } finally {
      tapClient.shutdown();
    }
  }

  private void checkTapKeys(HashMap<String, Boolean> items) {
    for (Entry<String, Boolean> kv : items.entrySet()) {
      if (!kv.getValue().booleanValue()) {
        assertTrue("Failed to receive one of the previously set items: \""
          + kv.getKey() + "\". Number of items received: " + items.size(),
          false);
      }
    }
    System.err.println("Received " + items.size() + " items over TAP.");
  }
}
