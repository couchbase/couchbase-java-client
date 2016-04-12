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
package com.couchbase.client.java;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.cluster.AsyncClusterManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.transcoder.Transcoder;
import rx.Observable;

import java.util.List;

/**
 * Represents a Couchbase Server {@link Cluster}.
 *
 * A {@link AsyncCluster} is able to open many {@link AsyncBucket}s while sharing the underlying resources very
 * efficiently. In addition, the {@link AsyncClusterManager} is available to perform cluster-wide operations.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface AsyncCluster {

    /**
     * Opens the default bucket with an empty password.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *   - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @return the opened bucket if successful.
     */
    Observable<AsyncBucket> openBucket();

    /**
     * Opens the bucket with the given name and an empty password.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *   - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @param name the name of the bucket.
     * @return the opened bucket if successful.
     */
    Observable<AsyncBucket> openBucket(String name);

    /**
     * Opens the bucket with the given name and password.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *   - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @param name the name of the bucket.
     * @return the opened bucket if successful.
     */
    Observable<AsyncBucket> openBucket(String name, String password);

    /**
     * Opens the bucket with the given name, password and a custom list of {@link Transcoder}s.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *   - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @param name the name of the bucket.
     * @return the opened bucket if successful.
     */
    Observable<AsyncBucket> openBucket(String name, String password, List<Transcoder<? extends Document, ?>> transcoders);

    /**
     * Provides access to the {@link AsyncClusterManager} to perform cluster-wide operations.
     *
     * Note that the credentials provided here are different from bucket-level credentials. As a rule of thumb, the
     * "Administrator" credentials need to be passed in here or any credentials with enough permissions to perform
     * the underlying operations. **Bucket level credentials will not work.**
     *
     * @param username the username to perform cluster-wide operations.
     * @param password the password associated with the username.
     * @return the {@link AsyncClusterManager} if successful.
     */
    Observable<AsyncClusterManager> clusterManager(String username, String password);

    /**
     * Disconnects form all open buckets and shuts down the {@link CouchbaseEnvironment} if it is the exclusive owner.
     *
     * @return true once done and everything succeeded, false otherwise.
     */
    Observable<Boolean> disconnect();

    /**
     * Returns the underlying "core-io" library through its {@link ClusterFacade}.
     *
     * Handle with care, with great power comes great responsibility. All additional checks which are normally performed
     * by this library are skipped.
     *
     * @return the underlying {@link ClusterFacade} from the "core-io" package.
     */
    Observable<ClusterFacade> core();

}
