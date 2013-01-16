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

import com.couchbase.client.CouchbaseClient;
import java.util.Iterator;
import net.spy.memcached.compat.SpyObject;

/**
 * The Paginator allows easy pagination over a ViewResult.
 *
 * It is recommended not to instantiate this object directly, but to obtain
 * a reference through the CouchbaseClient object like this:
 *
 *   View view = client.getView("design_doc", "viewname");
 *   Query query = new Query();
 *   int docsPerPage = 15;
 *   Paginator paginator = client.paginatedQuery(view, query, docsPerPage);
 *
 */
public class Paginator extends SpyObject
  implements Iterator<ViewResponse> {

  private final CouchbaseClient client;
  private final Query query;
  private final View view;
  private final int docsPerPage;
  private int docsOnPage;
  private int totalLimit = 0;
  private int totalDocs = 0;

  private ViewResponse page;
  private ViewResponse finalRow;
  private boolean first = true;
  private boolean done = false;

  /**
   * Create a new Paginator.
   *
   * @param client the CouchbaseClient object to run the queries on.
   * @param view the instance of the View to run the Queries against.
   * @param query the instance of the Query object for the params.
   * @param numDocs the number of the documents to return per page (must be
   *    greater than zero).
   */
  public Paginator(CouchbaseClient client, View view, Query query,
      int numDocs) {
    this.client = client;
    this.view = view;
    if (query.willReduce()) {
      throw new RuntimeException("Pagination is not supported for reduced"
          + " views");
    }
    this.query = query.copy();
    if(numDocs > 0) {
      this.docsPerPage = numDocs;
    } else {
      throw new IllegalArgumentException("Number of documents per page "
        + "must be greater than zero.");
    }
    this.docsOnPage = 0;
    this.totalLimit = query.getLimit();
    getNextPage(this.query);
  }

  /**
   * Check if there is still a ViewResult left to fetch.
   *
   * @return true or false whether there is a ViewResult set to return or not.
   */
  @Override
  public boolean hasNext() {
    if (first) {
      return true;
    }
    return !done;
  }

  /**
   * Returns the next ViewResult object to iterate over.
   *
   * @return the next ViewResult page.
   */
  @Override
  public ViewResponse next() {
    if (first) {
      first = false;
      return page;
    }
    if (!done) {
      query.setSkip(0);
      query.setStartkeyDocID(finalRow.iterator().next().getId());
      query.setRangeStart(finalRow.iterator().next().getKey());
      return getNextPage(query);
    } else {
      return null;
    }
  }

  /**
   * Remove is not supported while iterating over a ViewResult.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is unsupported");
  }

  /**
   * Reads the next page based on the given page params and the Query object.
   *
   * @param q the Query object to work against.
   * @return the next ViewResponse page.
   */
  private ViewResponse getNextPage(Query q) {
    int remaining = totalLimit > 0 ? totalLimit - totalDocs : docsPerPage;
    q.setLimit(remaining);
    page = client.query(view, q);
    docsOnPage = page.size();
    totalDocs += docsOnPage;
    if (docsOnPage >= docsPerPage) {
      q.setSkip(docsOnPage);
      q.setLimit(1);
      finalRow = client.query(view, q);
      if(!finalRow.iterator().hasNext()) {
        done = true;
      }
    } else {
      done = true;
    }
    return page;
  }
}
