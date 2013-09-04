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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.vbucket.ConfigurationProvider;
import com.couchbase.client.vbucket.ConfigurationProviderMemcacheMock;
import com.couchbase.client.vbucket.CouchbaseNodeOrder;
import com.couchbase.client.vbucket.config.Config;
import net.spy.memcached.TestConfig;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
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

  /**
   * Make sure that the first calls to pastReconnThreshold() yield false
   * and the first one who is over getMaxConfigCheck() yields true.
   *
   * @throws IOException
   */
  @Test
  public void testPastReconnectThreshold() throws IOException {
    CouchbaseConnectionFactory connFact = buildFactory();

    for(int i=1; i<connFact.getMaxConfigCheck(); i++) {
      boolean pastReconnThreshold = connFact.pastReconnThreshold();
      assertFalse(pastReconnThreshold);
    }

    boolean pastReconnThreshold = connFact.pastReconnThreshold();
    assertTrue(pastReconnThreshold);
  }

  /**
   * Verifies that when
   * {@link CouchbaseConnectionFactory#pastReconnectThreshold()} is called
   * in longer frames than the time period allows, no configuration update is
   * triggered.
   */
  @Test
  public void testPastReconnectThresholdWithSleep() throws Exception {
    CouchbaseConnectionFactory connFact = buildFactory();

    for(int i=1; i<=connFact.getMaxConfigCheck()-1; i++) {
      boolean pastReconnThreshold = connFact.pastReconnThreshold();
      assertFalse(pastReconnThreshold);
    }

    Thread.sleep(TimeUnit.SECONDS.toMillis(11));

    for(int i=1; i<=connFact.getMaxConfigCheck()-1; i++) {
      boolean pastReconnThreshold = connFact.pastReconnThreshold();
      assertFalse(pastReconnThreshold);
    }
  }

  @Test
  public void shouldRandomizeNodeList() throws Exception {
    ConfigurationProviderMemcacheMock providerMock = new ConfigurationProviderMemcacheMock(
      Arrays.asList("127.0.0.1:8091/pools", "127.0.0.2:8091/pools",
        "127.0.0.3:8091/pools", "127.0.0.4:8091/pools")
    );

    CouchbaseConnectionFactory connFact = new CouchbaseConnectionFactoryMock(
      Arrays.asList(
        new URI("http://127.0.0.1:8091/pools"), new URI("http://127.0.0.2:8091/pools"),
        new URI("http://127.0.0.3:8091/pools"), new URI("http://127.0.0.5:8091/pools")
      ), "default", "", providerMock, CouchbaseNodeOrder.RANDOM
    );

    List<URI> oldList = connFact.getStoredBaseList();
    int oldIndex = oldList.indexOf(new URI("http://127.0.0.1:8091/pools"));

    int tries = 10;
    for(int i = 0; i < tries; i++) {
      connFact.updateStoredBaseList(connFact.getVBucketConfig());
      assertTrue(providerMock.baseListUpdated);
      List<URI> newList = connFact.getStoredBaseList();
      int newIndex = newList.indexOf(new URI("http://127.0.0.1:8091/pools"));
      if (oldIndex != newIndex) {
        assertTrue(true);
        return;
      }
    }

    assertTrue("Node list was not different after " + tries + " tries", false);
  }

}
