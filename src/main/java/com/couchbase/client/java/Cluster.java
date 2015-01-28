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
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.transcoder.Transcoder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents a Couchbase Server {@link Cluster}.
 *
 * A {@link Cluster} is able to open many {@link Bucket}s while sharing the underlying resources very
 * efficiently. In addition, the {@link ClusterManager} is available to perform cluster-wide operations.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface Cluster {

    /**
     * Opens the default bucket with an empty password with the default connect timeout.
     *
     * This method throws:
     *
     *  - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @return the opened bucket if successful.
     */
    Bucket openBucket();

    /**
     * Opens the default bucket with an empty password with a custom timeout.
     *
     * This method throws:
     *
     *  - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the opened bucket if successful.
     */
    Bucket openBucket(long timeout, TimeUnit timeUnit);

    /**
     * Opens a bucket identified by its name with an empty password and with the default connect timeout.
     *
     * This method throws:
     *
     *  - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @return the opened bucket if successful.
     */
    Bucket openBucket(String name);

    /**
     * Opens a bucket identified by its name with an empty password and with a custom timeout.
     *
     * This method throws:
     *
     *  - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the opened bucket if successful.
     */
    Bucket openBucket(String name, long timeout, TimeUnit timeUnit);

    /**
     * Opens a bucket identified by its name and password with the default connect timeout.
     *
     * This method throws:
     *
     *  - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @return the opened bucket if successful.
     */
    Bucket openBucket(String name, String password);

    /**
     * Opens a bucket identified by its name and password with a custom timeout.
     *
     * This method throws:
     *
     *  - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the opened bucket if successful.
     */
    Bucket openBucket(String name, String password, long timeout, TimeUnit timeUnit);

    /**
     * Opens a bucket identified by its name and password with custom transcoders and with the default connect timeout.
     *
     * This method throws:
     *
     *  - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @return the opened bucket if successful.
     */
    Bucket openBucket(String name, String password, List<Transcoder<? extends Document, ?>> transcoders);

    /**
     * Opens a bucket identified by its name and password with custom transcoders and with a custom timeout.
     *
     * This method throws:
     *
     *  - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the opened bucket if successful.
     */
    Bucket openBucket(String name, String password, List<Transcoder<? extends Document, ?>> transcoders,
        long timeout, TimeUnit timeUnit);


    /**
     * Provides access to the {@link ClusterManager} to perform cluster-wide operations.
     *
     * Note that the credentials provided here are different from bucket-level credentials. As a rule of thumb, the
     * "Administrator" credentials need to be passed in here or any credentials with enough permissions to perform
     * the underlying operations. **Bucket level credentials will not work.**
     *
     * @param username the username to perform cluster-wide operations.
     * @param password the password associated with the username.
     * @return the {@link ClusterManager} if successful.
     */
    ClusterManager clusterManager(String username, String password);

    /**
     * Disconnects form all open buckets and shuts down the {@link CouchbaseEnvironment} if it is the exclusive owner
     * with the default disconnect timeout.
     *
     * @return true once done and everything succeeded, false otherwise.
     */
    Boolean disconnect();

    /**
     * Disconnects form all open buckets and shuts down the {@link CouchbaseEnvironment} if it is the exclusive owner
     * with a custom timeout.
     *
     * @return true once done and everything succeeded, false otherwise.
     */
    Boolean disconnect(long timeout, TimeUnit timeUnit);

    /**
     * Returns the underlying "core-io" library through its {@link ClusterFacade}.
     *
     * Handle with care, with great power comes great responsibility. All additional checks which are normally performed
     * by this library are skipped.
     *
     * @return the underlying {@link ClusterFacade} from the "core-io" package.
     */
    ClusterFacade core();

}
