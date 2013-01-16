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

import java.net.SocketAddress;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.TestConfig;
import net.spy.memcached.compat.SpyObject;

/**
 * A helper class for test cases that retries bucket creation, deletion, and
 * warmup since these processes can take a long time.
 */
public class BucketTool extends SpyObject {
  private static final long TIMEOUT = 120000;
  private static final long SLEEP_TIME = 2000;

  private final ClusterManager manager;

  public BucketTool() {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    manager = new ClusterManager(uris, CbTestConfig.CLUSTER_ADMINNAME,
        CbTestConfig.CLUSTER_PASS);
  }

  /**
   * A class for defining a simple callback. Used to define your own poll
   * function.
   */
  public static class FunctionCallback {
    public void callback() throws Exception {
      throw new UnsupportedOperationException("Must override this function");
    }

    public String success(long elapsedTime) {
      throw new UnsupportedOperationException("Must override this function");
    }
  }

  public void deleteAllBuckets() throws Exception {
    FunctionCallback callback = new FunctionCallback() {
      @Override
      public void callback() throws Exception {
        List<String> buckets = manager.listBuckets();
        for (int i = 0; i < buckets.size(); i++) {
          manager.deleteBucket(buckets.get(i));
        }
      }

      @Override
      public String success(long elapsedTime) {
        return "Bucket deletion took " + elapsedTime + "ms";
      }
    };
    poll(callback);
  }

  public void createDefaultBucket(final BucketType type, final int quota,
      final int replicas, final boolean flush) throws Exception {
    FunctionCallback callback = new FunctionCallback() {
      @Override
      public void callback() throws Exception {
        manager.createDefaultBucket(type, quota, replicas, flush);
      }

      @Override
      public String success(long elapsedTime) {
        return "Bucket creation took " + elapsedTime + "ms";
      }
    };
    poll(callback);
  }

  public void createSaslBucket(final String name, final BucketType type,
    final int quota, final int replicas, final boolean flush) throws Exception {
    FunctionCallback callback = new FunctionCallback() {
      @Override
      public void callback() throws Exception {
        manager.createNamedBucket(type, name, quota, replicas, name, flush);
      }

      @Override
      public String success(long elapsedTime) {
        return "Bucket creation took " + elapsedTime + "ms";
      }
    };
    poll(callback);
  }

  public void poll(FunctionCallback cb) throws Exception {
    long st = System.currentTimeMillis();

    while (true) {
      try {
        cb.callback();
        getLogger().info(cb.success(System.currentTimeMillis() - st));
        return;
      } catch (RuntimeException e) {
        if ((System.currentTimeMillis() - st) > TIMEOUT) {
          throw e;
        }
        Thread.sleep(SLEEP_TIME);
      }
    }
  }

  public void waitForWarmup(MemcachedClient client) throws Exception {
    boolean warmup = true;
    while (warmup) {
      warmup = false;
      Map<SocketAddress, Map<String, String>> stats = client.getStats();
      for (Entry<SocketAddress, Map<String, String>> server: stats.entrySet()) {
        Map<String, String> serverStats = server.getValue();
        if (!serverStats.containsKey("ep_degraded_mode")) {
          warmup = true;
          Thread.sleep(1000);
          break;
        }
        if (!serverStats.get("ep_degraded_mode").equals("0")) {
          warmup = true;
          Thread.sleep(1000);
          break;
        }
      }
    }
  }
}
