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

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.cluster.CloseBucketRequest;
import com.couchbase.client.core.message.cluster.CloseBucketResponse;
import com.couchbase.client.core.message.internal.PingReport;
import com.couchbase.client.core.message.kv.AppendRequest;
import com.couchbase.client.core.message.kv.AppendResponse;
import com.couchbase.client.core.message.kv.CounterRequest;
import com.couchbase.client.core.message.kv.CounterResponse;
import com.couchbase.client.core.message.kv.GetRequest;
import com.couchbase.client.core.message.kv.GetResponse;
import com.couchbase.client.core.message.kv.InsertRequest;
import com.couchbase.client.core.message.kv.InsertResponse;
import com.couchbase.client.core.message.kv.ObserveRequest;
import com.couchbase.client.core.message.kv.ObserveResponse;
import com.couchbase.client.core.message.kv.PrependRequest;
import com.couchbase.client.core.message.kv.PrependResponse;
import com.couchbase.client.core.message.kv.RemoveRequest;
import com.couchbase.client.core.message.kv.RemoveResponse;
import com.couchbase.client.core.message.kv.ReplaceRequest;
import com.couchbase.client.core.message.kv.ReplaceResponse;
import com.couchbase.client.core.message.kv.TouchRequest;
import com.couchbase.client.core.message.kv.TouchResponse;
import com.couchbase.client.core.message.kv.UnlockRequest;
import com.couchbase.client.core.message.kv.UnlockResponse;
import com.couchbase.client.core.message.kv.UpsertRequest;
import com.couchbase.client.core.message.kv.UpsertResponse;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.core.message.observe.Observe;
import com.couchbase.client.core.message.search.SearchQueryRequest;
import com.couchbase.client.core.message.search.SearchQueryResponse;
import com.couchbase.client.core.message.view.ViewQueryRequest;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.core.tracing.ThresholdLogReporter;
import com.couchbase.client.core.tracing.ThresholdLogSpan;
import com.couchbase.client.core.utils.HealthPinger;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.analytics.AnalyticsQuery;
import com.couchbase.client.java.analytics.AnalyticsQueryExecutor;
import com.couchbase.client.java.analytics.AsyncAnalyticsQueryResult;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.bucket.DefaultAsyncBucketManager;
import com.couchbase.client.java.bucket.ReplicaReader;
import com.couchbase.client.java.bucket.api.Exists;
import com.couchbase.client.java.bucket.api.Get;
import com.couchbase.client.java.bucket.api.Mutate;
import com.couchbase.client.java.bucket.api.Utils;
import com.couchbase.client.java.datastructures.MutationOptionBuilder;
import com.couchbase.client.java.datastructures.ResultMappingUtils;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.error.RequestTooBigException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.error.subdoc.PathNotFoundException;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.core.N1qlQueryExecutor;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.repository.CouchbaseAsyncRepository;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.core.SearchQueryExecutor;
import com.couchbase.client.java.search.result.AsyncSearchQueryResult;
import com.couchbase.client.java.search.result.impl.DefaultAsyncSearchQueryResult;
import com.couchbase.client.java.subdoc.AsyncLookupInBuilder;
import com.couchbase.client.java.subdoc.AsyncMutateInBuilder;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.transcoder.BinaryTranscoder;
import com.couchbase.client.java.transcoder.ByteArrayTranscoder;
import com.couchbase.client.java.transcoder.JacksonTransformers;
import com.couchbase.client.java.transcoder.JsonArrayTranscoder;
import com.couchbase.client.java.transcoder.JsonBooleanTranscoder;
import com.couchbase.client.java.transcoder.crypto.JsonCryptoTranscoder;
import com.couchbase.client.java.transcoder.JsonDoubleTranscoder;
import com.couchbase.client.java.transcoder.JsonLongTranscoder;
import com.couchbase.client.java.transcoder.JsonStringTranscoder;
import com.couchbase.client.java.transcoder.JsonTranscoder;
import com.couchbase.client.java.transcoder.LegacyTranscoder;
import com.couchbase.client.java.transcoder.RawJsonTranscoder;
import com.couchbase.client.java.transcoder.SerializableTranscoder;
import com.couchbase.client.java.transcoder.StringTranscoder;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.transcoder.subdoc.FragmentTranscoder;
import com.couchbase.client.java.transcoder.subdoc.JacksonFragmentTranscoder;
import com.couchbase.client.java.view.AsyncSpatialViewResult;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewQueryResponseMapper;
import com.couchbase.client.java.view.ViewRetryHandler;
import io.opentracing.Scope;
import io.opentracing.Span;
import rx.Observable;
import rx.Single;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.bucket.api.Utils.addRequestSpan;
import static com.couchbase.client.java.bucket.api.Utils.applyTimeout;
import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

public class CouchbaseAsyncBucket implements AsyncBucket {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(CouchbaseAsyncBucket.class);

    public static final int COUNTER_NOT_EXISTS_EXPIRY = 0xffffffff;

    private static final int MAX_CAS_RETRIES_DATASTRUCTURES = Integer.parseInt(System.getProperty("com.couchbase.datastructureCASRetryLimit", "10"));

    public static final String CURRENT_BUCKET_IDENTIFIER = "#CURRENT_BUCKET#";

    public static final JsonTranscoder JSON_OBJECT_TRANSCODER = new JsonTranscoder();
    public static final JsonArrayTranscoder JSON_ARRAY_TRANSCODER = new JsonArrayTranscoder();
    public static final JsonBooleanTranscoder JSON_BOOLEAN_TRANSCODER = new JsonBooleanTranscoder();
    public static final JsonDoubleTranscoder JSON_DOUBLE_TRANSCODER = new JsonDoubleTranscoder();
    public static final JsonLongTranscoder JSON_LONG_TRANSCODER = new JsonLongTranscoder();
    public static final JsonStringTranscoder JSON_STRING_TRANSCODER = new JsonStringTranscoder();
    public static final RawJsonTranscoder RAW_JSON_TRANSCODER = new RawJsonTranscoder();
    public static final ByteArrayTranscoder BYTE_ARRAY_TRANSCODER = new ByteArrayTranscoder();

    public static final LegacyTranscoder LEGACY_TRANSCODER = new LegacyTranscoder();
    public static final BinaryTranscoder BINARY_TRANSCODER = new BinaryTranscoder();
    public static final StringTranscoder STRING_TRANSCODER = new StringTranscoder();
    public static final SerializableTranscoder SERIALIZABLE_TRANSCODER = new SerializableTranscoder();

    private final String bucket;
    private final String username;
    private final String password;
    private final ClusterFacade core;
    private final Map<Class<? extends Document>, Transcoder<? extends Document, ?>> transcoders;
    //TODO this could be opened for customization like with transcoders
    private final FragmentTranscoder subdocumentTranscoder = new JacksonFragmentTranscoder(JacksonTransformers.MAPPER);
    private final AsyncBucketManager bucketManager;
    private final CouchbaseEnvironment environment;
    /** the bucket's {@link N1qlQueryExecutor}. Prefer using {@link #n1qlQueryExecutor()} since it allows mocking and testing */
    private final N1qlQueryExecutor n1qlQueryExecutor;
    private final AnalyticsQueryExecutor analyticsQueryExecutor;
    private final SearchQueryExecutor searchQueryExecutor;

    private volatile boolean closed;



    public CouchbaseAsyncBucket(final ClusterFacade core, final CouchbaseEnvironment environment, final String name,
                                final String password, final List<Transcoder<? extends Document, ?>> customTranscoders) {
        this(core, environment, name, name, password, customTranscoders);
    }

    public CouchbaseAsyncBucket(final ClusterFacade core, final CouchbaseEnvironment environment, final String name,
                                final String username, final String password, final List<Transcoder<? extends Document, ?>> customTranscoders) {
        bucket = name;
        this.username = username;
        this.password = password;
        this.core = core;
        this.environment = environment;
        this.closed = false;

        transcoders = new ConcurrentHashMap<Class<? extends Document>, Transcoder<? extends Document, ?>>();

        if (environment != null && environment.cryptoManager() != null) {
            JsonCryptoTranscoder transcoder = new JsonCryptoTranscoder(environment.cryptoManager());
            transcoders.put(transcoder.documentType(), transcoder);
        } else {
            transcoders.put(JSON_OBJECT_TRANSCODER.documentType(), JSON_OBJECT_TRANSCODER);
        }

        transcoders.put(JSON_ARRAY_TRANSCODER.documentType(), JSON_ARRAY_TRANSCODER);
        transcoders.put(JSON_BOOLEAN_TRANSCODER.documentType(), JSON_BOOLEAN_TRANSCODER);
        transcoders.put(JSON_DOUBLE_TRANSCODER.documentType(), JSON_DOUBLE_TRANSCODER);
        transcoders.put(JSON_LONG_TRANSCODER.documentType(), JSON_LONG_TRANSCODER);
        transcoders.put(JSON_STRING_TRANSCODER.documentType(), JSON_STRING_TRANSCODER);
        transcoders.put(RAW_JSON_TRANSCODER.documentType(), RAW_JSON_TRANSCODER);
        transcoders.put(LEGACY_TRANSCODER.documentType(), LEGACY_TRANSCODER);
        transcoders.put(BINARY_TRANSCODER.documentType(), BINARY_TRANSCODER);
        transcoders.put(STRING_TRANSCODER.documentType(), STRING_TRANSCODER);
        transcoders.put(SERIALIZABLE_TRANSCODER.documentType(), SERIALIZABLE_TRANSCODER);
        transcoders.put(BYTE_ARRAY_TRANSCODER.documentType(), BYTE_ARRAY_TRANSCODER);

        for (Transcoder<? extends Document, ?> custom : customTranscoders) {
            transcoders.put(custom.documentType(), custom);
        }

        bucketManager = DefaultAsyncBucketManager.create(bucket, username, password, core, environment);

        boolean n1qlPreparedEncodedPlanEnabled = "true".equalsIgnoreCase(System.getProperty(N1qlQueryExecutor.ENCODED_PLAN_ENABLED_PROPERTY, "true")); //active by default
        n1qlQueryExecutor = new N1qlQueryExecutor(core, bucket, username, password, n1qlPreparedEncodedPlanEnabled);
        analyticsQueryExecutor = new AnalyticsQueryExecutor(core, bucket, username, password);
        searchQueryExecutor = new SearchQueryExecutor(environment, core, bucket, username, password);
    }

    @Override
    public String name() {
        return bucket;
    }

    @Override
    public Observable<ClusterFacade> core() {
        return Observable.just(core);
    }

    @Override
    public FragmentTranscoder subdocumentTranscoder() {
        return subdocumentTranscoder;
    }

    /**
     * Returns the underlying {@link N1qlQueryExecutor} used to perform N1QL queries.
     *
     * Handle with care since all additional checks that are normally performed by this library may be skipped (hence
     * the protected visibility).
     */
    protected N1qlQueryExecutor n1qlQueryExecutor() {
        return this.n1qlQueryExecutor;
    }

    @Override
    public CouchbaseEnvironment environment() {
        return environment;
    }

    @Override
    public Observable<AsyncRepository> repository() {
        return Observable.just((AsyncRepository) new CouchbaseAsyncRepository(this));
    }

    @Override
    public Observable<JsonDocument> get(final String id) {
        return get(id, JsonDocument.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> get(D document) {
        return (Observable<D>) get(document.id(), document.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> get(final String id, final Class<D> target) {
        return Get.get(id, target, environment, bucket, core, transcoders, 0, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> get(final String id, final Class<D> target, long timeout, TimeUnit timeUnit) {
      return Get.get(id, target, environment, bucket, core, transcoders, timeout, timeUnit);
    }

    @Override
    public Observable<JsonDocument> get(String id, long timeout, TimeUnit timeUnit) {
        return get(id, JsonDocument.class, timeout, timeUnit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> get(D document, long timeout, TimeUnit timeUnit) {
        return (Observable<D>) Get.get(document.id(), document.getClass(), environment, bucket, core, transcoders, timeout, timeUnit);
    }

    @Override
    public Observable<Boolean> exists(String id, long timeout, TimeUnit timeUnit) {
        return Exists.exists(id, environment, core, bucket, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> exists(D document, long timeout, TimeUnit timeUnit) {
        return exists(document.id(), timeout, timeUnit);
    }

    @Override
    public Observable<Boolean> exists(final String id) {
        return Exists.exists(id, environment, core, bucket, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> exists(D document) {
        return exists(document.id());
    }

    @Override
    public Observable<JsonDocument> getAndLock(String id, int lockTime) {
        return getAndLock(id, lockTime, JsonDocument.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> getAndLock(D document, int lockTime) {
        return (Observable<D>) getAndLock(document.id(), lockTime, document.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> getAndLock(final String id, final int lockTime, final Class<D> target) {
        return Get.getAndLock(id, target, environment, bucket, core, transcoders, lockTime, 0, null);
    }

    @Override
    public Observable<JsonDocument> getAndTouch(String id, int expiry) {
        return getAndTouch(id, expiry, JsonDocument.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> getAndTouch(D document) {
        return (Observable<D>) getAndTouch(document.id(), document.expiry(), document.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> getAndTouch(final String id, final int expiry, final Class<D> target) {
        return Get.getAndTouch(id, target, environment, bucket, core, transcoders, expiry, 0, null);
    }

    @Override
    public Observable<JsonDocument> getFromReplica(final String id, final ReplicaMode type) {
        return getFromReplica(id, type, JsonDocument.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> getFromReplica(final D document, final ReplicaMode type) {
        return (Observable<D>) getFromReplica(document.id(), type, document.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> getFromReplica(final String id, final ReplicaMode type,
        final Class<D> target) {
        return getFromReplica(id, type, target, 0, null);
    }

    @Override
    public Observable<JsonDocument> getFromReplica(String id, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return getFromReplica(id, type, JsonDocument.class, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> getFromReplica(D document, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return (Observable<D>) getFromReplica(document.id(), type, document.getClass(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> getFromReplica(String id, ReplicaMode type, Class<D> target, long timeout, TimeUnit timeUnit) {
        return ReplicaReader.read(core, id, type, bucket, transcoders, target, environment, timeout, timeUnit);
    }

    @Override
    public Observable<JsonDocument> getAndLock(String id, int lockTime, long timeout, TimeUnit timeUnit) {
        return getAndLock(id, lockTime, JsonDocument.class, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> getAndLock(D document, int lockTime, long timeout, TimeUnit timeUnit) {
        return (Observable<D>) getAndLock(document.id(), lockTime, document.getClass(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> getAndLock(String id, int lockTime, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Get.getAndLock(id, target, environment, bucket, core, transcoders, lockTime, timeout, timeUnit);
    }

    @Override
    public Observable<JsonDocument> getAndTouch(String id, int expiry, long timeout, TimeUnit timeUnit) {
        return getAndTouch(id, expiry, JsonDocument.class, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> getAndTouch(D document, long timeout, TimeUnit timeUnit) {
        return (Observable<D>) getAndTouch(document.id(), document.expiry(), document.getClass(), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> getAndTouch(String id, int expiry, Class<D> target, long timeout, TimeUnit timeUnit) {
        return Get.getAndTouch(id, target, environment, bucket, core, transcoders, expiry, timeout, timeUnit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> insert(final D document) {
        return insert(document, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(final D document, final PersistTo persistTo,
        final ReplicateTo replicateTo, final long timeout, final TimeUnit timeUnit) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return insert(document, timeout, timeUnit);
        }


        final Span parent = startTracing("insert_with_durability");
        return insert(document, parent, timeout, timeUnit).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                Observable<D> or = Observe
                        .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(),
                                persistTo.value(), replicateTo.value(),
                                environment.observeIntervalDelay(), environment.retryStrategy(), parent)
                        .map(new Func1<Boolean, D>() {
                            @Override
                            public D call(Boolean aBoolean) {
                                return doc;
                            }
                        }).onErrorResumeNext(new Func1<Throwable, Observable<? extends D>>() {
                            @Override
                            public Observable<? extends D> call(Throwable throwable) {
                                return Observable.error(new DurabilityException(
                                        "Durability requirement failed: " + throwable.getMessage(),
                                        throwable));
                            }
                        });
                // we need a timeout here since observe doesn't have one yet
                return timeout > 0 ? or.timeout(timeout, timeUnit, environment.scheduler()) : or;
            }
        }).doOnTerminate(stopTracing(parent));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> upsert(final D document) {
        return upsert(document, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(final D document, final PersistTo persistTo,
        final ReplicateTo replicateTo, final long timeout, final TimeUnit timeUnit) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return upsert(document, timeout, timeUnit);
        }


        final Span parent = startTracing("upsert_with_durability");
        return upsert(document, parent, timeout, timeUnit)
            .flatMap(new Func1<D, Observable<D>>() {
                @Override
                public Observable<D> call(final D doc) {
                    Observable<D> or = Observe
                        .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(),
                            persistTo.value(), replicateTo.value(),
                            environment.observeIntervalDelay(), environment.retryStrategy(), parent)
                        .map(new Func1<Boolean, D>() {
                            @Override
                            public D call(Boolean aBoolean) {
                                return doc;
                            }
                        })
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends D>>() {
                            @Override
                            public Observable<? extends D> call(Throwable throwable) {
                                return Observable.error(new DurabilityException(
                                    "Durability requirement failed: " + throwable.getMessage(),
                                    throwable));
                            }
                        });
                    // we need a timeout here since observe doesn't have one yet
                    return timeout > 0 ? or.timeout(timeout, timeUnit, environment.scheduler()) : or;
                }
            })
            .doOnTerminate(stopTracing(parent));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> replace(final D document) {
        return replace(document, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(D document, long timeout, TimeUnit timeUnit) {
        return insert(document, (Span) null, timeout, timeUnit);
    }

    @SuppressWarnings({"unchecked"})
    private <D extends Document<?>> Observable<D> insert(D document, Span parent, long timeout, TimeUnit timeUnit) {
        final  Transcoder<Document<Object>, Object> transcoder =
            (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        return Mutate.insert(document, environment, transcoder, core, bucket, timeout, timeUnit, parent);
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return insert(document, persistTo, replicateTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return insert(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return insert(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(D document, long timeout, TimeUnit timeUnit) {
        return upsert(document, (Span) null, timeout, timeUnit);
    }

    @SuppressWarnings({"unchecked"})
    private <D extends Document<?>> Observable<D> upsert(D document, Span parent, long timeout, TimeUnit timeUnit) {
        final  Transcoder<Document<Object>, Object> transcoder =
            (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        return Mutate.upsert(document, environment, transcoder, core, bucket, timeout, timeUnit, parent);
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return upsert(document, persistTo, replicateTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return upsert(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return upsert(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> replace(D document, long timeout, TimeUnit timeUnit) {
        return replace(document, (Span) null, timeout, timeUnit);
    }

    @SuppressWarnings({"unchecked"})
    private <D extends Document<?>> Observable<D> replace(D document, Span parent, long timeout, TimeUnit timeUnit) {
        final  Transcoder<Document<Object>, Object> transcoder =
            (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        return Mutate.replace(document, environment, transcoder, core, bucket, timeout, timeUnit, parent);
    }

    @Override
    public <D extends Document<?>> Observable<D> replace(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return replace(document, persistTo, replicateTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> replace(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return replace(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> replace(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return replace(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> replace(final D document, final PersistTo persistTo,
        final ReplicateTo replicateTo, final long timeout, final TimeUnit timeUnit) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return replace(document, timeout, timeUnit);
        }

        final Span parent = startTracing("replace_with_durability");
        return replace(document, parent, timeout, timeUnit).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                Observable<D> or = Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(), persistTo.value(),
                        replicateTo.value(), environment.observeIntervalDelay(), environment.retryStrategy(), parent)
                    .map(new Func1<Boolean, D>() {
                        @Override
                        public D call(Boolean aBoolean) {
                            return doc;
                        }
                    }).onErrorResumeNext(new Func1<Throwable, Observable<? extends D>>() {
                        @Override
                        public Observable<? extends D> call(Throwable throwable) {
                            return Observable.error(new DurabilityException(
                                "Durability requirement failed: " + throwable.getMessage(),
                                throwable));
                        }
                    });
                // we need a timeout here since observe doesn't have one yet
                return timeout > 0 ? or.timeout(timeout, timeUnit, environment.scheduler()) : or;
            }
        }).doOnTerminate(stopTracing(parent));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> remove(final D document, long timeout, TimeUnit timeUnit) {
        return remove(document, (Span) null, timeout, timeUnit);
    }

    @SuppressWarnings({"unchecked"})
    private <D extends Document<?>> Observable<D> remove(final D document, Span parent, long timeout, TimeUnit timeUnit) {
        final Transcoder<Document<Object>, Object> transcoder =
            (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        return Mutate.remove(document, environment, transcoder, core, bucket, timeout, timeUnit, parent);
    }

    @Override
    public Observable<JsonDocument> remove(final String id) {
        return remove(id, JsonDocument.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> remove(final String id, final Class<D> target) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(target);
        return remove((D) transcoder.newDocument(id, 0, null, 0, null));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> remove(D document, final PersistTo persistTo, final ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return observeRemove(document, persistTo, replicateTo, timeout, timeUnit);
    }

    @Override
    public Observable<JsonDocument> remove(String id, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(id, persistTo, replicateTo, JsonDocument.class);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <D extends Document<?>> Observable<D> remove(String id, final PersistTo persistTo,
        final ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(target);
        return observeRemove((D) transcoder.newDocument(id, 0, null, 0, null), persistTo, replicateTo, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(D document) {
        return remove(document, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(document, persistTo, replicateTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return remove(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return remove(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public Observable<JsonDocument> remove(String id, long timeout, TimeUnit timeUnit) {
        return remove(id, JsonDocument.class, timeout, timeUnit);
    }

    @Override
    public Observable<JsonDocument> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return remove(id, persistTo, replicateTo, JsonDocument.class, timeout, timeUnit);
    }

    @Override
    public Observable<JsonDocument> remove(String id, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return remove(id, persistTo, ReplicateTo.NONE, JsonDocument.class, timeout, timeUnit);
    }

    @Override
    public Observable<JsonDocument> remove(String id, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return remove(id, PersistTo.NONE, replicateTo, JsonDocument.class, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(target);
        return remove((D) transcoder.newDocument(id, 0, null, 0, null), timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target) {
        return remove(id, persistTo, replicateTo, target, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(String id, PersistTo persistTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return remove(id, persistTo, ReplicateTo.NONE, target, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(String id, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return remove(id, PersistTo.NONE, replicateTo, target, timeout, timeUnit);
    }

    /**
     * Helper method to observe the result of a remove operation with the given durability
     * requirements.
     *
     * @param document the document that needs to be removed.
     * @param persistTo the persistence requirement given.
     * @param replicateTo the replication requirement given.
     * @return an observable reporting success or error of the observe operation.
     */
    private <D extends Document<?>> Observable<D> observeRemove(D document,
        final PersistTo persistTo, final ReplicateTo replicateTo, final long timeout, final TimeUnit timeUnit) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return remove(document, timeout, timeUnit);
        }

        final Span parent = startTracing("remove_with_durability");
        return remove(document, parent, timeout, timeUnit).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                Observable<D> or = Observe
                    .call(core, bucket, doc.id(), doc.cas(), true, doc.mutationToken(),
                        persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy(), parent)
                    .map(new Func1<Boolean, D>() {
                        @Override
                        public D call(Boolean aBoolean) {
                            return doc;
                        }
                    }).onErrorResumeNext(new Func1<Throwable, Observable<? extends D>>() {
                        @Override
                        public Observable<? extends D> call(Throwable throwable) {
                            return Observable.error(new DurabilityException(
                                "Durability requirement failed: " + throwable.getMessage(),
                                throwable));
                        }
                    });
                // we need a timeout here since observe doesn't have one yet
                return timeout > 0 ? or.timeout(timeout, timeUnit, environment.scheduler()) : or;
            }
        }).doOnTerminate(stopTracing(parent));
    }

    @Override
    public Observable<AsyncViewResult> query(final ViewQuery query) {
        return query(query, environment.viewTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<AsyncViewResult> query(final ViewQuery query, final long timeout, final TimeUnit timeUnit) {
        final Observable<ViewQueryResponse> source = deferAndWatch(
            new Func1<Subscriber, Observable<? extends ViewQueryResponse>>() {
                @Override
                public Observable<? extends ViewQueryResponse> call(final Subscriber subscriber) {
                final ViewQueryRequest request = new ViewQueryRequest(query.getDesign(), query.getView(),
                    query.isDevelopment(), query.toQueryString(), query.getKeys(), bucket, username, password);
                Utils.addRequestSpan(environment, request, "view");
                request.subscriber(subscriber);
                return applyTimeout(core.<ViewQueryResponse>send(request), request, environment, timeout, timeUnit);
            }
        });

        return ViewRetryHandler
            .retryOnCondition(source)
            .flatMap(new Func1<ViewQueryResponse, Observable<AsyncViewResult>>() {
                @Override
                public Observable<AsyncViewResult> call(final ViewQueryResponse response) {
                    return ViewQueryResponseMapper.mapToViewResult(CouchbaseAsyncBucket.this, query, response);
                }
            });
    }

    @Override
    public Observable<AsyncSearchQueryResult> query(SearchQuery query) {
        return query(query, environment.searchTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<AsyncSearchQueryResult> query(final SearchQuery query, final long timeout, final TimeUnit timeUnit) {
        //always set a server side timeout. if not explicit, set it to the client side timeout
        if (query.getServerSideTimeout() == null) {
            query.serverSideTimeout(environment().searchTimeout(), TimeUnit.MILLISECONDS);
        }
        return searchQueryExecutor.execute(query, timeout, timeUnit);
    }

    @Override
    public Observable<AsyncSpatialViewResult> query(final SpatialViewQuery query) {
        return query(query, environment.viewTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<AsyncSpatialViewResult> query(final SpatialViewQuery query, final long timeout, final TimeUnit timeUnit) {
        final Observable<ViewQueryResponse> source = deferAndWatch(
            new Func1<Subscriber, Observable<? extends ViewQueryResponse>>() {
                @Override
                public Observable<? extends ViewQueryResponse> call(Subscriber subscriber) {
                final ViewQueryRequest request = new ViewQueryRequest(query.getDesign(), query.getView(),
                    query.isDevelopment(), true, query.toString(), null, bucket, username, password);
                addRequestSpan(environment, request, "spatial_view");
                request.subscriber(subscriber);
                return applyTimeout(core.<ViewQueryResponse>send(request), request, environment, timeout, timeUnit);
            }
        });

        return ViewRetryHandler
            .retryOnCondition(source)
            .flatMap(new Func1<ViewQueryResponse, Observable<AsyncSpatialViewResult>>() {
                @Override
                public Observable<AsyncSpatialViewResult> call(final ViewQueryResponse response) {
                    return ViewQueryResponseMapper.mapToSpatialViewResult(CouchbaseAsyncBucket.this, query, response);
                }
            });
    }

    @Override
    public Observable<AsyncN1qlQueryResult> query(final Statement statement) {
        return query(N1qlQuery.simple(statement));
    }

    @Override
    public Observable<AsyncN1qlQueryResult> query(final N1qlQuery query) {
        return query(query, environment.queryTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<AsyncN1qlQueryResult> query(N1qlQuery query, long timeout, TimeUnit timeUnit) {
        if (!query.params().hasServerSideTimeout()) {
            query.params().serverSideTimeout(timeout, timeUnit);
        }
        if (query.params().clientContextId() == null || query.params().clientContextId().isEmpty()) {
            query.params().withContextId(UUID.randomUUID().toString());
        }
        return n1qlQueryExecutor.execute(query, environment, timeout, timeUnit);
    }

    @Override
    public Observable<AsyncAnalyticsQueryResult> query(final AnalyticsQuery query) {
        return query(query, environment.analyticsTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<AsyncAnalyticsQueryResult> query(final AnalyticsQuery query, long timeout, TimeUnit timeUnit) {
        if (!query.params().hasServerSideTimeout()) {
            query.params().serverSideTimeout(timeout, timeUnit);
        }
        if (query.params().clientContextId() == null || query.params().clientContextId().isEmpty()) {
            query.params().withContextId(UUID.randomUUID().toString());
        }
        return analyticsQueryExecutor.execute(query, environment, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, 0, COUNTER_NOT_EXISTS_EXPIRY, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, 0, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(final String id, final long delta, final long initial, final int expiry, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, expiry, (Span) null, timeout, timeUnit);
    }

    private Observable<JsonLongDocument> counter(final String id, final long delta, final long initial, final int expiry, Span parent, long timeout, TimeUnit timeUnit) {
        return Mutate.counter(id, delta, initial, expiry, environment, core, bucket, timeout, timeUnit, parent);
    }

    @Override
    public Observable<Boolean> unlock(final String id, final long cas, long timeout, TimeUnit timeUnit) {
        return Mutate.unlock(id, cas, environment, core, bucket, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> unlock(D document, long timeout, TimeUnit timeUnit) {
        return unlock(document.id(), document.cas(), timeout, timeUnit);
    }

    @Override
    public Observable<Boolean> unlock(String id, long cas) {
        return unlock(id, cas, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> unlock(D document) {
        return unlock(document, 0, null);
    }

    @Override
    public Observable<Boolean> touch(final String id, final int expiry, long timeout, TimeUnit timeUnit) {
        return Mutate.touch(id, expiry, environment, core, bucket, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> touch(D document, long timeout, TimeUnit timeUnit) {
        return touch(document.id(), document.expiry(), timeout, timeUnit);
    }

    @Override
    public Observable<Boolean> touch(String id, int expiry) {
        return touch(id, expiry, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> touch(D document) {
        return touch(document, 0, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> append(final D document, long timeout, TimeUnit timeUnit) {
        return append(document, (Span) null, timeout, timeUnit);
    }

    @SuppressWarnings("unchecked")
    private <D extends Document<?>> Observable<D> append(final D document, Span parent, long timeout, TimeUnit timeUnit) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        return Mutate.append(document, environment, transcoder, core, bucket, timeout, timeUnit, parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> prepend(final D document, long timeout, TimeUnit timeUnit) {
        return prepend(document, (Span) null, timeout, timeUnit);
    }

    @SuppressWarnings("unchecked")
    private <D extends Document<?>> Observable<D> prepend(final D document, Span parent, long timeout, TimeUnit timeUnit) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        return Mutate.prepend(document, environment, transcoder, core, bucket, timeout, timeUnit, parent);
    }

    @Override
    public Observable<AsyncBucketManager> bucketManager() {
        return Observable.just(bucketManager);
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(D document, PersistTo persistTo) {
        return insert(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(D document, ReplicateTo replicateTo) {
        return insert(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(D document, PersistTo persistTo) {
        return upsert(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(D document, ReplicateTo replicateTo) {
        return upsert(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <D extends Document<?>> Observable<D> replace(D document, PersistTo persistTo) {
        return replace(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <D extends Document<?>> Observable<D> replace(D document, ReplicateTo replicateTo) {
        return replace(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo) {
        return remove(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(D document, ReplicateTo replicateTo) {
        return remove(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public Observable<JsonDocument> remove(String id, PersistTo persistTo) {
        return remove(id, persistTo, ReplicateTo.NONE);
    }

    @Override
    public Observable<JsonDocument> remove(String id, ReplicateTo replicateTo) {
        return remove(id, PersistTo.NONE, replicateTo);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(String id, PersistTo persistTo, Class<D> target) {
        return remove(id, persistTo, ReplicateTo.NONE, target);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(String id, ReplicateTo replicateTo, Class<D> target) {
        return remove(id, PersistTo.NONE, replicateTo, target);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, expiry, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, expiry, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, initial, 0, persistTo, replicateTo, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return counter(id, delta, 0, COUNTER_NOT_EXISTS_EXPIRY, persistTo, replicateTo, timeout, timeUnit);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry,
        final PersistTo persistTo, final ReplicateTo replicateTo, final long timeout, final TimeUnit timeUnit) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return counter(id, delta, initial, expiry, timeout, timeUnit);
        }

        final Span parent = startTracing("counter_with_durability");
        return counter(id, delta, initial, expiry, parent, timeout, timeUnit)
            .flatMap(new Func1<JsonLongDocument, Observable<JsonLongDocument>>() {
                @Override
                public Observable<JsonLongDocument> call(final JsonLongDocument doc) {
                    Observable<JsonLongDocument> or = Observe
                            .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(),
                                    persistTo.value(), replicateTo.value(),
                                    environment.observeIntervalDelay(), environment.retryStrategy(), parent)
                            .map(new Func1<Boolean, JsonLongDocument>() {
                                @Override
                                public JsonLongDocument call(Boolean aBoolean) {
                                    return doc;
                                }
                            })
                            .onErrorResumeNext(new Func1<Throwable, Observable<? extends JsonLongDocument>>() {
                                @Override
                                public Observable<? extends JsonLongDocument> call(Throwable throwable) {
                                    return Observable.error(new DurabilityException(
                                            "Durability requirement failed: " + throwable.getMessage(),
                                            throwable));
                                }
                            });
                    // we need a timeout here since observe doesn't have one yet
                    return timeout > 0 ? or.timeout(timeout, timeUnit, environment.scheduler()) : or;
                }
            })
            .doOnTerminate(stopTracing(parent));
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta) {
        return counter(id, delta, 0, (TimeUnit) null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, PersistTo persistTo) {
        return counter(id, delta, persistTo, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, ReplicateTo replicateTo) {
        return counter(id, delta, replicateTo, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, PersistTo persistTo, ReplicateTo replicateTo) {
        return counter(id, delta, persistTo, replicateTo, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial) {
        return counter(id, delta, initial, 0, (TimeUnit) null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, PersistTo persistTo) {
        return counter(id, delta, initial, persistTo, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, ReplicateTo replicateTo) {
        return counter(id, delta, initial, replicateTo, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, PersistTo persistTo, ReplicateTo replicateTo) {
        return counter(id, delta, initial, persistTo, replicateTo, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry) {
        return counter(id, delta, initial, expiry, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, PersistTo persistTo) {
        return counter(id, delta, initial, expiry, persistTo, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, ReplicateTo replicateTo) {
        return counter(id, delta, initial, expiry, replicateTo, 0, null);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, PersistTo persistTo, ReplicateTo replicateTo) {
        return counter(id, delta, initial, expiry, persistTo, replicateTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return append(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return append(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, final PersistTo persistTo, final ReplicateTo replicateTo, final long timeout, final TimeUnit timeUnit) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return append(document, timeout, timeUnit);
        }

        final Span parent = startTracing("append_with_durability");
        return append(document, parent, timeout, timeUnit).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                Observable<D> or = Observe
                        .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(), persistTo.value(), replicateTo.value(),
                                environment.observeIntervalDelay(), environment.retryStrategy(), parent)
                        .map(new Func1<Boolean, D>() {
                            @Override
                            public D call(Boolean aBoolean) {
                                return doc;
                            }
                        }).onErrorResumeNext(new Func1<Throwable, Observable<? extends D>>() {
                            @Override
                            public Observable<? extends D> call(Throwable throwable) {
                                return Observable.error(new DurabilityException(
                                        "Durability requirement failed: " + throwable.getMessage(),
                                        throwable));
                            }
                        });
                // we need a timeout here since observe doesn't have one yet
                return timeout > 0 ? or.timeout(timeout, timeUnit, environment.scheduler()) : or;
            }
        }).doOnTerminate(stopTracing(parent));
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return prepend(document, persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return prepend(document, PersistTo.NONE, replicateTo, timeout, timeUnit);
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, final PersistTo persistTo, final ReplicateTo replicateTo, final long timeout, final TimeUnit timeUnit) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return prepend(document, timeout, timeUnit);
        }

        final Span parent = startTracing("prepend_with_durability");
        return prepend(document, parent, timeout, timeUnit).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                Observable<D> or = Observe
                        .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(), persistTo.value(), replicateTo.value(),
                                environment.observeIntervalDelay(), environment.retryStrategy(), parent)
                        .map(new Func1<Boolean, D>() {
                            @Override
                            public D call(Boolean aBoolean) {
                                return doc;
                            }
                        }).onErrorResumeNext(new Func1<Throwable, Observable<? extends D>>() {
                            @Override
                            public Observable<? extends D> call(Throwable throwable) {
                                return Observable.error(new DurabilityException(
                                        "Durability requirement failed: " + throwable.getMessage(),
                                        throwable));
                            }
                        });
                // we need a timeout here since observe doesn't have one yet
                return timeout > 0 ? or.timeout(timeout, timeUnit, environment.scheduler()) : or;
            }
        }).doOnTerminate(stopTracing(parent));
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document) {
        return append(document, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, PersistTo persistTo) {
        return append(document, persistTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, ReplicateTo replicateTo) {
        return append(document, replicateTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return append(document, persistTo, replicateTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document) {
        return prepend(document, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, PersistTo persistTo) {
        return prepend(document, persistTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, ReplicateTo replicateTo) {
        return prepend(document, replicateTo, 0, null);
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return prepend(document, persistTo, replicateTo, 0, null);
    }

    /*---------------------------*
     * START OF SUB-DOCUMENT API *
     *---------------------------*/

    @Override
    public AsyncLookupInBuilder lookupIn(String docId) {
        return new AsyncLookupInBuilder(core, bucket, environment, subdocumentTranscoder, docId);
    }

    @Override
    public AsyncMutateInBuilder mutateIn(String docId) {
        return new AsyncMutateInBuilder(core, bucket, environment, subdocumentTranscoder, docId);
    }

    /*-------------------------*
     * END OF SUB-DOCUMENT API *
     *-------------------------*/

    @Override
    public <V> Observable<V> mapGet(final String docId, final String key, Class<V> valueType) {
        final Func1<DocumentFragment<Lookup>, V> mapResult = new Func1<DocumentFragment<Lookup>, V>() {
            @Override
            public V call(DocumentFragment<Lookup> documentFragment) {
                ResponseStatus status = documentFragment.status(0);
                if (status == ResponseStatus.SUCCESS) {
                    return (V) documentFragment.content(0);
                } else if (status == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    throw new PathNotFoundException("Key not found in map");
                } else {
                    throw new CouchbaseException(status.toString());
                }
            }
        };
        return lookupIn(docId).get(key)
                .execute()
                .map(mapResult);
    }

    @Override
    public <V> Observable<Boolean> mapAdd(String docId, String key, V value) {
        return mapAdd(docId, key, value, MutationOptionBuilder.builder());
    }

    @Override
    public <V> Observable<Boolean> mapAdd(final String docId,
                                          final String key,
                                          final V value,
                                          final MutationOptionBuilder mutationOptionBuilder) {
        return mapSubdocAdd(docId, key, value, mutationOptionBuilder)
                .map(ResultMappingUtils.getMapResultFnForSubdocMutationToBoolean());
    }


    private <V> Observable<DocumentFragment<Mutation>> mapSubdocAdd(final String docId,
                                                                    final String key,
                                                                    final V value,
                                                                    final MutationOptionBuilder mutationOptionBuilder) {
        final Mutation mutationOperation = Mutation.DICT_UPSERT;
        final Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>> retryAddIfDocExists = new
                Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentAlreadyExistsException) {
                            return mapSubdocAdd(docId, key, value, mutationOptionBuilder);
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                };
        return mutateIn(docId).upsert(key, value)
                .withCas(mutationOptionBuilder.cas())
                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                .withExpiry(mutationOptionBuilder.expiry())
                .execute()
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentDoesNotExistException) {
                            if (mutationOptionBuilder.createDocument()) {
                                return insert(JsonDocument.create(docId, mutationOptionBuilder.expiry(),
                                        JsonObject.create().put(key, value)),
                                        mutationOptionBuilder.persistTo(),
                                        mutationOptionBuilder.replicateTo())
                                        .map(ResultMappingUtils.getMapFullDocResultToSubDocFn(mutationOperation))
                                        .onErrorResumeNext(retryAddIfDocExists);
                            } else {
                                return Observable.error(throwable);
                            }
                        } else {
                            //Wrap it up a subdoc result, since we dont want to throw it back as subdoc exception
                            if (throwable instanceof MultiMutationException) {
                                ResponseStatus status = ((MultiMutationException) throwable).firstFailureStatus();
                                return Observable.just(ResultMappingUtils.convertToSubDocumentResult(status, mutationOperation, value));
                            } else {
                                return Observable.error(throwable);
                            }
                        }
                    }
                });
    }

    @Override
    public Observable<Boolean> mapRemove(String docId, String key) {
        return mapRemove(docId, key, MutationOptionBuilder.builder());
    }

    @Override
    public Observable<Boolean> mapRemove(final String docId,
                                         final String key,
                                         final MutationOptionBuilder mutationOptionBuilder) {
        final Func1<Throwable, Observable<? extends Boolean>> handleSubdocException = new
                Func1<Throwable, Observable<? extends Boolean>>() {
                    @Override
                    public Observable<? extends Boolean> call(Throwable throwable) {
                        ResponseStatus status = ((MultiMutationException) throwable).firstFailureStatus();
                        if (status == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                            //fail silently if the map doesn't contain the key
                            return Observable.just(true);
                        } else {
                            throw new CouchbaseException(status.toString());
                        }
                    }
                };

        return mutateIn(docId).remove(key)
                .withCas(mutationOptionBuilder.cas())
                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                .withExpiry(mutationOptionBuilder.expiry())
                .execute()
                .map(ResultMappingUtils.getMapResultFnForSubdocMutationToBoolean())
                .onErrorResumeNext(handleSubdocException);
    }

    @Override
    public Observable<Integer> mapSize(final String docId) {
        return get(docId, JsonDocument.class)
                .toList()
                .map(new Func1<List<JsonDocument>, Integer>() {
                    @Override
                    public Integer call(List<JsonDocument> documents) {
                        if (documents.size() == 0) {
                            throw new DocumentDoesNotExistException();
                        } else {
                            return documents.get(0).content().size();
                        }
                    }
                });
    }

    @Override
    public <E> Observable<E> listGet(String docId, int index, Class<E> elementType) {
        final Func1<DocumentFragment<Lookup>, E> mapResult = new
                Func1<DocumentFragment<Lookup>, E>() {
                    @Override
                    public E call(DocumentFragment<Lookup> documentFragment) {
                        ResponseStatus status = documentFragment.status(0);
                        if (status == ResponseStatus.SUCCESS) {
                            return (E) documentFragment.content(0);
                        } else if (status == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                            throw new PathNotFoundException("Index not found in list");
                        } else {
                            throw new CouchbaseException(status.toString());
                        }
                    }
                };

        return lookupIn(docId).get("[" + index + "]")
                .execute()
                .map(mapResult);
    }

    @Override
    public <E> Observable<Boolean> listAppend(String docId, E element) {
        return listAppend(docId, element, MutationOptionBuilder.builder());
    }


    @Override
    public <E> Observable<Boolean> listAppend(final String docId, final E element, final MutationOptionBuilder mutationOptionBuilder) {
        return listSubdocPushLast(docId, element, mutationOptionBuilder)
                .map(ResultMappingUtils.getMapResultFnForSubdocMutationToBoolean());
    }


    private <E> Observable<DocumentFragment<Mutation>> listSubdocPushLast(final String docId,
                                                                          final E element,
                                                                          final MutationOptionBuilder mutationOptionBuilder) {
        final Mutation mutationOperation = Mutation.ARRAY_PUSH_LAST;
        final Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>> retryIfDocExists = new
                Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentAlreadyExistsException) {
                            return listSubdocPushLast(docId, element, mutationOptionBuilder);
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                };
        return mutateIn(docId).arrayAppend("", element)
                .withCas(mutationOptionBuilder.cas())
                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                .withExpiry(mutationOptionBuilder.expiry())
                .execute()
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentDoesNotExistException) {
                            if (mutationOptionBuilder.createDocument()) {
                                return insert(JsonArrayDocument.create(docId, mutationOptionBuilder.expiry(),
                                        JsonArray.create().add(element)),
                                        mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                                        .map(ResultMappingUtils.getMapFullArrayDocResultToSubDocFn(mutationOperation))
                                        .onErrorResumeNext(retryIfDocExists);
                            } else {
                                return Observable.error(throwable);
                            }
                        } else {
                            if (throwable instanceof MultiMutationException) {
                                ResponseStatus status = ((MultiMutationException) throwable).firstFailureStatus();
                                return Observable.just(ResultMappingUtils.convertToSubDocumentResult(status, mutationOperation, element));
                            } else {
                                return Observable.error(throwable);
                            }
                        }
                    }
                });
    }

    @Override
    public Observable<Boolean> listRemove(String docId, int index) {
        return listRemove(docId, index, MutationOptionBuilder.builder());
    }

    @Override
    public Observable<Boolean> listRemove(final String docId, final int index, final MutationOptionBuilder mutationOptionBuilder) {
        return listSubdocRemove(docId, index, mutationOptionBuilder)
                .map(ResultMappingUtils.getMapResultFnForSubdocMutationToBoolean());
    }

    private Observable<DocumentFragment<Mutation>> listSubdocRemove(final String docId,
                                                                    final int index,
                                                                    final MutationOptionBuilder mutationOptionBuilder) {
        return mutateIn(docId).remove("[" + index + "]")
                .withCas(mutationOptionBuilder.cas())
                .withExpiry(mutationOptionBuilder.expiry())
                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                .execute();
    }

    @Override
    public <E> Observable<Boolean> listSet(String docId, int index, E element) {
        return listSet(docId, index, element, MutationOptionBuilder.builder());
    }

    @Override
    public <E> Observable<Boolean> listSet(String docId, int index, E element, MutationOptionBuilder mutationOptionBuilder) {
        return listSubdocInsert(docId, index, element, mutationOptionBuilder)
                .map(ResultMappingUtils.getMapResultFnForSubdocMutationToBoolean());
    }

    private <E> Observable<DocumentFragment<Mutation>> listSubdocInsert(final String docId,
                                                                        final int index,
                                                                        final E element,
                                                                        final MutationOptionBuilder mutationOptionBuilder) {
        final Mutation mutationOperation = Mutation.ARRAY_INSERT;
        final Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>> retryIfDocExists = new
                Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentAlreadyExistsException) {
                            return listSubdocInsert(docId, index, element, mutationOptionBuilder);
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                };
        return mutateIn(docId).arrayInsert("[" + index + "]", element)
                .withCas(mutationOptionBuilder.cas())
                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                .withExpiry(mutationOptionBuilder.expiry())
                .execute()
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentDoesNotExistException) {
                            if (mutationOptionBuilder.createDocument()) {
                                return insert(JsonArrayDocument.create(docId, mutationOptionBuilder.expiry(),
                                        JsonArray.create().add(element)),
                                        mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                                        .map(ResultMappingUtils.getMapFullArrayDocResultToSubDocFn(mutationOperation))
                                        .onErrorResumeNext(retryIfDocExists);
                            } else {
                                return Observable.error(throwable);
                            }
                        } else {
                            if (throwable instanceof MultiMutationException) {
                                ResponseStatus status = ((MultiMutationException) throwable).firstFailureStatus();
                                return Observable.just(ResultMappingUtils.convertToSubDocumentResult(status, mutationOperation, element));
                            } else {
                                return Observable.error(throwable);
                            }
                        }
                    }
                });
    }

    @Override
    public <E> Observable<Boolean> listPrepend(String docId, E element) {
        return listPrepend(docId, element, MutationOptionBuilder.builder());
    }

    @Override
    public <E> Observable<Boolean> listPrepend(final String docId, final E element, final MutationOptionBuilder mutationOptionBuilder) {
        return listSubdocPushFirst(docId, element, mutationOptionBuilder)
                .map(ResultMappingUtils.getMapResultFnForSubdocMutationToBoolean());
    }


    private <E> Observable<DocumentFragment<Mutation>> listSubdocPushFirst(final String docId,
                                                                           final E element,
                                                                           final MutationOptionBuilder mutationOptionBuilder) {


        final Mutation mutationOperation = Mutation.ARRAY_PUSH_FIRST;
        final Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>> retryIfDocExists = new
                Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        return listSubdocPushFirst(docId, element, mutationOptionBuilder);
                    }
                };

        return mutateIn(docId).arrayPrepend("", element)
                .withCas(mutationOptionBuilder.cas())
                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                .withExpiry(mutationOptionBuilder.expiry())
                .execute()
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentDoesNotExistException) {
                            if (mutationOptionBuilder.createDocument()) {
                                return insert(JsonArrayDocument.create(docId, mutationOptionBuilder.expiry(),
                                        JsonArray.create().add(element)),
                                        mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                                        .map(ResultMappingUtils.getMapFullArrayDocResultToSubDocFn(mutationOperation))
                                        .onErrorResumeNext(retryIfDocExists);
                            } else {
                                return Observable.error(throwable);
                            }
                        } else {
                            if (throwable instanceof MultiMutationException) {
                                ResponseStatus status = ((MultiMutationException) throwable).firstFailureStatus();
                                return Observable.just(ResultMappingUtils.convertToSubDocumentResult(status, mutationOperation, element));
                            } else {
                                return Observable.error(throwable);
                            }
                        }
                    }
                });
    }

    @Override
    public Observable<Integer> listSize(String docId) {
        return get(docId, JsonArrayDocument.class)
                .toList()
                .map(new Func1<List<JsonArrayDocument>, Integer>() {
                    @Override
                    public Integer call(List<JsonArrayDocument> documents) {
                        if (documents.size() == 0) {
                            throw new DocumentDoesNotExistException();
                        } else {
                            return documents.get(0).content().size();
                        }
                    }
                });
    }

    @Override
    public <E> Observable<Boolean> setAdd(String docId, E element) {
        return setAdd(docId, element, MutationOptionBuilder.builder());
    }

    @Override
    public <E> Observable<Boolean> setAdd(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        final Func1<DocumentFragment<Mutation>, Boolean> mapResult = new Func1<DocumentFragment<Mutation>, Boolean>() {
            @Override
            public Boolean call(DocumentFragment<Mutation> documentFragment) {
                ResponseStatus status = documentFragment.status(0);
                if (status == ResponseStatus.SUCCESS) {
                    return true;
                } else {
                    if (status == ResponseStatus.SUBDOC_PATH_EXISTS) {
                        return false;
                    } else {
                        throw new CouchbaseException(status.toString());
                    }
                }
            }
        };
        return setSubdocAddUnique(docId, element, mutationOptionBuilder)
                .map(mapResult);
    }

    private <E> Observable<DocumentFragment<Mutation>> setSubdocAddUnique(final String docId,
                                                                            final E element,
                                                                            final MutationOptionBuilder mutationOptionBuilder) {
        final Mutation mutationOperation = Mutation.ARRAY_ADD_UNIQUE;
        final Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>> retryIfDocExists = new
                Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentAlreadyExistsException) {
                            return setSubdocAddUnique(docId, element, mutationOptionBuilder);
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                };

        return mutateIn(docId).arrayAddUnique("", element)
                .withCas(mutationOptionBuilder.cas())
                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                .withExpiry(mutationOptionBuilder.expiry())
                .execute()
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentDoesNotExistException) {
                            if (mutationOptionBuilder.createDocument()) {
                                return insert(JsonArrayDocument.create(docId, mutationOptionBuilder.expiry(),
                                        JsonArray.create().add(element)),
                                        mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                                        .map(ResultMappingUtils.getMapFullArrayDocResultToSubDocFn(mutationOperation))
                                        .onErrorResumeNext(retryIfDocExists);
                            } else {
                                return Observable.error(throwable);
                            }
                        } else {
                            if (throwable instanceof MultiMutationException) {
                                ResponseStatus status = ((MultiMutationException) throwable).firstFailureStatus();
                                return Observable.just(ResultMappingUtils.convertToSubDocumentResult(status, mutationOperation, element));
                            } else {
                                return Observable.error(throwable);
                            }
                        }
                    }
                });
    }

    @Override
    public <E> Observable<Boolean> setContains(final String docId, final E element) {
        return get(docId, JsonArrayDocument.class)
                .toList()
                .map(new Func1<List<JsonArrayDocument>, Boolean>() {
                    @Override
                    public Boolean call(List<JsonArrayDocument> documents) {
                        if (documents.size() == 0) {
                            throw new DocumentDoesNotExistException();
                        }
                        JsonArrayDocument document = documents.get(0);
                        JsonArray jsonArray = document.content();
                        for (Object next : jsonArray) {
                            if (next == null && element == null) {
                                return true;
                            } else if (next != null && next.equals(element)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    @Override
    public <E> Observable<E> setRemove(String docId, E element) {
        return setRemove(docId, element, MutationOptionBuilder.builder());
    }

    @Override
    public <E> Observable<E> setRemove(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        return setSubdocRemove(docId, element, mutationOptionBuilder, MAX_CAS_RETRIES_DATASTRUCTURES);
    }

    private <E> Observable<E> setSubdocRemove(final String docId,
                                              final E element,
                                              final MutationOptionBuilder mutationOptionBuilder,
                                              final int retryCount) {
        final Mutation mutationOperation = Mutation.DELETE;
        if (retryCount <= 0) return Observable.error(new CASMismatchException());
        return get(docId, JsonArrayDocument.class)
                .toList()
                .flatMap(new Func1<List<JsonArrayDocument>, Observable<E>>() {
                    @Override
                    public Observable<E> call(List<JsonArrayDocument> documents) {
                        if (documents.size() == 0) {
                            throw new DocumentDoesNotExistException();
                        }
                        JsonArrayDocument jsonArrayDocument = documents.get(0);
                        Iterator iterator = jsonArrayDocument.content().iterator();
                        int ii = 0, index = -1;
                        while (iterator.hasNext()) {
                            Object next = iterator.next();
                            if (next == null && element == null) {
                                index = ii;
                                break;
                            } else if (next != null && next.equals(element)) {
                                index = ii;
                                break;
                            }
                            ii++;
                        }
                        if (index == -1) {
                            return Observable.just(element);
                        }
                        Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>> handleCASMismatch = new
                                Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                                    @Override
                                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                                        if (throwable instanceof CASMismatchException) {
                                            return setSubdocRemove(docId, element, mutationOptionBuilder, retryCount-1).
                                                    map(new Func1<E, DocumentFragment<Mutation>>() {
                                                        @Override
                                                        public DocumentFragment<Mutation> call(E element) {
                                                           return ResultMappingUtils.convertToSubDocumentResult(ResponseStatus.SUCCESS, mutationOperation, element);
                                                        }
                                                    });
                                        } else {
                                            return Observable.error(throwable);
                                        }
                                    }
                                };
                        return mutateIn(docId).remove("[" + index + "]")
                                .withCas(jsonArrayDocument.cas())
                                .withExpiry(mutationOptionBuilder.expiry())
                                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                                .execute()
                                .onErrorResumeNext(handleCASMismatch)
                                .map(new Func1<DocumentFragment<Mutation>, E>() {
                                    @Override
                                    public E call(DocumentFragment<Mutation> documentFragment) {
                                        ResponseStatus status = documentFragment.status(0);
                                        if (status == ResponseStatus.SUCCESS) {
                                            return element;
                                        } else {
                                            if (status == ResponseStatus.SUBDOC_PATH_NOT_FOUND ||
                                                    status == ResponseStatus.SUBDOC_PATH_INVALID) {
                                                return element;
                                            }
                                            throw new CouchbaseException(status.toString());
                                        }
                                    }
                                });
                    }
                });
    }

    @Override
    public Observable<Integer> setSize(String docId) {
        return get(docId, JsonArrayDocument.class)
                .toList()
                .map(new Func1<List<JsonArrayDocument>, Integer>() {
                    @Override
                    public Integer call(List<JsonArrayDocument> documents) {
                        if (documents.size() == 0) {
                            throw new DocumentDoesNotExistException();
                        }
                        return documents.get(0).content().size();
                    }
                });
    }

    @Override
    public <E> Observable<Boolean> queuePush(String docId, E element) {
        return queuePush(docId, element, MutationOptionBuilder.builder());
    }

    @Override
    public <E> Observable<Boolean> queuePush(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        return queueSubdocAddFirst(docId, element, mutationOptionBuilder)
                .map(ResultMappingUtils.getMapResultFnForSubdocMutationToBoolean());
    }

    private <E> Observable<DocumentFragment<Mutation>> queueSubdocAddFirst(final String docId,
                                                                          final E element,
                                                                          final MutationOptionBuilder mutationOptionBuilder) {
        final Mutation mutationOperation = Mutation.ARRAY_PUSH_FIRST;
        final Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>> retryIfDocExists = new
                Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentAlreadyExistsException) {
                            return queueSubdocAddFirst(docId, element, mutationOptionBuilder);
                        } else {
                            return Observable.error(throwable);
                        }
                    }
                };

        return mutateIn(docId).arrayPrepend("", element)
                .withCas(mutationOptionBuilder.cas())
                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                .withExpiry(mutationOptionBuilder.expiry())
                .execute()
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                    @Override
                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                        if (throwable instanceof DocumentDoesNotExistException) {
                            if (mutationOptionBuilder.createDocument()) {
                                return insert(JsonArrayDocument.create(docId, mutationOptionBuilder.expiry(),
                                        JsonArray.create().add(element)),
                                        mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                                        .map(ResultMappingUtils.getMapFullArrayDocResultToSubDocFn(mutationOperation))
                                        .onErrorResumeNext(retryIfDocExists);
                            } else {
                                return Observable.error(throwable);
                            }
                        } else {
                            if (throwable instanceof MultiMutationException) {
                                ResponseStatus status = ((MultiMutationException) throwable).firstFailureStatus();
                                return Observable.just(ResultMappingUtils.convertToSubDocumentResult(status,mutationOperation, element));
                            } else {
                                return Observable.error(throwable);
                            }
                        }
                    }
                });
    }

    @Override
    public <E> Observable<E> queuePop(String docId, Class<E> elementType) {
        return queuePop(docId, elementType, MutationOptionBuilder.builder());
    }

    @Override
    public <E> Observable<E> queuePop(String docId, Class<E> elementType, MutationOptionBuilder mutationOptionBuilder) {
        return queueSubdocRemove(docId, mutationOptionBuilder, elementType, MAX_CAS_RETRIES_DATASTRUCTURES);
    }

    private <E> Observable<E> queueSubdocRemove(final String docId,
                                                final MutationOptionBuilder mutationOptionBuilder,
                                                final Class<E> elementType,
                                                final int retryCount) {
        if (retryCount <= 0) return Observable.error(new CASMismatchException());
        final Mutation mutationOperation = Mutation.DELETE;
        return get(docId, JsonArrayDocument.class)
                .toList()
                .flatMap(new Func1<List<JsonArrayDocument>, Observable<E>>() {
                    @Override
                    public Observable<E> call(List<JsonArrayDocument> jsonArrayDocuments) {
                        if (jsonArrayDocuments.size() == 0) {
                            throw new DocumentDoesNotExistException();
                        }
                        JsonArrayDocument jsonArrayDocument = jsonArrayDocuments.get(0);
                        int size = jsonArrayDocument.content().size();
                        final Object val;
                        if (size > 0) {
                            val = jsonArrayDocument.content().get(size - 1);
                        } else {
                            return Observable.just(null);
                        }
                        if (mutationOptionBuilder.cas() != 0 && jsonArrayDocument.cas() != mutationOptionBuilder.cas()) {
                            throw new CASMismatchException();
                        }
                        Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>> handleCASMismatch = new
                                Func1<Throwable, Observable<? extends DocumentFragment<Mutation>>>() {
                                    @Override
                                    public Observable<? extends DocumentFragment<Mutation>> call(Throwable throwable) {
                                        if (throwable instanceof CASMismatchException) {
                                            return queueSubdocRemove(docId, mutationOptionBuilder, elementType, retryCount-1)
                                                    .map(new Func1<E, DocumentFragment<Mutation>>() {
                                                        @Override
                                                        public DocumentFragment<Mutation> call(E element) {
                                                            if (element == null) {
                                                                throw new CASMismatchException();
                                                            }
                                                            return ResultMappingUtils.convertToSubDocumentResult(ResponseStatus.SUCCESS, mutationOperation, element);
                                                        }
                                                    });
                                        } else {
                                            return Observable.error(throwable);
                                        }
                                    }
                                };
                        return mutateIn(docId).remove("[" + -1 + "]")
                                .withCas(jsonArrayDocument.cas())
                                .withExpiry(mutationOptionBuilder.expiry())
                                .withDurability(mutationOptionBuilder.persistTo(), mutationOptionBuilder.replicateTo())
                                .execute()
                                .onErrorResumeNext(handleCASMismatch)
                                .map(new Func1<DocumentFragment<Mutation>, E>() {
                                    @Override
                                    public E call(DocumentFragment<Mutation> documentFragment) {
                                        ResponseStatus status = documentFragment.status(0);
                                        if (status == ResponseStatus.SUCCESS) {
                                            if (documentFragment.content(0) != null) {
                                                return (E) documentFragment.content(0);
                                            } else {
                                                return (E) val;
                                            }
                                        } else {
                                            throw new CouchbaseException(status.toString());
                                        }
                                    }
                                });
                    }
                });
    }

    @Override
    public Observable<Integer> queueSize(String docId) {
        return get(docId, JsonArrayDocument.class)
                .toList()
                .map(new Func1<List<JsonArrayDocument>, Integer>() {
                    @Override
                    public Integer call(List<JsonArrayDocument> documents) {
                        if (documents.size() == 0) {
                            throw new DocumentDoesNotExistException();
                        }
                        return documents.get(0).content().size();
                    }
                });
    }

    @Override
    public Observable<Boolean> close() {
        return Observable.defer(new Func0<Observable<CloseBucketResponse>>() {
            @Override
            public Observable<CloseBucketResponse> call() {
                return core.send(new CloseBucketRequest(bucket));
            }
        }).map(new Func1<CloseBucketResponse, Boolean>() {
            @Override
            public Boolean call(CloseBucketResponse response) {
                closed = true;
                return response.status().isSuccess();
            }
        });
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return "AsyncBucket[" + name() + "]";
    }

    @Override
    public Observable<Integer> invalidateQueryCache() {
        return Observable.just(n1qlQueryExecutor.invalidateQueryCache());
    }

    @Override
    public Single<PingReport> ping(String reportId, long timeout, TimeUnit timeUnit) {
        return HealthPinger.ping(environment, bucket, password, core, reportId, timeout, timeUnit);
    }

    @Override
    public Single<PingReport> ping(long timeout, TimeUnit timeUnit) {
        return HealthPinger.ping(environment, bucket, password, core, null, timeout, timeUnit);
    }

    @Override
    public Single<PingReport> ping(Collection<ServiceType> services, long timeout, TimeUnit timeUnit) {
        return HealthPinger.ping(environment, bucket, password, core, null, timeout, timeUnit,
          services.toArray(new ServiceType[services.size()]));
    }

    @Override
    public Single<PingReport> ping(String reportId, Collection<ServiceType> services, long timeout, TimeUnit timeUnit) {
        return HealthPinger.ping(environment, bucket, password, core, reportId, timeout, timeUnit,
          services.toArray(new ServiceType[services.size()]));
    }

    /**
     * Helper method to start tracing and return the span.
     */
    private Span startTracing(String spanName) {
        if (!environment.operationTracingEnabled()) {
            return null;
        }
        Scope scope = environment.tracer()
            .buildSpan(spanName)
            .startActive(false);
        Span parent = scope.span();
        scope.close();
        return parent;
    }

    /**
     * Helper method to stop tracing for the parent span given.
     */
    private Action0 stopTracing(final Span parent) {
        return new Action0() {
            @Override
            public void call() {
                if (parent != null) {
                    environment.tracer().scopeManager()
                        .activate(parent, true)
                        .close();
                }
            }
        };
    }

    /**
     * Helper method to encapsulate the logic of enriching the exception with detailed status info.
     */
    private static <X extends CouchbaseException, R extends CouchbaseResponse> X addDetails(X ex, R r) {
        return Utils.addDetails(ex, r);
    }
}
