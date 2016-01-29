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
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.MultiLookupResponse;
import com.couchbase.client.core.message.kv.subdoc.multi.SubMultiLookupRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SimpleSubdocResponse;
import com.couchbase.client.core.message.kv.subdoc.simple.SubArrayRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubCounterRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubDeleteRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubDictAddRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubDictUpsertRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubExistRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubGetRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubReplaceRequest;
import com.couchbase.client.core.message.observe.Observe;
import com.couchbase.client.core.message.search.SearchQueryRequest;
import com.couchbase.client.core.message.search.SearchQueryResponse;
import com.couchbase.client.core.message.view.ViewQueryRequest;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.bucket.DefaultAsyncBucketManager;
import com.couchbase.client.java.bucket.ReplicaReader;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.subdoc.DocumentFragment;
import com.couchbase.client.java.document.subdoc.ExtendDirection;
import com.couchbase.client.java.document.subdoc.LookupResult;
import com.couchbase.client.java.document.subdoc.LookupSpec;
import com.couchbase.client.java.document.subdoc.MultiLookupResult;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.error.RequestTooBigException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.subdoc.CannotInsertValueException;
import com.couchbase.client.java.error.subdoc.DeltaTooBigException;
import com.couchbase.client.java.error.subdoc.DocumentNotJsonException;
import com.couchbase.client.java.error.subdoc.DocumentTooDeepException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.error.subdoc.NumberTooBigException;
import com.couchbase.client.java.error.subdoc.PathExistsException;
import com.couchbase.client.java.error.subdoc.PathInvalidException;
import com.couchbase.client.java.error.subdoc.PathMismatchException;
import com.couchbase.client.java.error.subdoc.PathNotFoundException;
import com.couchbase.client.java.error.subdoc.PathTooDeepException;
import com.couchbase.client.java.error.subdoc.ValueTooDeepException;
import com.couchbase.client.java.error.subdoc.ZeroDeltaException;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.core.N1qlQueryExecutor;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.repository.CouchbaseAsyncRepository;
import com.couchbase.client.java.search.SearchQueryRow;
import com.couchbase.client.java.search.SearchQueryResult;
import com.couchbase.client.java.search.query.SearchQuery;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
        n1qlQueryExecutor = new N1qlQueryExecutor(core, bucket, password);
    }

    @Override
    public String name() {
        return bucket;
    }

    @Override
    public Observable<ClusterFacade> core() {
        return Observable.just(core);
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
                    query.isDevelopment(), query.toString(), query.getKeys(), bucket, password);
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
    public Observable<SearchQueryResult> query(final SearchQuery query) {
        Observable<SearchQueryResponse> source = Observable.defer(new Func0<Observable<SearchQueryResponse>>() {
            @Override
            public Observable<SearchQueryResponse> call() {
                final SearchQueryRequest request =
                    new SearchQueryRequest(query.index(), query.json().toString(), bucket, password);
                return core.send(request);
            }
        });

        // TODO: this needs to be refactored into its own class.
        return source.map(new Func1<SearchQueryResponse, SearchQueryResult>() {
            @Override
            public SearchQueryResult call(SearchQueryResponse response) {
                if (!response.status().isSuccess()) {
                    throw new CouchbaseException("Could not query search index: " + response.payload());
                }

                JsonObject json = JsonObject.fromJson(response.payload());
                long totalHits = json.getLong("total_hits");
                long took = json.getLong("took");
                double maxScore = json.getDouble("max_score");
                List<SearchQueryRow> hits = new ArrayList<SearchQueryRow>();
                for (Object rawHit : json.getArray("hits")) {
                    JsonObject hit = (JsonObject)rawHit;
                    String index = hit.getString("index");
                    String id = hit.getString("id");
                    double score = hit.getDouble("score");
                    String explanation = null;
                    JsonObject explanationJson = hit.getObject("explanation");
                    if (explanationJson != null) {
                        explanation = explanationJson.toString();
                    }
                    Map<String, Map<String, List<SearchQueryRow.Location>>> locations = null;
                    JsonObject locationsJson = hit.getObject("locations");
                    if (locationsJson != null) {
                        locations = new HashMap<String, Map<String, List<SearchQueryRow.Location>>>();
                        for (String field : locationsJson.getNames()) {
                            JsonObject termsJson = locationsJson.getObject(field);
                            Map<String, List<SearchQueryRow.Location>> terms = new HashMap<String, List<SearchQueryRow.Location>>();
                            for (String term : termsJson.getNames()) {
                                JsonArray locsJson = termsJson.getArray(term);
                                List<SearchQueryRow.Location> locs = new ArrayList<SearchQueryRow.Location>(locsJson.size());
                                for (int i = 0; i < locsJson.size(); i++) {
                                    JsonObject loc = locsJson.getObject(i);
                                    long pos = loc.getLong("pos");
                                    long start = loc.getLong("start");
                                    long end = loc.getLong("end");
                                    JsonArray arrayPositionsJson = loc.getArray("array_positions");
                                    long[] arrayPositions = null;
                                    if (arrayPositionsJson != null) {
                                        arrayPositions = new long[arrayPositionsJson.size()];
                                        for (int j = 0; j < arrayPositionsJson.size(); j++) {
                                            arrayPositions[j] = arrayPositionsJson.getLong(j);
                                        }
                                    }
                                    locs.add(new SearchQueryRow.Location(pos, start, end, arrayPositions));
                                }
                                terms.put(term, locs);
                            }
                            locations.put(field, terms);
                        }
                    }

                    Map<String, String[]> fragments = null;
                    JsonObject fragmentsJson = hit.getObject("fragments");
                    if (fragmentsJson != null) {
                        fragments = new HashMap<String, String[]>();
                        for (String field : fragmentsJson.getNames()) {
                            JsonArray fragmentJson = fragmentsJson.getArray(field);
                            String[] fragment = null;
                            if (fragmentJson != null) {
                                fragment = new String[fragmentJson.size()];
                                for (int i = 0; i < fragmentJson.size(); i++) {
                                    fragment[i] = fragmentJson.getString(i);
                                }
                            }
                            fragments.put(field, fragment);
                        }
                    }

                    Map<String, Object> fields = null;
                    JsonObject fieldsJson = hit.getObject("fields");
                    if (fieldsJson != null) {
                        fields = fieldsJson.toMap();
                    }

                    hits.add(new SearchQueryRow(index, id, score, explanation, locations, fragments, fields));
                }
                return new SearchQueryResult(took, totalHits, maxScore, hits);
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
    public <T> Observable<DocumentFragment<T>> getIn(final String id, final String path, final Class<T> fragmentType) {
        return Observable.defer(new Func0<Observable<SimpleSubdocResponse>>() {
            @Override
            public Observable<SimpleSubdocResponse> call() {
                SubGetRequest request = new SubGetRequest(id, path, bucket);
                return core.send(request);
            }
        }).filter(new Func1<SimpleSubdocResponse, Boolean>() {
            @Override
            public Boolean call(SimpleSubdocResponse response) {
                if (response.status().isSuccess()) {
                    return true;
                }

                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                switch(response.status()) {
                    case SUBDOC_PATH_NOT_FOUND:
                        return false;
                    default:
                        throw commonSubdocErrors(response.status(), id, path);
                }
            }
        }).map(new Func1<SimpleSubdocResponse, DocumentFragment<T>>() {
            @Override
            public DocumentFragment<T> call(SimpleSubdocResponse response) {
                try {
                    T content = subdocumentTranscoder.decodeWithMessage(response.content(), fragmentType,
                            "Couldn't decode subget fragment for " + id + "/" + path);
                    return DocumentFragment.create(id, path, 0, content, response.cas(), response.mutationToken());
                } finally {
                    if (response.content() != null) {
                        response.content().release();
                    }
                }
            }
        });
    }

    @Override
    public Observable<Boolean> existsIn(final String id, final String path) {
        return Observable.defer(new Func0<Observable<SimpleSubdocResponse>>() {
            @Override
            public Observable<SimpleSubdocResponse> call() {
                SubExistRequest request = new SubExistRequest(id, path, bucket);
                return core.send(request);
            }
        }).map(new Func1<SimpleSubdocResponse, Boolean>() {
            @Override
            public Boolean call(SimpleSubdocResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return true;
                } else if (response.status() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    return false;
                }

                throw commonSubdocErrors(response.status(), id, path);
            }
        });
    }

    @Override
    public <T> Observable<DocumentFragment<T>> upsertIn(final DocumentFragment<T> fragment, final boolean createParents,
            final PersistTo persistTo, final ReplicateTo replicateTo) {
        Observable<DocumentFragment<T>> mutation = Observable.defer(new Func0<Observable<SimpleSubdocResponse>>() {
            @Override
            public Observable<SimpleSubdocResponse> call() {
                ByteBuf buf;
                try {
                    buf = subdocumentTranscoder.encodeWithMessage(fragment.fragment(),
                            "Couldn't encode subdoc fragment " + fragment.id() + "/" + fragment.path() +
                            " \"" + fragment.fragment() + "\"");
                } catch (TranscodingException e) {
                    return Observable.error(e);
                }
                SubDictUpsertRequest request = new SubDictUpsertRequest(fragment.id(), fragment.path(), buf, bucket,
                        fragment.expiry(), fragment.cas());
                request.createIntermediaryPath(createParents);
                return core.send(request);
            }
        }).map(new Func1<SimpleSubdocResponse, DocumentFragment<T>>() {
            @Override
            public DocumentFragment<T> call(SimpleSubdocResponse response) {
                //empty response for mutations
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    return DocumentFragment.create(fragment.id(), fragment.path(), fragment.expiry(),
                            fragment.fragment(), response.cas(), response.mutationToken());
                }

                switch(response.status()) {
                    case SUBDOC_PATH_INVALID:
                        throw new PathInvalidException("Path " + fragment.path() + " ends in an array index in "
                                + fragment.id() + ", expected dictionary");
                    case SUBDOC_PATH_MISMATCH:
                        throw new PathMismatchException("Path " + fragment.path() + " ends in a scalar value in "
                            + fragment.id() + ", expected dictionary");
                    default:
                        throw commonSubdocErrors(response.status(), fragment);
                }
            }
        });

        return subdocObserveMutation(mutation, persistTo, replicateTo);
    }

    @Override
    public <T> Observable<DocumentFragment<T>> insertIn(final DocumentFragment<T> fragment, final boolean createParents,
            PersistTo persistTo, ReplicateTo replicateTo) {
        Observable<DocumentFragment<T>> mutation = Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        ByteBuf buf;
                        try {
                            buf = subdocumentTranscoder.encodeWithMessage(fragment.fragment(),
                                    "Couldn't encode subdoc fragment " + fragment.id() + "/" + fragment.path() +
                                            " \"" + fragment.fragment() + "\"");
                        } catch (TranscodingException e) {
                            return Observable.error(e);
                        }
                        SubDictAddRequest request = new SubDictAddRequest(fragment.id(), fragment.path(), buf, bucket,
                                fragment.expiry(), fragment.cas());
                        request.createIntermediaryPath(createParents);
                        return core.send(request);
                    }
                }).map(new Func1<SimpleSubdocResponse, DocumentFragment<T>>() {
                    @Override
                    public DocumentFragment<T> call(SimpleSubdocResponse response) {
                        //empty response for mutations
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (response.status().isSuccess()) {
                            return DocumentFragment.create(fragment.id(), fragment.path(), fragment.expiry(),
                                    fragment.fragment(), response.cas(), response.mutationToken());
                        }

                        switch(response.status()) {
                            case SUBDOC_PATH_INVALID:
                                throw new PathInvalidException("Path " + fragment.path() + " ends in an array index in "
                                        + fragment.id() + ", expected dictionary");
                            case SUBDOC_PATH_MISMATCH:
                                throw new PathMismatchException("Path " + fragment.path() + " ends in a scalar value in "
                                        + fragment.id() + ", expected dictionary");
                            case SUBDOC_PATH_EXISTS:
                                throw new PathExistsException(fragment.id(), fragment.path());
                            default:
                                throw commonSubdocErrors(response.status(), fragment);
                        }
                    }
                });

        return subdocObserveMutation(mutation, persistTo, replicateTo);
    }

    @Override
    public <T> Observable<DocumentFragment<T>> replaceIn(final DocumentFragment<T> fragment, PersistTo persistTo,
            ReplicateTo replicateTo) {
        Observable<DocumentFragment<T>> mutation = Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        ByteBuf buf;
                        try {
                            buf = subdocumentTranscoder.encodeWithMessage(fragment.fragment(), "Couldn't encode subdoc fragment "
                                    + fragment.id() + "/" + fragment.path() + " \"" + fragment.fragment() + "\"");
                        } catch (TranscodingException e) {
                            return Observable.error(e);
                        }
                        SubReplaceRequest request = new SubReplaceRequest(fragment.id(), fragment.path(), buf, bucket,
                                fragment.expiry(), fragment.cas());
                        return core.send(request);
                    }
                }).map(new Func1<SimpleSubdocResponse, DocumentFragment<T>>() {
                    @Override
                    public DocumentFragment<T> call(SimpleSubdocResponse response) {
                        //empty response for mutations
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (response.status().isSuccess()) {
                            return DocumentFragment.create(fragment.id(), fragment.path(), fragment.expiry(),
                                    fragment.fragment(), response.cas(), response.mutationToken());
                        }

                        switch(response.status()) {
                            case SUBDOC_PATH_NOT_FOUND:
                                throw new PathNotFoundException("Path to be replaced " + fragment.path() + " not found in " + fragment.id());
                            case SUBDOC_PATH_MISMATCH:
                                throw new PathMismatchException("Path " + fragment.path() + " ends in a scalar value in "
                                        + fragment.id() + ", expected dictionary");
                            default:
                                throw commonSubdocErrors(response.status(), fragment);
                        }
                    }
                });

        return subdocObserveMutation(mutation, persistTo, replicateTo);
    }

    @Override
    public <T> Observable<DocumentFragment<T>> extendIn(final DocumentFragment<T> fragment, final ExtendDirection direction,
            final boolean createParents, PersistTo persistTo, ReplicateTo replicateTo) {
        Observable<DocumentFragment<T>> mutation = Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        ByteBuf buf;
                        try {
                            buf = subdocumentTranscoder.encodeWithMessage(fragment.fragment(), "Couldn't encode subdoc fragment "
                                    + fragment.id() + "/" + fragment.path() + " \"" + fragment.fragment() + "\"");
                        } catch (TranscodingException e) {
                            return Observable.error(e);
                        }
                        SubArrayRequest.ArrayOperation op;
                        switch (direction) {
                            case FRONT:
                                op = SubArrayRequest.ArrayOperation.PUSH_FIRST;
                                break;
                            case BACK:
                            default:
                                op = SubArrayRequest.ArrayOperation.PUSH_LAST;
                                break;
                        }

                        SubArrayRequest request = new SubArrayRequest(fragment.id(), fragment.path(), op,
                                buf, bucket, fragment.expiry(), fragment.cas());
                        request.createIntermediaryPath(createParents);
                        return core.send(request);
                    }
                }).map(new Func1<SimpleSubdocResponse, DocumentFragment<T>>() {
                    @Override
                    public DocumentFragment<T> call(SimpleSubdocResponse response) {
                        //empty response for mutations
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (response.status().isSuccess()) {
                            return DocumentFragment.create(fragment.id(), fragment.path(), fragment.expiry(),
                                    fragment.fragment(), response.cas(), response.mutationToken());
                        }

                        throw commonSubdocErrors(response.status(), fragment);
                    }
                });

        return subdocObserveMutation(mutation, persistTo, replicateTo);
    }

    @Override
    public <T> Observable<DocumentFragment<T>> arrayInsertIn(final DocumentFragment<T> fragment, PersistTo persistTo,
            ReplicateTo replicateTo) {
        Observable<DocumentFragment<T>> mutation = Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        ByteBuf buf;
                        try {
                            buf = subdocumentTranscoder.encodeWithMessage(fragment.fragment(), "Couldn't encode subdoc fragment "
                                    + fragment.id() + "/" + fragment.path() + " \"" + fragment.fragment() + "\"");
                        } catch (TranscodingException e) {
                            return Observable.error(e);
                        }
                        SubArrayRequest request = new SubArrayRequest(fragment.id(), fragment.path(),
                                SubArrayRequest.ArrayOperation.INSERT,
                                buf, bucket, fragment.expiry(), fragment.cas());
                        return core.send(request);
                    }
                }).map(new Func1<SimpleSubdocResponse, DocumentFragment<T>>() {
                    @Override
                    public DocumentFragment<T> call(SimpleSubdocResponse response) {
                        //empty response for mutations
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (response.status().isSuccess()) {
                            return DocumentFragment.create(fragment.id(), fragment.path(), fragment.expiry(),
                                    fragment.fragment(), response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
                            case SUBDOC_PATH_MISMATCH:
                                throw new PathMismatchException("The last component of path " + fragment.path()
                                        + " in " + fragment.id() + " was expected to be an array element");
                            default:
                                throw commonSubdocErrors(response.status(), fragment);
                        }
                    }
                });

        return subdocObserveMutation(mutation, persistTo, replicateTo);
    }

    @Override
    public <T> Observable<DocumentFragment<T>> addUniqueIn(final DocumentFragment<T> fragment, final boolean createParents,
            PersistTo persistTo, ReplicateTo replicateTo) {
        Observable<DocumentFragment<T>> mutation = Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        ByteBuf buf;
                        try {
                            buf = subdocumentTranscoder.encodeWithMessage(fragment.fragment(), "Couldn't encode subdoc fragment "
                                    + fragment.id() + "/" + fragment.path() + " \"" + fragment.fragment() + "\"");
                        } catch (TranscodingException e) {
                            return Observable.error(e);
                        }
                        SubArrayRequest request = new SubArrayRequest(fragment.id(), fragment.path(),
                                SubArrayRequest.ArrayOperation.ADD_UNIQUE,
                                buf, bucket, fragment.expiry(), fragment.cas());
                        request.createIntermediaryPath(createParents);
                        return core.send(request);
                    }
                }).map(new Func1<SimpleSubdocResponse, DocumentFragment<T>>() {
                    @Override
                    public DocumentFragment<T> call(SimpleSubdocResponse response) {
                        //empty response for mutations
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (response.status().isSuccess()) {
                            return DocumentFragment.create(fragment.id(), fragment.path(), fragment.expiry(),
                                    fragment.fragment(), response.cas(), response.mutationToken());
                        }

                        switch (response.status()) {
                            case SUBDOC_PATH_EXISTS:
                                throw new PathExistsException("The unique value already exist in array " + fragment.path()
                                        + " in document " + fragment.id());
                            case SUBDOC_VALUE_CANTINSERT:
                                throw new CannotInsertValueException("The unique value provided is not a JSON primitive");
                            case SUBDOC_PATH_MISMATCH:
                                throw new PathMismatchException("The array at " + fragment.path()
                                        + " contains non-primitive JSON elements in document " + fragment.id());
                            default:
                                throw commonSubdocErrors(response.status(), fragment);
                        }
                    }
                });

        return subdocObserveMutation(mutation, persistTo, replicateTo);
    }

    @Override
    public <T> Observable<DocumentFragment<T>> removeIn(final DocumentFragment<T> fragment, PersistTo persistTo,
            ReplicateTo replicateTo) {
        Observable<DocumentFragment<T>> mutation = Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        SubDeleteRequest request = new SubDeleteRequest(fragment.id(), fragment.path(), bucket,
                                fragment.expiry(), fragment.cas());
                        return core.send(request);
                    }
                }).map(new Func1<SimpleSubdocResponse, DocumentFragment<T>>() {
                    @Override
                    public DocumentFragment<T> call(SimpleSubdocResponse response) {
                        //empty response for mutations
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (response.status().isSuccess()) {
                            return DocumentFragment.create(fragment.id(), fragment.path(), fragment.expiry(), null,
                                    response.cas(), response.mutationToken());
                        }

                        throw commonSubdocErrors(response.status(), fragment);
                    }
                });

        return subdocObserveMutation(mutation, persistTo, replicateTo);
    }

    @Override
    public Observable<DocumentFragment<Long>> counterIn(final DocumentFragment<Long> fragment, final boolean createParents,
            PersistTo persistTo, ReplicateTo replicateTo) {
//        shortcircuit if delta is zero
        if (fragment.fragment() == null || fragment.fragment() == 0L) {
            return Observable.error(new ZeroDeltaException());
        }

        Observable<DocumentFragment<Long>> mutation = Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        long delta = fragment.fragment();
                        SubCounterRequest request = new SubCounterRequest(fragment.id(), fragment.path(),
                                delta, bucket, fragment.expiry(), fragment.cas());
                        request.createIntermediaryPath(createParents);
                        return core.send(request);
                    }
                }).filter(new Func1<SimpleSubdocResponse, Boolean>() {
            @Override
            public Boolean call(SimpleSubdocResponse response) {
                //empty response for mutations
                if (response.status().isSuccess()) {
                    return true;
                }
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                switch (response.status()) {
                    default:
                        throw commonSubdocErrors(response.status(), fragment);
                }
            }
        }).map(new Func1<SimpleSubdocResponse, DocumentFragment<Long>>() {
            @Override
            public DocumentFragment<Long> call(SimpleSubdocResponse response) {
                try {
                    Long newValue = Long.parseLong(response.content().toString(CharsetUtil.UTF_8));
                    return DocumentFragment.create(fragment.id(), fragment.path(), fragment.expiry(), newValue,
                            response.cas(), response.mutationToken());
                } catch (NumberFormatException e) {
                    throw new TranscodingException("Couldn't parse counter response into a long", e);
                } finally {
                    if (response.content() != null) {
                        response.content().release();
                    }
                }
            }
        });

        return subdocObserveMutation(mutation, persistTo, replicateTo);
    }

    private <T> CouchbaseException commonSubdocErrors(ResponseStatus status, DocumentFragment<T> fragment) {
        return commonSubdocErrors(status, fragment.id(), fragment.path());
    }

    private CouchbaseException commonSubdocErrors(ResponseStatus status, String id, String path) {
        switch (status) {
            case NOT_EXISTS:
                return new DocumentDoesNotExistException("Document not found for subdoc API: " + id);
            case TEMPORARY_FAILURE:
            case SERVER_BUSY:
                return  new TemporaryFailureException();
            case OUT_OF_MEMORY:
                return new CouchbaseOutOfMemoryException();
        //a bit specific for subdoc mutations
            case EXISTS:
                return new CASMismatchException("CAS provided in subdoc mutation didn't match the CAS of stored document " + id);
            case TOO_BIG:
                return new RequestTooBigException();
        //subdoc errors
            case SUBDOC_PATH_NOT_FOUND:
                return new PathNotFoundException(id, path);
            case SUBDOC_PATH_EXISTS:
                return new PathExistsException(id, path);
            case SUBDOC_DOC_NOT_JSON:
                return new DocumentNotJsonException(id);
            case SUBDOC_DOC_TOO_DEEP:
                return new DocumentTooDeepException(id);
            case SUBDOC_DELTA_RANGE:
                return new DeltaTooBigException();
            case SUBDOC_NUM_RANGE:
                return new NumberTooBigException();
            case SUBDOC_VALUE_TOO_DEEP:
                return new ValueTooDeepException(id, path);
            case SUBDOC_PATH_TOO_BIG:
                return new PathTooDeepException(path);
            //these two are a bit generic and should usually be handled upstream with a more meaningful message
            case SUBDOC_PATH_INVALID:
                return new PathInvalidException(id, path);
            case SUBDOC_PATH_MISMATCH:
                return new PathMismatchException(id, path);
            case SUBDOC_VALUE_CANTINSERT: //this shouldn't happen outside of add-unique, since we use JSON serializer
                return new CannotInsertValueException("Provided subdocument fragment is not valid JSON");
            default:
                return new CouchbaseException(status.toString());
        }
    }

    private <T> Observable<DocumentFragment<T>> subdocObserveMutation(Observable<DocumentFragment<T>> mutation,
            final PersistTo persistTo, final ReplicateTo replicateTo) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return mutation;
        }

        return mutation.flatMap(new Func1<DocumentFragment<T>, Observable<DocumentFragment<T>>>() {
            @Override
            public Observable<DocumentFragment<T>> call(final DocumentFragment<T> frag) {
                return Observe
                    .call(core, bucket, frag.id(), frag.cas(), false, frag.mutationToken(), persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
                    .map(new Func1<Boolean, DocumentFragment<T>>() {
                        @Override
                        public DocumentFragment<T> call(Boolean aBoolean) {
                            return frag;
                        }
                    })
                    .onErrorResumeNext(new Func1<Throwable, Observable<DocumentFragment<T>>>() {
                        @Override
                        public Observable<DocumentFragment<T>> call(Throwable throwable) {
                            return Observable.error(new DurabilityException(
                                "Durability requirement failed: " + throwable.getMessage(),
                                throwable));
                        }
                    });
            }
        });
    }

    @Override
    public Observable<MultiLookupResult> lookupIn(final String id, final LookupSpec... lookupSpecs) {
        if (lookupSpecs == null) {
            throw new NullPointerException("At least one LookupCommand is necessary for lookupIn");
        }
        if (lookupSpecs.length == 0) {
            throw new IllegalArgumentException("At least one LookupCommand is necessary for lookupIn");
        }

        return Observable.defer(new Func0<Observable<MultiLookupResponse>>() {
            @Override
            public Observable<MultiLookupResponse> call() {
                return core.send(new SubMultiLookupRequest(id, bucket, lookupSpecs));
            }
        }).filter(new Func1<MultiLookupResponse, Boolean>() {
            @Override
            public Boolean call(MultiLookupResponse response) {
                if (response.status().isSuccess() || response.status() == ResponseStatus.SUBDOC_MULTI_PATH_FAILURE) {
                    return true;
                }

                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                switch(response.status()) {
                    default:
                        throw commonSubdocErrors(response.status(), id, "MULTI-LOOKUP");
                }
            }
        }).flatMap(new Func1<MultiLookupResponse, Observable<MultiLookupResult>>() {
            @Override
            public Observable<MultiLookupResult> call(final MultiLookupResponse multiLookupResponse) {
                return Observable.from(multiLookupResponse.responses())
                        .map(new Func1<com.couchbase.client.core.message.kv.subdoc.multi.LookupResult, LookupResult>() {
                            @Override
                            public LookupResult call(com.couchbase.client.core.message.kv.subdoc.multi.LookupResult lookupResult) {
                                String path = lookupResult.path();
                                boolean isExist = lookupResult.operation() == Lookup.EXIST;
                                boolean success = lookupResult.status().isSuccess();

                                try {
                                    if (isExist) {
                                        return LookupResult.createExistResult(path, lookupResult.status());
                                    } else if (success) {
                                        try {
                                            //generic, so will transform dictionaries into JsonObject and arrays into JsonArray
                                            Object content = subdocumentTranscoder.decode(lookupResult.value(), Object.class);
                                            return LookupResult.createGetResult(path, lookupResult.status(), content);
                                        } catch (TranscodingException e) {
                                            LOGGER.error("Couldn't decode multi-lookup " + lookupResult.operation() + " for " + id + "/" + path, e);
                                            return LookupResult.createFatal(path, lookupResult.operation(), e);
                                        }
                                    } else {
                                        return LookupResult.createGetResult(path, lookupResult.status(), null);
                                    }
                                } finally {
                                    if (lookupResult.value() != null) {
                                        lookupResult.value().release();
                                    }
                                }
                            }
                        }).toList()
                        .map(new Func1<List<LookupResult>, MultiLookupResult>() {
                            @Override
                            public MultiLookupResult call(List<LookupResult> lookupResults) {
                                return new MultiLookupResult(id, lookupSpecs, lookupResults);
                            }
                        });
            }
        });
    }

    //TODO reintroduce mutateIn once the protocol has been stabilized
//    @Override
//    public Observable<MultiMutationResult> mutateIn(final JsonDocument doc, PersistTo persistTo, ReplicateTo replicateTo,
//            final MutationSpec... mutationSpecs) {
//        return mutateIn(doc.id(), doc.cas(), doc.expiry(), persistTo, replicateTo, mutationSpecs);
//    }
//
//    @Override
//    public Observable<MultiMutationResult> mutateIn(String docId, PersistTo persistTo, ReplicateTo replicateTo,
//            final MutationSpec... mutationSpecs) {
//        return mutateIn(docId, 0L, 0, persistTo, replicateTo, mutationSpecs);
//    }
//
//    protected Observable<MultiMutationResult> mutateIn(final String docId, final long cas, final int expiry,
//            final PersistTo persistTo, final ReplicateTo replicateTo, final MutationSpec... mutationSpecs) {
//        if (mutationSpecs == null) {
//            throw new NullPointerException("At least one MutationSpec is necessary for mutateIn");
//        }
//        if (mutationSpecs.length == 0) {
//            throw new IllegalArgumentException("At least one MutationSpec is necessary for mutateIn");
//        }
//
//        Observable<MultiMutationResult> mutations = Observable.defer(new Func0<Observable<MutationCommand>>() {
//            @Override
//            public Observable<MutationCommand> call() {
//                List<ByteBuf> bufList = new ArrayList<ByteBuf>(mutationSpecs.length);
//                final List<MutationCommand> commands = new ArrayList<MutationCommand>(mutationSpecs.length);
//
//                for (int i = 0; i < mutationSpecs.length; i++) {
//                    MutationSpec spec = mutationSpecs[i];
//                    if (spec.type() == Mutation.DELETE) {
//                        commands.add(new MutationCommand(Mutation.DELETE, spec.path()));
//                    } else {
//                        try {
//                            ByteBuf buf = subdocumentTranscoder.encodeWithMessage(spec.fragment(), "Couldn't encode MutationSpec #" +
//                                    i + " (" + spec.type() + " on " + spec.path() + ") in " + docId);
//                            bufList.add(buf);
//                            commands.add(new MutationCommand(spec.type(), spec.path(), buf, spec.createParents()));
//                        } catch (TranscodingException e) {
//                            releaseAll(bufList);
//                            return Observable.error(e);
//                        }
//                    }
//                }
//                return Observable.from(commands);
//            }
//        }).toList()
//        .flatMap(new Func1<List<MutationCommand>, Observable<MultiMutationResponse>>(){
//            @Override
//            public Observable<MultiMutationResponse> call(List<MutationCommand> mutationCommands) {
//                return core.send(new SubMultiMutationRequest(docId, bucket, expiry, cas, mutationCommands));
//            }
//        }).flatMap(new Func1<MultiMutationResponse, Observable<MultiMutationResult>>() {
//            @Override
//            public Observable<MultiMutationResult> call(MultiMutationResponse response) {
//                if (response.content() != null && response.content().refCnt() > 0) {
//                    response.content().release();
//                }
//
//                if (response.status().isSuccess()) {
//                    return Observable.just(
//                            new MultiMutationResult(docId, response.cas(), response.mutationToken()));
//                }
//
//                switch(response.status()) {
//                    case SUBDOC_MULTI_PATH_FAILURE:
//                        int index = response.firstErrorIndex();
//                        ResponseStatus errorStatus = response.firstErrorStatus();
//                        String errorPath = mutationSpecs[index].path();
//                        CouchbaseException errorException = commonSubdocErrors(errorStatus, docId, errorPath);
//
//                        return Observable.error(new MultiMutationException(index, errorStatus,
//                                Arrays.asList(mutationSpecs), errorException));
//                    default:
//                        return Observable.error(commonSubdocErrors(response.status(), docId, "MULTI-MUTATION"));
//                }
//            }
//        });
//
//        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
//            return mutations;
//        }
//
//        return mutations.flatMap(new Func1<MultiMutationResult, Observable<MultiMutationResult>>() {
//            @Override
//            public Observable<MultiMutationResult> call(final MultiMutationResult result) {
//                return Observe
//                        .call(core, bucket, result.id(), result.cas(), false, result.mutationToken(),
//                                persistTo.value(), replicateTo.value(),
//                                environment.observeIntervalDelay(), environment.retryStrategy())
//                        .map(new Func1<Boolean, MultiMutationResult>() {
//                            @Override
//                            public MultiMutationResult call(Boolean aBoolean) {
//                                return result;
//                            }
//                        })
//                        .onErrorResumeNext(new Func1<Throwable, Observable<MultiMutationResult>>() {
//                            @Override
//                            public Observable<MultiMutationResult> call(Throwable throwable) {
//                                return Observable.error(new DurabilityException(
//                                        "Durability requirement failed: " + throwable.getMessage(),
//                                        throwable));
//                            }
//                        });
//            }
//        });
//    }

    private static void releaseAll(List<ByteBuf> byteBufs) {
        for (ByteBuf byteBuf : byteBufs) {
            if (byteBuf != null && byteBuf.refCnt() > 0) {
                byteBuf.release();
            }
        }
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
