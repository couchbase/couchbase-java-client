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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides management capabilities for a Couchbase Server {@link Cluster}.
 *
 * The underlying asynchronous capabilities can be leveraged through the {@link #async()} method.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface ClusterManager {

    /**
     * Accesses the underlying {@link AsyncClusterManager} to perform asynchronous operations on the cluster.
     *
     * @return the underlying  {@link AsyncClusterManager}.
     */
    AsyncClusterManager async();

    /**
     * Provides information about the cluster with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @return cluster information wrapped into a {@link ClusterInfo} object.
     */
    ClusterInfo info();

    /**
     * Provides information about the cluster with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return cluster information wrapped into a {@link ClusterInfo} object.
     */
    ClusterInfo info(long timeout, TimeUnit timeUnit);

    /**
     * Returns a list of {@link BucketSettings} for all available {@link Bucket}s with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @return a potentially empty list with {@link BucketSettings}.
     */
    List<BucketSettings> getBuckets();

    /**
     * Returns a list of {@link BucketSettings} for all available {@link Bucket}s with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return a potentially empty list with {@link BucketSettings}.
     */
    List<BucketSettings> getBuckets(long timeout, TimeUnit timeUnit);

    /**
     * Returns the {@link BucketSettings} for the {@link Bucket} identified by name with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param name the name of the bucket.
     * @return the {@link BucketSettings} if found or null.
     */
    BucketSettings getBucket(String name);

    /**
     * Returns the {@link BucketSettings} for the {@link Bucket} identified by name with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param name the name of the bucket.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the {@link BucketSettings} if found or null.
     */
    BucketSettings getBucket(String name, long timeout, TimeUnit timeUnit);

    /**
     * Checks if the cluster has a {@link Bucket} identified by the given name with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param name the name of the bucket.
     * @return true if it was found, false otherwise.
     */
    Boolean hasBucket(String name);

    /**
     * Checks if the cluster has a {@link Bucket} identified by the given name with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param name the name of the bucket.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return true if it was found, false otherwise.
     */
    Boolean hasBucket(String name, long timeout, TimeUnit timeUnit);

    /**
     * Inserts a {@link Bucket} with its {@link BucketSettings} if it does not already exist with the default
     * management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
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
    BucketSettings insertBucket(BucketSettings settings);

    /**
     * Inserts a {@link Bucket} with its {@link BucketSettings} if it does not already exist with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
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
    BucketSettings insertBucket(BucketSettings settings, long timeout, TimeUnit timeUnit);

    /**
     * Updates a {@link Bucket} with its {@link BucketSettings} if it does already exist with the default management
     * timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
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
    BucketSettings updateBucket(BucketSettings settings);

    /**
     * Updates a {@link Bucket} with its {@link BucketSettings} if it does already exist with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     * - com.couchbase.client.java.error.BucketDoesNotExistException: If the bucket does not exist.
     *
     * **Note:** Updating a Bucket is an asynchronous operation on the server side, so even if the
     * response is returned there is no guarantee that the operation has finished on the server itself.
     *
     * @param settings the bucket settings that should be applied.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the updated bucket settings if succeeded.
     */
    BucketSettings updateBucket(BucketSettings settings, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Bucket} identified by its name with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * **Note:** Removing a Bucket is an asynchronous operation on the server side, so even if the
     * response is returned there is no guarantee that the operation has finished on the server itself.
     *
     * @param name the name of the bucket.
     * @return true if the removal was successful, false otherwise.
     */
    Boolean removeBucket(String name);

    /**
     * Removes a {@link Bucket} identified by its name with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * **Note:** Removing a Bucket is an asynchronous operation on the server side, so even if the
     * response is returned there is no guarantee that the operation has finished on the server itself.
     *
     * @param name the name of the bucket.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return true if the removal was successful, false otherwise.
     */
    Boolean removeBucket(String name, long timeout, TimeUnit timeUnit);
}
