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
 * The ViewDesign object represents a view to be stored and retrieved
 * from the Couchbase cluster.
 */
public class ViewDesign {

  /**
   * The name of the view.
   */
  private String name = null;

  /**
   * The map function of the view.
   */
  private String map = null;

  /**
   * The optional reduce function of the view.
   */
  private String reduce = null;

  /**
   * Create a ViewDesign with a name and a map function.
   *
   * @param name the name of the view.
   * @param map the map function of the view.
   */
  public ViewDesign(String name, String map) {
    this(name, map, "");
  }

  /**
   * Create a ViewDesign with a name, map and reduce function.
   *
   * @param name the name of the view.
   * @param map the map function of the view.
   * @param reduce the reduce function of the view.
   */
  public ViewDesign(String name, String map, String reduce) {
    this.name = name;
    this.map = map;
    this.reduce = reduce;
  }

  /**
   * Get the name of the view.
   *
   * @return the name of the view.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the map function of the view.
   *
   * @return the map function of the view.
   */
  public String getMap() {
    return map;
  }

  /**
   * Get the reduce function of the view.
   *
   * @return the reduce function of the view.
   */
  public String getReduce() {
    return reduce;
  }

}
