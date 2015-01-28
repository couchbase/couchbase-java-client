/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.client.java;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.bucket.DefaultBucketManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.query.AsyncQueryResult;
import com.couchbase.client.java.query.AsyncQueryRow;
import com.couchbase.client.java.query.DefaultQueryResult;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryPlan;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.util.Blocking;
import com.couchbase.client.java.view.AsyncSpatialViewResult;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.DefaultSpatialViewResult;
import com.couchbase.client.java.view.DefaultViewResult;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.SpatialViewResult;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func4;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CouchbaseBucket implements Bucket {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncBucket asyncBucket;
    private final CouchbaseEnvironment environment;
    private final long kvTimeout;
    private final String name;
    private final String password;
    private final ClusterFacade core;

    public CouchbaseBucket(final CouchbaseEnvironment env, final ClusterFacade core, final String name, final String password,
        final List<Transcoder<? extends Document, ?>> customTranscoders) {
        asyncBucket = new CouchbaseAsyncBucket(core, env, name, password, customTranscoders);
        this.environment = env;
        this.kvTimeout = env.kvTimeout();
        this.name = name;
        this.password = password;
        this.core = core;
    }

    @Override
    public AsyncBucket async() {
        return asyncBucket;
    }

    @Override
    public String name() {
        return asyncBucket.name();
    }

    @Override
    public JsonDocument get(String id) {
        return get(id, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument get(String id, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.get(id).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D get(D document) {
        return get(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D get(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.get(document).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D get(String id, Class<D> target) {
        return get(id, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D get(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.get(id, target).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public List<JsonDocument> getFromReplica(String id, ReplicaMode type) {
        return getFromReplica(id, type, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public List<JsonDocument> getFromReplica(String id, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.getFromReplica(id, type).toList(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type) {
        return getFromReplica(document, type, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.getFromReplica(document, type).toList(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target) {
        return getFromReplica(id, type, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.getFromReplica(id, type, target).toList(), timeout, timeUnit);
    }

    @Override
    public JsonDocument getAndLock(String id, int lockTime) {
        return getAndLock(id, lockTime, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument getAndLock(String id, int lockTime, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.getAndLock(id, lockTime).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D getAndLock(D document, int lockTime) {
        return getAndLock(document, lockTime, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndLock(D document, int lockTime, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.getAndLock(document, lockTime).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target) {
        return getAndLock(id, lockTime, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.getAndLock(id, lockTime, target).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public JsonDocument getAndTouch(String id, int expiry) {
        return getAndTouch(id, expiry, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument getAndTouch(String id, int expiry, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.getAndTouch(id, expiry).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(D document) {
        return getAndTouch(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.getAndTouch(document).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target) {
        return getAndTouch(id, expiry, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.getAndTouch(id, expiry, target).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D insert(D document) {
        return insert(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.insert(document).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return insert(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.insert(document, persistTo, replicateTo).single(), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo) {
        return insert(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.insert(document, persistTo).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D insert(D document, ReplicateTo replicateTo) {
        return insert(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.insert(document, replicateTo).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D upsert(D document) {
        return upsert(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.upsert(document).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return upsert(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.upsert(document, persistTo, replicateTo).single(), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo) {
        return upsert(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.upsert(document, persistTo).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo) {
        return upsert(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.upsert(document, replicateTo).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D replace(D document) {
        return replace(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.replace(document).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return replace(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.replace(document, persistTo, replicateTo).single(), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo) {
        return replace(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.replace(document, persistTo).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D replace(D document, ReplicateTo replicateTo) {
        return replace(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.replace(document, replicateTo).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D remove(D document) {
        return remove(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo) {
        return remove(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(D document, ReplicateTo replicateTo) {
        return remove(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.remove(document).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.remove(document, persistTo, replicateTo).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.remove(document, persistTo).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D remove(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.remove(document, replicateTo).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public JsonDocument remove(String id) {
        return remove(id, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(id, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo) {
        return remove(id, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument remove(String id, ReplicateTo replicateTo) {
        return remove(id, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument remove(String id, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.remove(id).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.remove(id, persistTo, replicateTo).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.remove(id, persistTo).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public JsonDocument remove(String id, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.remove(id, replicateTo).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D remove(String id, Class<D> target) {
        return remove(id, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target) {
        return remove(id, persistTo, replicateTo, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target) {
        return remove(id, persistTo, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target) {
        return remove(id, replicateTo, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.remove(id, target).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.remove(id, persistTo, replicateTo, target).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.remove(id, persistTo, target).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucket.remove(id, replicateTo, target).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public ViewResult query(ViewQuery query) {
        return query(query, environment.viewTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public QueryResult query(Statement statement) {
        return query(statement, environment.queryTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public QueryResult query(Query query) {
        return query(query, environment.queryTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public QueryPlan prepare(String statement) {
        return prepare(statement, environment.queryTimeout(), TIMEOUT_UNIT);
    }
    @Override
    public QueryPlan prepare(Statement statement) {
        return prepare(statement, environment.queryTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public SpatialViewResult query(SpatialViewQuery query) {
        return query(query, environment.queryTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public ViewResult query(ViewQuery query, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket
                .query(query)
                .map(new Func1<AsyncViewResult, ViewResult>() {
                    @Override
                    public ViewResult call(AsyncViewResult asyncViewResult) {
                        return new DefaultViewResult(environment, CouchbaseBucket.this,
                                asyncViewResult.rows(), asyncViewResult.totalRows(), asyncViewResult.success(),
                                asyncViewResult.error(), asyncViewResult.debug()
                        );
                    }
                })
                .single(), timeout, timeUnit);
    }


    @Override
    public SpatialViewResult query(SpatialViewQuery query, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket
            .query(query)
            .map(new Func1<AsyncSpatialViewResult, SpatialViewResult>() {
                @Override
                public SpatialViewResult call(AsyncSpatialViewResult asyncSpatialViewResult) {
                    return new DefaultSpatialViewResult(environment, CouchbaseBucket.this,
                        asyncSpatialViewResult.rows(), asyncSpatialViewResult.success(),
                        asyncSpatialViewResult.error(), asyncSpatialViewResult.debug()
                    );
                }
            })
            .single(), timeout, timeUnit);
    }

    @Override
    public QueryResult query(Statement statement, final long timeout, final TimeUnit timeUnit) {
        return query(Query.simple(statement), timeout, timeUnit);
    }

    @Override
    public QueryResult query(Query query, final long timeout, final TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket
                .query(query)
                .flatMap(new Func1<AsyncQueryResult, Observable<QueryResult>>() {
                    @Override
                    public Observable<QueryResult> call(AsyncQueryResult aqr) {
                        final boolean parseSuccess = aqr.parseSuccess();
                        final String requestId = aqr.requestId();
                        final String clientContextId = aqr.clientContextId();

                        return Observable.zip(aqr.rows().toList(),
                                aqr.info().singleOrDefault(JsonObject.empty()),
                                aqr.errors().toList(),
                                aqr.finalSuccess().singleOrDefault(Boolean.FALSE),
                                new Func4<List<AsyncQueryRow>, JsonObject, List<JsonObject>, Boolean, QueryResult>() {
                                    @Override
                                    public QueryResult call(List<AsyncQueryRow> rows, JsonObject info,
                                            List<JsonObject> errors, Boolean finalSuccess) {
                                        return new DefaultQueryResult(rows, info, errors, finalSuccess, parseSuccess,
                                                requestId, clientContextId);
                                    }
                                });
                    }
                })
                .single(), timeout, timeUnit);
    }

    @Override
    public QueryPlan prepare(String statement, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket
            .prepare(statement)
            .single(), timeout, timeUnit);
    }
    @Override
    public QueryPlan prepare(Statement statement, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket
            .prepare(statement)
            .single(), timeout, timeUnit);
    }

    @Override
    public Boolean unlock(String id, long cas) {
        return unlock(id, cas, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> Boolean unlock(D document) {
        return unlock(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean unlock(String id, long cas, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.unlock(id, cas).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Boolean unlock(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.unlock(document).single(), timeout, timeUnit);
    }

    @Override
    public Boolean touch(String id, int expiry) {
        return touch(id, expiry, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> Boolean touch(D document) {
        return touch(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean touch(String id, int expiry, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.touch(id, expiry).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Boolean touch(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.touch(document).single(), timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta) {
        return counter(id, delta, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial) {
        return counter(id, delta, initial, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry) {
        return counter(id, delta, initial, expiry, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.counter(id, delta).single(), timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.counter(id, delta, initial).single(), timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.counter(id, delta, initial, expiry).single(), timeout, timeUnit);
    }

    @Override
    public BucketManager bucketManager() {
        return asyncBucket
            .bucketManager()
            .map(new Func1<AsyncBucketManager, BucketManager>() {
                @Override
                public BucketManager call(AsyncBucketManager asyncBucketManager) {
                    return DefaultBucketManager.create(environment, name, password, core);
                }
            })
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D append(D document) {
        return append(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D prepend(D document) {
        return prepend(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D append(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.append(document).single(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D prepend(D document, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.prepend(document).single(), timeout, timeUnit);
    }

    @Override
    public Boolean close() {
        return close(environment.managementTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public Boolean close(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.close().single(), timeout, timeUnit);
    }

    @Override
    public String toString() {
        return "Bucket[" + name() + "]";
    }
}
