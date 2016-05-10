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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.cluster.CloseBucketRequest;
import com.couchbase.client.core.message.cluster.CloseBucketResponse;
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
import com.couchbase.client.core.message.observe.Observe;
import com.couchbase.client.core.message.search.SearchQueryRequest;
import com.couchbase.client.core.message.search.SearchQueryResponse;
import com.couchbase.client.core.message.view.ViewQueryRequest;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.bucket.DefaultAsyncBucketManager;
import com.couchbase.client.java.bucket.ReplicaReader;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
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
import com.couchbase.client.java.fts.SearchParams;
import com.couchbase.client.java.fts.SearchQuery;
import com.couchbase.client.java.fts.queries.AbstractFtsQuery;
import com.couchbase.client.java.fts.result.AsyncSearchQueryResult;
import com.couchbase.client.java.fts.result.impl.DefaultAsyncSearchQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.core.N1qlQueryExecutor;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.repository.CouchbaseAsyncRepository;
import com.couchbase.client.java.subdoc.AsyncLookupInBuilder;
import com.couchbase.client.java.subdoc.AsyncMutateInBuilder;
import com.couchbase.client.java.transcoder.BinaryTranscoder;
import com.couchbase.client.java.transcoder.JacksonTransformers;
import com.couchbase.client.java.transcoder.JsonArrayTranscoder;
import com.couchbase.client.java.transcoder.JsonBooleanTranscoder;
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
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

public class CouchbaseAsyncBucket implements AsyncBucket {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(CouchbaseAsyncBucket.class);

    private static final int COUNTER_NOT_EXISTS_EXPIRY = 0xffffffff;

    public static final String CURRENT_BUCKET_IDENTIFIER = "#CURRENT_BUCKET#";

    public static final JsonTranscoder JSON_OBJECT_TRANSCODER = new JsonTranscoder();
    public static final JsonArrayTranscoder JSON_ARRAY_TRANSCODER = new JsonArrayTranscoder();
    public static final JsonBooleanTranscoder JSON_BOOLEAN_TRANSCODER = new JsonBooleanTranscoder();
    public static final JsonDoubleTranscoder JSON_DOUBLE_TRANSCODER = new JsonDoubleTranscoder();
    public static final JsonLongTranscoder JSON_LONG_TRANSCODER = new JsonLongTranscoder();
    public static final JsonStringTranscoder JSON_STRING_TRANSCODER = new JsonStringTranscoder();
    public static final RawJsonTranscoder RAW_JSON_TRANSCODER = new RawJsonTranscoder();

    public static final LegacyTranscoder LEGACY_TRANSCODER = new LegacyTranscoder();
    public static final BinaryTranscoder BINARY_TRANSCODER = new BinaryTranscoder();
    public static final StringTranscoder STRING_TRANSCODER = new StringTranscoder();
    public static final SerializableTranscoder SERIALIZABLE_TRANSCODER = new SerializableTranscoder();

    private final String bucket;
    private final String password;
    private final ClusterFacade core;
    private final Map<Class<? extends Document>, Transcoder<? extends Document, ?>> transcoders;
    //TODO this could be opened for customization like with transcoders
    private final FragmentTranscoder subdocumentTranscoder = new JacksonFragmentTranscoder(JacksonTransformers.MAPPER);
    private final AsyncBucketManager bucketManager;
    private final CouchbaseEnvironment environment;
    /** the bucket's {@link N1qlQueryExecutor}. Prefer using {@link #n1qlQueryExecutor()} since it allows mocking and testing */
    private final N1qlQueryExecutor n1qlQueryExecutor;

    private volatile boolean closed;


    public CouchbaseAsyncBucket(final ClusterFacade core, final CouchbaseEnvironment environment, final String name,
        final String password, final List<Transcoder<? extends Document, ?>> customTranscoders) {
        bucket = name;
        this.password = password;
        this.core = core;
        this.environment = environment;
        this.closed = false;

        transcoders = new ConcurrentHashMap<Class<? extends Document>, Transcoder<? extends Document, ?>>();
        transcoders.put(JSON_OBJECT_TRANSCODER.documentType(), JSON_OBJECT_TRANSCODER);
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

        for (Transcoder<? extends Document, ?> custom : customTranscoders) {
            transcoders.put(custom.documentType(), custom);
        }

        bucketManager = DefaultAsyncBucketManager.create(bucket, password, core);

        boolean n1qlPreparedEncodedPlanEnabled = "true".equalsIgnoreCase(System.getProperty(N1qlQueryExecutor.ENCODED_PLAN_ENABLED_PROPERTY, "true")); //active by default
        n1qlQueryExecutor = new N1qlQueryExecutor(core, bucket, password, n1qlPreparedEncodedPlanEnabled);
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
        return Observable.defer(new Func0<Observable<GetResponse>>() {
                @Override
                public Observable<GetResponse> call() {
                    return core.send(new GetRequest(id, bucket));
                }
            })
            .filter(new Func1<GetResponse, Boolean>() {
                @Override
                public Boolean call(GetResponse response) {
                    if (response.status().isSuccess()) {
                        return true;
                    }
                    ByteBuf content = response.content();
                    if (content != null && content.refCnt() > 0) {
                        content.release();
                    }

                    switch(response.status()) {
                        case NOT_EXISTS:
                            return false;
                        case TEMPORARY_FAILURE:
                        case SERVER_BUSY:
                            throw new TemporaryFailureException();
                        case OUT_OF_MEMORY:
                            throw new CouchbaseOutOfMemoryException();
                        default:
                            throw new CouchbaseException(response.status().toString());
                    }
                }
            })
            .map(new Func1<GetResponse, D>() {
                @Override
                public D call(final GetResponse response) {
                    Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);
                    return (D) transcoder.decode(id, response.content(), response.cas(), 0, response.flags(),
                        response.status());
                }
            });
    }

    @Override
    public Observable<Boolean> exists(final String id) {
        return Observable.defer(new Func0<Observable<ObserveResponse>>() {
            @Override
            public Observable<ObserveResponse> call() {
                return core.send(new ObserveRequest(id, 0, true, (short) 0, bucket));
            }
        })
            .map(new Func1<ObserveResponse, Boolean>() {
                @Override
                public Boolean call(ObserveResponse response) {
                    ByteBuf content = response.content();
                    if (content != null && content.refCnt() > 0) {
                        content.release();
                    }

                    ObserveResponse.ObserveStatus foundStatus = response.observeStatus();
                    if (foundStatus == ObserveResponse.ObserveStatus.FOUND_PERSISTED
                        || foundStatus == ObserveResponse.ObserveStatus.FOUND_NOT_PERSISTED) {
                        return true;
                    }

                    return false;
                }
            });
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
        return Observable.defer(new Func0<Observable<GetResponse>>() {
                @Override
                public Observable<GetResponse> call() {
                    return core.send(new GetRequest(id, bucket, true, false, lockTime));
                }
            })
            .filter(new Func1<GetResponse, Boolean>() {
                @Override
                public Boolean call(GetResponse response) {
                    if (response.status().isSuccess()) {
                        return true;
                    }
                    ByteBuf content = response.content();
                    if (content != null && content.refCnt() > 0) {
                        content.release();
                    }

                    switch (response.status()) {
                        case NOT_EXISTS:
                            return false;
                        case TEMPORARY_FAILURE:
                            throw new TemporaryLockFailureException();
                        case SERVER_BUSY:
                            throw new TemporaryFailureException();
                        case OUT_OF_MEMORY:
                            throw new CouchbaseOutOfMemoryException();
                        default:
                            throw new CouchbaseException(response.status().toString());
                    }
                }
            })
            .map(new Func1<GetResponse, D>() {
                @Override
                public D call(final GetResponse response) {
                    Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);
                    return (D) transcoder.decode(id, response.content(), response.cas(), 0, response.flags(),
                        response.status());
                }
            });
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
        return Observable.defer(new Func0<Observable<GetResponse>>() {
                @Override
                public Observable<GetResponse> call() {
                    return core.send(new GetRequest(id, bucket, false, true, expiry));
                }
            })
            .filter(new Func1<GetResponse, Boolean>() {
                @Override
                public Boolean call(GetResponse response) {
                    if (response.status().isSuccess()) {
                        return true;
                    }
                    ByteBuf content = response.content();
                    if (content != null && content.refCnt() > 0) {
                        content.release();
                    }

                    switch (response.status()) {
                        case NOT_EXISTS:
                            return false;
                        case TEMPORARY_FAILURE:
                        case SERVER_BUSY:
                            throw new TemporaryFailureException();
                        case OUT_OF_MEMORY:
                            throw new CouchbaseOutOfMemoryException();
                        default:
                            throw new CouchbaseException(response.status().toString());
                    }
                }
            })
            .map(new Func1<GetResponse, D>() {
                @Override
                public D call(final GetResponse response) {
                    Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);
                    return (D) transcoder.decode(id, response.content(), response.cas(), 0, response.flags(),
                        response.status());
                }
            });
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
        return ReplicaReader
            .read(core, id, type, bucket)
            .map(new Func1<GetResponse, D>() {
                @Override
                public D call(final GetResponse response) {
                    Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);
                    return (D) transcoder.decode(id, response.content(), response.cas(), 0, response.flags(),
                        response.status());
                }
            })
            .cache(type.maxAffectedNodes());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> insert(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());

        return Observable.defer(new Func0<Observable<InsertResponse>>() {
            @Override
            public Observable<InsertResponse> call() {
                Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
                return core.send(new InsertRequest(document.id(), encoded.value1(), document.expiry(), encoded.value2(), bucket));
            }
        }).map(new Func1<InsertResponse, D>() {
            @Override
            public D call(InsertResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return (D) transcoder.newDocument(document.id(), document.expiry(),
                        document.content(), response.cas(), response.mutationToken());
                }

                switch (response.status()) {
                    case TOO_BIG:
                        throw new RequestTooBigException();
                    case EXISTS:
                        throw new DocumentAlreadyExistsException();
                    case TEMPORARY_FAILURE:
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(final D document, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
        Observable<D> insertResult = insert(document);

        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return insertResult;
        }

        return insertResult.flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(), persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
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
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> upsert(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());

        return Observable.defer(new Func0<Observable<UpsertResponse>>() {
            @Override
            public Observable<UpsertResponse> call() {
                Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
                return core.send(new UpsertRequest(document.id(), encoded.value1(), document.expiry(), encoded.value2(), bucket));
            }
        }).map(new Func1<UpsertResponse, D>() {
            @Override
            public D call(UpsertResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return (D) transcoder.newDocument(document.id(), document.expiry(),
                        document.content(), response.cas(), response.mutationToken());
                }

                switch (response.status()) {
                    case TOO_BIG:
                        throw new RequestTooBigException();
                    case EXISTS:
                        throw new CASMismatchException();
                    case TEMPORARY_FAILURE:
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(final D document, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
        Observable<D> upsertResult = upsert(document);

        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return upsertResult;
        }

        return upsertResult.flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(), persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
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
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> replace(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());

        return Observable.defer(new Func0<Observable<ReplaceResponse>>() {
            @Override
            public Observable<ReplaceResponse> call() {
                Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
                return core.send(new ReplaceRequest(document.id(), encoded.value1(), document.cas(), document.expiry(), encoded.value2(), bucket));
            }
        }).map(new Func1<ReplaceResponse, D>() {
            @Override
            public D call(ReplaceResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return (D) transcoder.newDocument(document.id(), document.expiry(),
                        document.content(), response.cas(), response.mutationToken());
                }

                switch (response.status()) {
                    case TOO_BIG:
                        throw new RequestTooBigException();
                    case NOT_EXISTS:
                        throw new DocumentDoesNotExistException();
                    case EXISTS:
                        throw new CASMismatchException();
                    case TEMPORARY_FAILURE:
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
    }

    @Override
    public <D extends Document<?>> Observable<D> replace(final D document, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
        Observable<D> replaceResult = replace(document);

        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return replaceResult;
        }

        return replaceResult.flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(), persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
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
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> remove(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        return Observable.defer(new Func0<Observable<RemoveResponse>>() {
            @Override
            public Observable<RemoveResponse> call() {
                return core.send(new RemoveRequest(document.id(), document.cas(), bucket));
            }
        }).map(new Func1<RemoveResponse, D>() {
            @Override
            public D call(final RemoveResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return (D) transcoder.newDocument(document.id(), 0, null, response.cas(), response.mutationToken());
                }

                switch (response.status()) {
                    case NOT_EXISTS:
                        throw new DocumentDoesNotExistException();
                    case EXISTS:
                        throw new CASMismatchException();
                    case TEMPORARY_FAILURE:
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
    }

    @Override
    public Observable<JsonDocument> remove(final String id) {
        return remove(id, JsonDocument.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> remove(final String id, final Class<D> target) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(target);
        return remove((D) transcoder.newDocument(id, 0, null, 0));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> remove(D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        return observeRemove(remove(document), persistTo, replicateTo);
    }

    @Override
    public Observable<JsonDocument> remove(String id, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(id, persistTo, replicateTo, JsonDocument.class);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(String id, final PersistTo persistTo,
        final ReplicateTo replicateTo, Class<D> target) {
        return observeRemove(remove(id, target), persistTo, replicateTo);
    }

    /**
     * Helper method to observe the result of a remove operation with the given durability
     * requirements.
     *
     * @param removeResult the original result of the actual remove operation.
     * @param persistTo the persistence requirement given.
     * @param replicateTo the replication requirement given.
     * @return an observable reporting success or error of the observe operation.
     */
    private <D extends Document<?>> Observable<D> observeRemove(Observable<D> removeResult,
        final PersistTo persistTo, final ReplicateTo replicateTo) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return removeResult;
        }

        return removeResult.flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), true, doc.mutationToken(),
                        persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
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
            }
        });
    }

    @Override
    public Observable<AsyncViewResult> query(final ViewQuery query) {
        Observable<ViewQueryResponse> source = Observable.defer(new Func0<Observable<ViewQueryResponse>>() {
            @Override
            public Observable<ViewQueryResponse> call() {
                final ViewQueryRequest request = new ViewQueryRequest(query.getDesign(), query.getView(),
                    query.isDevelopment(), query.toQueryString(), query.getKeys(), bucket, password);
                return core.send(request);
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
        final String indexName = query.indexName();
        final AbstractFtsQuery queryPart = query.query();
        final SearchParams params = query.params();

        //always set a server side timeout. if not explicit, set it to the client side timeout
        if (params.getServerSideTimeout() == null) {
            params.serverSideTimeout(environment().searchTimeout(), TimeUnit.MILLISECONDS);
        }

        Observable<SearchQueryResponse> source = Observable.defer(new Func0<Observable<SearchQueryResponse>>() {
            @Override
            public Observable<SearchQueryResponse> call() {
                final SearchQueryRequest request =
                    new SearchQueryRequest(indexName, queryPart.export(params).toString(), bucket, password);
                return core.send(request);
            }
        });

        return source.map(new Func1<SearchQueryResponse, AsyncSearchQueryResult>() {
            @Override
            public AsyncSearchQueryResult call(SearchQueryResponse response) {
                if (response.status().isSuccess()) {
                    JsonObject json = JsonObject.fromJson(response.payload());
                    return DefaultAsyncSearchQueryResult.fromJson(json);
                } else if (response.status() == ResponseStatus.INVALID_ARGUMENTS) {
                    return DefaultAsyncSearchQueryResult.fromHttp400(response.payload());
                } else {
                    throw new CouchbaseException("Could not query search index, " + response.status() + ": " + response.payload());
                }
            }
        });
    }

    @Override
    public Observable<AsyncSpatialViewResult> query(final SpatialViewQuery query) {
        Observable<ViewQueryResponse> source = Observable.defer(new Func0<Observable<ViewQueryResponse>>() {
            @Override
            public Observable<ViewQueryResponse> call() {
                final ViewQueryRequest request = new ViewQueryRequest(query.getDesign(), query.getView(),
                    query.isDevelopment(), true, query.toString(), null, bucket, password);
                return core.send(request);
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
        if (!query.params().hasServerSideTimeout()) {
            query.params().serverSideTimeout(environment().queryTimeout(), TimeUnit.MILLISECONDS);
        }
        return n1qlQueryExecutor.execute(query);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta) {
        return counter(id, delta, 0, COUNTER_NOT_EXISTS_EXPIRY);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial) {
        return counter(id, delta, initial, 0);
    }

    @Override
    public Observable<JsonLongDocument> counter(final String id, final long delta, final long initial, final int expiry) {
        return Observable.defer(new Func0<Observable<CounterResponse>>() {
            @Override
            public Observable<CounterResponse> call() {
                return core.send(new CounterRequest(id, initial, delta, expiry, bucket));
            }
        }).map(new Func1<CounterResponse, JsonLongDocument>() {
            @Override
            public JsonLongDocument call(CounterResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    int returnedExpiry = expiry == COUNTER_NOT_EXISTS_EXPIRY ? 0 : expiry;
                    return JsonLongDocument.create(id, returnedExpiry, response.value(),
                        response.cas(), response.mutationToken());
                }

                switch (response.status()) {
                    case NOT_EXISTS:
                        throw new DocumentDoesNotExistException();
                    case TEMPORARY_FAILURE:
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
    }

    @Override
    public Observable<Boolean> unlock(final String id, final long cas) {
        return Observable.defer(new Func0<Observable<UnlockResponse>>() {
            @Override
            public Observable<UnlockResponse> call() {
                return core.send(new UnlockRequest(id, cas, bucket));
            }
        }).map(new Func1<UnlockResponse, Boolean>() {
            @Override
            public Boolean call(UnlockResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return true;
                }

                switch (response.status()) {
                    case NOT_EXISTS:
                        throw new DocumentDoesNotExistException();
                    case TEMPORARY_FAILURE:
                        throw new TemporaryLockFailureException();
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> unlock(D document) {
        return unlock(document.id(), document.cas());
    }

    @Override
    public Observable<Boolean> touch(final String id, final int expiry) {
        return Observable.defer(new Func0<Observable<TouchResponse>>() {
            @Override
            public Observable<TouchResponse> call() {
                return core.send(new TouchRequest(id, expiry, bucket));
            }
        }).map(new Func1<TouchResponse, Boolean>() {
            @Override
            public Boolean call(TouchResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return true;
                }

                switch (response.status()) {
                    case NOT_EXISTS:
                        throw new DocumentDoesNotExistException();
                    case TEMPORARY_FAILURE:
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> touch(D document) {
        return touch(document.id(), document.expiry());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> append(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());

        return Observable.defer(new Func0<Observable<AppendResponse>>() {
            @Override
            public Observable<AppendResponse> call() {
                Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
                return core.send(new AppendRequest(document.id(), document.cas(), encoded.value1(), bucket));
            }
        }).map(new Func1<AppendResponse, D>() {
            @Override
            public D call(final AppendResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return (D) transcoder.newDocument(document.id(), 0, null, response.cas(), response.mutationToken());
                }

                switch (response.status()) {
                    case TOO_BIG:
                        throw new RequestTooBigException();
                    case NOT_STORED:
                        throw new DocumentDoesNotExistException();
                    case TEMPORARY_FAILURE:
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    case EXISTS:
                        throw new CASMismatchException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> prepend(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        return Observable.defer(new Func0<Observable<PrependResponse>>() {
            @Override
            public Observable<PrependResponse> call() {
                Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
                return core.send(new PrependRequest(document.id(), document.cas(), encoded.value1(), bucket));
            }
        }).map(new Func1<PrependResponse, D>() {
            @Override
            public D call(final PrependResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return (D) transcoder.newDocument(document.id(), 0, null, response.cas(), response.mutationToken());
                }

                switch (response.status()) {
                    case TOO_BIG:
                        throw new RequestTooBigException();
                    case NOT_STORED:
                        throw new DocumentDoesNotExistException();
                    case TEMPORARY_FAILURE:
                    case SERVER_BUSY:
                        throw new TemporaryFailureException();
                    case OUT_OF_MEMORY:
                        throw new CouchbaseOutOfMemoryException();
                    case EXISTS:
                        throw new CASMismatchException();
                    default:
                        throw new CouchbaseException(response.status().toString());
                }
            }
        });
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
    public Observable<JsonLongDocument> counter(String id, long delta, PersistTo persistTo) {
        return counter(id, delta, persistTo, ReplicateTo.NONE);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, ReplicateTo replicateTo) {
        return counter(id, delta, PersistTo.NONE, replicateTo);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, PersistTo persistTo) {
        return counter(id, delta, initial, persistTo, ReplicateTo.NONE);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, ReplicateTo replicateTo) {
        return counter(id, delta, initial, PersistTo.NONE, replicateTo);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, PersistTo persistTo) {
        return counter(id, delta, initial, expiry, persistTo, ReplicateTo.NONE);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, ReplicateTo replicateTo) {
        return counter(id, delta, initial, expiry, PersistTo.NONE, replicateTo);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, PersistTo persistTo, ReplicateTo replicateTo) {
        return counter(id, delta, initial, 0, persistTo, replicateTo);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, PersistTo persistTo, ReplicateTo replicateTo) {
        return counter(id, delta, 0, COUNTER_NOT_EXISTS_EXPIRY, persistTo, replicateTo);
    }

    @Override
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, final PersistTo persistTo, final ReplicateTo replicateTo) {

        Observable<JsonLongDocument> counterResult = counter(id, delta, initial, expiry);

        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return counterResult;
        }

        return counterResult.flatMap(new Func1<JsonLongDocument, Observable<JsonLongDocument>>() {
            @Override
            public Observable<JsonLongDocument> call(final JsonLongDocument doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(),
                        persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
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
            }
        });
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, PersistTo persistTo) {
        return append(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, ReplicateTo replicateTo) {
        return append(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <D extends Document<?>> Observable<D> append(D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        Observable<D> appendResult = append(document);

        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return appendResult;
        }

        return appendResult.flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(), persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
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
            }
        });
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, PersistTo persistTo) {
        return prepend(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, ReplicateTo replicateTo) {
        return prepend(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <D extends Document<?>> Observable<D> prepend(D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        Observable<D> prependResult = prepend(document);

        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return prependResult;
        }

        return prependResult.flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, doc.mutationToken(), persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
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
            }
        });
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
}
