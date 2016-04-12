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
import rx.Observable;

/**
 * The async repository abstraction for entities on top of an AsyncBucket.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public interface AsyncRepository {

    <T> Observable<EntityDocument<T>> get(String id, Class<T> entityClass);
    <T> Observable<EntityDocument<T>> getFromReplica(String id, ReplicaMode type, Class<T> entityClass);
    <T> Observable<EntityDocument<T>> getAndLock(String id, int lockTime, Class<T> entityClass);
    <T> Observable<EntityDocument<T>> getAndTouch(String id, int expiry, Class<T> entityClass);

    Observable<Boolean> exists(String id);
    <T> Observable<Boolean> exists(EntityDocument<T> document);

    <T> Observable<EntityDocument<T>> upsert(EntityDocument<T> document);
    <T> Observable<EntityDocument<T>> upsert(EntityDocument<T> document, PersistTo persistTo);
    <T> Observable<EntityDocument<T>> upsert(EntityDocument<T> document, ReplicateTo replicateTo);
    <T> Observable<EntityDocument<T>> upsert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo);

    <T> Observable<EntityDocument<T>> insert(EntityDocument<T> document);
    <T> Observable<EntityDocument<T>> insert(EntityDocument<T> document, PersistTo persistTo);
    <T> Observable<EntityDocument<T>> insert(EntityDocument<T> document, ReplicateTo replicateTo);
    <T> Observable<EntityDocument<T>> insert(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo);

    <T> Observable<EntityDocument<T>> replace(EntityDocument<T> document);
    <T> Observable<EntityDocument<T>> replace(EntityDocument<T> document, PersistTo persistTo);
    <T> Observable<EntityDocument<T>> replace(EntityDocument<T> document, ReplicateTo replicateTo);
    <T> Observable<EntityDocument<T>> replace(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo);

    <T> Observable<EntityDocument<T>> remove(EntityDocument<T> document);
    <T> Observable<EntityDocument<T>> remove(EntityDocument<T> document, PersistTo persistTo);
    <T> Observable<EntityDocument<T>> remove(EntityDocument<T> document, ReplicateTo replicateTo);
    <T> Observable<EntityDocument<T>> remove(EntityDocument<T> document, PersistTo persistTo, ReplicateTo replicateTo);
    <T> Observable<EntityDocument<T>> remove(String id, Class<T> entityClass);
    <T> Observable<EntityDocument<T>> remove(String id, PersistTo persistTo, Class<T> entityClass);
    <T> Observable<EntityDocument<T>> remove(String id, ReplicateTo replicateTo, Class<T> entityClass);
    <T> Observable<EntityDocument<T>> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<T> entityClass);

}
