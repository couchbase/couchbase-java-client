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

package com.couchbase.client.vbucket;

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
  private static final String REST_USER = "Administrator";
  private static final String REST_PWD = "password";
  private static final String DEFAULT_BUCKET_NAME = "default";
  private ConfigurationProviderHTTP configProvider;
  private ReconfigurableMock reconfigurable = new ReconfigurableMock();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    List<URI> baseList = Arrays.asList(new URI("http://"
        + TestConfig.IPV4_ADDR + ":8091/pools"));
    configProvider = new ConfigurationProviderHTTP(baseList, REST_USER,
        REST_PWD);
    assertNotNull(configProvider);
  }

  public void testGetBucketConfiguration() throws Exception {
    Bucket bucket = configProvider.getBucketConfiguration(DEFAULT_BUCKET_NAME);
    assertNotNull(bucket);
  }

  public void testSubscribe() throws Exception {
    configProvider.subscribe(DEFAULT_BUCKET_NAME, reconfigurable);
  }

  public void testUnsubscribe() throws Exception {
    configProvider.unsubscribe(DEFAULT_BUCKET_NAME, reconfigurable);
  }

  public void testShutdown() throws Exception {
    configProvider.shutdown();
  }

  public void testGetAnonymousAuthBucket() throws Exception {
    assertEquals("default", configProvider.getAnonymousAuthBucket());
  }
}
