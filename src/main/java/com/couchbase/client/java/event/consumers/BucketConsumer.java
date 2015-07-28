/**
 * Copyright (c) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.event.consumers;

import com.couchbase.client.core.event.CouchbaseEvent;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.utils.Events;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.RawJsonDocument;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Consumes {@link CouchbaseEvent}s and logs them into a bucket as JSON.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public class BucketConsumer extends Subscriber<CouchbaseEvent> {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(BucketConsumer.class);

    private final Bucket bucket;
    private final StoreType storeType;

    private BucketConsumer(Bucket bucket, StoreType storeType) {
        this.bucket = bucket;
        this.storeType = storeType;
    }

    public static BucketConsumer create(Bucket bucket) {
        return create(bucket, StoreType.UPSERT);
    }

    public static BucketConsumer create(Bucket bucket, StoreType storeType) {
        return new BucketConsumer(bucket, storeType);
    }

    @Override
    public void onCompleted() {
        LOGGER.trace("Event stream completed in bucket consumer.");
    }

    @Override
    public void onError(Throwable ex) {
        LOGGER.warn("Received error in bucket consumer.", ex);
    }

    @Override
    public void onNext(CouchbaseEvent event) {
        RawJsonDocument doc = RawJsonDocument.create(
            generateKey(event),
            Events.toJson(event, false)
        );

        Observable<RawJsonDocument> stored;
        if (storeType == StoreType.INSERT) {
            stored = bucket.async().insert(doc);
        } else if (storeType == StoreType.UPSERT) {
            stored = bucket.async().upsert(doc);
        } else {
            throw new UnsupportedOperationException("Store type " + storeType + " is not supported");
        }

        stored
            .onErrorResumeNext(new Func1<Throwable, Observable<? extends RawJsonDocument>>() {
                @Override
                public Observable<? extends RawJsonDocument> call(Throwable ex) {
                    LOGGER.warn("Received error while storing document in bucket consumer.", ex);
                    return Observable.empty();
                }
            })
            .subscribe();
    }

    /**
     * Default method to generate the key for the given event.
     *
     * @param event the event to store.
     * @return the generated key, not null.
     */
    protected String generateKey(CouchbaseEvent event) {
        return System.nanoTime() + "-" + event.getClass().getSimpleName();
    }

    /**
     * How the event should be stored.
     */
    public enum StoreType {

        /**
         * Try to upsert the document.
         */
        UPSERT,

        /**
         * Try to insert the document.
         */
        INSERT
    }

}
