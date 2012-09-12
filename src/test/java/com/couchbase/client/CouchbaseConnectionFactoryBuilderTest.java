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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Test for basic things in the CouchbaseConnectionFactoryBuilder.
 *
 */
public class CouchbaseConnectionFactoryBuilderTest {

  private List<URI> uris = Arrays.asList(
      URI.create("http://localhost:8091/pools"));

  public CouchbaseConnectionFactoryBuilderTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of setObsPollInterval method, of class
   * CouchbaseConnectionFactoryBuilder.
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
   * Test of setObsPollMax method, of class CouchbaseConnectionFactoryBuilder.
   */
  @Test
  public void testSetObsPollMax() throws IOException {
    System.out.println("setObsPollMax");
    int maxPoll = 40;
    CouchbaseConnectionFactoryBuilder instance =
      new CouchbaseConnectionFactoryBuilder();
    CouchbaseConnectionFactoryBuilder instanceResult
      = instance.setObsPollMax(40);
    assertEquals(instance, instanceResult);
    assertEquals(40, instanceResult.getObsPollMax());
    instance.buildCouchbaseConnection(uris, "default", "");
  }
}
