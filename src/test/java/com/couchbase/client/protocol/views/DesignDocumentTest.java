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

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the various creation/modification ways of the DesignDocument class.
 */
public class DesignDocumentTest {

  /**
   * Tests the design documents without passing any
   * spatial parameters or dimensions.
   *
   * @pre Create list of ViewDesign elements and
   * create a DesignDocument using the same.
   * @post Asserts true if the expression string
   * matches the generated JSON
   */
  @Test
  public void testDesignDocumentWithoutSpatial() {
    String viewname = "myview";
    String viewmap = "function()...";
    ViewDesign view = new ViewDesign(viewname, viewmap);
    List<ViewDesign> views = new ArrayList<ViewDesign>();
    views.add(view);

    DesignDocument doc = new DesignDocument("mydesigndoc", views, null);
    String expected = "{\"language\":\"javascript\",\"views\":{\""
      + viewname + "\":{\"map\":\"" + viewmap + "\"}}}";
    assertEquals(expected, doc.toJson());
  }

  /**
   * Tests the design documents with spatial
   * parameters or dimensions.
   *
   * @pre Create list of ViewDesign elements.
   * Also create a list of SpatialViewDesign using
   * maps of the coordinates of a point and create
   * a DesignDocument using both the lists.
   * @post Asserts true if the expression string
   * matches the generated JSON
   */
  @Test
  public void testDesignDocumentWithSpatial() {
    String viewname = "myview";
    String viewmap = "function()...";
    ViewDesign view = new ViewDesign(viewname, viewmap);
    List<ViewDesign> views = new ArrayList<ViewDesign>();
    views.add(view);

    String spatialname = "points";
    String spatialmap = "emit({\"type\": \"Point\", coordinates: "
      + "[1.0, 0.0]}, null);";
    SpatialViewDesign spatialView = new SpatialViewDesign(spatialname,
      spatialmap);
    List<SpatialViewDesign> spatialViews = new ArrayList<SpatialViewDesign>();
    spatialViews.add(spatialView);

    String expectedSpatialMap = "emit({\\\"type\\\": \\\"Point\\\", "
      + "coordinates: [1.0, 0.0]}, null);";
    DesignDocument doc = new DesignDocument("mydesigndoc", views, spatialViews);
    String expected = "{\"language\":\"javascript\",\"views\":{\""
      + viewname + "\":{\"map\":\"" + viewmap + "\"}},\"spatial\":{\""
      + spatialname +"\":\"" + expectedSpatialMap + "\"}}";
    assertEquals(expected, doc.toJson());
  }

}
