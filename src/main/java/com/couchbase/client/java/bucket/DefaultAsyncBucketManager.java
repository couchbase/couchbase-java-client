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

import com.couchbase.client.core.ClusterFacade;
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
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DesignDocumentAlreadyExistsException;
import com.couchbase.client.java.error.DesignDocumentException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.view.DesignDocument;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of a {@link AsyncBucketManager}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DefaultAsyncBucketManager implements AsyncBucketManager {

    private final ClusterFacade core;
    private final String bucket;
    private final String password;

    DefaultAsyncBucketManager(String bucket, String password, ClusterFacade core) {
        this.bucket = bucket;
        this.password = password;
        this.core = core;
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
}
