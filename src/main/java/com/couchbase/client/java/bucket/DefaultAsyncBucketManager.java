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
package com.couchbase.client.java.bucket;

import static com.couchbase.client.java.query.Select.select;
import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.s;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.util.retry.RetryBuilder.anyOf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.config.BucketConfigRequest;
import com.couchbase.client.core.message.config.BucketConfigResponse;
import com.couchbase.client.core.message.config.GetDesignDocumentsRequest;
import com.couchbase.client.core.message.config.GetDesignDocumentsResponse;
import com.couchbase.client.core.message.view.GetDesignDocumentRequest;
import com.couchbase.client.core.message.view.GetDesignDocumentResponse;
import com.couchbase.client.core.message.view.RemoveDesignDocumentRequest;
import com.couchbase.client.core.message.view.RemoveDesignDocumentResponse;
import com.couchbase.client.core.message.view.UpsertDesignDocumentRequest;
import com.couchbase.client.core.message.view.UpsertDesignDocumentResponse;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CannotRetryException;
import com.couchbase.client.java.error.DesignDocumentAlreadyExistsException;
import com.couchbase.client.java.error.DesignDocumentException;
import com.couchbase.client.java.error.IndexAlreadyExistsException;
import com.couchbase.client.java.error.IndexDoesNotExistException;
import com.couchbase.client.java.error.IndexesNotReadyException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.query.core.N1qlQueryExecutor;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.Sort;
import com.couchbase.client.java.query.dsl.path.index.IndexType;
import com.couchbase.client.java.query.dsl.path.index.UsingWithPath;
import com.couchbase.client.java.query.util.IndexInfo;
import com.couchbase.client.java.view.DesignDocument;
import rx.Notification;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Default implementation of a {@link AsyncBucketManager}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultAsyncBucketManager implements AsyncBucketManager {

    /** the name of the logger dedicated to index watching **/
    public static final String INDEX_WATCH_LOG_NAME = "indexWatch";

    //index watching related constants
    private static final CouchbaseLogger INDEX_WATCH_LOG = CouchbaseLoggerFactory.getInstance(INDEX_WATCH_LOG_NAME);
    //big enough as to only really consider the timeout, but without risk of overflowing
    private static final int INDEX_WATCH_MAX_ATTEMPTS = Integer.MAX_VALUE - 5;
    private static final Delay INDEX_WATCH_DELAY = Delay.linear(TimeUnit.MILLISECONDS, 1000, 50, 500);

    private final ClusterFacade core;
    private final String bucket;
    private final String password;
    private final N1qlQueryExecutor queryExecutor;

    DefaultAsyncBucketManager(String bucket, String password, ClusterFacade core) {
        this.bucket = bucket;
        this.password = password;
        this.core = core;
        this.queryExecutor = new N1qlQueryExecutor(core, bucket, password);
    }

    public static DefaultAsyncBucketManager create(String bucket, String password, ClusterFacade core) {
        return new DefaultAsyncBucketManager(bucket, password, core);
    }

    @Override
    public Observable<BucketInfo> info() {
        return Observable.defer(new Func0<Observable<BucketConfigResponse>>() {
            @Override
            public Observable<BucketConfigResponse> call() {
                return core.send(new BucketConfigRequest("/pools/default/buckets/", null, bucket, password));
            }
        }).map(new Func1<BucketConfigResponse, BucketInfo>() {
            @Override
            public BucketInfo call(BucketConfigResponse response) {
                try {
                    return DefaultBucketInfo.create(
                        CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(response.config())
                    );
                } catch (Exception ex) {
                    throw new TranscodingException("Could not decode bucket info.", ex);
                }
            }
        });
    }


    @Override
    public Observable<Boolean> flush() {
        return BucketFlusher.flush(core, bucket, password);
    }

    @Override
    public Observable<DesignDocument> getDesignDocuments() {
        return getDesignDocuments(false);
    }

    @Override
    public Observable<DesignDocument> getDesignDocuments(final boolean development) {
        return Observable.defer(new Func0<Observable<GetDesignDocumentsResponse>>() {
            @Override
            public Observable<GetDesignDocumentsResponse> call() {
                return core.send(new GetDesignDocumentsRequest(bucket, password));
            }
        }).flatMap(new Func1<GetDesignDocumentsResponse, Observable<DesignDocument>>() {
            @Override
            public Observable<DesignDocument> call(GetDesignDocumentsResponse response) {
                JsonObject converted;
                try {
                    converted = CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(response.content());
                } catch (Exception e) {
                    throw new TranscodingException("Could not decode design document.", e);
                }
                JsonArray rows = converted.getArray("rows");
                List<DesignDocument> docs = new ArrayList<DesignDocument>();
                for (Object doc : rows) {
                    JsonObject docObj = ((JsonObject) doc).getObject("doc");
                    String id = docObj.getObject("meta").getString("id");
                    String[] idSplit = id.split("/");
                    String fullName = idSplit[1];
                    boolean isDev = fullName.startsWith("dev_");
                    if (isDev != development) {
                        continue;
                    }
                    String name = fullName.replace("dev_", "");
                    docs.add(DesignDocument.from(name, docObj.getObject("json")));
                }
                return Observable.from(docs);
            }
        });
    }

    @Override
    public Observable<DesignDocument> getDesignDocument(String name) {
        return getDesignDocument(name, false);
    }

    @Override
    public Observable<DesignDocument> getDesignDocument(final String name, final boolean development) {
        return Observable.defer(new Func0<Observable<GetDesignDocumentResponse>>() {
            @Override
            public Observable<GetDesignDocumentResponse> call() {
                return core.send(new GetDesignDocumentRequest(name, development, bucket, password));
            }
        }).filter(new Func1<GetDesignDocumentResponse, Boolean>() {
            @Override
            public Boolean call(GetDesignDocumentResponse response) {
                boolean success = response.status().isSuccess();
                if (!success) {
                    if (response.content() != null && response.content().refCnt() > 0) {
                        response.content().release();
                    }
                }
                return success;
            }
        })
            .map(new Func1<GetDesignDocumentResponse, DesignDocument>() {
                @Override
                public DesignDocument call(GetDesignDocumentResponse response) {
                    JsonObject converted;
                    try {
                        converted = CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.stringToJsonObject(
                            response.content().toString(CharsetUtil.UTF_8));
                    } catch (Exception e) {
                        throw new TranscodingException("Could not decode design document.", e);
                    } finally {
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }
                    }
                    return DesignDocument.from(response.name(), converted);
                }
            });
    }

    @Override
    public Observable<DesignDocument> insertDesignDocument(final DesignDocument designDocument) {
        return insertDesignDocument(designDocument, false);
    }

    @Override
    public Observable<DesignDocument> insertDesignDocument(final DesignDocument designDocument, final boolean development) {
        return getDesignDocument(designDocument.name(), development)
            .isEmpty()
            .flatMap(new Func1<Boolean, Observable<DesignDocument>>() {
                @Override
                public Observable<DesignDocument> call(Boolean doesNotExist) {
                    if (doesNotExist) {
                        return upsertDesignDocument(designDocument, development);
                    } else {
                        return Observable.error(new DesignDocumentAlreadyExistsException());
                    }
                }
            });
    }

    @Override
    public Observable<DesignDocument> upsertDesignDocument(DesignDocument designDocument) {
        return upsertDesignDocument(designDocument, false);
    }

    @Override
    public Observable<DesignDocument> upsertDesignDocument(final DesignDocument designDocument, final boolean development) {
        String body;
        try {
            body = CouchbaseAsyncBucket.JSON_OBJECT_TRANSCODER.jsonObjectToString(designDocument.toJsonObject());
        } catch (Exception e) {
            throw new TranscodingException("Could not encode design document: ", e);
        }

        final String b = body;
        return Observable.defer(new Func0<Observable<UpsertDesignDocumentResponse>>() {
            @Override
            public Observable<UpsertDesignDocumentResponse> call() {
                return core.send(new UpsertDesignDocumentRequest(designDocument.name(), b, development, bucket, password));
            }
        }).map(new Func1<UpsertDesignDocumentResponse, DesignDocument>() {
            @Override
            public DesignDocument call(UpsertDesignDocumentResponse response) {
                try {
                    if (!response.status().isSuccess()) {
                        String msg = response.content().toString(CharsetUtil.UTF_8);
                        throw new DesignDocumentException("Could not store DesignDocument: " + msg);
                    }
                } finally {
                    if (response.content() != null && response.content().refCnt() > 0) {
                        response.content().release();
                    }
                }
                return designDocument;
            }
        });
    }

    @Override
    public Observable<Boolean> removeDesignDocument(String name) {
        return removeDesignDocument(name, false);
    }

    @Override
    public Observable<Boolean> removeDesignDocument(final String name, final boolean development) {
        return Observable.defer(new Func0<Observable<RemoveDesignDocumentResponse>>() {
            @Override
            public Observable<RemoveDesignDocumentResponse> call() {
                return core.send(new RemoveDesignDocumentRequest(name, development, bucket, password));
            }
        }).map(new Func1<RemoveDesignDocumentResponse, Boolean>() {
            @Override
            public Boolean call(RemoveDesignDocumentResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }
                return response.status().isSuccess();
            }
        });
    }

    @Override
    public Observable<DesignDocument> publishDesignDocument(String name) {
        return publishDesignDocument(name, false);
    }

    @Override
    public Observable<DesignDocument> publishDesignDocument(final String name, final boolean overwrite) {
        return getDesignDocument(name, false)
            .isEmpty()
            .flatMap(new Func1<Boolean, Observable<DesignDocument>>() {
                @Override
                public Observable<DesignDocument> call(Boolean doesNotExist) {
                    if (!doesNotExist && !overwrite) {
                        return Observable.error(new DesignDocumentAlreadyExistsException("Document exists in " +
                            "production and not overwriting."));
                    }
                    return getDesignDocument(name, true);
                }
            })
            .flatMap(new Func1<DesignDocument, Observable<DesignDocument>>() {
                @Override
                public Observable<DesignDocument> call(DesignDocument designDocument) {
                    return upsertDesignDocument(designDocument);
                }
            });
    }

    /*==== INDEX MANAGEMENT ====*/

    private static <T> Func1<List<JsonObject>, Observable<T>> errorsToThrowable(final String messagePrefix) {
        return new Func1<List<JsonObject>, Observable<T>>() {
            @Override
            public Observable<T> call(List<JsonObject> errors) {
                return Observable.<T>error(new CouchbaseException(messagePrefix + errors));
            }
        };
    }

    private static Func1<AsyncN1qlQueryRow, IndexInfo> ROW_VALUE_TO_INDEXINFO =
        new Func1<AsyncN1qlQueryRow, IndexInfo>() {
            @Override
            public IndexInfo call(AsyncN1qlQueryRow asyncN1qlQueryRow) {
                return new IndexInfo(asyncN1qlQueryRow.value());
            }
        };

    @Override
    public Observable<IndexInfo> listIndexes() {
        Statement listIndexes = select("idx.*").from(x("system:indexes").as("idx")).where(x("keyspace_id").eq(s(bucket)))
                .orderBy(Sort.desc("is_primary"), Sort.asc("name"));

        final Func1<List<JsonObject>, Observable<AsyncN1qlQueryRow>> errorHandler = errorsToThrowable(
                "Error while listing indexes: ");

        return queryExecutor.execute(
                N1qlQuery.simple(listIndexes, N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS)))
                            .flatMap(new Func1<AsyncN1qlQueryResult, Observable<AsyncN1qlQueryRow>>() {
                                @Override
                                public Observable<AsyncN1qlQueryRow> call(final AsyncN1qlQueryResult aqr) {
                                    return aqr.finalSuccess()
                                            .flatMap(new Func1<Boolean, Observable<AsyncN1qlQueryRow>>() {
                                                @Override
                                                public Observable<AsyncN1qlQueryRow> call(Boolean success) {
                                                    if (success) {
                                                        return aqr.rows();
                                                    } else {
                                                        return aqr.errors().toList().flatMap(errorHandler);
                                                    }
                                                }
                                            });
                                }
                            }).map(ROW_VALUE_TO_INDEXINFO);
    }

    @Override
    public Observable<Boolean> createPrimaryIndex(final boolean ignoreIfExist, boolean defer) {
        Statement createIndex;
        UsingWithPath usingWithPath = Index.createPrimaryIndex().on(bucket);
        if (defer) {
            createIndex = usingWithPath.withDefer();
        } else {
            createIndex = usingWithPath;
        }

        return queryExecutor.execute(N1qlQuery.simple(createIndex))
            .compose(checkIndexCreation(ignoreIfExist, "Error creating primary index"));
    }

    private static Expression expressionOrIdentifier(Object o) {
        if (o instanceof Expression) {
            return (Expression) o;
        } else if (o instanceof String) {
            return i((String) o);
        } else {
            throw new IllegalArgumentException("Fields for index must be either an Expression or a String identifier");
        }
    }

    @Override
    public Observable<Boolean> createIndex(final String indexName, final boolean ignoreIfExist, boolean defer, Object... fields) {
        if (fields == null || fields.length < 1) {
            throw new IllegalArgumentException("At least one field is required for secondary index");
        }

        Expression firstExpression = expressionOrIdentifier(fields[0]);
        Expression[] otherExpressions = new Expression[fields.length - 1];
        for (int i = 1; i < fields.length; i++) {
            otherExpressions[i - 1] = expressionOrIdentifier(fields[i]);
        }

        Statement createIndex;
        UsingWithPath usingWithPath = Index.createIndex(indexName).on(bucket, firstExpression, otherExpressions);
        if (defer) {
            createIndex = usingWithPath.withDefer();
        } else {
            createIndex = usingWithPath;
        }

        return queryExecutor.execute(N1qlQuery.simple(createIndex))
                .compose(checkIndexCreation(ignoreIfExist, "Error creating secondary index " + indexName));
    }


    @Override
    public Observable<Boolean> dropPrimaryIndex(final boolean ignoreIfNotExist) {
        return drop(ignoreIfNotExist, Index.dropPrimaryIndex(bucket).using(IndexType.GSI), "Error dropping primary index");
    }

    @Override
    public Observable<Boolean> dropIndex(String name, boolean ignoreIfNotExist) {
        return drop(ignoreIfNotExist, Index.dropIndex(bucket, name).using(IndexType.GSI), "Error dropping index \"" + name + "\"");
    }

    private Observable<Boolean> drop(final boolean ignoreIfNotExist, Statement dropIndex, final String errorPrefix) {
        return queryExecutor.execute(N1qlQuery.simple(dropIndex))
                .flatMap(new Func1<AsyncN1qlQueryResult, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(final AsyncN1qlQueryResult aqr) {
                        return aqr.finalSuccess()
                                .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> call(Boolean success) {
                                        if (success) {
                                            return Observable.just(true);
                                        } else {
                                            return aqr.errors().toList()
                                                .flatMap(new Func1<List<JsonObject>, Observable<Boolean>>() {
                                                    @Override
                                                    public Observable<Boolean> call(List<JsonObject> errors) {
                                                        if (errors.size() == 1 && errors.get(0).getString("msg").contains("not found")) {
                                                            if (ignoreIfNotExist) {
                                                                return Observable.just(false);
                                                            } else {
                                                                return Observable.error(new IndexDoesNotExistException(errorPrefix));
                                                            }
                                                        } else {
                                                            return Observable.error(new CouchbaseException(errorPrefix + ": " + errors));
                                                        }
                                                    }
                                                });
                                        }
                                    }
                                });
                    }
                });
    }


    @Override
    public Observable<List<String>> buildDeferredIndexes() {

        final Func1<List<JsonObject>, Observable<List<String>>> errorHandler = errorsToThrowable(
                "Error while triggering index build: ");

        return listIndexes()
                .filter(new Func1<IndexInfo, Boolean>() {
                    @Override
                    public Boolean call(IndexInfo indexInfo) {
                        //since 4.5, pending is split into deferred then building... (see MB-14679)
                        //here we want to list the indexes that are currently deferred, build them and return that list
                        return indexInfo.state().equals("pending") || indexInfo.state().equals("deferred");
                    }
                })
                .map(new Func1<IndexInfo, String>() {
                    @Override
                    public String call(IndexInfo indexInfo) {
                        return indexInfo.name();
                    }
                })
                .toList()
                .flatMap(new Func1<List<String>, Observable<List<String>>>() {
                    @Override
                    public Observable<List<String>> call(final List<String> pendingIndexes) {
                        if (pendingIndexes.isEmpty()) {
                            return Observable.just(pendingIndexes);
                        }
                        Statement buildStatement = Index.buildIndex().on(bucket)
                                .indexes(pendingIndexes)
                                .using(IndexType.GSI);

                        return queryExecutor.execute(N1qlQuery.simple(buildStatement))
                                .flatMap(new Func1<AsyncN1qlQueryResult, Observable<List<String>>>() {
                                    @Override
                                    public Observable<List<String>> call(final AsyncN1qlQueryResult aqr) {
                                        return aqr.finalSuccess()
                                                .flatMap(new Func1<Boolean, Observable<List<String>>>() {
                                                    @Override
                                                    public Observable<List<String>> call(
                                                            Boolean success) {
                                                        if (success) {
                                                            return Observable.just(pendingIndexes);
                                                        } else {
                                                            return aqr.errors().toList().flatMap(errorHandler);
                                                        }
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    @Override
    public Observable<IndexInfo> watchIndexes(List<String> watchList, boolean watchPrimary, final long watchTimeout,
            final TimeUnit watchTimeUnit) {
        final Set<String> watchSet = new HashSet<String>(watchList);
        if (watchPrimary) {
            watchSet.add(Index.PRIMARY_NAME);
        }

        return listIndexes()
                .flatMap(new Func1<IndexInfo, Observable<IndexInfo>>() {
                    @Override
                    public Observable<IndexInfo> call(IndexInfo i) {
                        if (!watchSet.contains(i.name())) {
                            return Observable.empty();
                        } else if (!"online".equals(i.state()))
                            return Observable.error(new IndexesNotReadyException("Index not ready: " + i.name()));
                        else {
                            return Observable.just(i);
                        }
                    }
                })
                .doOnEach(new Action1<Notification<? super IndexInfo>>() {
                    @Override
                    public void call(Notification<? super IndexInfo> notification) {
                        if (INDEX_WATCH_LOG.isDebugEnabled()) {
                            if (notification.isOnNext()) {
                                IndexInfo info = (IndexInfo) notification.getValue();
                                String indexShortInfo = info.name() + "(" + info.state() + ")";
                                INDEX_WATCH_LOG.debug("Index ready: " + indexShortInfo);
                            } else if (notification.isOnError()) {
                                Throwable e = notification.getThrowable();
                                if (e instanceof IndexesNotReadyException) {
                                    INDEX_WATCH_LOG.debug("Will retry: " + e.getMessage());
                                }
                            }
                        }
                    }
                })
                .retryWhen(anyOf(IndexesNotReadyException.class)
                        .delay(INDEX_WATCH_DELAY)
                        .max(INDEX_WATCH_MAX_ATTEMPTS)
                        .build())
                .compose(safeAbort(watchTimeout, watchTimeUnit, null));
    }

    /**
     * A transformer (to be used with {@link Observable#compose(Observable.Transformer)}) that puts a timeout on an
     * {@link IndexInfo} Observable and will consider {@link IndexesNotReadyException} inside a
     * {@link CannotRetryException} and {@link TimeoutException} to be non-failing cases (resulting in an empty
     * observable), whereas all other errors are propagated as is.
     *
     * @param watchTimeout the timeout to set.
     * @param watchTimeUnit the time unit for the timeout.
     * @param indexName null for a global timeout, or the name of the single index being watched.
     */
    private static Observable.Transformer<IndexInfo, IndexInfo> safeAbort(final long watchTimeout, final TimeUnit watchTimeUnit,
            final String indexName) {
        return new Observable.Transformer<IndexInfo, IndexInfo>() {
            @Override
            public Observable<IndexInfo> call(Observable<IndexInfo> source) {
                return source.timeout(watchTimeout, watchTimeUnit)
                .onErrorResumeNext(new Func1<Throwable, rx.Observable<IndexInfo>>() {
                    @Override
                    public rx.Observable<IndexInfo> call(Throwable t) {
                        if (t instanceof TimeoutException) {
                            if (indexName == null) {
                                INDEX_WATCH_LOG.debug("Watched indexes were not all online after the given {} {}", watchTimeout, watchTimeUnit);
                            } else {
                                INDEX_WATCH_LOG.debug("Index {} was not online after the given {} {}", indexName, watchTimeout, watchTimeUnit);
                            }
                            return Observable.empty();
                        }
                        //should not happen with the INDEX_WATCH_MAX_ATTEMPTS set close to Integer.MAX_VALUE, but in case it is later tuned down...
                        if (t instanceof CannotRetryException && t.getCause() instanceof IndexesNotReadyException) {
                            INDEX_WATCH_LOG.debug("{} after {} attempts", INDEX_WATCH_MAX_ATTEMPTS, t.getCause().getMessage());
                            return Observable.empty();
                        }
                        return rx.Observable.error(t);
                    }
                });
            }
        };
    }

    /**
     * A transformer (to be used with {@link Observable#compose(Observable.Transformer)}) that takes a N1QL query result
     * and inspects the status and errors.
     *
     * If the query succeeded, emits TRUE. If there is an error but it is only that the index exists, and ignoreIfExist
     * is true, emits FALSE. Otherwise the error is propagated as a CouchbaseException.
     */
    private static Observable.Transformer<AsyncN1qlQueryResult, Boolean> checkIndexCreation(final boolean ignoreIfExist,
            final String prefixMsg) {
        return new Observable.Transformer<AsyncN1qlQueryResult, Boolean>() {
            @Override
            public Observable<Boolean> call(Observable<AsyncN1qlQueryResult> sourceObservable) {
                return sourceObservable
                        .flatMap(new Func1<AsyncN1qlQueryResult, Observable<Boolean>>() {
                            @Override
                            public Observable<Boolean> call(final AsyncN1qlQueryResult aqr) {
                                return aqr.finalSuccess()
                                        .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                                            @Override
                                            public Observable<Boolean> call(Boolean success) {
                                                if (success) {
                                                    return Observable.just(true);
                                                } else {
                                                    return aqr.errors()
                                                    .toList()
                                                    .flatMap(new Func1<List<JsonObject>, Observable<Boolean>>() {
                                                        @Override
                                                        public Observable<Boolean> call(List<JsonObject> errors) {
                                                            if (errors.size() == 1 && errors.get(0).getString("msg").contains("already exist")) {
                                                                if (ignoreIfExist) {
                                                                    return Observable.just(false);
                                                                } else {
                                                                    return Observable.error(new IndexAlreadyExistsException(prefixMsg));
                                                                }
                                                            } else {
                                                                return Observable.error(new CouchbaseException(prefixMsg + ": " + errors));
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        });
                            }
                        });
            }
        };
    }
}
