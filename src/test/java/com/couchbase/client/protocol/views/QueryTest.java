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

package com.couchbase.client.protocol.views;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the various creation/modification ways of the Query class.
 */
public class QueryTest {

  public QueryTest() {
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
   * Tests the default settings of the Query object.
   */
  @Test
  public void testInit() {
    Query query = new Query();

    assertEquals(0, query.getArgs().size());
    assertEquals(false, query.willReduce());
    assertEquals(false, query.willIncludeDocs());
    assertEquals("", query.toString());
  }

  /**
   * Tests the "descending" argument.
   */
  @Test
  public void testDescending() {
    Query query = new Query();
    query.setDescending(true);

    assertEquals(1, query.getArgs().size());
    assertEquals("?descending=true", query.toString());
  }

  /**
   * Tests the "startkey_docid" argument.
   */
  @Test
  public void testStartkey() {
    Query query = new Query();
    query.setStartkeyDocID("1234");

    assertEquals(1, query.getArgs().size());
    assertEquals("?startkey_docid=1234", query.toString());
  }

  /**
   * Tests the "endkey_docid" argument.
   */
  @Test
  public void testEndkey() {
    Query query = new Query();
    query.setEndkeyDocID("56789111");

    assertEquals(1, query.getArgs().size());
    assertEquals("?endkey_docid=56789111", query.toString());
  }

  /**
   * Tests both the "group" and the "group_level" arguments.
   */
  @Test
  public void testGroup() {
    Query query = new Query();
    query.setGroup(true);
    query.setGroupLevel(2);

    assertEquals(2, query.getArgs().size());
    assertEquals("?group_level=2&group=true", query.toString());
  }

  /**
   * Tests the "inclusive_end" argument.
   */
  @Test
  public void testInclusiveEnd() {
    Query query = new Query();
    query.setInclusiveEnd(true);

    assertEquals(1, query.getArgs().size());
    assertEquals("?inclusive_end=true", query.toString());
  }

  /**
   * Tests the "key" argument.
   */
  @Test
  public void testKey() throws UnsupportedEncodingException {
    Query query = new Query();
    query.setKey("foobar");

    assertEquals(1, query.getArgs().size());
    String result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=\"foobar\"", result);
  }

  /**
   * Tests the "keys" argument.
   */
  @Test
  public void testKeys() throws UnsupportedEncodingException {
    Query query = new Query();
    query.setKeys("[2, 3, 4]");

    assertEquals(1, query.getArgs().size());
    String result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?keys=[2, 3, 4]", result);
  }

  /**
   * Tests the "limit" argument.
   */
  @Test
  public void testLimit() {
    Query query = new Query();
    query.setLimit(5);

    assertEquals(5, query.getLimit());
    assertEquals(1, query.getArgs().size());
    assertEquals("?limit=5", query.toString());
  }

  /**
   * Tests the "reduce" argument.
   */
  @Test
  public void testReduce() {
    Query query = new Query();
    query.setReduce(true);

    assertEquals(1, query.getArgs().size());
    assertEquals("?reduce=true", query.toString());
  }

  /**
   * Tests the "range" argument.
   */
  @Test
  public void testRange() throws UnsupportedEncodingException {
    Query query = new Query();
    query.setRangeStart("startkey");
    query.setRangeEnd("endkey");
    assertEquals(2, query.getArgs().size());
    String result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?startkey=\"startkey\"&endkey=\"endkey\"", result);

    query = new Query();
    query.setRange("foo", "bar");
    assertEquals(2, query.getArgs().size());
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?startkey=\"foo\"&endkey=\"bar\"", result);
  }

  /**
   * Test docs to skip.
   */
  @Test
  public void testSkip() {
    Query query = new Query();
    query.setSkip(50);

    assertEquals(1, query.getArgs().size());
    assertEquals("?skip=50", query.toString());
  }

  /**
   * Tests the "stale" argument.
   */
  @Test
  public void testStale() {
    Query query = new Query();
    query.setStale(Stale.UPDATE_AFTER);

    assertEquals(1, query.getArgs().size());
    assertEquals("?stale=update_after", query.toString());
  }

  /**
   * Test the "include_docs" argument.
   */
  @Test
  public void testIncludeDocs() {
    Query query = new Query();

    assertEquals(false, query.willIncludeDocs());
    query.setIncludeDocs(true);
    assertEquals(true, query.willIncludeDocs());
    assertTrue(query.toString().isEmpty());
  }

  /**
   * Tests the "on_error" argument.
   */
  @Test
  public void testOnError() {
    Query query = new Query();
    query.setOnError(OnError.CONTINUE);

    assertEquals(1, query.getArgs().size());
    assertEquals("?on_error=continue", query.toString());
  }

  /**
   * Tests the "bbox" argument.
   */
  @Test
  public void testBbox() throws UnsupportedEncodingException {
    Query query = new Query();
    query.setBbox(0, 1, 2.0, 3);

    assertEquals(1, query.getArgs().size());
    String result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?bbox=0.0,1.0,2.0,3.0", result);
  }

  /**
   * Tests the "debug" argument.
   */
  @Test
  public void testDebug() {
    Query query = new Query();
    query.setDebug(true);

    assertEquals(1, query.getArgs().size());
    assertEquals("?debug=true", query.toString());
  }

  /**
   * Tests the usage of complex keys for supported methods.
   */
  @Test
  public void testComplexKeys() throws UnsupportedEncodingException {
    ComplexKey start = ComplexKey.of(2012, 05, 05);
    ComplexKey end = ComplexKey.of(2012, 05, 12);
    ComplexKey key = ComplexKey.of("mykey");
    ComplexKey keys = ComplexKey.of("users:10", "users:11");

    Query query = new Query();
    query.setRangeStart(start);
    query.setRangeEnd(end);
    assertEquals(2, query.getArgs().size());
    String result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?startkey=[2012,5,5]&endkey=[2012,5,12]", result);

    query = new Query();
    query.setRange(start, end);
    assertEquals(2, query.getArgs().size());
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?startkey=[2012,5,5]&endkey=[2012,5,12]", result);

    query = new Query();
    query.setKey(key);
    assertEquals(1, query.getArgs().size());
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=\"mykey\"", result);

    query = new Query();
    query.setKeys(keys);
    assertEquals(1, query.getArgs().size());
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?keys=[\"users:10\",\"users:11\"]", result);
  }

  /**
   * This test verifies how numeric strings passed in are casted to
   * integer values and how the ComplexKey class can be used to pass
   * in numeric strings accordingly.
   */
  @Test
  public void testNumericStrings() throws UnsupportedEncodingException {
    Query query = new Query();
    query.setKey("300");
    String result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=300", result);

    query.setKey("\"300\"");
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=\"300\"", result);

    query.setKey("[300,400,\"500\"]");
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=[300,400,\"500\"]", result);

    query.setKey(ComplexKey.of("300"));
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=\"300\"", result);

    query.setKey(ComplexKey.of(300));
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=300", result);

    query.setKey(ComplexKey.of(99999999999l));
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=99999999999", result);

    query.setKey(ComplexKey.of(300, 400, "500"));
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?key=[300,400,\"500\"]", result);

    query = new Query();
    query.setRangeStart(ComplexKey.of("0000"));
    query.setRangeEnd(ComplexKey.of("Level+2"));
    result = URLDecoder.decode(query.toString(), "UTF-8");
    assertEquals("?startkey=\"0000\"&endkey=\"Level+2\"", result);
  }

  @Test
  public void testBooleans() {
    Query query = new Query();
    query.setKey(ComplexKey.of(true));
    query.setKeys(ComplexKey.of(false));

    assertEquals("?keys=false&key=true", query.toString());
  }

}
