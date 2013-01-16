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

/**
 * The base class for Views and Spatial Views.
 *
 * This class acts as a base class for both map/reduce views and spatial
 * views. Do not use this class directly, but instead create instances from
 * either the View or the SpatialView classes.
 */
public abstract class AbstractView {
  private final String viewName;
  private final String designDocumentName;
  private final String databaseName;

  /**
   * Instantiate a AbstractView object.
   *
   * This should only be used by subclasses like View or SpatialView.
   *
   * @param database the name of the database.
   * @param designDoc the name of the corresponding design document.
   * @param view the name of the view itself.
   */
  public AbstractView(String database, String designDoc, String view) {
    databaseName = database;
    designDocumentName = designDoc;
    viewName = view;
  }

  /**
   * Returns the database (bucket) name.
   *
   * @return the database (bucket) name.
   */
  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * Returns the design document name.
   *
   * @return the name of the design document.
   */
  public String getDesignDocumentName() {
    return designDocumentName;
  }

  /**
   * Returns the view name.
   *
   * @return the name of the view.
   */
  public String getViewName() {
    return viewName;
  }

  /**
   * Checks if the view has a "map" method defined.
   *
   * @return true if it has a "map" method defined, false otherwise.
   */
  public abstract boolean hasMap();

  /**
   * Checks if the view has a "reduce" method defined.
   *
   * @return true if it has a "reduce" method defined, false otherwise.
   */
  public abstract boolean hasReduce();

  /**
   * Returns the URI/String representation of the View.
   *
   * @return the URI path of the View to query against the cluster.
   */
  public abstract String getURI();
}
