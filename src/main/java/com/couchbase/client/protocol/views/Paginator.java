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

/**
 * The {@link Paginator} makes it possible to iterate over a
 * {@link ViewResponse} in pages.
 *
 * <p>It is possible to iterate over both reduced and non-reduced results, but
 * iterating over non-reduced results is considerably faster, because a
 * more efficient pagination approach can be used.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 *
 *View view = client.getView("design_doc", "view_name");
 *Query query = new Query();
 *int docsPerPage = 20;
 *
 *Paginator paginator = client.paginatedQuery(view, query, docsPerPage);
 *while(paginator.hasNext()) {
 *  ViewResponse response = paginator.next();
 *  for(ViewRow row : response) {
 *    System.out.println(row.getKey());
 *  }
 *}
 * }</pre>
 *
 * <p>Note that if a custom limit is set on the {@link Query} object when it
 * gets passed in into the {@link Paginator}, then it is considered as an
 * absolute limit. The {@link Paginator} will stop iterating once this absolute
 * limit is reached. If no limit is provided, the {@link Paginator} will move
 * forward until no more documents are returned by the view.</p>
 *
 * <p>If you encounter an infinite loop when emitting stringified numbers from
 * your View, see the {@link Paginator#forcedKeyType} method for instructions
 * to remedy this situation.</p>
 */
public class Paginator implements Iterator<ViewResponse> {

  private final CouchbaseClient client;
  private final View view;
  private final Query query;
  private final int limit;

  /**
   * Contains the current state of the Paginator.
   */
  private volatile State currentState;

  /**
   * Holds the next response that will be returned on {@link #next()}.
   */
  private ViewResponse nextResponse = null;

  /**
   * A counter indicating the current page (used to for skipping).
   */
  private int currentPage;

  /**
   * The next ID to start when paging with non-reduced views.
   */
  private String nextStartKeyDocID = null;

  /**
   * The next key to start when paging with non-reduced views.
   */
  private String nextStartKey = null;

  /**
   * Helps to prevent errors when {@link #hasNext()} is called twice or more
   * before actually calling {@link #next()}.
   */
  private boolean alreadyCalled;

  /**
   * Used to make sure a total limit on the view result can still be
   * paginated correctly.
   */
  private int totalLimit;

  /**
   * Defines into which the key will be casted into.
   */
  private Class<?> forcedKeyType = null;

  /**
   * Create a new Paginator by passing in the needed params.
   *
   * @param client the client object to work against.
   * @param view the corresponding view to query.
   * @param query the query object to customize the pages.
   * @param limit the amount of docs to return per page.
   */
  public Paginator(final CouchbaseClient client, final View view,
    final Query query, final int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("Number of documents per page "
        + "must be greater than zero.");
    }

    this.client = client;
    this.view = view;
    this.query = query.copy();
    this.limit = limit;

    if (this.query.getLimit() > 0) {
      this.totalLimit = this.query.getLimit();
    } else {
      this.totalLimit = -1;
    }

    this.query.setLimit(limit + 1);
    this.currentState = State.INITIALIZED;
    this.currentPage = 1;
    this.alreadyCalled = false;
  }

  /**
   * Check if another Page is available.
   *
   * @return true if a page is available, false otherwise.
   */
  public final boolean hasNext() {
    if (currentState == State.FINISHED) {
      return false;
    }

    if (alreadyCalled) {
      return true;
    } else {
      alreadyCalled = true;
    }

    fetchNextPage();
    if (currentState == State.INITIALIZED) {
      currentState = State.PAGING;
    }

    return true;
  }

  /**
   * Fetch the next page.
   *
   * Depending on if reduce is used or not, it uses a different approach on
   * how to handle paging.
   */
  private void fetchNextPage() {
    if (currentState == State.PAGING) {
      if (query.willReduce()) {
        query.setSkip(limit * (currentPage - 1));
      } else {
        query.setStartkeyDocID(nextStartKeyDocID);
        query.setRangeStart(convertKey(nextStartKey));
      }
    }

    if (totalLimit > 0 && (currentPage * limit) >= totalLimit) {
      int reduceBy = (currentPage * limit) - totalLimit;
      query.setLimit(limit - reduceBy);
    }

    nextResponse = client.query(view, query);

    if (nextResponse.size() == limit + 1) {
      ViewRow nextRow = nextResponse.removeLastElement();
      if (!query.willReduce()) {
        nextStartKeyDocID = nextRow.getId();
        nextStartKey = nextRow.getKey();
      }
    } else {
      currentState = State.FINISHED;
    }

    currentPage++;
  }

  /**
   * Returns the next {@link ViewResponse}.
   *
   * @return returns a {@link ViewResponse} which represents the next page
   *         or null if there is none (check with {@link #hasNext()} first.
   */
  public final ViewResponse next() {
    alreadyCalled = false;

    if (currentState == State.INITIALIZED) {
      return null;
    }

    return nextResponse;
  }

  /**
   * Allows one to override the type of the row key.
   *
   * <p>This should only be used to enforce a different type if absolutely
   * needed, especially if you emit a number as a string from the view. If
   * nothing else is specified, the data will be passed in 1:1 which may lead
   * to infinite loops on stringified numbers. To remedy this situation,
   * forcing to stringify the number again like this helps:</p>
   *
   *<pre>{@code
   *paginatedQuery.forceKeyType(String.class);
   *}</pre>
   *
   * <p>Setting it to Integer.class will force a conversion to integer
   * the other way round. Note that if the class type is not recognized,
   * the original value will be passed straight through as a String towards
   * the {@link ComplexKey} class.</p>
   *
   * <p>This is ignored on reduced and spatial views, because a different
   * strategy (skip) is used there.</p>
   *
   * @param clazz the enforced key type.
   */
  public void forceKeyType(Class<?> clazz) {
    this.forcedKeyType = clazz;
  }

  /**
   * Converts the paginator key to the intended type.
   *
   * @param original original Key from the View as string
   * @return the modified key.
   */
  private String convertKey(String original) {
    if(forcedKeyType == null) {
      return original;
    }

    if(forcedKeyType.getSimpleName().equals("Integer")) {
      return ComplexKey.of(Integer.parseInt(original)).toJson();
    }
    return ComplexKey.of(original).toJson();
  }

  /**
   * The {@link #remove()} method is not supported in this context.
   */
  public final void remove() {
    throw new UnsupportedOperationException("Remove is unsupported");
  }

  /**
   * Defines the States in which the Paginator is in at any given time.
   */
  enum State {
    /** No Page has yet been fetched, it has just been initialized. */
    INITIALIZED,
    /** Currently in the process of paging, more pages available. */
    PAGING,
    /** Last page reached and/or finished already. */
    FINISHED
  }

}
