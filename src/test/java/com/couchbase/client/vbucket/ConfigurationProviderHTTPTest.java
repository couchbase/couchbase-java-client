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

package com.couchbase.client.vbucket;

import com.couchbase.client.CbTestConfig;
import com.couchbase.client.vbucket.config.Bucket;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import net.spy.memcached.TestConfig;

/**
 * A ConfigurationHTTPTest.
 */
public class ConfigurationProviderHTTPTest extends TestCase {
  private static final String DEFAULT_BUCKET_NAME = "default";
  private ConfigurationProviderHTTP configProvider;
  private final ReconfigurableMock reconfigurable = new ReconfigurableMock();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    List<URI> baseList = Arrays.asList(new URI("http://"
        + TestConfig.IPV4_ADDR + ":8091/pools"));
    configProvider = new ConfigurationProviderHTTP(baseList,
      CbTestConfig.CLUSTER_ADMINNAME, CbTestConfig.CLUSTER_PASS);
    assertNotNull(configProvider);
  }

  /**
   * Get bucket configuration.
   *
   * @pre  Using config provider instance,
   * get the bucket configuration.
   * @post  Asserts that an bucket isn't null.
   * @throws Exception the exception
   */
  public void testGetBucketConfiguration() throws Exception {
    Bucket bucket = configProvider.getBucketConfiguration(DEFAULT_BUCKET_NAME);
    assertNotNull(bucket);
  }

  /**
   * Test the Subscription for configuration updates.
   *
   * @pre  Use config provider instance to call subscribe method.
   * @throws Exception the exception
   */
  public void testSubscribe() throws Exception {
    configProvider.subscribe(DEFAULT_BUCKET_NAME, reconfigurable);
  }

  /**
   * Test reverting the Subscription for configuration updates.
   *
   * @pre  Use config provider instance to call unsubscribe method.
   * @throws Exception the exception
   */
  public void testUnsubscribe() throws Exception {
    configProvider.unsubscribe(DEFAULT_BUCKET_NAME, reconfigurable);
  }

  /**
   * Test the shutdown of configProvider.
   *
   * @pre  Use config provider instance to call shutdown method.
   * @throws Exception the exception
   */
  public void testShutdown() throws Exception {
    configProvider.shutdown();
  }

  /**
   * Test the retrieval of anonymous auth bucket.
   *
   * @pre  Use config provider instance to call
   * getAnonymousAuthBucket method.
   * @post  Asserts true as the anonymous auth
   * bucket is the default bucket.
   * @throws Exception the exception
   */
  public void testGetAnonymousAuthBucket() throws Exception {
    assertEquals("default", configProvider.getAnonymousAuthBucket());
  }
}
