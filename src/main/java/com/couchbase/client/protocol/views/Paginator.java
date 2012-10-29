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

import com.couchbase.client.CouchbaseClient;

import java.util.Iterator;

import net.spy.memcached.compat.SpyObject;

/**
 * A Paginator.
 */
public class Paginator extends SpyObject
  implements Iterator<ViewResponse> {

  private static final int MIN_RESULTS = 15;

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

  public Paginator(CouchbaseClient client, View view, Query query,
      int numDocs) {
    this.client = client;
    this.view = view;
    this.query = query.copy();
    this.docsPerPage = (MIN_RESULTS > numDocs) ? MIN_RESULTS : numDocs;
    this.docsOnPage = 0;
    this.totalLimit = query.getLimit();
    getNextPage(this.query);
  }

  @Override
  public boolean hasNext() {
    if (first) {
      return true;
    }
    return !done;
  }

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

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is unsupported");
  }

  private ViewResponse getNextPage(Query q) {
    if (query.willReduce()) {
      throw new RuntimeException("Pagination is not supported for reduced"
          + " views");
    }
    int remaining = totalLimit > 0 ? totalLimit - totalDocs : docsPerPage;
    q.setLimit(remaining);
    page = client.query(view, q);
    docsOnPage = page.size();
    totalDocs += docsOnPage;
    if (docsOnPage >= docsPerPage) {
      q.setSkip(docsOnPage);
      q.setLimit(1);
      finalRow = client.query(view, q);
    } else {
      done = true;
    }
    return page;
  }
}
