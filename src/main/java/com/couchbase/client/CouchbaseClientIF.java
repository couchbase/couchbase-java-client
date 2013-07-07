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

package com.couchbase.client;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.Future;

import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.protocol.views.*;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ObserveResponse;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.transcoders.Transcoder;

/**
 * This interface is provided as a helper for testing clients of the
 * CouchbaseClient.
 */
public interface CouchbaseClientIF extends MemcachedClientIF {

  Future<CASValue<Object>> asyncGetAndLock(final String key, int exp);

  <T> Future<CASValue<T>> asyncGetAndLock(final String key, int exp,
      final Transcoder<T> tc);

  <T> CASValue<T> getAndLock(String key, int exp, Transcoder<T> tc);

  CASValue<Object> getAndLock(String key, int exp);

  <T> OperationFuture<Boolean> asyncUnlock(final String key,
          long casId, final Transcoder<T> tc);

  OperationFuture<Boolean> asyncUnlock(final String key,
          long casId);

  <T> Boolean unlock(final String key,
          long casId, final Transcoder<T> tc);

  Boolean unlock(final String key,
          long casId);

  Map<MemcachedNode, ObserveResponse> observe(final String key, long cas);

  OperationFuture<Boolean> set(String key,
          Object value);
  OperationFuture<Boolean> set(String key, int exp,
          Object value, PersistTo persist);
  OperationFuture<Boolean> set(String key,
          Object value, PersistTo persist);
  OperationFuture<Boolean> set(String key, int exp,
          Object value, ReplicateTo replicate);
  OperationFuture<Boolean> set(String key,
          Object value, ReplicateTo replicate);
  OperationFuture<Boolean> set(String key, int exp,
          Object value, PersistTo persist, ReplicateTo replicate);
  OperationFuture<Boolean> set(String key,
          Object value, PersistTo persist, ReplicateTo replicate);
  OperationFuture<Boolean> add(String key,
          Object value);
  OperationFuture<Boolean> add(String key, int exp,
          Object value, PersistTo persist);
  OperationFuture<Boolean> add(String key,
          Object value, PersistTo persist);
  OperationFuture<Boolean> add(String key, int exp,
          Object value, ReplicateTo replicate);
  OperationFuture<Boolean> add(String key,
          Object value, ReplicateTo replicate);
  OperationFuture<Boolean> add(String key, int exp,
          Object value, PersistTo persist, ReplicateTo replicate);
  OperationFuture<Boolean> add(String key,
          Object value, PersistTo persist, ReplicateTo replicate);
  OperationFuture<Boolean> replace(String key,
           Object value);
  OperationFuture<Boolean> replace(String key, int exp,
          Object value, PersistTo persist);
  OperationFuture<Boolean> replace(String key,
          Object value, PersistTo persist);
  OperationFuture<Boolean> replace(String key, int exp,
          Object value, ReplicateTo replicate);
  OperationFuture<Boolean> replace(String key,
          Object value, ReplicateTo replicate);
  OperationFuture<Boolean> replace(String key, int exp,
          Object value, PersistTo persist, ReplicateTo replicate);
  OperationFuture<Boolean> replace(String key,
          Object value, PersistTo persist, ReplicateTo replicate);
  CASResponse cas(String key, long cas,
          Object value, PersistTo req, ReplicateTo rep);
  CASResponse cas(String key, long cas,
          Object value, PersistTo req);
  CASResponse cas(String key, long cas,
          Object value, ReplicateTo rep);
  OperationFuture<Boolean> delete(String key, PersistTo persist);
  OperationFuture<Boolean> delete(String key, PersistTo persist,
          ReplicateTo replicate);
  OperationFuture<Boolean> delete(String key, ReplicateTo replicate);

  int getNumVBuckets();

    /**
     * Gets access to a view contained in a design document from the cluster.
     *
     * The purpose of a view is take the structured data stored within the
     * Couchbase Server database as JSON documents, extract the fields and
     * information, and to produce an index of the selected information.
     *
     * The result is a view on the stored data. The view that is created
     * during this process allows you to iterate, select and query the
     * information in your database from the raw data objects that have
     * been stored.
     *
     * Note that since an HttpFuture is returned, the caller must also check to
     * see if the View is null. The HttpFuture does provide a getStatus() method
     * which can be used to check whether or not the view request has been
     * successful.
     *
     * @param designDocumentName the name of the design document.
     * @param viewName the name of the view to get.
     * @return a View object from the cluster.
     * @throws InterruptedException if the operation is interrupted while in
     *           flight
     * @throws java.util.concurrent.ExecutionException if an error occurs during execution
     */
    HttpFuture<View> asyncGetView(String designDocumentName,
                                  String viewName);

    /**
     * Gets access to a spatial view contained in a design document from the
     * cluster.
     *
     *
     * Note that since an HttpFuture is returned, the caller must also check to
     * see if the View is null. The HttpFuture does provide a getStatus() method
     * which can be used to check whether or not the view request has been
     * successful.
     *
     * @param designDocumentName the name of the design document.
     * @param viewName the name of the spatial view to get.
     * @return a HttpFuture<SpatialView> object from the cluster.
     * @throws InterruptedException if the operation is interrupted while in
     *           flight
     * @throws java.util.concurrent.ExecutionException if an error occurs during execution
     */
    HttpFuture<SpatialView> asyncGetSpatialView(String designDocumentName,
                                                String viewName);

    /**
     * Gets a future with a design document from the cluster.
     *
     * If no design document was found, the enclosed DesignDocument inside
     * the future will be null.
     *
     * @param designDocumentName the name of the design document.
     * @return a future containing a DesignDocument from the cluster.
     */
    HttpFuture<DesignDocument> asyncGetDesignDocument(
            String designDocumentName);

    /**
     * Gets access to a view contained in a design document from the cluster.
     *
     * The purpose of a view is take the structured data stored within the
     * Couchbase Server database as JSON documents, extract the fields and
     * information, and to produce an index of the selected information.
     *
     * The result is a view on the stored data. The view that is created
     * during this process allows you to iterate, select and query the
     * information in your database from the raw data objects that have
     * been stored.
     *
     * @param designDocumentName the name of the design document.
     * @param viewName the name of the view to get.
     * @return a View object from the cluster.
     * @throws com.couchbase.client.protocol.views.InvalidViewException if no design document or view was found.
     * @throws java.util.concurrent.CancellationException if operation was canceled.
     */
    View getView(String designDocumentName, String viewName);

    /**
     * Gets access to a spatial view contained in a design document from the
     * cluster.
     *
     * Spatial views enable you to return recorded geometry data in the bucket
     * and perform queries which return information based on whether the recorded
     * geometries existing within a given two-dimensional range such as a
     * bounding box.
     *
     * @param designDocumentName the name of the design document.
     * @param viewName the name of the view to get.
     * @return a SpatialView object from the cluster.
     * @throws com.couchbase.client.protocol.views.InvalidViewException if no design document or view was found.
     * @throws java.util.concurrent.CancellationException if operation was canceled.
     */
    SpatialView getSpatialView(String designDocumentName,
                               String viewName);

    /**
     * Returns a representation of a design document stored in the cluster.
     *
     * @param designDocumentName the name of the design document.
     * @return a DesignDocument object from the cluster.
     * @throws com.couchbase.client.protocol.views.InvalidViewException if no design document or view was found.
     * @throws java.util.concurrent.CancellationException if operation was canceled.
     */
    DesignDocument getDesignDocument(String designDocumentName);

    /**
     * Store a design document in the cluster.
     *
     * @param doc the design document to store.
     * @return the result of the creation operation.
     * @throws java.util.concurrent.CancellationException if operation was canceled.
     */
    Boolean createDesignDoc(DesignDocument doc);

    /**
    * Store a design document in the cluster.
    *
    * @param name the name of the design document.
    * @param value the full design document definition as a string.
    * @return a future containing the result of the creation operation.
    */
    HttpFuture<Boolean> asyncCreateDesignDoc(String name, String value)
     throws UnsupportedEncodingException;

    /**
     * Store a design document in the cluster.
     *
     * @param doc the design document to store.
     * @return a future containing the result of the creation operation.
     */
    HttpFuture<Boolean> asyncCreateDesignDoc(DesignDocument doc)
      throws UnsupportedEncodingException;

    /**
     * Delete a design document in the cluster.
     *
     * @param name the design document to delete.
     * @return the result of the deletion operation.
     * @throws java.util.concurrent.CancellationException if operation was canceled.
     */
    Boolean deleteDesignDoc(String name);

    /**
    * Delete a design document in the cluster.
    *
    * @param name the design document to delete.
    * @return a future containing the result of the deletion operation.
    */
    HttpFuture<Boolean> asyncDeleteDesignDoc(String name)
     throws UnsupportedEncodingException;

    HttpFuture<ViewResponse> asyncQuery(AbstractView view, Query query);

    /**
     * Queries a Couchbase view and returns the result.
     * The result can be accessed row-wise via an iterator.
     * This type of query will return the view result along
     * with all of the documents for each row in
     * the query.
     *
     * @param view the view to run the query against.
     * @param query the type of query to run against the view.
     * @return a ViewResponseWithDocs containing the results of the query.
     * @throws java.util.concurrent.CancellationException if operation was canceled.
     */
    ViewResponse query(AbstractView view, Query query);

    /**
     * A paginated query allows the user to get the results of a large query in
     * small chunks allowing for better performance. The result allows you
     * to iterate through the results of the query and when you get to the end
     * of the current result set the client will automatically fetch the next set
     * of results.
     *
     * @param view the view to query against.
     * @param query the query for this request.
     * @param docsPerPage the amount of documents per page.
     * @return A Paginator (iterator) to use for reading the results of the query.
     */
    Paginator paginatedQuery(View view, Query query, int docsPerPage);
}
