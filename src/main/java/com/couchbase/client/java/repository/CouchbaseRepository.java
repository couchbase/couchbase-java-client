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
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.util.Blocking;

import java.util.List;
import java.util.concurrent.TimeUnit;

@InterfaceAudience.Public
@InterfaceStability.Experimental
public class CouchbaseRepository implements Repository {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

    private final AsyncRepository asyncRepository;
    private final long timeout;

    public CouchbaseRepository(Bucket bucket, CouchbaseEnvironment environment) {
        this.timeout = environment.kvTimeout();
        this.asyncRepository = bucket.async().repository().toBlocking().single();
    }

    @Override
    public AsyncRepository async() {
        return asyncRepository;
    }

    @Override
    public <T> EntityDocument<T> get(String id, Class<T> entityClass) {
        return get(id, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> get(String id, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.get(id, entityClass), timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> upsert(EntityDocument<T> document) {
        return upsert(document, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> upsert(EntityDocument<T> document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.upsert(document), timeout, timeUnit);
    }

    @Override
    public <T> List<EntityDocument<T>> getFromReplica(String id, ReplicaMode type, Class<T> entityClass) {
        return getFromReplica(id, type, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> List<EntityDocument<T>> getFromReplica(String id, ReplicaMode type, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.getFromReplica(id, type, entityClass).toList(), timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> getAndLock(String id, int lockTime, Class<T> entityClass) {
        return getAndLock(id, lockTime, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> getAndLock(String id, int lockTime, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.getAndLock(id, lockTime, entityClass), timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> getAndTouch(String id, int expiry, Class<T> entityClass) {
        return getAndTouch(id, expiry, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> getAndTouch(String id, int expiry, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.getAndTouch(id, expiry, entityClass), timeout, timeUnit);
    }

    @Override
    public boolean exists(String id) {
        return exists(id, timeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean exists(String id, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.exists(id), timeout, timeUnit);
    }

    @Override
    public <T> boolean exists(EntityDocument<T> document) {
        return exists(document, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> boolean exists(EntityDocument<T> document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.exists(document), timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> upsert(EntityDocument<T> document, PersistTo persistTo) {
        return upsert(document, persistTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> upsert(EntityDocument<T> document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return upsert(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> upsert(EntityDocument<T> document, ReplicateTo replicateTo) {
        return upsert(document, replicateTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> upsert(EntityDocument<T> document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return upsert(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> upsert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo) {
        return upsert(document, persistTo, replicateTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> upsert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.upsert(document, persistTo, replicateTo), timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> insert(EntityDocument<T> document, PersistTo persistTo) {
        return insert(document, persistTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> insert(EntityDocument<T> document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return insert(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> insert(EntityDocument<T> document, ReplicateTo replicateTo) {
        return insert(document, replicateTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> insert(EntityDocument<T> document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return insert(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> insert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo) {
        return insert(document, persistTo, replicateTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> insert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.insert(document, persistTo, replicateTo), timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> insert(EntityDocument<T> document) {
        return insert(document, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> insert(EntityDocument<T> document, long timeout, TimeUnit timeUnit) {
        return insert(document, PersistTo.NONE, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> replace(EntityDocument<T> document) {
        return replace(document, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> replace(EntityDocument<T> document, long timeout, TimeUnit timeUnit) {
        return replace(document, PersistTo.NONE, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> replace(EntityDocument<T> document, PersistTo persistTo) {
        return replace(document, persistTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> replace(EntityDocument<T> document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return replace(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> replace(EntityDocument<T> document, ReplicateTo replicateTo) {
        return replace(document, replicateTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> replace(EntityDocument<T> document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return replace(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> replace(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo) {
        return replace(document, persistTo, replicateTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> replace(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.replace(document, persistTo, replicateTo), timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> remove(EntityDocument<T> document) {
        return remove(document, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> remove(EntityDocument<T> document, long timeout, TimeUnit timeUnit) {
        return remove(document, PersistTo.NONE, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> remove(EntityDocument<T> document, PersistTo persistTo) {
        return remove(document, persistTo, ReplicateTo.NONE, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> remove(EntityDocument<T> document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return remove(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> remove(EntityDocument<T> document, ReplicateTo replicateTo) {
        return remove(document, PersistTo.NONE, replicateTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> remove(EntityDocument<T> document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return remove(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> remove(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(document, persistTo, replicateTo, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> remove(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.remove(document, persistTo, replicateTo), timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> remove(String id, Class<T> entityClass) {
        return remove(id, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> remove(String id, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return remove(id, PersistTo.NONE, ReplicateTo.NONE, entityClass, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> remove(String id, PersistTo persistTo, Class<T> entityClass) {
        return remove(id, persistTo, ReplicateTo.NONE, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> remove(String id, PersistTo persistTo, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return remove(id, persistTo, ReplicateTo.NONE, entityClass, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> remove(String id, ReplicateTo replicateTo, Class<T> entityClass) {
        return remove(id, PersistTo.NONE, replicateTo, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> remove(String id, ReplicateTo replicateTo, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return remove(id, PersistTo.NONE, replicateTo, entityClass, timeout, timeUnit);
    }

    @Override
    public <T> EntityDocument<T> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<T> entityClass) {
        return remove(id, persistTo, replicateTo, entityClass, timeout, TIMEOUT_UNIT);
    }

    @Override
    public <T> EntityDocument<T> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<T> entityClass, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncRepository.remove(id, persistTo, replicateTo, entityClass), timeout, timeUnit);
    }
}
