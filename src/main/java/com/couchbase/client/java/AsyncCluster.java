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
