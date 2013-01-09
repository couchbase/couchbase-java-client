/**
 * Copyright (C) 2006-2009 Dustin Sallings
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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import net.spy.memcached.TestConfig;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test for basic things in the CouchbaseConnectionFactoryBuilder.
 *
 */
public class CouchbaseConnectionFactoryBuilderTest {

  private List<URI> uris = Arrays.asList(URI.create(
    "http://" + TestConfig.IPV4_ADDR + ":8091/pools"));

  /**
   * Test setting the observer poll interval as 600.
   *
   * @pre Instantiate the connection factory builder and
   * set the observer poll interval as 600 and assert it.
   * @post Assert that the observer poll interval was not set.
   * And build the connection.
   *
   * @throws IOException
   * Signals that an I/O exception has occurred.
   */
  @Test
  public void testSetObsPollInterval() throws IOException {
    long timeInterval = 600L;
    CouchbaseConnectionFactoryBuilder instance =
      new CouchbaseConnectionFactoryBuilder();
    CouchbaseConnectionFactoryBuilder instanceResult =
      instance.setObsPollInterval(timeInterval);
    assertEquals("Failed to set observe poll interval.", 600L,
      instanceResult.getObsPollInterval());
    assertEquals(instance, instanceResult);
    instance.buildCouchbaseConnection(uris, "default", "");
  }

  /**
   * Test setting the max observer poll interval as 40.
   *
   * @pre Instantiate the connection factory builder and
   * set the observer poll interval as 40 and assert it.
   * @post Asserts no error if correctly set
   * and then build connection.
   *
   * @throws IOException
   * Signals that an I/O exception has occurred.
   */
  @Test
  public void testSetObsPollMax() throws IOException {
    int maxPoll = 40;
    CouchbaseConnectionFactoryBuilder instance =
      new CouchbaseConnectionFactoryBuilder();
    CouchbaseConnectionFactoryBuilder instanceResult
      = instance.setObsPollMax(maxPoll);
    assertEquals(instance, instanceResult);
    assertEquals(maxPoll, instanceResult.getObsPollMax());
    instance.buildCouchbaseConnection(uris, "default", "");
  }

  /**
   * Test setting the time out limits.
   *
   * @pre  Instantiate the connection factory builder
   * and set the view timeout as 30000 and assert it.
   * Also try to set 200 and as its not equal to 500
   * then an assert with error message will appear.
   * @post  Asserts no error if correctly set and
   * then build connection.
   *
   * @throws IOException
   * Signals that an I/O exception has occurred.
   */
  @Test
  public void testSetViewTimeout() throws IOException {
    int viewTimeout = 30000;
    int lowTimeout = 200;
    int lowerTimeoutLimit = 500;

    CouchbaseConnectionFactoryBuilder instance =
      new CouchbaseConnectionFactoryBuilder();
    CouchbaseConnectionFactoryBuilder instanceResult
      = instance.setViewTimeout(viewTimeout);
    assertEquals(instance, instanceResult);
    assertEquals(viewTimeout, instanceResult.getViewTimeout());

    instanceResult = instance.setViewTimeout(lowTimeout);
    assertEquals(lowerTimeoutLimit, instanceResult.getViewTimeout());

    instance.buildCouchbaseConnection(uris, "default", "");
  }

  /**
   * Test to be sure that the default values are the expected values.
   *
   * This especially verifies the view timeout, which has been reported in
   * JCBC-168.
   *
   * @throws IOException
   */
  @Test
  public void testDefaultValues() throws IOException {

    CouchbaseConnectionFactoryBuilder instance =
      new CouchbaseConnectionFactoryBuilder();

    CouchbaseConnectionFactory connFact =
      instance.buildCouchbaseConnection(uris, "default", "");

    assertEquals(CouchbaseConnectionFactory.DEFAULT_VIEW_TIMEOUT,
      connFact.getViewTimeout());
    assertEquals(CouchbaseConnectionFactory.DEFAULT_OBS_POLL_INTERVAL,
      connFact.getObsPollInterval());
    assertEquals(CouchbaseConnectionFactory.DEFAULT_OBS_POLL_MAX,
      connFact.getObsPollMax());
    assertEquals(CouchbaseConnectionFactory.DEFAULT_MIN_RECONNECT_INTERVAL,
      connFact.getMinReconnectInterval());
  }

}
