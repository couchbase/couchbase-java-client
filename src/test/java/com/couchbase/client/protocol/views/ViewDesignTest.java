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

package com.couchbase.client.protocol.views;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the various creation/modification ways of the ViewDesign class.
 */
public class ViewDesignTest {

  /**
   * Tests the ViewDesign instance created
   * from a view name and a map.
   *
   * @pre Prepare ViewDesign using view
   * name and a map as the criteria.
   * @post Asserts true.
   */
  @Test
  public void testViewDesignWithoutReduce() {
    String name = "beers";
    String map = "function(){}";
    ViewDesign view = new ViewDesign(name, map);
    assertEquals(name, view.getName());
    assertEquals(map, view.getMap());
    assertEquals("", view.getReduce());
  }

  /**
   * Tests the ViewDesign instance created from
   * a view name, map and reduce functions.
   *
   * @pre Prepare ViewDesign using view name,
   * map and reduce functions as the criteria.
   * @post Asserts true.
   */
  @Test
  public void testViewDesignWithReduce() {
    String name = "beers";
    String map = "function(){}";
    String reduce = "function(){}";
    ViewDesign view = new ViewDesign(name, map, reduce);
    assertEquals(name, view.getName());
    assertEquals(map, view.getMap());
    assertEquals(reduce, view.getReduce());
  }
}
