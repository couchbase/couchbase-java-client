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
package com.couchbase.client.java.cluster;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import rx.Observable;

/**
 * Provides management capabilities for a Couchbase Server {@link Cluster}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface AsyncClusterManager {

    /**
     * Provides information about the cluster.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @return cluster information wrapped into a {@link ClusterInfo} object.
     */
    Observable<ClusterInfo> info();

    /**
     * Returns {@link BucketSettings} for all available {@link Bucket}s.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @return zero to N {@link BucketSettings}.
     */
    Observable<BucketSettings> getBuckets();

    /**
     * Returns the {@link BucketSettings} for the {@link Bucket} identified by name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param name the name of the bucket.
     * @return the {@link BucketSettings} if found or an empty observable if not found.
     */
    Observable<BucketSettings> getBucket(String name);

    /**
     * Checks if the cluster has a {@link Bucket} identified by the given name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param name the name of the bucket.
     * @return true if it was found, false otherwise.
     */
    Observable<Boolean> hasBucket(String name);

    /**
     * Inserts a {@link Bucket} with its {@link BucketSettings} if it does not already exist.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     * - com.couchbase.client.java.error.BucketAlreadyExistsException: If the bucket already exists.
     *
     * **Note:** Inserting a Bucket is an asynchronous operation on the server side, so even if the
     * response is returned there is no guarantee that the operation has finished on the server itself.
     *
     * @param settings the bucket settings that should be applied.
     * @return the stored bucket settings if succeeded.
     */
    Observable<BucketSettings> insertBucket(BucketSettings settings);

    /**
     * Updates a {@link Bucket} with its {@link BucketSettings} if it does already exist.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     * - com.couchbase.client.java.error.BucketDoesNotExistException: If the bucket does not exist.
     *
     * **Note:** Updating a Bucket is an asynchronous operation on the server side, so even if the
     * response is returned there is no guarantee that the operation has finished on the server itself.
     *
     * @param settings the bucket settings that should be applied.
     * @return the updated bucket settings if succeeded.
     */
    Observable<BucketSettings> updateBucket(BucketSettings settings);

    /**
     * Removes a {@link Bucket} identified by its name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * **Note:** Removing a Bucket is an asynchronous operation on the server side, so even if the
     * response is returned there is no guarantee that the operation has finished on the server itself.
     *
     * @param name the name of the bucket.
     * @return true if the removal was successful, false otherwise.
     */
    Observable<Boolean> removeBucket(String name);
}
