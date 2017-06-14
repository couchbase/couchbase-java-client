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
package com.couchbase.client.java.cluster;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.cluster.api.AsyncClusterApiClient;
import com.couchbase.client.java.cluster.api.ClusterApiClient;
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

    /**
     * Creates/Updates a user with its {@link UserSettings}.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     *
     * **Note:** Updating a user is an asynchronous operation on the server side, so even if the
     * response is returned there is no guarantee that the operation has finished on the server itself.
     *
     * @param domain the authentication to use, most likely {@link AuthDomain#LOCAL}
     * @param username the user name of the user that should be updated.
     * @param settings the user settings that should be applied.
     * @return true if the update was successful, false otherwise.
     */
    @InterfaceStability.Experimental
    Observable<Boolean> upsertUser(AuthDomain domain, String username, UserSettings settings);

    /**
     * Removes a user identified by user name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     *
     * **Note:** Removing a user is an asynchronous operation on the server side, so even if the
     * response is returned there is no guarantee that the operation has finished on the server itself.
     *
     * @param domain the authentication to use, most likely {@link AuthDomain#LOCAL}
     * @param username the user name of the user that should be updated.
     * @return true if the removal was successful, false otherwise.
     */
    @InterfaceStability.Experimental
    Observable<Boolean> removeUser(AuthDomain domain, String username);

    /**
     * Get all users in Couchbase Server.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param domain the authentication to use, most likely {@link AuthDomain#LOCAL}
     * @return users list of users.
     */
    @InterfaceStability.Experimental
    Observable<User> getUsers(AuthDomain domain);

    /**
     * Get user info from Couchbase Server.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.core.CouchbaseException: If the underlying resources could not be enabled properly.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @param domain the authentication to use, most likely {@link AuthDomain#LOCAL}
     * @return user info
     */
    @InterfaceStability.Experimental
    Observable<User> getUser(AuthDomain domain, String username);

    /**
     * @return an {@link Observable} emitting a single new {@link AsyncClusterApiClient} to prepare and perform
     * REST API asynchronous requests on this cluster.
     */
    @InterfaceStability.Experimental
    Observable<AsyncClusterApiClient> apiClient();
}
