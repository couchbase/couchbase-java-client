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

import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.internal.ReplicaGetFuture;
import com.couchbase.client.protocol.views.AbstractView;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.Paginator;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.SpatialView;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.Future;

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
  void observePoll(String key, long cas, PersistTo persist,
      ReplicateTo replicate, boolean isDelete);

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

  HttpFuture<Boolean> asyncCreateDesignDoc(final DesignDocument doc)
    throws UnsupportedEncodingException;
  HttpFuture<Boolean> asyncCreateDesignDoc(String name, String value)
    throws UnsupportedEncodingException;
  HttpFuture<Boolean> asyncDeleteDesignDoc(final String name)
    throws UnsupportedEncodingException;
  HttpFuture<DesignDocument> asyncGetDesignDocument(
    String designDocumentName);
  Boolean createDesignDoc(final DesignDocument doc);
  Boolean deleteDesignDoc(final String name);
  DesignDocument getDesignDocument(final String designDocumentName);

  Object getFromReplica(String key);
  <T> T getFromReplica(String key, Transcoder<T> tc);
  ReplicaGetFuture<Object> asyncGetFromReplica(final String key);
  <T> ReplicaGetFuture<T> asyncGetFromReplica(final String key,
    final Transcoder<T> tc);

  HttpFuture<View> asyncGetView(String designDocumentName,
      final String viewName);
  HttpFuture<SpatialView> asyncGetSpatialView(String designDocumentName,
      final String viewName);
  HttpFuture<ViewResponse> asyncQuery(AbstractView view, Query query);
  ViewResponse query(AbstractView view, Query query);
  Paginator paginatedQuery(View view, Query query, int docsPerPage);
  View getView(final String designDocumentName, final String viewName);
  SpatialView getSpatialView(final String designDocumentName,
    final String viewName);

  OperationFuture<Map<String, String>> getKeyStats(String key);

}
