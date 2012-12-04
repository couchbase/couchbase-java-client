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
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import net.spy.memcached.TestConfig;

/**
 * Test to ensure a down node in the URI list specified won't stop us from
 * finding an up node.
 */
public class ConfigurationProviderHTTPDownNodeTest extends TestCase {
  private static final String REST_USER = "Administrator";
  private static final String REST_PASSWORD = "password";
  private ConfigurationProviderHTTP configProvider;
  private static final String DEFAULT_BUCKET_NAME = "default";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    List<URI> baseList = new ArrayList<URI>();
    baseList.add(new URI("http://bogus:8091/pools"));
    baseList.add(new URI("http://bogustoo:8091/pools"));
    baseList.add(new URI("http://" + TestConfig.IPV4_ADDR + ":8091/pools"));
    baseList.add(new URI("http://morebogus:8091/pools"));
    configProvider = new ConfigurationProviderHTTP(baseList, REST_USER,
      REST_PASSWORD);
    assertNotNull(configProvider);
  }

  /**
   * Tests to get bucket configuration.
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
}
