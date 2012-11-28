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

/**
 * Represents a View definition inside the Couchbase cluster.
 *
 * This class knows whether the view contains "map" and/or "reduce" functions.
 * It also is able to generate the URI representation of itself to be used
 * against the cluster. Also, instances of a View can be used in combination
 * with DesignDocuments to actually create them.
 */
public class View extends AbstractView {

  private final boolean map;
  private final boolean reduce;

  /**
   * Create a new View object.
   *
   * @param database the name of the database.
   * @param designDoc the name of the corresponding design document.
   * @param viewName the name of the view itself.
   * @param map if the View contains a map function or not.
   * @param reduce if the View contains a reduce function or not.
   */
  public View(String database, String designDoc, String viewName,
		boolean map, boolean reduce) {
    super(database, designDoc, viewName);
    this.map = map;
    this.reduce = reduce;
  }

  /**
   * Checks if the view has a "map" method defined.
   *
   * @return true if it has a "map" method defined, false otherwise.
   */
  @Override
  public boolean hasMap() {
    return map;
  }

  /**
   * Checks if the view has a "reduce" method defined.
   *
   * @return true if it has a "reduce" method defined, false otherwise.
   */
  @Override
  public boolean hasReduce() {
    return reduce;
  }

  /**
   * Returns the URI/String representation of the View.
   *
   * @return the URI path of the View to query against the cluster.
   */
  @Override
  public String getURI() {
    return "/" + getDatabaseName() + "/_design/" + getDesignDocumentName()
      + "/_view/" + getViewName();
  }
}