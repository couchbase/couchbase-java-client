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

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.internal.DiagnosticsReport;
import com.couchbase.client.java.auth.Authenticator;
import com.couchbase.client.java.auth.ClassicAuthenticator;
import com.couchbase.client.java.auth.CredentialContext;
import com.couchbase.client.java.cluster.AsyncClusterManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.AuthenticatorException;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.transcoder.Transcoder;
import rx.Observable;

import java.util.List;
import java.util.concurrent.TimeoutException;

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
     * Opens the bucket with the given name using the password from the {@link Authenticator} that was last
     * {@link #authenticate(Authenticator) set}
     *
     * If no credential context can be found for the bucket when using {@link ClassicAuthenticator} , the old behavior of defaulting to an empty
     * password is used.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *  - {@link AuthenticatorException}: If more than one credentials was returned by the Authenticator for this bucket.
     *
     * @param name the name of the bucket.
     * @return the opened bucket if successful.
     */
    Observable<AsyncBucket> openBucket(String name);

    /**
     * Opens the bucket with the given name using the password from the {@link Authenticator} that was last
     * {@link #authenticate(Authenticator) set}
     *
     * If no credential context can be found for the bucket when using {@link ClassicAuthenticator} , the old behavior of defaulting to an empty
     * password is used.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - com.couchbase.client.core.CouchbaseException: If the bucket could not be opened (see logs and nested stack
     *    trace for more details why it failed).
     *  - com.couchbase.client.core.BackpressureException: If the incoming request rate is too high to be processed.
     *  - {@link AuthenticatorException}: If more than one credentials was returned by the Authenticator for this bucket.
     *
     * @param name the name of the bucket.
     * @return the opened bucket if successful.
     */
    Observable<AsyncBucket> openBucket(String name, List<Transcoder<? extends Document, ?>> transcoders);

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
     * Asynchronously perform a N1QL query that can span multiple buckets. The query will use any credential set
     * through this cluster's {@link #authenticate(Authenticator) Authenticator}.
     *
     * In order to use that method, at least one {@link AsyncBucket} must currently be opened. Note that if you
     * are only performing queries spanning a single bucket, you should prefer opening that {@link Bucket} and
     * use the query API at the bucket level.
     *
     * The Observable can fail in the following notable conditions:
     *
     * - {@link UnsupportedOperationException}: no bucket is currently opened.
     * - {@link IllegalStateException}: no {@link Authenticator} is set or no credentials are available in it for cluster
     *   level querying.
     * - {@link TimeoutException}: the operation takes longer than the specified timeout.
     * - {@link BackpressureException}: the producer outpaces the SDK.
     * - {@link RequestCancelledException}: the operation had to be cancelled while on the wire or the retry strategy
     *   cancelled it instead of retrying.
     *
     * @param query the {@link N1qlQuery} to execute.
     * @return an observable emitting at most a single {@link AsyncN1qlQueryResult query result}.
     */
    @InterfaceStability.Uncommitted
    Observable<AsyncN1qlQueryResult> query(N1qlQuery query);

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
     * Provides access to the {@link AsyncClusterManager} to perform cluster-wide operations, using the
     * credentials set through the configured {@link #authenticate(Authenticator) Authenticator}, for the
     * {@link CredentialContext#CLUSTER_MANAGEMENT} context.
     *
     * The Observable can error under the following notable condition:
     *
     *  - {@link AuthenticatorException}: if no {@link Authenticator} is set or it doesn't contains a cluster
     *  management credential.
     *
     * @return the {@link AsyncClusterManager} if successful.
     */
    Observable<AsyncClusterManager> clusterManager();

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

    /**
     * Sets the {@link Authenticator} to use when credentials are needed for an operation
     * but no explicit credentials are provided.
     *
     * Note that setting a new Authenticator will not be propagated to any {@link Bucket} that
     * has been opened with the previous Authenticator, as the instance is passed to the Bucket
     * for its own use.
     *
     * @param auth the new {@link Authenticator} to use.
     * @return this AsyncCluster instance for chaining.
     */
    AsyncCluster authenticate(Authenticator auth);

    /**
     * Shortcut method to directly authenticate with a username and a password.
     *
     * @param username the username to authenticate
     * @param password the password for the username
     * @return this Cluster instance for chaining.
     */
    AsyncCluster authenticate(String username, String password);

    /**
     * Provides a simple health check which allows insight into the current state of
     * services and endpoints.
     *
     * @return health services in the form of {@link DiagnosticsReport}.
     */
    @InterfaceStability.Experimental
    @InterfaceAudience.Public
    Observable<DiagnosticsReport> diagnostics();

    /**
     * Provides a simple health check which allows insight into the current state of
     * services and endpoints.
     *
     * @return health services in the form of {@link DiagnosticsReport}.
     */
    @InterfaceStability.Experimental
    @InterfaceAudience.Public
    Observable<DiagnosticsReport> diagnostics(String reportId);
}
