/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

import com.couchbase.client.vbucket.config.Config;
import net.spy.memcached.TestConfig;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Makes sure the CouchbaseConnectionFactory works as expected.
 */
public class CouchbaseConnectionFactoryTest {

  private List<URI> uris;
  private CouchbaseConnectionFactoryBuilder instance;

  @Before
  public void setUp() {
    instance = new CouchbaseConnectionFactoryBuilder();
    uris = Arrays.asList(URI.create("http://" + TestConfig.IPV4_ADDR
      + ":8091/pools"));
  }

  private CouchbaseConnectionFactory buildFactory() throws IOException {
    return instance.buildCouchbaseConnection(uris, "default", "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIfBucketIsNull() throws Exception {
    new CouchbaseConnectionFactory(uris, null, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIfBucketIsEmpty() throws Exception {
    new CouchbaseConnectionFactory(uris, "", "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIfPasswordIsNull() throws Exception {
    new CouchbaseConnectionFactory(uris, "default", null);
  }

  @Test
  public void shouldBootstrapThroughProperties() throws Exception {
    System.setProperty("cbclient.nodes", "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools");
    System.setProperty("cbclient.bucket", "default");
    System.setProperty("cbclient.password", "");

    CouchbaseConnectionFactory factory = new CouchbaseConnectionFactory();
    Config config = factory.getVBucketConfig();

    assertTrue(config.getServersCount() > 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailIfNoNodeProperty() throws Exception {
    System.clearProperty("cbclient.nodes");
    System.setProperty("cbclient.bucket", "default");
    System.setProperty("cbclient.password", "");

    new CouchbaseConnectionFactory();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailIfNoBucketProperty() throws Exception {
    System.clearProperty("cbclient.bucket");
    System.setProperty("cbclient.password", "");
    System.setProperty("cbclient.nodes", "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools");

    new CouchbaseConnectionFactory();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailIfNoPasswordProperty() throws Exception {
    System.clearProperty("cbclient.password");
    System.setProperty("cbclient.bucket", "default");
    System.setProperty("cbclient.nodes", "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools");

    new CouchbaseConnectionFactory();
  }

}
