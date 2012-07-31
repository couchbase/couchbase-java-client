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

package com.couchbase.client;

import java.util.concurrent.Future;

import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClientIF;
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

  ObserveResponse[] observe(final String key, long cas);

  OperationFuture<Boolean> set(String key, int exp,
          String value, PersistTo persist);
  OperationFuture<Boolean> set(String key, int exp,
          String value, PersistTo persist, ReplicateTo replicate);
  int getNumVBuckets();
}
