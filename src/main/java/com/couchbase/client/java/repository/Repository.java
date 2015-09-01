/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.repository;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.EntityDocument;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The repository abstraction for entities on top of a bucket.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public interface Repository {

    AsyncRepository async();

    <T> EntityDocument<T> get(String id, Class<T> entityClass);
    <T> EntityDocument<T> get(String id, Class<T> entityClass, long timeout, TimeUnit timeUnit);

    <T> List<EntityDocument<T>> getFromReplica(String id, ReplicaMode type, Class<T> entityClass);
    <T> List<EntityDocument<T>> getFromReplica(String id, ReplicaMode type, Class<T> entityClass, long timeout, TimeUnit timeUnit);

    <T> EntityDocument<T> getAndLock(String id, int lockTime, Class<T> entityClass);
    <T> EntityDocument<T> getAndLock(String id, int lockTime, Class<T> entityClass, long timeout, TimeUnit timeUnit);

    <T> EntityDocument<T> getAndTouch(String id, int expiry, Class<T> entityClass);
    <T> EntityDocument<T> getAndTouch(String id, int expiry, Class<T> entityClass, long timeout, TimeUnit timeUnit);

    boolean exists(String id);
    boolean exists(String id, long timeout, TimeUnit timeUnit);
    <T> boolean exists(EntityDocument<T> document);
    <T> boolean exists(EntityDocument<T> document, long timeout, TimeUnit timeUnit);

    <T> EntityDocument<T> upsert(EntityDocument<T> document);
    <T> EntityDocument<T> upsert(EntityDocument<T> document, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> upsert(EntityDocument<T> document, PersistTo persistTo);
    <T> EntityDocument<T> upsert(EntityDocument<T> document, PersistTo persistTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> upsert(EntityDocument<T> document, ReplicateTo replicateTo);
    <T> EntityDocument<T> upsert(EntityDocument<T> document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> upsert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo);
    <T> EntityDocument<T> upsert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    <T> EntityDocument<T> insert(EntityDocument<T> document);
    <T> EntityDocument<T> insert(EntityDocument<T> document, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> insert(EntityDocument<T> document, PersistTo persistTo);
    <T> EntityDocument<T> insert(EntityDocument<T> document, PersistTo persistTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> insert(EntityDocument<T> document, ReplicateTo replicateTo);
    <T> EntityDocument<T> insert(EntityDocument<T> document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> insert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo);
    <T> EntityDocument<T> insert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    <T> EntityDocument<T> replace(EntityDocument<T> document);
    <T> EntityDocument<T> replace(EntityDocument<T> document, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> replace(EntityDocument<T> document, PersistTo persistTo);
    <T> EntityDocument<T> replace(EntityDocument<T> document, PersistTo persistTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> replace(EntityDocument<T> document, ReplicateTo replicateTo);
    <T> EntityDocument<T> replace(EntityDocument<T> document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> replace(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo);
    <T> EntityDocument<T> replace(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    <T> EntityDocument<T> remove(EntityDocument<T> document);
    <T> EntityDocument<T> remove(EntityDocument<T> document, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> remove(EntityDocument<T> document, PersistTo persistTo);
    <T> EntityDocument<T> remove(EntityDocument<T> document, PersistTo persistTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> remove(EntityDocument<T> document, ReplicateTo replicateTo);
    <T> EntityDocument<T> remove(EntityDocument<T> document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> remove(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo);
    <T> EntityDocument<T> remove(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> remove(String id, Class<T> entityClass);
    <T> EntityDocument<T> remove(String id, Class<T> entityClass, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> remove(String id, PersistTo persistTo, Class<T> entityClass);
    <T> EntityDocument<T> remove(String id, PersistTo persistTo, Class<T> entityClass, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> remove(String id, ReplicateTo replicateTo, Class<T> entityClass);
    <T> EntityDocument<T> remove(String id, ReplicateTo replicateTo, Class<T> entityClass, long timeout, TimeUnit timeUnit);
    <T> EntityDocument<T> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<T> entityClass);
    <T> EntityDocument<T> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<T> entityClass, long timeout, TimeUnit timeUnit);

}
