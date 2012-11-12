/**
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

import com.couchbase.client.clustermanager.BucketType;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import net.spy.memcached.TestConfig;

import org.apache.http.HttpException;

/**
 * Tests the Couchbase ClusterManager.
 */
public class ClusterManagerTest extends TestCase {

  private ClusterManager manager;

  public void setUp() throws HttpException {
    manager = getClusterManager();
    deleteAllBuckets(manager);
  }

  public void tearDown() throws HttpException {
    deleteAllBuckets(manager);
    manager.shutdown();
  }

  private ClusterManager getClusterManager() {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://"
      + TestConfig.IPV4_ADDR + ":8091/pools"));
    return new ClusterManager(uris, CbTestConfig.CLUSTER_ADMINNAME,
      CbTestConfig.CLUSTER_PASS);
  }

  private void deleteAllBuckets(ClusterManager cm)
    throws HttpException {
    List<String> buckets = cm.listBuckets();
    for (int i = 0; i < buckets.size(); i++) {
      cm.deleteBucket(buckets.get(i));
    }
  }

  public void testCreateDefaultBucket() throws Exception {
    manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0, true);
    Thread.sleep(1000);
    manager.deleteBucket("default");
    manager.createDefaultBucket(BucketType.MEMCACHED, 100, 0, true);
    Thread.sleep(1000);
    manager.deleteBucket("default");
  }

  public void testCreateSaslBucket() throws Exception {
    manager.createSaslBucket(BucketType.COUCHBASE, "saslbucket", 100, 0,
        "password", true);
    Thread.sleep(1000);
    manager.deleteBucket("saslbucket");
    manager.createSaslBucket(BucketType.MEMCACHED, "saslbucket", 100, 0,
        "password", false);
    Thread.sleep(1000);
    manager.deleteBucket("saslbucket");
  }

  public void testCreatePortBucket() throws Exception {
    manager.createPortBucket(BucketType.COUCHBASE, "portbucket", 100, 0,
        11212, true);
    Thread.sleep(1000);
    manager.deleteBucket("portbucket");
    manager.createPortBucket(BucketType.MEMCACHED, "portbucket", 100, 0,
        11212, true);
    Thread.sleep(1000);
    manager.deleteBucket("portbucket");
  }

  public void testGetBuckets() throws Exception {
    assertEquals(manager.listBuckets().size(), 0);
    manager.createSaslBucket(BucketType.COUCHBASE, "bucket1", 100, 0,
        "password", false);
    manager.createSaslBucket(BucketType.COUCHBASE, "bucket2", 100, 0,
        "password", false);
    manager.createSaslBucket(BucketType.COUCHBASE, "bucket3", 100, 0,
        "password", false);
    List<String> buckets = manager.listBuckets();
    assertTrue(buckets.contains("bucket1"));
    assertTrue(buckets.contains("bucket2"));
    assertTrue(buckets.contains("bucket3"));
    assertEquals(manager.listBuckets().size(), 3);
  }

  public void testCreateBucketQuotaTooSmall() {
    try {
      manager.createSaslBucket(BucketType.COUCHBASE, "bucket1", 25, 0,
          "password", false);
      fail("Bucket quota too small, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"ramQuotaMB\":\"RAM quota cannot be less than"
          + " 100 MB\"}");
    }
  }

  public void testCreateBucketQuotaTooBig() {
    try {
      manager.createSaslBucket(BucketType.COUCHBASE, "bucket1", 100000, 0,
          "password", false);
      fail("Bucket quota too large, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"ramQuotaMB\":\"RAM quota specified is too large to be"
          + " provisioned into this cluster.\"}");
    }
  }

  public void testCreateBucketTooManyReplicas() {
    try {
      manager.createSaslBucket(BucketType.COUCHBASE, "bucket1", 100, 4,
          "password", false);
      fail("Replica number too large, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"replicaNumber\":\"Replica number larger than 3 is not "
          + "supported.\"}");
    }
  }

  public void testCreateBucketTooFewReplicas() {
    try {
      manager.createSaslBucket(BucketType.COUCHBASE, "bucket1", 100, -1,
          "password", false);
      fail("Replica number too small, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"replicaNumber\":\"The replica number cannot be"
          + " negative.\"}");
    }
  }

  public void testCreateBucketBadName() {
    try {
      manager.createSaslBucket(BucketType.COUCHBASE, "$$$", 100, 0,
          "password", false);
      fail("Invalid bucket name, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"name\":\"Bucket name can only contain characters in "
          + "range A-Z, a-z, 0-9 as well as underscore, period, dash & "
          + "percent. Consult the documentation.\"}");
    }
  }

  public void testWithOnlyBadAddrs() throws InterruptedException {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://badurl:8091/pools"));
    uris.add(URI.create("http://anotherbadurl:8091/pools"));
    manager.shutdown();
    manager = new ClusterManager(uris, "Administrator", "password");

    try {
      manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0, true);
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Unable to connect to cluster");
    }
    manager = getClusterManager();
  }

  public void testWithSomeBadAddrs() throws InterruptedException {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://badurl:8091/pools"));
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    uris.add(URI.create("http://anotherbadurl:8091/pools"));

    manager.shutdown();
    manager = new ClusterManager(uris, "Administrator", "password");
    manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0, true);
    Thread.sleep(1000);
    manager.deleteBucket("default");
  }
}
