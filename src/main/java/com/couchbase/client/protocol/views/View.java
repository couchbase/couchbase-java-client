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
 * Holds information about a view that can be queried in
 * Couchbase Server.
 */
public class View extends AbstractView {

  private final boolean map;
  private final boolean reduce;

  public View(String dn, String ddn, String vn, boolean m, boolean r) {
    super(dn, ddn, vn);
    map = m;
    reduce = r;
  }

  @Override
  public boolean hasMap() {
    return map;
  }

  @Override
  public boolean hasReduce() {
    return reduce;
  }

  @Override
  public String getURI() {
    return "/" + getDatabaseName() + "/_design/" + getDesignDocumentName()
      + "/_view/" + getViewName();
  }
}
