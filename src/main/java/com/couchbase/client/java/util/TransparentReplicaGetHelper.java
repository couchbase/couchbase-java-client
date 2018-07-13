/**
 * Copyright (c) 2017 Couchbase, Inc.
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
package com.couchbase.client.java.util;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import rx.Observable;
import rx.Single;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * The {@link TransparentReplicaGetHelper} abstracts common logic to first grab the
 * active document and if that fails tries all available replicas and returns the first
 * result.
 *
 * NOTE: Using these APIs is eventually consistent meaning that you cannot rely on
 * a previous successful mutation to a document be reflected in the result. Use this
 * API only if you favor availability over consistency on the read path.
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class TransparentReplicaGetHelper {

    /**
     * Asynchronously fetch the document from the primary and if that operations fails try
     * all the replicas and return the first document that comes back from them (with custom
     * primary and replica timeout values).
     *
     * @param id the document ID to fetch.
     * @param bucket the bucket to use when fetching the doc.
     * @param primaryTimeout the timeout to use in MS when fetching the primary.
     * @param replicaTimeout the timeout to use in MS when subsequently fetching the replicas and primary.
     * @return an {@link Single} with either 0 or 1 {@link JsonDocument}.
     */
    @InterfaceStability.Experimental
    @InterfaceAudience.Public
    public Single<JsonDocument> getFirstPrimaryOrReplica(final String id,
        final Bucket bucket, final long primaryTimeout, final long replicaTimeout) {
        return getFirstPrimaryOrReplica(id, JsonDocument.class, bucket, primaryTimeout, replicaTimeout);
    }

    /**
     * Asynchronously fetch the document from the primary and if that operations fails try
     * all the replicas and return the first document that comes back from them (with a custom
     * timeout value applied to both primary and replica).
     *
     * @param id the document ID to fetch.
     * @param bucket the bucket to use when fetching the doc.
     * @param timeout the timeout to use for both primary and replica fetches (separately)
     * @return an {@link Single} with either 0 or 1 {@link JsonDocument}.
     */
    @InterfaceStability.Experimental
    @InterfaceAudience.Public
    public Single<JsonDocument> getFirstPrimaryOrReplica(final String id,
        final Bucket bucket, final long timeout) {
        return getFirstPrimaryOrReplica(id, bucket, timeout, timeout);
    }

    /**
     * Asynchronously fetch the document from the primary and if that operations fails try
     * all the replicas and return the first document that comes back from them (using the
     * environments KV timeout for both primary and replica).
     *
     *
     * @param id the document ID to fetch.
     * @param bucket the bucket to use when fetching the doc.
     * @return an {@link Single} with either 0 or 1 {@link JsonDocument}.
     */
    @InterfaceStability.Experimental
    @InterfaceAudience.Public
    public Single<JsonDocument> getFirstPrimaryOrReplica(final String id, final Bucket bucket) {
        return getFirstPrimaryOrReplica(id, bucket, bucket.environment().kvTimeout());
    }

    /**
     * Asynchronously fetch the document from the primary and if that operations fails try
     * all the replicas and return the first document that comes back from them (using the
     * environments KV timeout for both primary and replica).
     *
     * @param id the document ID to fetch.
     * @param target the custom document type to use.
     * @param bucket the bucket to use when fetching the doc.
     * @return @return a {@link Single} with either 0 or 1 {@link Document}.
     */
    @InterfaceStability.Experimental
    @InterfaceAudience.Public
    public static <D extends Document<?>> Single<D> getFirstPrimaryOrReplica(final String id,
        final Class<D> target, final Bucket bucket) {
        return getFirstPrimaryOrReplica(id, target, bucket, bucket.environment().kvTimeout());
    }

    /**
     * Asynchronously fetch the document from the primary and if that operations fails try
     * all the replicas and return the first document that comes back from them (with a custom
     * timeout value applied to both primary and replica).
     *
     * @param id the document ID to fetch.
     * @param target the custom document type to use.
     * @param bucket the bucket to use when fetching the doc.
     * @param timeout the timeout to use for both primary and replica fetches (separately)
     * @return @return a {@link Single} with either 0 or 1 {@link Document}.
     */
    @InterfaceStability.Experimental
    @InterfaceAudience.Public
    public static <D extends Document<?>> Single<D> getFirstPrimaryOrReplica(final String id,
        final Class<D> target, final Bucket bucket, final long timeout) {
        return getFirstPrimaryOrReplica(id, target, bucket, timeout, timeout);
    }

    /**
     * Asynchronously fetch the document from the primary and if that operations fails try
     * all the replicas and return the first document that comes back from them.
     *
     * @param id the document ID to fetch.
     * @param target the custom document type to use.
     * @param bucket the bucket to use when fetching the doc.
     * @param primaryTimeout the timeout to use in MS when fetching the primary.
     * @param replicaTimeout the timeout to use in MS when subsequently fetching the replicas and primary.
     * @return @return a {@link Single} with either 0 or 1 {@link Document}.
     */
    @InterfaceStability.Experimental
    @InterfaceAudience.Public
    public static <D extends Document<?>> Single<D> getFirstPrimaryOrReplica(final String id,
        final Class<D> target, final Bucket bucket, final long primaryTimeout, final long replicaTimeout) {
        if (primaryTimeout <= 0) {
            throw new IllegalArgumentException("Primary timeout must be greater than 0ms");
        }
        if (replicaTimeout <= 0) {
            throw new IllegalArgumentException("Replica timeout must be greater than 0ms");
        }

        Observable<D> fallback = bucket
            .async()
            .getFromReplica(id, ReplicaMode.ALL, target)
            .timeout(replicaTimeout, TimeUnit.MILLISECONDS)
            .firstOrDefault(null)
            .filter(new Func1<D, Boolean>() {
                @Override
                public Boolean call(D d) {
                    return d != null;
                }
            });

        return bucket
            .async()
            .get(id, target)
            .timeout(primaryTimeout, TimeUnit.MILLISECONDS)
            .onErrorResumeNext(fallback)
            .toSingle();
    }
}
