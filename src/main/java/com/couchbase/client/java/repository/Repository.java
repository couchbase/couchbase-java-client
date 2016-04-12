/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
