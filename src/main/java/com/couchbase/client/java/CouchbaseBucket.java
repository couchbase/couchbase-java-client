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
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.binary.*;
import com.couchbase.client.core.message.cluster.CloseBucketRequest;
import com.couchbase.client.core.message.cluster.CloseBucketResponse;
import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.core.message.config.BucketConfigRequest;
import com.couchbase.client.core.message.config.BucketConfigResponse;
import com.couchbase.client.core.message.query.GenericQueryRequest;
import com.couchbase.client.core.message.query.GenericQueryResponse;
import com.couchbase.client.core.message.view.ViewQueryRequest;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.bucket.CouchbaseBucketManager;
import com.couchbase.client.java.bucket.DefaultBucketInfo;
import com.couchbase.client.java.bucket.Observe;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.LegacyDocument;
import com.couchbase.client.java.document.LongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.query.*;
import com.couchbase.client.java.transcoder.JsonTranscoder;
import com.couchbase.client.java.transcoder.LegacyTranscoder;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.transcoder.TranscodingException;
import com.couchbase.client.java.view.*;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CouchbaseBucket implements Bucket {

  private final String bucket;
  private final String password;
  private final ClusterFacade core;
  private final Map<Class<?>, Transcoder<?, ?>> transcoders;
  private final BucketManager bucketManager;
  private static final JsonTranscoder JSON_TRANSCODER = new JsonTranscoder();

    public CouchbaseBucket(final ClusterFacade core, final String name, final String password) {
        bucket = name;
        this.password = password;
        this.core = core;

        transcoders = new HashMap<Class<?>, Transcoder<?, ?>>();
        transcoders.put(JsonDocument.class, JSON_TRANSCODER);
        transcoders.put(LegacyDocument.class, new LegacyTranscoder());
        bucketManager = new CouchbaseBucketManager(bucket, password, core);
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
        return core
            .<GetResponse>send(new GetRequest(id, bucket))
            .filter(new Func1<GetResponse, Boolean>() {
                @Override
                public Boolean call(GetResponse getResponse) {
                    return getResponse.status() == ResponseStatus.SUCCESS;
                }
            })
            .map(new Func1<GetResponse, D>() {
                @Override
                public D call(final GetResponse response) {
                    Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);
                    return (D) transcoder.decode(id, response.content(), response.cas(), 0, response.flags(), response.status());
                }
            });
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
        return core.<GetResponse>send(new GetRequest(id, bucket, true, false, lockTime))
            .filter(new Func1<GetResponse, Boolean>() {
                @Override
                public Boolean call(GetResponse getResponse) {
                    return getResponse.status() == ResponseStatus.SUCCESS;
                }
            })
            .map(new Func1<GetResponse, D>() {
                @Override
                public D call(final GetResponse response) {
                    Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);
                    return (D) transcoder.decode(id, response.content(), response.cas(), 0, response.flags(), response.status());
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
        return core.<GetResponse>send(new GetRequest(id, bucket, false, true, expiry))
            .filter(new Func1<GetResponse, Boolean>() {
                @Override
                public Boolean call(GetResponse getResponse) {
                    return getResponse.status() == ResponseStatus.SUCCESS;
                }
            })
            .map(new Func1<GetResponse, D>() {
                @Override
                public D call(final GetResponse response) {
                    Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);
                    return (D) transcoder.decode(id, response.content(), response.cas(), 0, response.flags(), response.status());
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

        Observable<GetResponse> incoming;
        if (type == ReplicaMode.ALL) {
            incoming = core
                .<GetClusterConfigResponse>send(new GetClusterConfigRequest())
                .map(new Func1<GetClusterConfigResponse, Integer>() {
                    @Override
                    public Integer call(GetClusterConfigResponse response) {
                        CouchbaseBucketConfig conf = (CouchbaseBucketConfig) response.config().bucketConfig(bucket);
                        return conf.numberOfReplicas();
                    }
                }).flatMap(new Func1<Integer, Observable<BinaryRequest>>() {
                    @Override
                    public Observable<BinaryRequest> call(Integer max) {
                        List<BinaryRequest> requests = new ArrayList<BinaryRequest>();

                        requests.add(new GetRequest(id, bucket));
                        for (int i = 0; i < max; i++) {
                            requests.add(new ReplicaGetRequest(id, bucket, (short)(i+1)));
                        }
                        return Observable.from(requests);
                    }
                }).flatMap(new Func1<BinaryRequest, Observable<GetResponse>>() {
                    @Override
                    public Observable<GetResponse> call(BinaryRequest req) {
                        return core.send(req);
                    }
                });
        } else {
            incoming = core.send(new ReplicaGetRequest(id, bucket, (short) type.ordinal()));
        }

        return incoming
            .filter(new Func1<GetResponse, Boolean>() {
                @Override
                public Boolean call(GetResponse getResponse) {
                    return getResponse.status() == ResponseStatus.SUCCESS;
                }
            })
            .map(new Func1<GetResponse, D>() {
                @Override
                public D call(final GetResponse response) {
                    Transcoder<?, Object> transcoder = (Transcoder<?, Object>) transcoders.get(target);
                    return (D) transcoder.decode(id, response.content(), response.cas(), 0, response.flags(), response.status());
                }
            });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> insert(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
        return core
            .<InsertResponse>send(new InsertRequest(document.id(), encoded.value1(), document.expiry(), encoded.value2(), bucket))
            .flatMap(new Func1<InsertResponse, Observable<? extends D>>() {
                @Override
                public Observable<? extends D> call(InsertResponse response) {
                    if (response.status() == ResponseStatus.EXISTS) {
                        return Observable.error(new DocumentAlreadyExistsException());
                    }
                    return Observable.just((D) transcoder.newDocument(document.id(), document.expiry(), document.content(), response.cas()));
                }
            });
    }

    @Override
    public <D extends Document<?>> Observable<D> insert(final D document, final PersistTo persistTo,
        final ReplicateTo replicateTo) {
        return insert(document).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, persistTo, replicateTo)
                    .map(new Func1<Boolean, D>() {
                        @Override
                        public D call(Boolean aBoolean) {
                            return doc;
                        }
                    }).onErrorResumeNext(new Func1<Throwable, Observable<? extends D>>() {
                        @Override
                        public Observable<? extends D> call(Throwable throwable) {
                            return Observable.error(new DurabilityException("Durability constraint failed.", throwable));
                        }
                    });
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> upsert(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
        return core
            .<UpsertResponse>send(new UpsertRequest(document.id(), encoded.value1(), document.expiry(), encoded.value2(), bucket))
            .flatMap(new Func1<UpsertResponse, Observable<D>>() {
                @Override
                public Observable<D> call(UpsertResponse response) {
                    if (response.status() == ResponseStatus.EXISTS) {
                        return Observable.error(new CASMismatchException());
                    }
                    return Observable.just((D) transcoder.newDocument(document.id(), document.expiry(), document.content(), response.cas()));
                }
            });
    }

    @Override
    public <D extends Document<?>> Observable<D> upsert(final D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        return upsert(document).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, persistTo, replicateTo)
                    .map(new Func1<Boolean, D>() {
                        @Override
                        public D call(Boolean aBoolean) {
                            return doc;
                        }
                    });
            }
        });
    }

    @Override
  @SuppressWarnings("unchecked")
  public <D extends Document<?>> Observable<D> replace(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
    return core.<ReplaceResponse>send(new ReplaceRequest(document.id(), encoded.value1(), document.cas(), document.expiry(), encoded.value2(), bucket))

        .flatMap(new Func1<ReplaceResponse, Observable<D>>() {
            @Override
            public Observable<D> call(ReplaceResponse response) {
                if (response.status() == ResponseStatus.NOT_EXISTS) {
                    return Observable.error(new DocumentDoesNotExistException());
                }
                if (response.status() == ResponseStatus.EXISTS) {
                    return Observable.error(new CASMismatchException());
                }
                return Observable.just((D) transcoder.newDocument(document.id(), document.expiry(), document.content(), response.cas()));
            }
        });
  }

    @Override
    public <D extends Document<?>> Observable<D> replace(final D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        return replace(document).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), false, persistTo, replicateTo)
                    .map(new Func1<Boolean, D>() {
                        @Override
                        public D call(Boolean aBoolean) {
                            return doc;
                        }
                    });
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> remove(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        RemoveRequest request = new RemoveRequest(document.id(), document.cas(),
            bucket);
        return core.<RemoveResponse>send(request).map(new Func1<RemoveResponse, D>() {
            @Override
            public D call(RemoveResponse response) {
                return (D) transcoder.newDocument(document.id(), document.expiry(), document.content(), document.cas());
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
    public <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return (Observable<D>) remove(document.id(), persistTo, replicateTo, document.getClass());
    }

    @Override
    public Observable<JsonDocument> remove(String id, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(id, persistTo, replicateTo, JsonDocument.class);
    }

    @Override
    public <D extends Document<?>> Observable<D> remove(String id, final PersistTo persistTo,
        final ReplicateTo replicateTo, Class<D> target) {
        return remove(id, target).flatMap(new Func1<D, Observable<D>>() {
            @Override
            public Observable<D> call(final D doc) {
                return Observe
                    .call(core, bucket, doc.id(), doc.cas(), true, persistTo, replicateTo)
                    .map(new Func1<Boolean, D>() {
                        @Override
                        public D call(Boolean aBoolean) {
                            return doc;
                        }
                    });
            }
        });
    }

    @Override
  public Observable<ViewResult> query(final ViewQuery query) {
    final ViewQueryRequest request = new ViewQueryRequest(query.getDesign(), query.getView(), query.isDevelopment(),
        query.toString(), bucket, password);
        return core.<ViewQueryResponse>send(request)
            .flatMap(new Func1<ViewQueryResponse, Observable<ViewResult>>() {
                @Override
                public Observable<ViewResult> call(final ViewQueryResponse response) {
                    return response.info().map(new Func1<ByteBuf, JsonObject>() {
                        @Override
                        public JsonObject call(ByteBuf byteBuf) {
                            if (byteBuf == null || byteBuf.readableBytes() == 0) {
                                return JsonObject.empty();
                            }
                            try {
                                return JSON_TRANSCODER.byteBufToJsonObject(byteBuf);
                            } catch (Exception e) {
                                throw new TranscodingException("Could not decode View Info.", e);
                            }
                        }
                    }).map(new Func1<JsonObject, ViewResult>() {
                        @Override
                        public ViewResult call(JsonObject jsonInfo) {
                            JsonObject error = null;
                            JsonObject debug = null;
                            int totalRows = 0;
                            boolean success = response.status().isSuccess();
                            if (success) {
                                debug = jsonInfo.getObject("debug_info");
                                Integer trows = jsonInfo.getInt("total_rows");
                                if (trows != null) {
                                    totalRows = trows;
                                }
                            } else {
                                error = jsonInfo;
                            }

                            Observable<ViewRow> rows = response.rows().map(new Func1<ByteBuf, ViewRow>() {
                                @Override
                                public ViewRow call(final ByteBuf byteBuf) {
                                    JsonObject doc;
                                    try {
                                        doc = JSON_TRANSCODER.byteBufToJsonObject(byteBuf);
                                    } catch (Exception e) {
                                        throw new TranscodingException("Could not decode View Info.", e);
                                    }
                                    String id = doc.getString("id");
                                    return new DefaultViewRow(CouchbaseBucket.this, id, doc.get("key"), doc.get("value"));
                                }
                            });
                            return new DefaultViewResult(rows, totalRows, success, error, debug);
                        }
                    });
                }
            });
  }

    @Override
    public Observable<QueryResult> query(final Query query) {
        return query(query.toString());
    }

    @Override
    public Observable<QueryResult> query(final String query) {
        final Transcoder<?, ?> converter = transcoders.get(JsonDocument.class);
        GenericQueryRequest request = new GenericQueryRequest(query, bucket, password);
        return core
            .<GenericQueryResponse>send(request)
            .flatMap(new Func1<GenericQueryResponse, Observable<QueryResult>>() {
                @Override
                public Observable<QueryResult> call(final GenericQueryResponse response) {
                    final Observable<QueryRow> rows = response.rows().map(new Func1<ByteBuf, QueryRow>() {
                        @Override
                        public QueryRow call(ByteBuf byteBuf) {
                            try {
                                JsonObject value = JSON_TRANSCODER.byteBufToJsonObject(byteBuf);
                                return new DefaultQueryRow(value);
                            } catch (Exception e) {
                                throw new TranscodingException("Could not decode View Info.", e);
                            }
                        }
                    });
                    final Observable<JsonObject> info = response.info().map(new Func1<ByteBuf, JsonObject>() {
                        @Override
                        public JsonObject call(ByteBuf byteBuf) {
                            try {
                                return JSON_TRANSCODER.byteBufToJsonObject(byteBuf);
                            } catch (Exception e) {
                                throw new TranscodingException("Could not decode View Info.", e);
                            }
                        }
                    });
                    if (response.status().isSuccess()) {
                        return Observable.just((QueryResult) new DefaultQueryResult(rows, info, null,
                            response.status().isSuccess()));
                    } else {
                        return response.info().map(new Func1<ByteBuf, QueryResult>() {
                            @Override
                            public QueryResult call(ByteBuf byteBuf) {
                                try {
                                    JsonObject error = JSON_TRANSCODER.byteBufToJsonObject(byteBuf);
                                    return new DefaultQueryResult(rows, info, error, response.status().isSuccess());
                                } catch (Exception e) {
                                    throw new TranscodingException("Could not decode View Info.", e);
                                }

                            }
                        });
                    }
                }
            });
    }

    @Override
    public Observable<LongDocument> counter(String id, long delta) {
        return counter(id, delta, delta);
    }

    @Override
    public Observable<LongDocument> counter(String id, long delta, long initial) {
        return counter(id, delta, initial, 0);
    }

    @Override
    public Observable<LongDocument> counter(final String id, final long delta, final long initial, final int expiry) {
        return core
            .<CounterResponse>send(new CounterRequest(id, initial, delta, expiry, bucket))
            .map(new Func1<CounterResponse, LongDocument>() {
                @Override
                public LongDocument call(CounterResponse response) {
                    return new LongDocument(id, expiry, response.value(), response.cas());
                }
            });
    }

    @Override
    public Observable<Boolean> unlock(String id, long cas) {
        return core
            .<UnlockResponse>send(new UnlockRequest(id, cas, bucket))
            .map(new Func1<UnlockResponse, Boolean>() {
                @Override
                public Boolean call(UnlockResponse response) {
                    if (response.status() == ResponseStatus.NOT_EXISTS) {
                        throw new DocumentDoesNotExistException();
                    }
                    if (response.status() == ResponseStatus.FAILURE) {
                        throw new CASMismatchException();
                    }
                    return response.status().isSuccess();
                }
            });
    }

    @Override
    public <D extends Document<?>> Observable<Boolean> unlock(D document) {
        return unlock(document.id(), document.cas());
    }

    @Override
    public Observable<Boolean> touch(String id, int expiry) {
        return core.<TouchResponse>send(new TouchRequest(id, expiry, bucket)).map(new Func1<TouchResponse, Boolean>() {
            @Override
            public Boolean call(TouchResponse touchResponse) {
                return touchResponse.status().isSuccess();
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
        Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
        return core
            .<AppendResponse>send(new AppendRequest(document.id(), document.cas(), encoded.value1(), bucket))
            .map(new Func1<AppendResponse, D>() {
                @Override
                public D call(final AppendResponse response) {
                    if (response.status() == ResponseStatus.FAILURE) {
                        throw new DocumentDoesNotExistException();
                    }

                    return (D) transcoder.newDocument(document.id(), document.expiry(), document.content(), response.cas());
                }
            });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> prepend(final D document) {
        final  Transcoder<Document<Object>, Object> transcoder = (Transcoder<Document<Object>, Object>) transcoders.get(document.getClass());
        Tuple2<ByteBuf, Integer> encoded = transcoder.encode((Document<Object>) document);
        return core
            .<PrependResponse>send(new PrependRequest(document.id(), document.cas(), encoded.value1(), bucket))
            .map(new Func1<PrependResponse, D>() {
                @Override
                public D call(final PrependResponse response) {
                    if (response.status() == ResponseStatus.FAILURE) {
                        throw new DocumentDoesNotExistException();
                    }

                    return (D) transcoder.newDocument(document.id(),  document.expiry(), document.content(), response.cas());
                }
            });
    }

    @Override
    public Observable<BucketManager> bucketManager() {
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
    public Observable<BucketInfo> info() {
        return core
            .<BucketConfigResponse>send(new BucketConfigRequest("/pools/default/buckets/", null, bucket, password))
            .map(new Func1<BucketConfigResponse, BucketInfo>() {
                @Override
                public BucketInfo call(BucketConfigResponse response) {
                    try {
                        return DefaultBucketInfo.create(JSON_TRANSCODER.stringToJsonObject(response.config()));
                    } catch (Exception ex) {
                        throw new CouchbaseException("Could not parse bucket info.", ex);
                    }
                }
            });
    }

    @Override
    public Observable<Boolean> close() {
        return core.<CloseBucketResponse>send(new CloseBucketRequest(bucket))
            .map(new Func1<CloseBucketResponse, Boolean>() {
                @Override
                public Boolean call(CloseBucketResponse response) {
                    return response.status().isSuccess();
                }
            });
    }
}
