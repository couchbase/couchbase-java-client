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
package com.couchbase.client.java;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.message.internal.PingReport;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.java.analytics.AnalyticsDeferredResultHandle;
import com.couchbase.client.java.analytics.AnalyticsQuery;
import com.couchbase.client.java.analytics.AnalyticsQueryExecutor;
import com.couchbase.client.java.analytics.AnalyticsQueryResult;
import com.couchbase.client.java.analytics.AsyncAnalyticsDeferredResultHandle;
import com.couchbase.client.java.analytics.AsyncAnalyticsQueryResult;
import com.couchbase.client.java.analytics.DefaultAnalyticsDeferredResultHandle;
import com.couchbase.client.java.analytics.DefaultAnalyticsQueryResult;
import com.couchbase.client.java.analytics.DefaultAsyncAnalyticsDeferredResultHandle;
import com.couchbase.client.java.analytics.DefaultAsyncAnalyticsQueryResult;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.bucket.DefaultBucketManager;
import com.couchbase.client.java.datastructures.MutationOptionBuilder;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.core.N1qlQueryExecutor;
import com.couchbase.client.java.repository.CouchbaseRepository;
import com.couchbase.client.java.repository.Repository;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.impl.DefaultSearchQueryResult;
import com.couchbase.client.java.subdoc.AsyncLookupInBuilder;
import com.couchbase.client.java.subdoc.AsyncMutateInBuilder;
import com.couchbase.client.java.subdoc.LookupInBuilder;
import com.couchbase.client.java.subdoc.MutateInBuilder;
import com.couchbase.client.java.transcoder.JacksonTransformers;
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
import rx.functions.Func1;

public class CouchbaseBucket implements Bucket {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncBucket asyncBucket;
    private final CouchbaseEnvironment environment;
    private final long kvTimeout;
    private final String name;
    private final String username;
    private final String password;
    private final ClusterFacade core;

    /**
     * Create a {@link CouchbaseBucket} that doesn't reuse an existing {@link AsyncBucket} but rather creates one internally. Prefer using the alternative constructor
     * {@link #CouchbaseBucket(AsyncBucket, CouchbaseEnvironment, ClusterFacade, String, String, String)} if you can obtain an AsyncBucket externally.
     */
    public CouchbaseBucket(final CouchbaseEnvironment env, final ClusterFacade core, final String name, final String username, final String password,
                           final List<Transcoder<? extends Document, ?>> customTranscoders) {
        this(new CouchbaseAsyncBucket(core, env, name, username, password, customTranscoders), env, core, name, username, password);
    }

    /**
     * Create a {@link CouchbaseBucket} that relies on the provided {@link AsyncBucket}.
     */
    public CouchbaseBucket(AsyncBucket asyncBucket, final CouchbaseEnvironment env, final ClusterFacade core, final String name,
                           final String username, final String password) {
        this.asyncBucket = asyncBucket;
        this.environment = env;
        this.kvTimeout = env.kvTimeout();
        this.name = name;
        this.username = username;
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
    public ClusterFacade core() {
        return asyncBucket.core().toBlocking().single();
    }

    @Override
    public CouchbaseEnvironment environment() {
        return environment;
    }

    @Override
    public Repository repository() {
        return new CouchbaseRepository(this, environment);
    }

    @Override
    public JsonDocument get(String id) {
        return get(id, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument get(String id, long timeout, TimeUnit timeUnit) {
        return asyncBucket.get(id, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D get(D document) {
        return get(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D get(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket.get(document, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D get(String id, Class<D> target) {
        return get(id, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D get(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket.get(id, target, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public boolean exists(String id) {
        return exists(id, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean exists(String id, long timeout, TimeUnit timeUnit) {
        return asyncBucket.exists(id, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> boolean exists(D document) {
        return exists(document.id());
    }

    @Override
    public <D extends Document<?>> boolean exists(D document, long timeout, TimeUnit timeUnit) {
        return exists(document.id(), timeout, timeUnit);
    }

    @Override
    public List<JsonDocument> getFromReplica(String id, ReplicaMode type) {
        return getFromReplica(id, type, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public List<JsonDocument> getFromReplica(String id, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getFromReplica(id, type, timeout, timeUnit).toList().toBlocking().single();
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type) {
        return getFromReplica(document, type, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getFromReplica(document, type, timeout, timeUnit).toList().toBlocking().single();
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target) {
        return getFromReplica(id, type, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getFromReplica(id, type, target, timeout, timeUnit).toList().toBlocking().single();
    }

    @Override
    public Iterator<JsonDocument> getFromReplica(String id) {
        return getFromReplica(id, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> Iterator<D> getFromReplica(D document) {
        return getFromReplica(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> Iterator<D> getFromReplica(String id, Class<D> target) {
        return getFromReplica(id, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> Iterator<D> getFromReplica(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getFromReplica(id, ReplicaMode.ALL, target, timeout, timeUnit)
            .toBlocking()
            .getIterator();
    }

    @Override
    public <D extends Document<?>> Iterator<D> getFromReplica(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getFromReplica(document, ReplicaMode.ALL, timeout, timeUnit)
            .toBlocking()
            .getIterator();
    }

    @Override
    public Iterator<JsonDocument> getFromReplica(String id, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getFromReplica(id, ReplicaMode.ALL, timeout, timeUnit)
            .toBlocking()
            .getIterator();
    }

    @Override
    public JsonDocument getAndLock(String id, int lockTime) {
        return getAndLock(id, lockTime, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument getAndLock(String id, int lockTime, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getAndLock(id, lockTime, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D getAndLock(D document, int lockTime) {
        return getAndLock(document, lockTime, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndLock(D document, int lockTime, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getAndLock(document, lockTime, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target) {
        return getAndLock(id, lockTime, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getAndLock(id, lockTime, target, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public JsonDocument getAndTouch(String id, int expiry) {
        return getAndTouch(id, expiry, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument getAndTouch(String id, int expiry, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getAndTouch(id, expiry, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(D document) {
        return getAndTouch(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getAndTouch(document, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target) {
        return getAndTouch(id, expiry, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket.getAndTouch(id, expiry, target, timeout, timeUnit).toBlocking().singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D insert(D document) {
        return insert(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket.insert(document, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return insert(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.insert(document, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo) {
        return insert(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.insert(document, persistTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D insert(D document, ReplicateTo replicateTo) {
        return insert(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.insert(document, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D upsert(D document) {
        return upsert(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket.upsert(document, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return upsert(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.upsert(document, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo) {
        return upsert(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.upsert(document, persistTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo) {
        return upsert(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.upsert(document, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D replace(D document) {
        return replace(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket.replace(document, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return replace(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.replace(document, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();

    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo) {
        return replace(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.replace(document, persistTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D replace(D document, ReplicateTo replicateTo) {
        return replace(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.replace(document, replicateTo, timeout, timeUnit).toBlocking().single();
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
        return asyncBucket.remove(document, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(document, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(document, persistTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D remove(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(document, replicateTo, timeout, timeUnit).toBlocking().single();
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
        return asyncBucket.remove(id, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(id, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(id, persistTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public JsonDocument remove(String id, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(id, replicateTo, timeout, timeUnit).toBlocking().single();
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
        return asyncBucket.remove(id, target, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(id, persistTo, replicateTo, target, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(id, persistTo, target, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket.remove(id, replicateTo, target, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public ViewResult query(ViewQuery query) {
        return query(query, environment.viewTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public N1qlQueryResult query(Statement statement) {
        return query(statement, environment.queryTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public N1qlQueryResult query(N1qlQuery query) {
        return query(query, environment.queryTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public SearchQueryResult query(SearchQuery query) {
        return query(query, environment.searchTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public SearchQueryResult query(SearchQuery query, long timeout, TimeUnit timeUnit) {
        return asyncBucket.query(query, timeout, timeUnit)
            .flatMap(DefaultSearchQueryResult.FROM_ASYNC)
            .toBlocking()
            .single();
    }

    @Override
    public AnalyticsQueryResult query(AnalyticsQuery query) {
        return query(query, environment.analyticsTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public AnalyticsQueryResult query(AnalyticsQuery query, long timeout, TimeUnit timeUnit) {
        if (!query.params().deferred()) {
            return asyncBucket.query(query, timeout, timeUnit)
                    .flatMap(AnalyticsQueryExecutor.ASYNC_RESULT_TO_SYNC)
                    .toBlocking()
                    .single();
        } else {
            return asyncBucket.query(query, timeout, timeUnit)
                    .flatMap(AnalyticsQueryExecutor.ASYNC_RESULT_TO_SYNC_DEFERRED)
                    .toBlocking()
                    .single();
        }

    }

    @Override
    public SpatialViewResult query(SpatialViewQuery query) {
        return query(query, environment.viewTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public ViewResult query(ViewQuery query, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .query(query, timeout, timeUnit)
            .map(new Func1<AsyncViewResult, ViewResult>() {
                @Override
                public ViewResult call(AsyncViewResult asyncViewResult) {
                    return new DefaultViewResult(environment, CouchbaseBucket.this,
                        asyncViewResult.rows(), asyncViewResult.totalRows(), asyncViewResult.success(),
                        asyncViewResult.error(), asyncViewResult.debug()
                    );
                }
            })
            .toBlocking()
            .single();
    }

    @Override
    public SpatialViewResult query(SpatialViewQuery query, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .query(query, timeout, timeUnit)
            .map(new Func1<AsyncSpatialViewResult, SpatialViewResult>() {
                @Override
                public SpatialViewResult call(AsyncSpatialViewResult asyncSpatialViewResult) {
                    return new DefaultSpatialViewResult(environment, CouchbaseBucket.this,
                        asyncSpatialViewResult.rows(), asyncSpatialViewResult.success(),
                        asyncSpatialViewResult.error(), asyncSpatialViewResult.debug()
                    );
                }
            })
            .toBlocking()
            .single();
    }

    @Override
    public N1qlQueryResult query(Statement statement, final long timeout, final TimeUnit timeUnit) {
        return query(N1qlQuery.simple(statement), timeout, timeUnit);
    }

    @Override
    public N1qlQueryResult query(N1qlQuery query, final long timeout, final TimeUnit timeUnit) {
        return asyncBucket.query(query, timeout, timeUnit)
            .flatMap(N1qlQueryExecutor.ASYNC_RESULT_TO_SYNC)
            .toBlocking()
            .single();
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
        return asyncBucket.unlock(id, cas, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> Boolean unlock(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket.unlock(document, timeout, timeUnit).toBlocking().single();
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
        return asyncBucket.touch(id, expiry, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> Boolean touch(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket.touch(document, timeout, timeUnit).toBlocking().single();
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
        return asyncBucket.counter(id, delta, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, long timeout, TimeUnit timeUnit) {
        return asyncBucket.counter(id, delta, initial, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, long timeout, TimeUnit timeUnit) {
        return asyncBucket.counter(id, delta, initial, expiry, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public BucketManager bucketManager() {
        return asyncBucket
            .bucketManager()
            .map(new Func1<AsyncBucketManager, BucketManager>() {
                @Override
                public BucketManager call(AsyncBucketManager asyncBucketManager) {
                    return DefaultBucketManager.create(environment, name, username, password, core);
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
        return asyncBucket.append(document, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D prepend(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket.prepend(document, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public JsonLongDocument counter(String id, long delta, PersistTo persistTo) {
        return counter(id, delta, persistTo, ReplicateTo.NONE);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, ReplicateTo replicateTo) {
        return counter(id, delta, PersistTo.NONE, replicateTo);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, PersistTo persistTo, ReplicateTo replicateTo) {
        return counter(id, delta, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, PersistTo persistTo) {
        return counter(id, delta, initial, persistTo, ReplicateTo.NONE);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, ReplicateTo replicateTo) {
        return counter(id, delta, initial, PersistTo.NONE, replicateTo);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, PersistTo persistTo, ReplicateTo replicateTo) {
        return counter(id, delta, initial, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, PersistTo persistTo) {
        return counter(id, delta, initial, expiry, persistTo, ReplicateTo.NONE);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, ReplicateTo replicateTo) {
        return counter(id, delta, initial, expiry, PersistTo.NONE, replicateTo);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, PersistTo persistTo, ReplicateTo replicateTo) {
        return counter(id, delta, initial, expiry, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, expiry, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, expiry, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.counter(id, delta, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.counter(id, delta, initial, persistTo, replicateTo).toBlocking().single();
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.counter(id, delta, initial, expiry, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D append(D document, PersistTo persistTo) {
        return append(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <D extends Document<?>> D append(D document, ReplicateTo replicateTo) {
        return append(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <D extends Document<?>> D append(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return append(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D append(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return append(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D append(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return append(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D append(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.append(document, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    @Override
    public <D extends Document<?>> D prepend(D document, PersistTo persistTo) {
        return prepend(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <D extends Document<?>> D prepend(D document, ReplicateTo replicateTo) {
        return prepend(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <D extends Document<?>> D prepend(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return prepend(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D prepend(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return prepend(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D prepend(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return prepend(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> D prepend(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket.prepend(document, persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    /*---------------------------*
     * START OF SUB-DOCUMENT API *
     *---------------------------*/
    @Override
    public LookupInBuilder lookupIn(String docId) {
        AsyncLookupInBuilder asyncBuilder = asyncBucket.lookupIn(docId);
        return new LookupInBuilder(asyncBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public MutateInBuilder mutateIn(String docId) {
        AsyncMutateInBuilder asyncBuilder = asyncBucket.mutateIn(docId);
        return new MutateInBuilder(asyncBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    /*-------------------------*
     * END OF SUB-DOCUMENT API *
     *-------------------------*/

    @Override
    public <V> boolean mapAdd(String docId, String key, V value) {
        return mapAdd(docId, key, value, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <V> boolean mapAdd(String docId, String key, V value, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.mapAdd(docId, key, value), timeout, timeUnit);
    }

    @Override
    public <V> boolean mapAdd(String docId, String key, V value, MutationOptionBuilder mutationOptionBuilder) {
        return mapAdd(docId, key, value, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <V> boolean mapAdd(String docId, String key, V value, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.mapAdd(docId, key, value, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public <V> V mapGet(String docId, String key, Class<V> valueType) {
        return mapGet(docId, key, valueType, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <V> V mapGet(String docId, String key, Class<V> valueType, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.mapGet(docId, key, valueType), timeout, timeUnit);
    }

    @Override
    public boolean mapRemove(String docId, String key) {
        return mapRemove(docId, key, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean mapRemove(String docId, String key, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.mapRemove(docId, key), timeout, timeUnit);
    }

    @Override
    public boolean mapRemove(String docId, String key, MutationOptionBuilder mutationOptionBuilder) {
        return mapRemove(docId, key, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean mapRemove(String docId, String key, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.mapRemove(docId, key, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public int mapSize(String docId) {
        return mapSize(docId, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public int mapSize(String docId, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.mapSize(docId), timeout, timeUnit);
    }


    @Override
    public <E> E listGet(String docId, int index, Class<E> elementType) {
        return listGet(docId, index, elementType, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> E listGet(String docId, int index, Class<E> elementType, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listGet(docId, index, elementType), timeout, timeUnit);
    }

    @Override
    public <E> boolean listAppend(String docId, E element) {
        return listAppend(docId, element, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean listAppend(String docId, E element, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listAppend(docId, element), timeout, timeUnit);
    }

    @Override
    public <E> boolean listAppend(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        return listAppend(docId, element, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean listAppend(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listAppend(docId, element, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public <E> boolean listPrepend(String docId, E element) {
        return listPrepend(docId, element, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean listPrepend(String docId, E element, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listPrepend(docId, element), timeout, timeUnit);
    }

    @Override
    public <E> boolean listPrepend(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        return listPrepend(docId, element, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }


    @Override
    public <E> boolean listPrepend(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listPrepend(docId, element, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public boolean listRemove(String docId, int index) {
        return listRemove(docId, index, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean listRemove(String docId, int index, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listRemove(docId, index), timeout, timeUnit);
    }

    @Override
    public boolean listRemove(String docId, int index, MutationOptionBuilder mutationOptionBuilder) {
        return listRemove(docId, index, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean listRemove(String docId, int index, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listRemove(docId, index, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public <E> boolean listSet(String docId, int index, E element) {
        return listSet(docId, index, element, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean listSet(String docId, int index, E element, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listSet(docId, index, element), timeout, timeUnit);
    }

    @Override
    public <E> boolean listSet(String docId, int index, E element, MutationOptionBuilder mutationOptionBuilder) {
        return listSet(docId, index, element, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean listSet(String docId, int index, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listSet(docId, index, element, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public int listSize(String docId) {
        return listSize(docId, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public int listSize(String docId, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.listSize(docId), timeout, timeUnit);
    }

    @Override
    public <E> boolean setAdd(String docId, E element) {
        return setAdd(docId, element, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean setAdd(String docId, E element, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.setAdd(docId, element), timeout, timeUnit);
    }

    @Override
    public <E> boolean setAdd(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        return setAdd(docId, element, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean setAdd(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.setAdd(docId, element, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public <E> boolean setContains(String docId, E element) {
        return setContains(docId, element, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean setContains(String docId, E element, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.setContains(docId, element), timeout, timeUnit);
    }

    @Override
    public <E> E setRemove(String docId, E element) {
        return setRemove(docId, element, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> E setRemove(String docId, E element, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.setRemove(docId, element), timeout, timeUnit);
    }

    @Override
    public <E> E setRemove(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        return setRemove(docId, element, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> E setRemove(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.setRemove(docId, element, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public int setSize(String docId) {
        return setSize(docId, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public int setSize(String docId, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.setSize(docId), timeout, timeUnit);
    }

    @Override
    public <E> boolean queuePush(String docId, E element) {
        return queuePush(docId, element, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean queuePush(String docId, E element, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.queuePush(docId, element), timeout, timeUnit);
    }

    @Override
    public <E> boolean queuePush(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        return queuePush(docId, element, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> boolean queuePush(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.queuePush(docId, element, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public <E> E queuePop(String docId, Class<E> elementType) {
        return queuePop(docId, elementType, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> E queuePop(String docId, Class<E> elementType, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.queuePop(docId, elementType), timeout, timeUnit);
    }

    @Override
    public <E> E queuePop(String docId, Class<E> elementType, MutationOptionBuilder mutationOptionBuilder) {
        return queuePop(docId, elementType, mutationOptionBuilder, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <E> E queuePop(String docId, Class<E> elementType, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.queuePop(docId, elementType, mutationOptionBuilder), timeout, timeUnit);
    }

    @Override
    public int queueSize(String docId) {
        return queueSize(docId, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public int queueSize(String docId, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.queueSize(docId), timeout, timeUnit);
    }

    @Override
    public Boolean close() {
        return close(environment.disconnectTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public Boolean close(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucket.close().single(), timeout, timeUnit);
    }

    @Override
    public boolean isClosed() {
        return asyncBucket.isClosed();
    }

    @Override
    public String toString() {
        return "Bucket[" + name() + "]";
    }

    @Override
    public int invalidateQueryCache() {
        return Blocking.blockForSingle(
            asyncBucket.invalidateQueryCache(), environment.managementTimeout(), TIMEOUT_UNIT
        );
    }

    @Override
    public PingReport ping(String reportId, long timeout, TimeUnit timeUnit) {
        return asyncBucket.ping(reportId, timeout, timeUnit).toBlocking().value();
    }

    @Override
    public PingReport ping(long timeout, TimeUnit timeUnit) {
        return asyncBucket.ping(timeout, timeUnit).toBlocking().value();
    }

    @Override
    public PingReport ping(Collection<ServiceType> services, long timeout, TimeUnit timeUnit) {
        return asyncBucket.ping(services, timeout, timeUnit).toBlocking().value();
    }

    @Override
    public PingReport ping(String reportId, Collection<ServiceType> services, long timeout, TimeUnit timeUnit) {
        return asyncBucket.ping(reportId, services, timeout, timeUnit).toBlocking().value();
    }

    @Override
    public byte[] exportAnalyticsDeferredResultHandle(AnalyticsDeferredResultHandle handle) {
        try {
            JsonObject jsonObject = JsonObject.create();
            jsonObject.put("v", 1);
            jsonObject.put("uri", handle.getStatusHandleUri());
            return JacksonTransformers.MAPPER.writeValueAsBytes(jsonObject);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot convert handle to Json String", e);
        }
    }

    @Override
    public AnalyticsDeferredResultHandle importAnalyticsDeferredResultHandle(byte[] b) {
        try {
            JsonObject jsonObj = CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(new String(b, StandardCharsets.UTF_8));
            if (jsonObj.getInt("v") != 1) {
                throw new IllegalArgumentException("Version is not supported");
            }
            return new DefaultAnalyticsDeferredResultHandle(new DefaultAsyncAnalyticsDeferredResultHandle(jsonObj.getString("uri"), this.environment(), this.core(), this.name(), username, password, environment.analyticsTimeout(), TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot import", e);
        }
    }

    @Override
    public PingReport ping(String reportId) {
        return ping(reportId, environment.managementTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public PingReport ping() {
        return ping(environment.managementTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public PingReport ping(Collection<ServiceType> services) {
        return ping(services, environment.managementTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public PingReport ping(String reportId, Collection<ServiceType> services) {
        return ping(reportId, services, environment.managementTimeout(), TIMEOUT_UNIT);
    }
}