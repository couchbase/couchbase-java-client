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
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.binary.*;
import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.core.message.query.GenericQueryRequest;
import com.couchbase.client.core.message.query.GenericQueryResponse;
import com.couchbase.client.core.message.view.ViewQueryRequest;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.bucket.CouchbaseBucketManager;
import com.couchbase.client.java.bucket.Observe;
import com.couchbase.client.java.convert.Converter;
import com.couchbase.client.java.convert.JacksonJsonConverter;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.LongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.query.*;
import com.couchbase.client.java.view.*;
import rx.Observable;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CouchbaseBucket implements Bucket {

  private final String bucket;
  private final String password;
  private final ClusterFacade core;
  private final Map<Class<?>, Converter<?, ?>> converters;
  private final BucketManager bucketManager;

    public CouchbaseBucket(final ClusterFacade core, final String name, final String password) {
        bucket = name;
        this.password = password;
        this.core = core;

        converters = new HashMap<Class<?>, Converter<?, ?>>();
        converters.put(JsonDocument.class, new JacksonJsonConverter());
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
                    Converter<?, Object> converter = (Converter<?, Object>) converters.get(target);
                    Object content = response.status() == ResponseStatus.SUCCESS ? converter.decode(response.content()) : null;
                    return (D) converter.newDocument(id, content, response.cas(), 0, response.status());
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
                    Converter<?, Object> converter = (Converter<?, Object>) converters.get(target);
                    Object content = response.status() == ResponseStatus.SUCCESS ? converter.decode(response.content()) : null;
                    return (D) converter.newDocument(id, content, response.cas(), 0, response.status());
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
                    Converter<?, Object> converter = (Converter<?, Object>) converters.get(target);
                    Object content = response.status() == ResponseStatus.SUCCESS ? converter.decode(response.content()) : null;
                    return (D) converter.newDocument(id, content, response.cas(), 0, response.status());
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
                    Converter<?, Object> converter = (Converter<?, Object>) converters.get(target);
                    Object content = response.status() == ResponseStatus.SUCCESS ? converter.decode(response.content()) : null;
                    return (D) converter.newDocument(id, content, response.cas(), 0, response.status());
                }
            });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> insert(final D document) {
        final Converter<?, Object> converter = (Converter<?, Object>) converters.get(document.getClass());
        ByteBuf content = converter.encode(document.content());
        return core
            .<InsertResponse>send(new InsertRequest(document.id(), content, document.expiry(), 0, bucket))
            .flatMap(new Func1<InsertResponse, Observable<? extends D>>() {
                @Override
                public Observable<? extends D> call(InsertResponse response) {
                    if (response.status() == ResponseStatus.EXISTS) {
                        return Observable.error(new DocumentAlreadyExistsException());
                    }
                    return Observable.just((D) converter.newDocument(document.id(), document.content(), response.cas(),
                        document.expiry(), response.status()));
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
                    }).onErrorFlatMap(new Func1<OnErrorThrowable, Observable<? extends D>>() {
                        @Override
                        public Observable<? extends D> call(OnErrorThrowable onErrorThrowable) {
                            return Observable.error(new DurabilityException("Durability constraint failed.", onErrorThrowable));
                        }
                    });
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> upsert(final D document) {
        final Converter<?, Object> converter = (Converter<?, Object>) converters.get(document.getClass());
        ByteBuf content = converter.encode(document.content());
        return core
            .<UpsertResponse>send(new UpsertRequest(document.id(), content, document.expiry(), 0, bucket))
            .map(new Func1<UpsertResponse, D>() {
                @Override
                public D call(UpsertResponse response) {
                    return (D) converter.newDocument(document.id(), document.content(), response.cas(), document.expiry(),
                        response.status());
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
    final Converter<?, Object> converter = (Converter<?, Object>) converters.get(document.getClass());
    ByteBuf content = converter.encode(document.content());
    return core.<ReplaceResponse>send(new ReplaceRequest(document.id(), content, document.cas(), document.expiry(), 0, bucket))
      .flatMap(new Func1<ReplaceResponse, Observable<D>>() {
          @Override
          public Observable<D> call(ReplaceResponse response) {
              if (response.status() == ResponseStatus.NOT_EXISTS) {
                  return Observable.error(new DocumentDoesNotExistException());
              }
              return Observable.just((D) converter.newDocument(document.id(), document.content(), response.cas(),
                  document.expiry(), response.status()));
          }
      });
  }

    @Override
    public <D extends Document<?>> Observable<D> replace(final D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
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
                    });
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D extends Document<?>> Observable<D> remove(final D document) {
        final Converter<?, Object> converter = (Converter<?, Object>) converters.get(document.getClass());
        RemoveRequest request = new RemoveRequest(document.id(), document.cas(),
            bucket);
        return core.<RemoveResponse>send(request).map(new Func1<RemoveResponse, D>() {
            @Override
            public D call(RemoveResponse response) {
                return (D) converter.newDocument(document.id(), document.content(), document.cas(), document.expiry(),
                    response.status());
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
        Converter<?, ?> converter = converters.get(target);
        return remove((D) converter.newDocument(id, null, 0, 0, null));
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

        final Converter<?, ?> converter = converters.get(JsonDocument.class);
        return core.<ViewQueryResponse>send(request)
            .flatMap(new Func1<ViewQueryResponse, Observable<ViewResult>>() {
                @Override
                public Observable<ViewResult> call(final ViewQueryResponse response) {
                    return response.info().map(new Func1<ByteBuf, JsonObject>() {
                        @Override
                        public JsonObject call(ByteBuf byteBuf) {
                            return (JsonObject) converter.decode(byteBuf);
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
                                totalRows = jsonInfo.getInt("total_rows");
                            } else {
                                error = jsonInfo;
                            }

                            Observable<ViewRow> rows = response.rows().map(new Func1<ByteBuf, ViewRow>() {
                                @Override
                                public ViewRow call(final ByteBuf byteBuf) {
                                    JsonObject doc = (JsonObject) converter.decode(byteBuf);
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
        final Converter<?, ?> converter = converters.get(JsonDocument.class);
        GenericQueryRequest request = new GenericQueryRequest(query, bucket, password);
        return core
            .<GenericQueryResponse>send(request)
            .flatMap(new Func1<GenericQueryResponse, Observable<QueryResult>>() {
                @Override
                public Observable<QueryResult> call(final GenericQueryResponse response) {
                    final Observable<QueryRow> rows = response.rows().map(new Func1<ByteBuf, QueryRow>() {
                        @Override
                        public QueryRow call(ByteBuf byteBuf) {
                            JsonObject value = (JsonObject) converter.decode(byteBuf);
                            return new DefaultQueryRow(value);
                        }
                    });
                    final Observable<JsonObject> info = response.info().map(new Func1<ByteBuf, JsonObject>() {
                        @Override
                        public JsonObject call(ByteBuf byteBuf) {
                            JsonObject value = (JsonObject) converter.decode(byteBuf);
                            return value;
                        }
                    });
                    if (response.status().isSuccess()) {
                        return Observable.just((QueryResult) new DefaultQueryResult(rows, info, null,
                            response.status().isSuccess()));
                    } else {
                        return response.info().map(new Func1<ByteBuf, QueryResult>() {
                            @Override
                            public QueryResult call(ByteBuf byteBuf) {
                                JsonObject error = (JsonObject) converter.decode(byteBuf);
                                return new DefaultQueryResult(rows, info, error, response.status().isSuccess());
                            }
                        });
                    }
                }
            });
    }

    @Override
    public Observable<LongDocument> counter(final String id, final long delta, final long initial, final int expiry) {
        return core
            .<CounterResponse>send(new CounterRequest(id, initial, delta, expiry, bucket))
            .map(new Func1<CounterResponse, LongDocument>() {
                @Override
                public LongDocument call(CounterResponse response) {
                    return new LongDocument(id, response.value(), response.cas(), expiry, response.status());
                }
            });
    }

    @Override
    public Observable<Boolean> unlock(String id, long cas) {
        return core.<UnlockResponse>send(new UnlockRequest(id, cas, bucket)).map(new Func1<UnlockResponse, Boolean>() {
            @Override
            public Boolean call(UnlockResponse unlockResponse) {
                return unlockResponse.status().isSuccess();
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
    public Observable<BucketManager> bucketManager() {
        return Observable.just(bucketManager);
    }
}
