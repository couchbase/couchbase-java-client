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
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.config.FlushRequest;
import com.couchbase.client.core.message.config.FlushResponse;
import com.couchbase.client.core.message.kv.GetRequest;
import com.couchbase.client.core.message.kv.GetResponse;
import com.couchbase.client.core.message.kv.UpsertRequest;
import com.couchbase.client.core.message.kv.UpsertResponse;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.error.FlushDisabledException;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to flush a bucket properly and wait for it to be completed.
 *
 * @author Michael Nitschinger
 * @since 2.1.1
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class BucketFlusher {

    /**
     * The number of marker documents to create, defaults to the number of partitions.
     *
     * This is important to make sure all the individual vbuckets are actually flushed.
     */
    static final int FLUSH_MARKER_SIZE = 1024;

    private static final List<String> FLUSH_MARKERS = new ArrayList<String>();

    static {
        for (int i = 0; i < FLUSH_MARKER_SIZE; i++) {
            FLUSH_MARKERS.add("__flush_marker_" + i);
        }
    }

    private BucketFlusher() {
    }

    /**
     * Flush the bucket and make sure flush is complete before completing the observable.
     *
     * @param core the core reference.
     * @param bucket the bucket to flush.
     * @param password the password of the bucket.
     * @return an observable which is completed once the flush process is done.
     */
    public static Observable<Boolean> flush(final ClusterFacade core, final String bucket, final String password) {
       return createMarkerDocuments(core, bucket)
           .flatMap(new Func1<List<String>, Observable<Boolean>>() {
               @Override
               public Observable<Boolean> call(List<String> strings) {
                   return initiateFlush(core, bucket, password);
               }
           })
           .flatMap(new Func1<Boolean, Observable<Boolean>>() {
               @Override
               public Observable<Boolean> call(Boolean isDone) {
                   return isDone ? Observable.just(true) : pollMarkerDocuments(core, bucket);
               }
           });
    }

    /**
     * Helper method to create marker documents for each partition.
     *
     * @param core the core reference.
     * @param bucket the name of the bucket.
     * @return a list of created flush marker IDs once they are completely upserted.
     */
    private static Observable<List<String>> createMarkerDocuments(final ClusterFacade core, final String bucket) {
        return Observable
            .from(FLUSH_MARKERS)
            .flatMap(new Func1<String, Observable<UpsertResponse>>() {
                @Override
                public Observable<UpsertResponse> call(String id) {
                    return core.send(new UpsertRequest(id, Unpooled.copiedBuffer(id, CharsetUtil.UTF_8), bucket));
                }
            })
            .doOnNext(new Action1<UpsertResponse>() {
                @Override
                public void call(UpsertResponse response) {
                    if (response.content() != null && response.content().refCnt() > 0) {
                        response.content().release();
                    }
                }
            })
            .last()
            .map(new Func1<UpsertResponse, List<String>>() {
                @Override
                public List<String> call(UpsertResponse response) {
                    return FLUSH_MARKERS;
                }
            });
    }

    /**
     * Initiates a flush request against the server.
     *
     * The result indicates if polling needs to be done or the flush is already complete. It can also fail in case
     * flush is disabled or something else went wrong in the server response.
     *
     * @param core the core reference.
     * @param bucket the bucket to flush.
     * @param password the password of the bucket.
     * @return an observable indicating if done (true) or polling needs to happen (false).
     */
    private static Observable<Boolean> initiateFlush(final ClusterFacade core, final String bucket, final String password) {
        return core
            .<FlushResponse>send(new FlushRequest(bucket, password))
            .map(new Func1<FlushResponse, Boolean>() {
                @Override
                public Boolean call(FlushResponse flushResponse) {
                    if (flushResponse.status() == ResponseStatus.FAILURE) {
                        if (flushResponse.content().contains("disabled")) {
                            throw new FlushDisabledException("Flush is disabled for this bucket.");
                        } else {
                            throw new CouchbaseException("Flush failed because of: " + flushResponse.content());
                        }
                    }
                    return flushResponse.isDone();
                }
            });
    }

    /**
     * Helper method to poll the list of marker documents until all of them are gone.
     *
     * @param core the core reference.
     * @param bucket the name of the bucket.
     * @return an observable completing when all marker documents are gone.
     */
    private static Observable<Boolean> pollMarkerDocuments(final ClusterFacade core, final String bucket) {
        return Observable
            .from(FLUSH_MARKERS)
            .flatMap(new Func1<String, Observable<GetResponse>>() {
                @Override
                public Observable<GetResponse> call(String id) {
                    return core.send(new GetRequest(id, bucket));
                }
            })
            .reduce(0, new Func2<Integer, GetResponse, Integer>() {
                @Override
                public Integer call(Integer foundDocs, GetResponse response) {
                    if (response.content() != null && response.content().refCnt() > 0) {
                        response.content().release();
                    }
                    if (response.status() == ResponseStatus.SUCCESS) {
                        foundDocs++;
                    }
                    return foundDocs;
                }
            })
            .filter(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer foundDocs) {
                    return foundDocs == 0;
                }
            })
            .repeatWhen(new Func1<Observable<? extends Void>, Observable<?>>() {
                @Override
                public Observable<?> call(Observable<? extends Void> observable) {
                    return observable.flatMap(new Func1<Void, Observable<?>>() {
                        @Override
                        public Observable<?> call(Void aVoid) {
                            return Observable.timer(500, TimeUnit.MILLISECONDS);
                        }
                    });
                }
            })
            .take(1)
            .map(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer integer) {
                    return true;
                }
            });
    }


}
