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
