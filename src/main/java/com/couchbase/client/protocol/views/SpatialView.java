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

package com.couchbase.client.protocol.views;

/**
 * Holds information about a spatial view that can be queried in
 * Couchbase Server.
 */
public class SpatialView extends AbstractView {

  /**
   * Create a new Spatial View object.
   *
   * @param database the name of the database.
   * @param designDoc the name of the corresponding design document.
   * @param viewName the name of the view itself.
   */
  public SpatialView(String database, String designDoc, String viewName) {
    super(database, designDoc, viewName);
  }

  /**
   * Will always return true, because Spatial Views need to have a map
   * function.
   *
   * @return true.
   */
  @Override
  public boolean hasMap() {
    return true;
  }

  /**
   * Will always return false, because Spatial Views can't have reduce
   * functions.
   *
   * @return false.
   */
  @Override
  public boolean hasReduce() {
    return false;
  }

  /**
   * Returns the URI/String representation of the Spatial View.
   *
   * @return the URI path of the Spatial View to query against the cluster.
   */
  @Override
  public String getURI() {
    return "/" + getDatabaseName() + "/_design/" + getDesignDocumentName()
      + "/_spatial/" + getViewName();
  }
}