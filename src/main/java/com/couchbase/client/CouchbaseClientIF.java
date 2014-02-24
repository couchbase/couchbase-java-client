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

  /**
   * Get and lock the given key asynchronously and decode with the default
   * transcoder. By default the maximum allowed timeout is 30 seconds. Timeouts
   * greater than this will be set to 30 seconds.
   *
   * @param key the key to fetch and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  Future<CASValue<Object>> asyncGetAndLock(final String key, int exp);

  /**
   * Gets and locks the given key asynchronously. By default the maximum allowed
   * timeout is 30 seconds. Timeouts greater than this will be set to 30
   * seconds.
   *
   * @param key the key to fetch and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @param tc the transcoder to serialize and unserialize value
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  <T> Future<CASValue<T>> asyncGetAndLock(final String key, int exp,
      final Transcoder<T> tc);

  /**
   * Getl with a single key. By default the maximum allowed timeout is 30
   * seconds. Timeouts greater than this will be set to 30 seconds.
   *
   * @param key the key to get and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @param tc the transcoder to serialize and unserialize value
   * @return the result from the cache (null if there is none)
   * @throws net.spy.memcached.OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   * @throws java.util.concurrent.CancellationException if operation was canceled
   */
  <T> CASValue<T> getAndLock(String key, int exp, Transcoder<T> tc);

  /**
   * Get and lock with a single key and decode using the default transcoder. By
   * default the maximum allowed timeout is 30 seconds. Timeouts greater than
   * this will be set to 30 seconds.
   *
   * @param key the key to get and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @return the result from the cache (null if there is none)
   * @throws net.spy.memcached.OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  CASValue<Object> getAndLock(String key, int exp);

  /**
   * Unlock the given key asynchronously from the cache.
   *
   * @param key the key to unlock
   * @param casId the CAS identifier
   * @param tc the transcoder to serialize and unserialize value
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  <T> OperationFuture<Boolean> asyncUnlock(final String key,
          long casId, final Transcoder<T> tc);

  /**
   * Unlock the given key asynchronously from the cache with the default
   * transcoder.
   *
   * @param key the key to unlock
   * @param casId the CAS identifier
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  OperationFuture<Boolean> asyncUnlock(final String key,
          long casId);

  /**
   * Unlock the given key synchronously from the cache.
   *
   * @param key the key to unlock
   * @param casId the CAS identifier
   * @param tc the transcoder to serialize and unserialize value
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   * @throws java.util.concurrent.CancellationException if operation was canceled
   */
  <T> Boolean unlock(final String key,
          long casId, final Transcoder<T> tc);

  /**
   * Unlock the given key synchronously from the cache with the default
   * transcoder.
   *
   * @param key the key to unlock
   * @param casId the CAS identifier
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  Boolean unlock(final String key, long casId);

  /**
   * Observe a key with a associated CAS.
   *
   * This method allows you to check immediately on the state of a given
   * key/CAS combination. It is normally used by higher-level methods when
   * used in combination with durability constraints (ReplicateTo,
   * PersistTo), but can also be used separately.
   *
   * @param key the key to observe.
   * @param cas the CAS of the key (0 will ignore it).
   * @return ObserveReponse the Response on master and replicas.
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests.
   */
  Map<MemcachedNode, ObserveResponse> observe(final String key, long cas);


  /**
   * Poll and observe a key with the given CAS and persist settings.
   *
   * Based on the given persistence and replication settings, it observes the
   * key and raises an exception if a timeout has been reached. This method is
   * normally utilized through higher-level methods but can also be used
   * directly.
   *
   * If persist is null, it will default to PersistTo.ZERO and if replicate is
   * null, it will default to ReplicateTo.ZERO. This is the default behavior
   * and is the same as not observing at all.
   *
   * @param key the key to observe.
   * @param cas the CAS value for the key.
   * @param persist the persistence settings.
   * @param replicate the replication settings.
   * @param isDelete if the key is to be deleted.
   */
  void observePoll(String key, long cas, PersistTo persist,
      ReplicateTo replicate, boolean isDelete);

  /**
   * Set a value without any durability options with no TTL.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal set()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @return the future result of the set operation.
   */
  OperationFuture<Boolean> set(String key,
          Object value);

  /**
   * Set a value with durability options.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the set() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the set operation.
   */
  OperationFuture<Boolean> set(String key, int exp, Object value,
    PersistTo req);

  /**
   * Set a value with durability options with no TTL
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the set() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the set operation.
   */
  OperationFuture<Boolean> set(String key, Object value, PersistTo req);

  /**
   * Set a value with durability options.
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the set() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the set operation.
   */
  OperationFuture<Boolean> set(String key, int exp, Object value,
    ReplicateTo rep);

  /**
   * Set a value with durability option and no TTL
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the set() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the set operation.
   */
  OperationFuture<Boolean> set(String key,
          Object value, ReplicateTo rep);
  /**
   * Set a value with durability options.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal set()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the set operation.
   */
  OperationFuture<Boolean> set(String key, int exp,
          Object value, PersistTo req, ReplicateTo rep);

  /**
   * Set a value with durability options and not TTL.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal set()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the set operation.
   */
  OperationFuture<Boolean> set(String key,
          Object value, PersistTo req, ReplicateTo rep);

  /**
   * Set a value with durability options and no TTL.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the set() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @return the future result of the set operation.
   */
  OperationFuture<Boolean> add(String key, Object value);

  /**
   * Add a value with durability options.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the add() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the add operation.
   */
  OperationFuture<Boolean> add(String key, int exp,
          Object value, PersistTo req);

  /**
   * Add a value with durability options with No TTL
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the add() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the add operation.
   */
  OperationFuture<Boolean> add(String key,
          Object value, PersistTo req);

  /**
   * Add a value with durability options.
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the add() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the add operation.
   */
  OperationFuture<Boolean> add(String key, int exp,
          Object value, ReplicateTo rep);

  /**
   * Add a value with durability options with no TTL
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the add() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the add operation.
   */
  OperationFuture<Boolean> add(String key,
          Object value, ReplicateTo rep);

  /**
   * Add a value with durability options.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal add()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the add operation.
   */
  OperationFuture<Boolean> add(String key, int exp,
          Object value, PersistTo req, ReplicateTo rep);

  /**
   * Add a value with durability options with no TTL
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal add()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the add operation.
   */
  OperationFuture<Boolean> add(String key,
          Object value, PersistTo req, ReplicateTo rep);

  /**
   * Add a value with durability options.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the add() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @return the future result of the add operation.
   */
  OperationFuture<Boolean> replace(String key,
           Object value);

  /**
   * Replace a value with durability options.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the replace() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the replace operation.
   */
  OperationFuture<Boolean> replace(String key, int exp,
          Object value, PersistTo req);

  /**
   * Replace a value with durability options with no TTL
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the replace() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the replace operation.
   */
  OperationFuture<Boolean> replace(String key, Object value, PersistTo req);

  /**
   * Replace a value with durability options.
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the replace() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the replace operation.
   */
  OperationFuture<Boolean> replace(String key, int exp,
          Object value, ReplicateTo rep);


  /**
   * Replace a value with durability options with no TTL
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the replace() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the replace operation.
   */
  OperationFuture<Boolean> replace(String key, Object value, ReplicateTo rep);


  /**
   * Replace a value with durability options.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal replace()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param exp the expiry value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the replace operation.
   */
  OperationFuture<Boolean> replace(String key, int exp,
          Object value, PersistTo req, ReplicateTo rep);

  /**
   * Replace a value with durability options with no TTL.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal replace()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the replace operation.
   */
  OperationFuture<Boolean> replace(String key,
          Object value, PersistTo req, ReplicateTo rep);

  /**
   * Set a value with a CAS and durability options.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal asyncCAS()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  CASResponse cas(String key, long cas, Object value, PersistTo req,
    ReplicateTo rep);

  /**
   * Set a value with a CAS and durability options.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal asyncCAS()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param exp expiration time for the key.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  CASResponse cas(String key, long cas, int exp, Object value, PersistTo req,
    ReplicateTo rep);

  /**
   * Set a value with a CAS and durability options.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the cas() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  CASResponse cas(String key, long cas, Object value, PersistTo req);


  /**
   * Set a value with a CAS and durability options.
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the cas() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  CASResponse cas(String key, long cas, Object value, ReplicateTo rep);

  /**
   * Set a value with a CAS and durability options.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the cas() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param exp the TTL of the document.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  CASResponse cas(String key, long cas, int exp, Object value, PersistTo req);


  /**
   * Set a value with a CAS and durability options.
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the cas() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param exp the TTL of the document.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  CASResponse cas(String key, long cas, int exp, Object value, ReplicateTo rep);

  /**
   * Set a value with a CAS and durability options.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal asyncCAS()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  OperationFuture<CASResponse> asyncCas(String key, long cas, Object value,
    PersistTo req, ReplicateTo rep);

  /**
   * Set a value with a CAS and durability options.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the cas() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  OperationFuture<CASResponse> asyncCas(String key, long cas, Object value,
    PersistTo req);

  /**
   * Set a value with a CAS and durability options.
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the cas() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the CAS operation.
   */Future<CASResponse> asyncCas(String key, long cas, Object value,
    ReplicateTo rep);

  /**
   * Set a value with a CAS and durability options.
   *
   * This is a shorthand method so that you only need to provide a
   * PersistTo value if you don't care if the value is already replicated.
   * A PersistTo.TWO durability setting implies a replication to at least
   * one node.
   *
   * For more information on how the durability options work, see the docblock
   * for the cas() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param exp the TTL of the document.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  OperationFuture<CASResponse> asyncCas(String key, long cas, int exp,
    Object value, PersistTo req);

  /**
   * Set a value with a CAS and durability options.
   *
   * This method allows you to express durability at the replication level
   * only and is the functional equivalent of PersistTo.ZERO.
   *
   * A common use case for this would be to achieve good insert-performance
   * and at the same time making sure that the data is at least replicated
   * to the given amount of nodes to provide a better level of data safety.
   *
   * For more information on how the durability options work, see the docblock
   * for the cas() operation with both PersistTo and ReplicateTo settings.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param exp the TTL of the document.
   * @param value the value of the key.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  OperationFuture<CASResponse> asyncCas(String key, long cas, int exp,
    Object value, ReplicateTo rep);

  /**
   * Set a value with a CAS and durability options.
   *
   * To make sure that a value is stored the way you want it to in the
   * cluster, you can use the PersistTo and ReplicateTo arguments. The
   * operation will block until the desired state is satisfied or
   * otherwise an exception is raised. There are many reasons why this could
   * happen, the more frequent ones are as follows:
   *
   * - The given replication settings are invalid.
   * - The operation could not be completed within the timeout.
   * - Something goes wrong and a cluster failover is triggered.
   *
   * The client does not attempt to guarantee the given durability
   * constraints, it just reports whether the operation has been completed
   * or not. If it is not achieved, it is the responsibility of the
   * application code using this API to re-retrieve the items to verify
   * desired state, redo the operation or both.
   *
   * Note that even if an exception during the observation is raised,
   * this doesn't mean that the operation has failed. A normal asyncCAS()
   * operation is initiated and after the OperationFuture has returned,
   * the key itself is observed with the given durability options (watch
   * out for Observed*Exceptions) in this case.
   *
   * @param key the key to store.
   * @param cas the CAS value to use.
   * @param exp expiration time for the key.
   * @param value the value of the key.
   * @param req the amount of nodes the item should be persisted to before
   *            returning.
   * @param rep the amount of nodes the item should be replicated to before
   *            returning.
   * @return the future result of the CAS operation.
   */
  OperationFuture<CASResponse> asyncCas(String key, long cas, int exp,
    Object value, PersistTo req, ReplicateTo rep);

  /**
   * Delete a value with durability options for persistence.
   *
   * @param key the key to set
   * @param req the persistence option requested
   * @return whether or not the operation was performed
   */
  OperationFuture<Boolean> delete(String key, PersistTo req);

  /**
   * Delete a value with durability options.
   *
   * The durability options here operate similarly to those documented in
   * the set method.
   *
   * @param key the key to set
   * @param req the Persistence to Master value
   * @param rep the Persistence to Replicas
   * @return whether or not the operation was performed
   */
  OperationFuture<Boolean> delete(String key, PersistTo req,
          ReplicateTo rep);

  /**
   * Delete a value with durability options for replication.
   *
   * @param key the key to set
   * @param req the replication option requested
   * @return whether or not the operation was performed
   *
   */
  OperationFuture<Boolean> delete(String key, ReplicateTo req);

  /**
   * Gets the number of vBuckets that are contained in the cluster. This
   * function is for internal use only and should rarely be since there
   * are few use cases in which it is necessary.
   */
  int getNumVBuckets();

  /**
   * Store a design document in the cluster.
   *
   * @param doc the design document to store.
   * @return a future containing the result of the creation operation.
   */
  HttpFuture<Boolean> asyncCreateDesignDoc(final DesignDocument doc)
    throws UnsupportedEncodingException;

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
   * Delete a design document in the cluster.
   *
   * @param name the design document to delete.
   * @return a future containing the result of the deletion operation.
   */
  HttpFuture<Boolean> asyncDeleteDesignDoc(final String name)
    throws UnsupportedEncodingException;

  /**
   * Gets a future with a design document from the cluster.
   *
   * If no design document was found, the enclosed DesignDocument inside
   * the future will be null.
   *
   * Use {@link #asyncGetDesignDoc(String)} instead.
   *
   * @param designDocumentName the name of the design document.
   * @return a future containing a DesignDocument from the cluster.
   */
  @Deprecated
  HttpFuture<DesignDocument> asyncGetDesignDocument(String designDocumentName);

  /**
   * Gets a future with a design document from the cluster.
   *
   * If no design document was found, the enclosed DesignDocument inside
   * the future will be null.
   *
   * @param designDocumentName the name of the design document.
   * @return a future containing a DesignDocument from the cluster.
   */
  HttpFuture<DesignDocument> asyncGetDesignDoc(String designDocumentName);

  /**
   * Store a design document in the cluster.
   *
   * @param doc the design document to store.
   * @return the result of the creation operation.
   * @throws java.util.concurrent.CancellationException if operation was canceled.
   */
  Boolean createDesignDoc(final DesignDocument doc);

  /**
   * Delete a design document in the cluster.
   *
   * @param name the design document to delete.
   * @return the result of the deletion operation.
   * @throws java.util.concurrent.CancellationException if operation was canceled.
   */
  Boolean deleteDesignDoc(final String name);

  /**
   * Returns a representation of a design document stored in the cluster.
   *
   * Use {@link #getDesignDoc(String)} instead.
   *
   * @param designDocumentName the name of the design document.
   * @return a DesignDocument object from the cluster.
   * @throws com.couchbase.client.protocol.views.InvalidViewException if no design document or view was found.
   * @throws java.util.concurrent.CancellationException if operation was canceled.
   */
  @Deprecated
  DesignDocument getDesignDocument(final String designDocumentName);

  /**
   * Returns a representation of a design document stored in the cluster.
   *
   * @param designDocumentName the name of the design document.
   * @return a DesignDocument object from the cluster.
   * @throws com.couchbase.client.protocol.views.InvalidViewException if no design document or view was found.
   * @throws java.util.concurrent.CancellationException if operation was canceled.
   */
  DesignDocument getDesignDoc(final String designDocumentName);

  /**
   * Get a document from a replica node.
   *
   * This method allows you to explicitly load a document from a replica
   * instead of the master node.
   *
   * This command only works on couchbase type buckets.
   *
   * @param key the key to fetch.
   * @return the fetched document or null when no document available.
   * @throws RuntimeException when less replicas available then in the index
   *         argument defined.
   */
  Object getFromReplica(String key);

  /**
   * Get a document from a replica node including its CAS value.
   *
   * This method allows you to explicitly load a document from a replica
   * instead of the master node including its CAS value.
   *
   * This command only works on couchbase type buckets.
   *
   * @param key the key to fetch.
   * @return the fetched document or null when no document available.
   * @throws RuntimeException when less replicas available then in the index
   *         argument defined.
   */
  CASValue<Object> getsFromReplica(String key);

  /**
   * Get a document from a replica node.
   *
   * This method allows you to explicitly load a document from a replica
   * instead from the master node.
   *
   * This command only works on couchbase type buckets.
   *
   * @param key the key to fetch.
   * @param tc a custom document transcoder.
   * @return the fetched document or null when no document available.
   * @throws RuntimeException when less replicas available then in the index
   *         argument defined.
   */
  <T> T getFromReplica(String key, Transcoder<T> tc);

  /**
   * Get a document from a replica node including its CAS value.
   *
   * This method allows you to explicitly load a document from a replica
   * instead of the master node including its CAS value.
   *
   * This command only works on couchbase type buckets.
   *
   * @param key the key to fetch.
   * @param tc a custom document transcoder.
   * @return the fetched document or null when no document available.
   * @throws RuntimeException when less replicas available then in the index
   *         argument defined.
   */
  <T> CASValue<T> getsFromReplica(String key, Transcoder<T> tc);

  /**
   * Get a document from a replica node asynchronously.
   *
   * This method allows you to explicitly load a document from a replica
   * instead from the master node. This command only works on couchbase
   * type buckets.
   *
   * @param key the key to fetch.
   * @return a future containing the fetched document or null when no document
   *         available.
   * @throws RuntimeException when less replicas available then in the index
   *         argument defined.
   */
  ReplicaGetFuture<Object> asyncGetFromReplica(final String key);

  /**
   * Get a document from a replica node asynchronously and load the CAS.
   *
   * This method allows you to explicitly load a document from a replica
   * instead from the master node. This command only works on couchbase
   * type buckets.
   *
   * @param key the key to fetch.
   * @return a future containing the fetched document or null when no document
   *         available.
   * @throws RuntimeException when less replicas available then in the index
   *         argument defined.
   */
  ReplicaGetFuture<CASValue<Object>> asyncGetsFromReplica(final String key);

  /**
   * Get a document from a replica node asynchronously.
   *
   * This method allows you to explicitly load a document from a replica
   * instead from the master node. This command only works on couchbase
   * type buckets.
   *
   * @param key the key to fetch.
   * @param tc a custom document transcoder.
   * @return a future containing the fetched document or null when no document
   *         available.
   * @throws RuntimeException when less replicas available then in the index
   *         argument defined.
   */
  <T> ReplicaGetFuture<T> asyncGetFromReplica(final String key,
    final Transcoder<T> tc);

  /**
   * Get a document from a replica node asynchronously and load the CAS.
   *
   * This method allows you to explicitly load a document from a replica
   * instead from the master node. This command only works on couchbase
   * type buckets.
   *
   * @param key the key to fetch.
   * @param tc a custom document transcoder.
   * @return a future containing the fetched document or null when no document
   *         available.
   * @throws RuntimeException when less replicas available then in the index
   *         argument defined.
   */
  <T> ReplicaGetFuture<CASValue<T>> asyncGetsFromReplica(final String key,
    final Transcoder<T> tc);

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
  HttpFuture<View> asyncGetView(String designDocumentName, final String viewName);

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
      final String viewName);

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
  View getView(final String designDocumentName, final String viewName);


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
  SpatialView getSpatialView(final String designDocumentName,
    final String viewName);

  OperationFuture<Map<String, String>> getKeyStats(String key);

}
