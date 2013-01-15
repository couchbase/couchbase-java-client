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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * Verify the correct behavior of the CouchbaseProperties class.
 */
public class CouchbasePropertiesTest {

  @Test
  public void testDefaults() {
    String namespace = CouchbaseProperties.getNamespace();
    assertEquals("cbclient", namespace);
    assertFalse(CouchbaseProperties.hasFileProperties());

    assertNull(CouchbaseProperties.getProperty("viewmode"));
    assertNull(CouchbaseProperties.getProperty("viewmode", true));
    assertEquals("default",
      CouchbaseProperties.getProperty("viewmode", "default"));
    assertEquals("default",
      CouchbaseProperties.getProperty("viewmode", "default", true));
  }

  @Test
  public void testPropertiesThroughCode() {
    String viewmode = "development";
    System.setProperty("viewmode", viewmode);
    System.setProperty("cbclient.viewmode", viewmode);

    assertEquals(viewmode, CouchbaseProperties.getProperty("viewmode"));
    assertEquals(viewmode, CouchbaseProperties.getProperty("viewmode", true));
  }

  @Test
  public void testPropertiesThroughFile() {
    assertFalse(CouchbaseProperties.hasFileProperties());

    assertNull(CouchbaseProperties.getProperty("throttler"));
    CouchbaseProperties.setPropertyFile("cbclient-test.properties");
    assertEquals("demo_throttler", CouchbaseProperties.getProperty("throttler"));
    assertEquals("demo_throttler",
      CouchbaseProperties.getProperty("throttler", true));

    CouchbaseProperties.resetFileProperties();
  }

  @Test
  public void testInvalidPropertiesFile() {
    assertFalse(CouchbaseProperties.hasFileProperties());

    CouchbaseProperties.setPropertyFile(null);
    assertFalse(CouchbaseProperties.hasFileProperties());

    CouchbaseProperties.setPropertyFile("invalid_filename.properties");
    assertFalse(CouchbaseProperties.hasFileProperties());
  }

  @Test
  public void testMixedProperties() {
    assertFalse(CouchbaseProperties.hasFileProperties());
    System.setProperty("throttler", "overridden");
    CouchbaseProperties.setPropertyFile("cbclient-test.properties");
    assertEquals("demo_throttler",
      CouchbaseProperties.getProperty("throttler"));
    assertEquals("overridden",
      CouchbaseProperties.getProperty("throttler", true));
    CouchbaseProperties.resetFileProperties();
  }

}
