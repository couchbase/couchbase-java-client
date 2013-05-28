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

import com.couchbase.client.clustermanager.AuthType;
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

  private static final String BUCKET = "bucket1";
  private ClusterManager manager;

  /**
   * This method is used to do the initial setUp of the cluster manager.
   * It creates a new instance using the server configuration stored in the
   * build.xml and the CbTestConfig class. The existing buckets are
   * all deleted initially.
   */
  @Override
  public void setUp() throws HttpException {
    manager = getClusterManager();
    deleteAllBuckets(manager);
  }
  /**
   * This method is used to shut down the cluster manager's running instance.
   * The existing buckets are first deleted and then the connection is closed.
   */
  @Override
  public void tearDown() throws HttpException {
    deleteAllBuckets(manager);
    manager.shutdown();
  }
  /**
   * This method is used to get the cluster manager instance
   * using the URI and the login configuration information
   * in stored in build.xml and CbTestConfig.
   *
   * @return the cluster manager
   */
  private ClusterManager getClusterManager() {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://"
      + TestConfig.IPV4_ADDR + ":8091/pools"));
    return new ClusterManager(uris, CbTestConfig.CLUSTER_ADMINNAME,
      CbTestConfig.CLUSTER_PASS);
  }
  /**
   * This method is called every time the user needs
   * to delete all the buckets from the cluster.
   *
   * @param cm the cm
   * @throws HttpException the http exception
   */
  private void deleteAllBuckets(ClusterManager cm)
    throws HttpException {
    List<String> buckets = cm.listBuckets();
    for (int i = 0; i < buckets.size(); i++) {
      cm.deleteBucket(buckets.get(i));
    }
  }

  /**
   * This unit test creates default buckets.
   *
   * @pre First it creates a default couchbase bucket then sleeps
   * for 1000ms and then deletes this bucket. Another default bucket
   * is then created, this time of the type - Memcached. This bucket
   * is also deleted, after a delay of 1000ms of its creation.
   * @post Creation and deletion of the default buckets succeeds
   * without exception.
   * @throws Exception
   */
  public void testCreateDefaultBucket() throws Exception {
    manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0, true);
    Thread.sleep(1000);
    manager.deleteBucket("default");
    manager.createDefaultBucket(BucketType.MEMCACHED, 100, 0, true);
    Thread.sleep(1000);
    manager.deleteBucket("default");
  }

  /**
   * This test creates SASL buckets.
   *
   * @pre First it creates a SASL couchbase bucket then sleeps
   * for 1000ms and then deletes this bucket. Another SASL bucket
   * is then created, this time of the type - Memcached. This bucket
   * is also deleted, after a delay of 1000ms of its creation.
   * @post  Creation and deletion of the SASL buckets succeeds
   * without exception.
   * @throws Exception
   */
  public void testCreateSaslBucket() throws Exception {
    manager.createNamedBucket(BucketType.COUCHBASE, "saslbucket", 100, 0,
        "password", true);
    Thread.sleep(1000);
    manager.deleteBucket("saslbucket");
    manager.createNamedBucket(BucketType.MEMCACHED, "saslbucket", 100, 0,
        "password", false);
    Thread.sleep(1000);
    manager.deleteBucket("saslbucket");
  }

  /**
   * Creates port buckets i.e. buckets on alternate port.
   *
   * @pre  First it creates a port couchbase bucket,
   * sleeps for 1000ms and then deletes this bucket.
   * Another port bucket is then created, this time
   * of the type - Memcached. This bucket is also deleted,
   * after a delay of 1000ms of its creation.
   * @post  Creation and deletion of the port
   * buckets succeeds without exception.
   * @throws Exception
   */
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

  /**
   * Get SASL buckets from a cluster.
   *
   * @pre Three SASL buckets are created.
   * @post Asserts true if all the three SASL buckets
   * are successfully created.
   * @throws Exception
   */
  public void testGetBuckets() throws Exception {
    assertEquals(manager.listBuckets().size(), 0);
    manager.createNamedBucket(BucketType.COUCHBASE, BUCKET, 100, 0,
        "password", false);
    manager.createNamedBucket(BucketType.COUCHBASE, "bucket2", 100, 0,
        "password", false);
    manager.createNamedBucket(BucketType.COUCHBASE, "bucket3", 100, 0,
        "password", false);
    List<String> buckets = manager.listBuckets();
    assertTrue(buckets.contains(BUCKET));
    assertTrue(buckets.contains("bucket2"));
    assertTrue(buckets.contains("bucket3"));
    assertEquals(manager.listBuckets().size(), 3);
  }

  /**
   * Create SASL bucket even if the specified bucket quota is too small.
   *
   * @pre A SASL couchbase bucket is created with RAM share as 25MB.
   * @post Test fails and gives a message that the bucket quota is too
   * small. Asserts an error message if due to this the test runs into
   * RuntimeException.
   */
  public void testCreateBucketQuotaTooSmall() {
    try {
      manager.createNamedBucket(BucketType.COUCHBASE, BUCKET, 25, 0,
          "password", false);
      fail("Bucket quota too small, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"ramQuotaMB\":\"RAM quota cannot be less than"
          + " 100 MB\"}");
    }
  }

  /**
   * Create SASL bucket even if the specified bucket quota is too large.
   *
   * @pre A SASL couchbase bucket is created with RAM share as 100000MB.
   * @post Test fails and gives a message that the bucket quota is too
   * large. Asserts an error message if due to this the test runs into
   * RuntimeException.
   */
  public void testCreateBucketQuotaTooBig() {
    try {
      manager.createNamedBucket(BucketType.COUCHBASE, BUCKET, 100000, 0,
          "password", false);
      fail("Bucket quota too large, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"ramQuotaMB\":\"RAM quota specified is too large to be"
          + " provisioned into this cluster.\"}");
    }
  }

  /**
   * Create SASL bucket even if the specified bucket
   * replica nodes exceeds maximum possible.
   *
   * @pre A SASL couchbase bucket is created with 4 replicas,
   * when we know that a max of 3 replicas can only exist.
   * @post Test fails and gives a message that the replica
   * number is too large. Asserts an error message if due
   * to this the test runs into a RuntimeException.
   */
  public void testCreateBucketTooManyReplicas() {
    try {
      manager.createNamedBucket(BucketType.COUCHBASE, BUCKET, 100, 4,
          "password", false);
      fail("Replica number too large, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"replicaNumber\":\"Replica number larger than 3 is not "
          + "supported.\"}");
    }
  }

  /**
   * Create the SASL buckets even if replicas nodes are
   * less than minimum possible.
   *
   * @pre  A SASL couchbase bucket is created with -1 replicas,
   * when we know that a min of 0 replicas can only exist.
   * @post  A failure message stating that the replica number
   * is too small, is returned and in case due to this,
   * if the code runs into a runtime exception,
   * then the assert message is returned.
   */
  public void testCreateBucketTooFewReplicas() {
    try {
      manager.createNamedBucket(BucketType.COUCHBASE, BUCKET, 100, -1,
          "password", false);
      fail("Replica number too small, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"replicaNumber\":\"The replica number cannot be"
          + " negative.\"}");
    }
  }

  /**
   * Verify the creation of the SASL bucket with invalid name.
   *
   * @pre  Use the cluster manager instance to create a SASL
   * bucket even if invalid name is used while creating it.
   * @post  A failure message stating that the bucket name is
   * invalid is returned and in case the code runs into a
   * runtime exception, then the assert message is returned.
   */
  public void testCreateBucketBadName() {
    try {
      manager.createNamedBucket(BucketType.COUCHBASE, "$$$", 100, 0,
          "password", false);
      fail("Invalid bucket name, but bucket was still created");
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Http Error: 400 Reason: Bad Request "
          + "Details: {\"name\":\"Bucket name can only contain characters in "
          + "range A-Z, a-z, 0-9 as well as underscore, period, dash & "
          + "percent. Consult the documentation.\"}");
    }
  }

  /**
   * Connects to the cluster and its default bucket using invalid
   * server addresses.
   *
   * @pre  First the running client is shut down and a new instance
   * is created using URIs of invalid addresses. Next an attempt is
   * made to create the default bucket for this new instance.
   * @post  The connection should not succeed, after which the
   * previous cluster manager instance is restored.
   * @throws InterruptedException the interrupted exception
   */
  public void testWithOnlyBadAddrs() throws InterruptedException {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://badurl:8091/pools"));
    uris.add(URI.create("http://anotherbadurl:8091/pools"));
    manager.shutdown();
    manager = new ClusterManager(uris, CbTestConfig.CLUSTER_ADMINNAME,
      CbTestConfig.CLUSTER_PASS);
    try {
      manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0, true);
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Unable to connect to cluster");
    }
    manager = getClusterManager();
  }

  /**
   * This test is performed by having the client connect to a host
   * which is up, but using a bad port (i.e. not the default 8091)
   *
   * @pre  First the running client is shut down and a new instance
   * is created using URIs of invalid addresses. Next an attempt is
   * made to create the default bucket for this new instance.
   * @post  The connection should not succeed, after which the
   * previous cluster manager instance is restored.
   * @throws InterruptedException the interrupted exception
   */
  public void testConnectionRefused() throws InterruptedException {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":3454/pools"));
    manager.shutdown();
    manager = new ClusterManager(uris, CbTestConfig.CLUSTER_ADMINNAME,
      CbTestConfig.CLUSTER_PASS);
    String message = "";
    try {
      manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0, true);
    } catch (RuntimeException e) {
      message = e.getMessage();
    }
    assertEquals("Unable to connect to cluster", message);
    manager = getClusterManager();
  }

  /**
   * This test is performed by having the client connect
   * to an IP for which no valid host is assigned.
   *
   * @pre  First the running client is shut down and a new instance
   * is created using URIs of invalid addresses. Next an attempt is
   * made to create the default bucket for this new instance.
   * @post  The connection should not succeed, after which the
   * previous cluster manager instance is restored.
   * @throws InterruptedException the interrupted exception
   */
  public void testNetworkUnreachable() throws InterruptedException {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://123.123.123.123:8091/pools"));
    manager.shutdown();
    manager = new ClusterManager(uris, CbTestConfig.CLUSTER_ADMINNAME,
      CbTestConfig.CLUSTER_PASS);
    String message = "";
    try {
      manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0, true);
    } catch (RuntimeException e) {
      message = e.getMessage();
    }
    assertEquals("Unable to connect to cluster", message);
    manager = getClusterManager();
  }
  /**
   * Create and delete the default buckets with some bad server addresses.
   *
   * @pre  It first creates a list of URIs and shuts down the cluster manager.
   * Then a new cluster manager instance is retrieved using the list of URIs,
   * and default bucket is created.
   * @post  The working thread sleeps for 1000ms and
   * then deletes the bucket just created.
   * @throws InterruptedException the interrupted exception
   */
  public void testWithSomeBadAddrs() throws InterruptedException {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create("http://badurl:8091/pools"));
    uris.add(URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    uris.add(URI.create("http://anotherbadurl:8091/pools"));

    manager.shutdown();
    manager = new ClusterManager(uris, CbTestConfig.CLUSTER_ADMINNAME,
      CbTestConfig.CLUSTER_PASS);
    manager.createDefaultBucket(BucketType.COUCHBASE, 100, 0, true);
    Thread.sleep(1000);
    manager.deleteBucket("default");
  }

  /**
   * Update parameters of an existing bucket.
   *
   * @pre SASL bucket is created.
   * @post The bucket is updated with new password.
   * @throws Exception
   */
  public void testUpdateBucketPswd() throws Exception {
    manager.createNamedBucket(BucketType.COUCHBASE,BUCKET, 100, 0, "", true);
    Thread.sleep(1000);
    manager.updateBucket(BUCKET, 100, AuthType.SASL, 0, 11212, "password", true);
  }

  /**
   * Update parameters of an existing bucket.
   *
   * @pre SASL bucket is created.
   * @post The bucket is updated with new ram size.
   * @throws Exception
   */
  public void testUpdateBucketRAM() throws Exception {
    manager.createNamedBucket(BucketType.COUCHBASE,BUCKET, 100, 0, "", true);
    Thread.sleep(1000);
    manager.updateBucket(BUCKET, 200, AuthType.SASL, 0, 11212, "", true);
  }

  /**
   * Update parameters of an existing bucket.
   *
   * @pre SASL bucket is created.
   * @post The bucket is updated with new auth type.
   * @throws Exception
   */
  public void testUpdateBucketAuth() throws Exception {
    manager.createNamedBucket(BucketType.COUCHBASE,BUCKET, 100, 0, "", true);
    Thread.sleep(1000);
    manager.updateBucket(BUCKET, 100, AuthType.NONE, 0, 11212, "", true);
  }

  /**
   * Update parameters of an existing bucket.
   *
   * @pre Port bucket is created.
   * @post The bucket is updated to SASL auth type.
   * @throws Exception
   */
  public void testUpdateBucketPort() throws Exception {
    manager.createPortBucket(BucketType.COUCHBASE,BUCKET, 100, 0, 8090, true);
    Thread.sleep(1000);
    manager.updateBucket(BUCKET, 100, AuthType.SASL, 0, 11212, "", true);
  }

  /**
   * Update parameters of an existing bucket.
   *
   * @pre Default bucket is created.
   * @post The bucket is updated using BucketTool.
   * The auth type does not get updated as it is a
   * default bucket. But the RAM gets updated.
   * @throws Exception
   */
  public void testUpdateBucket() throws Exception {
    BucketTool bucketTool = new BucketTool();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 0, true);
    Thread.sleep(1000);
    bucketTool.updateBucket("default", AuthType.SASL, 456, 1, 11212, "", true);
  }

}