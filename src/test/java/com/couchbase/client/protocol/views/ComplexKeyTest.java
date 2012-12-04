/**
 * Copyright (C) 2012-2012 Couchbase, Inc.
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

import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the creation of complex keys for views.
 */
public class ComplexKeyTest {

  /**
   * Instantiates a new complex key test.
   */
  public ComplexKeyTest() {
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
   * Prepare complex keys using different expressions.
   *
   * @pre  Prepare the expressions for the complex keys.
   * @post Asserts true if the expression string
   * matches the generated JSON
   */
  @Test
  public void testOf() {
    String expResult = "[2012,9,7]";
    ComplexKey result = ComplexKey.of(2012, 9, 7);
    assertEquals(expResult, result.toJson());

    expResult = "[\"Hello\",\"World\",5.12]";
    result = ComplexKey.of("Hello", "World", 5.12);
    assertEquals(expResult, result.toJson());

    expResult = "[true,false]";
    result = ComplexKey.of(true, false);
    assertEquals(expResult, result.toJson());
  }

  /**
   * Prepare complex keys using empty array.
   *
   * @pre  Prepare the expression string.
   * @post Asserts true if the expression string
   * matches the generated JSON
   */
  @Test
  public void testOfEmptyArray() {
    String expResult = "[]";
    ComplexKey result = ComplexKey.of(ComplexKey.emptyArray());
    assertEquals(expResult, result.toJson());
  }

  /**
   * Prepare complex keys using empty object.
   *
   * @pre  Prepare the expression string.
   * @post Asserts true if the expression string
   * matches the generated JSON
   */
  @Test
  public void testOfEmptyObject() {
    String expResult = "{}";
    ComplexKey result = ComplexKey.of(ComplexKey.emptyObject());
    assertEquals(expResult, result.toJson());
  }

  /**
   * Prepare complex keys using an array of objects.
   *
   * @pre  Prepare the expression string.
   * @post Asserts true if the expression string
   * matches the generated JSON
   */
  @Test
  public void testEmptyArray() {
    Object[] expResult = new Object[] {};
    Object[] result = ComplexKey.emptyArray();
    assertArrayEquals(expResult, result);
  }

  /**
   * Prepare complex keys using an empty object.
   *
   * @pre  Prepare the expression string.
   * @post Asserts true if the expression string
   * matches the generated JSON
   */
  @Test
  public void testEmptyObject() {
    Object expResult = new Object();
    Object result = ComplexKey.emptyObject();
    assertEquals(expResult.getClass().getName(), result.getClass().getName());
  }

  /**
   * Tests the construction of more complex JSON strings with Dates.
   *
   * @pre This test case shows how the implicit typecasting happens
   * during the JSON generation phase. If you work with ComplexKeys
   * and you're not dealing with trivial types make sure they have
   * a proper "toString" method implemented.
   * @post Asserts true if the expression string
   * matches the generated JSON
   */
  @Test
  public void testDateInput() {
    Date start = new Date();
    Date end   = new Date();

    String expResult = "[\""+start.toString()+"\",\""+end.toString()+"\"]";
    ComplexKey result = ComplexKey.of(start, end);
    assertEquals(expResult, result.toJson());
  }

  /**
   * Tests forcing the cast to a JSON array even when
   * there is only one element.
   *
   * @pre Work with ComplexKeys to create simple JSON
   * objects and then force it into an array
   * @post Asserts true if json string of the
   * complex key before and after calling force
   * array is 40.
   */
  @Test
  public void testForceArray() {
    ComplexKey simple = ComplexKey.of("40");
    assertEquals("\"40\"", simple.toJson());
    simple.forceArray(true);
    assertEquals("[\"40\"]", simple.toJson());
  }

  /**
   * Tests making complex keys from numeric values.
   *
   * @pre Work with ComplexKeys to create simple JSON
   * objects using numeric values.
   * @post Asserts true if json string of the
   * complex key is equal to the numeric value itself.
   */
  @Test
  public void testNumericValues() {
    ComplexKey singleInt = ComplexKey.of(4444);
    assertEquals("4444", singleInt.toJson());

    ComplexKey singleLong = ComplexKey.of(99999999999L);
    assertEquals("99999999999", singleLong.toJson());

    ComplexKey singleFloat = ComplexKey.of(3.141159f);
    assertEquals("3.141159", singleFloat.toJson());

    ComplexKey singleDouble = ComplexKey.of(3.141159);
    assertEquals("3.141159", singleDouble.toJson());
  }

  /**
   * Tests making complex keys from null object.
   *
   * @pre Work with ComplexKeys to create simple JSON
   * objects from null object.
   * @post Asserts true if json string of the
   * complex key is equal to null.
   */
  // TODO: eventually support this
  @Ignore("Null argument not yet implemented") @Test
  public void testNullSingleValues() {
    ComplexKey singleNull = ComplexKey.of((Object[]) null); // NPE here
    String aNullJsonString = singleNull.toJson();
    assertEquals("null", aNullJsonString);
  }

  /**
   * Tests making complex keys from an array containing null.
   *
   * @pre Work with ComplexKeys to create simple JSON
   * objects from an array containing null.
   * @post Asserts true if json string of the
   * complex key equals comma separated values in
   * the array.
   */
  @Test
  public void testNullInArray() {
    ComplexKey withNull = ComplexKey.of("Matt", null);
    String wNullJsonString = withNull.toJson();
    assertEquals("[\"Matt\",null]", wNullJsonString);
  }

  /**
   * Tests making complex keys from boolean value.
   *
   * @pre Work with ComplexKeys to create simple JSON
   * objects  from boolean value.
   * @post Asserts true if json string of the
   * complex key equals corresponding boolean values.
   */
  @Test
  public void testBoolValues() {
    ComplexKey singleTrue = ComplexKey.of(true);
    assertEquals("true", singleTrue.toJson());

    ComplexKey arrBools = ComplexKey.of(true, false);
    assertEquals("[true,false]", arrBools.toJson());
  }
}
