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
import com.couchbase.client.java.cluster.ClusterManager;
import rx.Observable;

/**
 * Represents a Couchbase Server {@link Cluster}.
 *
 * A {@link Cluster} is able to open many {@link Bucket}s while sharing the underlying resources (like sockets)
 * very efficiently. In addition, a {@link ClusterManager} is available to perform cluster-wide operations.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public interface Cluster {

    /**
     * Open the default {@link Bucket}.
     *
     * @return a {@link Observable} containing the {@link Bucket} reference once opened.
     */
    Observable<Bucket> openBucket();

    /**
     * Open the given {@link Bucket} without a password (if not set during creation).
     *
     * @param name the name of the bucket.
     * @return a {@link Observable} containing the {@link Bucket} reference once opened.
     */
    Observable<Bucket> openBucket(String name);

    /**
     * Open the given {@link Bucket} with a password (set during creation).
     *
     * @param name the name of the bucket.
     * @param password the password of the bucket, can be an empty string.
     * @return a {@link Observable} containing the {@link Bucket} reference once opened.
     */
    Observable<Bucket> openBucket(String name, String password);

    /**
     * Returns a reference to the {@link ClusterManager}.
     *
     * The {@link ClusterManager} allows to perform cluster level management operations. It requires administrative
     * credentials, which have been set during cluster configuration. Bucket level credentials are not enough to perform
     * cluster-level operations.
     *
     * @param username privileged username.
     * @param password privileged password.
     * @return a {@link Observable} containing the {@link ClusterManager}.
     */
    Observable<ClusterManager> clusterManager(String username, String password);

    /**
     * Disconnects from the {@link Cluster} and closes all open {@link Bucket}s.
     *
     * @return a {@link Observable} containing true if successful and failing the {@link Observable} otherwise.
     */
    Observable<Boolean> disconnect();

    /**
     * Returns a reference to the underlying core engine.
     *
     * Since the {@link ClusterFacade} provides direct access to low-level semantics, no sanity checks are performed as
     * with the Java SDK itself. Handle with care and only use it when absolutely needed.
     *
     * @return a {@link Observable} containing the core engine.
     */
    Observable<ClusterFacade> core();

}
