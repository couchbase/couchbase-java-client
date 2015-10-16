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
import com.couchbase.client.core.CouchbaseCore;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.config.ConfigurationException;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.cluster.DisconnectRequest;
import com.couchbase.client.core.message.cluster.DisconnectResponse;
import com.couchbase.client.core.message.cluster.OpenBucketRequest;
import com.couchbase.client.core.message.cluster.OpenBucketResponse;
import com.couchbase.client.core.message.cluster.SeedNodesRequest;
import com.couchbase.client.java.cluster.AsyncClusterManager;
import com.couchbase.client.java.cluster.DefaultAsyncClusterManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.BucketDoesNotExistException;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.util.Bootstrap;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main asynchronous entry point to a Couchbase Cluster.
 *
 * The {@link CouchbaseAsyncCluster} object is the main entry point when connecting to a remote
 * Couchbase Server cluster. It will either create a bundled stateful environment or accept
 * one passed in, in case the application needs to connect to more clusters at the same time.
 *
 * It provides cluster level management facilities through the {@link AsyncClusterManager} class,
 * but mainly provides facilities to open {@link AsyncBucket}s where the actual CRUD and query
 * operations are performed against.
 *
 * The simplest way to initialize a {@link CouchbaseAsyncCluster} is by using the {@link #create()}
 * factory method. This is only recommended during development, since it will connect to a Cluster
 * residing on `127.0.0.1`.
 *
 * ```java
 * AsyncCluster cluster = CouchbaseAsyncCluster.create();
 * ```
 *
 * In production, it is recommended that at least two or three hosts are passed in, so in case one
 * fails the SDK is able to bootstrap through alternative options.
 *
 * ```java
 * AsyncCluster cluster = CouchbaseAsyncCluster.create(
 *   "192.168.56.101", "192.168.56.102", "192.168.56.103"
 * );
 * ```
 *
 * Please make sure that these hosts are part of the same cluster, otherwise non-deterministic
 * connecting behaviour will arise (the SDK may connect to the wrong cluster).
 *
 * If you need to customize {@link CouchbaseEnvironment} options or connect to multiple clusters,
 * it is recommended to explicitly create one and then reuse it. Keep in mind that the cluster will
 * not shut down the environment if it didn't create it, so this is up to the caller.
 *
 * ```java
 * CouchbaseEnvironment environment = DefaultCouchbaseEnvironment.builder()
 * .kvTimeout(3000) // change the default kv timeout
 * .build();
 *
 * AsyncCluster cluster = CouchbaseAsyncCluster.create(environment, "192.168.56.101",
 *   "192.168.56.102");
 * Observable&lt;Bucket&gt; bucket = cluster.openBucket("travel-sample");
 *
 * // Perform your work here...
 *
 * cluster.disconnect();
 * environment.shutdown().toBlocking().single();
 * ```
 *
 * @since 2.0.0
 * @author Michael Nitschinger
 * @author Simon Baslé
 */
public class CouchbaseAsyncCluster implements AsyncCluster {

    private static final CouchbaseLogger LOGGER =
        CouchbaseLoggerFactory.getInstance(CouchbaseAsyncCluster.class);

    /**
     * The default bucket used when {@link #openBucket()} is called.
     *
     * Defaults to "default".
     */
    public static final String DEFAULT_BUCKET = "default";

    /**
     * The default hostname used to bootstrap then {@link #create()} is used.
     *
     * Defaults to "127.0.0.1".
     */
    public static final String DEFAULT_HOST = "127.0.0.1";

    private final ClusterFacade core;
    private final CouchbaseEnvironment environment;
    private final ConnectionString connectionString;
    private final Map<String, AsyncBucket> bucketCache;
    private final boolean sharedEnvironment;

    /**
     * Creates a new {@link CouchbaseAsyncCluster} reference against the {@link #DEFAULT_HOST}.
     *
     * **Note:** It is recommended to use this method only during development, since it does not
     * allow you to pass in hostnames when connecting to a remote cluster. Please use
     * {@link #create(String...)} or similar instead.
     *
     * The {@link CouchbaseEnvironment} will be automatically created and its lifecycle managed.
     *
     * @return a new {@link CouchbaseAsyncCluster} reference.
     */
    public static CouchbaseAsyncCluster create() {
        return create(DEFAULT_HOST);
    }

    /**
     * Creates a new {@link CouchbaseAsyncCluster} reference against the {@link #DEFAULT_HOST}.
     *
     * **Note:** It is recommended to use this method only during development, since it does not
     * allow you to pass in hostnames when connecting to a remote cluster. Please use
     * {@link #create(String...)} or similar instead.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @return a new {@link CouchbaseAsyncCluster} reference.
     */
    public static CouchbaseAsyncCluster create(final CouchbaseEnvironment environment) {
        return create(environment, DEFAULT_HOST);
    }

    /**
     * Creates a new {@link CouchbaseAsyncCluster} reference against the nodes passed in.
     *
     * The {@link CouchbaseEnvironment} will be automatically created and its lifecycle managed.
     *
     * @param nodes the list of nodes to use when connecting to the cluster reference.
     * @return a new {@link CouchbaseAsyncCluster} reference.
     */
    public static CouchbaseAsyncCluster create(final String... nodes) {
        return create(Arrays.asList(nodes));
    }

    /**
     * Creates a new {@link CouchbaseAsyncCluster} reference against the nodes passed in.
     *
     * The {@link CouchbaseEnvironment} will be automatically created and its lifecycle managed.
     *
     * @param nodes the list of nodes to use when connecting to the cluster reference.
     * @return a new {@link CouchbaseAsyncCluster} reference.
     */
    public static CouchbaseAsyncCluster create(final List<String> nodes) {
        return new CouchbaseAsyncCluster(
            DefaultCouchbaseEnvironment.create(),
            ConnectionString.fromHostnames(nodes),
            false
        );
    }

    /**
     * Creates a new {@link CouchbaseAsyncCluster} reference against the nodes passed in.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @param nodes the list of nodes to use when connecting to the cluster reference.
     * @return a new {@link CouchbaseAsyncCluster} reference.
     */
    public static CouchbaseAsyncCluster create(final CouchbaseEnvironment environment,
        final String... nodes) {
        return create(environment, Arrays.asList(nodes));
    }

    /**
     * Creates a new {@link CouchbaseAsyncCluster} reference against the nodes passed in.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @param nodes the list of nodes to use when connecting to the cluster reference.
     * @return a new {@link CouchbaseAsyncCluster} reference.
     */
    public static CouchbaseAsyncCluster create(final CouchbaseEnvironment environment,
        final List<String> nodes) {
        return new CouchbaseAsyncCluster(environment, ConnectionString.fromHostnames(nodes), true);
    }

    /**
     * Creates a new {@link CouchbaseAsyncCluster} reference using the connection string.
     *
     * The {@link CouchbaseEnvironment} will be automatically created and its lifecycle managed.
     *
     * @param connectionString the connection string to identify the remote cluster.
     * @return a new {@link CouchbaseAsyncCluster} reference.
     */
    public static CouchbaseAsyncCluster fromConnectionString(final String connectionString) {
        return new CouchbaseAsyncCluster(
            DefaultCouchbaseEnvironment.create(),
            ConnectionString.create(connectionString),
            false
        );
    }

    /**
     * Creates a new {@link CouchbaseAsyncCluster} reference using the connection string.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @param connectionString the connection string to identify the remote cluster.
     * @return a new {@link CouchbaseAsyncCluster} reference.
     */
    public static CouchbaseAsyncCluster fromConnectionString(final CouchbaseEnvironment environment,
        final String connectionString) {
        return new CouchbaseAsyncCluster(
            environment,
            ConnectionString.create(connectionString),
            true
        );
    }

    /**
     * Package private constructor to create the {@link CouchbaseAsyncCluster}.
     *
     * This method should not be called directly, but rather through the many factory methods
     * available on this class.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @param connectionString the connection string to identify the remote cluster.
     * @param sharedEnvironment if the environment is managed by this class or not.
     */
    CouchbaseAsyncCluster(final CouchbaseEnvironment environment,
        final ConnectionString connectionString, final boolean sharedEnvironment) {
        this.sharedEnvironment = sharedEnvironment;
        core = new CouchbaseCore(environment);
        SeedNodesRequest request = new SeedNodesRequest(
            assembleSeedNodes(connectionString, environment)
        );
        core.send(request).toBlocking().single();
        this.environment = environment;
        this.connectionString = connectionString;
        this.bucketCache = new ConcurrentHashMap<String, AsyncBucket>();
    }

    /**
     * Helper method to assemble list of seed nodes depending on the given input.
     *
     * If DNS SRV is enabled, see
     * {@link #seedNodesViaDnsSrv(ConnectionString, CouchbaseEnvironment, List)} for more
     * details.
     *
     * @param connectionString the connection string to check.
     * @param environment the environment for context.
     * @return a list of seed nodes ready to send.
     */
    private static List<String> assembleSeedNodes(ConnectionString connectionString,
        CouchbaseEnvironment environment) {
        List<String> seedNodes = new ArrayList<String>();

        if (environment.dnsSrvEnabled()) {
            seedNodesViaDnsSrv(connectionString, environment, seedNodes);
        } else {
            for (InetSocketAddress node : connectionString.hosts()) {
                seedNodes.add(node.getHostName());
            }
        }

        if (seedNodes.isEmpty()) {
            seedNodes.add(DEFAULT_HOST);
        }

        return seedNodes;
    }

    /**
     * Helper method to assemble the list of seed nodes via DNS SRV.
     *
     * If DNS SRV is enabled on the environment and exactly one hostname is passed in (not an IP
     * address), the code performs a DNS SRV lookup, but falls back to the A record if nothing
     * suitable is found. Since the user is expected to enable it manually, a warning will be
     * issued if so.
     *
     * @param connectionString the connection string to check.
     * @param environment the environment for context.
     * @param seedNodes the assembled seed nodes.
     */
    private static void seedNodesViaDnsSrv(ConnectionString connectionString,
       CouchbaseEnvironment environment, List<String> seedNodes) {
        if (connectionString.hosts().size() == 1) {
            InetSocketAddress lookupNode = connectionString.hosts().get(0);
            LOGGER.debug(
                "Attempting to load DNS SRV records from {}.",
                connectionString.hosts().get(0)
            );

            try {
                List<String> foundNodes = Bootstrap.fromDnsSrv(lookupNode.getHostName(), false,
                    environment.sslEnabled());
                if (foundNodes.isEmpty()) {
                    throw new IllegalStateException("DNS SRV list is empty.");
                }
                seedNodes.addAll(foundNodes);
                LOGGER.info("Loaded seed nodes from DNS SRV {}.", foundNodes);
            } catch (Exception ex) {
                LOGGER.warn("DNS SRV lookup failed, proceeding with normal bootstrap.", ex);
                seedNodes.add(lookupNode.getHostName());
            }
        } else {
            LOGGER.info("DNS SRV enabled, but less or more than one seed node given. "
                + "Proceeding with normal bootstrap.");
            for (InetSocketAddress node : connectionString.hosts()) {
                seedNodes.add(node.getHostName());
            }
        }
    }

    @Override
    public Observable<AsyncBucket> openBucket() {
        return openBucket(DEFAULT_BUCKET);
    }

    @Override
    public Observable<AsyncBucket> openBucket(final String name) {
        return openBucket(name, null);
    }

    @Override
    public Observable<AsyncBucket> openBucket(final String name, final String password) {
        return openBucket(name, password, null);
    }

    @Override
    public Observable<AsyncBucket> openBucket(final String name, final String password,
        final List<Transcoder<? extends Document, ?>> transcoders) {
        if (name == null || name.isEmpty()) {
            return Observable.error(
                new IllegalArgumentException("Bucket name is not allowed to be null or empty.")
            );
        }

        AsyncBucket cachedBucket = getCachedBucket(name);
        if (cachedBucket != null) {
            return Observable.just(cachedBucket);
        }

        final String pass = password == null ? "" : password;
        final List<Transcoder<? extends Document, ?>> trans = transcoders == null
            ? new ArrayList<Transcoder<? extends Document, ?>>() : transcoders;

        return Observable.defer(new Func0<Observable<OpenBucketResponse>>() {
                @Override
                public Observable<OpenBucketResponse> call() {
                    return core.send(new OpenBucketRequest(name, pass));
                }
            })
            .map(new Func1<CouchbaseResponse, AsyncBucket>() {
                @Override
                public AsyncBucket call(CouchbaseResponse response) {
                    if (response.status() != ResponseStatus.SUCCESS) {
                        throw new CouchbaseException("Could not open bucket.");
                    }

                    AsyncBucket bucket = new CouchbaseAsyncBucket(core, environment, name, pass,
                        trans);
                    bucketCache.put(name, bucket);
                    return bucket;
                }
            })
            .onErrorResumeNext(new OpenBucketErrorHandler(name));
    }

    /**
     * Helper method to get a bucket instead of opening it if it is cached already.
     *
     * @param name the name of the bucket
     * @return the cached bucket if found, null if not.
     */
    private AsyncBucket getCachedBucket(final String name) {
        AsyncBucket cachedBucket = bucketCache.get(name);

        if(cachedBucket != null) {
            if (cachedBucket.isClosed()) {
                LOGGER.debug(
                    "Not returning cached async bucket \"{}\", because it is closed.",
                    name
                );
                bucketCache.remove(name);
            } else {
                LOGGER.debug("Returning still open, cached async bucket \"{}\"", name);
                return cachedBucket;
            }
        }

        return null;
    }

    @Override
    public Observable<Boolean> disconnect() {
        return core
            .<DisconnectResponse>send(new DisconnectRequest())
            .flatMap(new Func1<DisconnectResponse, Observable<Boolean>>() {
                @Override
                public Observable<Boolean> call(DisconnectResponse disconnectResponse) {
                    return sharedEnvironment ? Observable.just(true) : environment.shutdown();
                }
            })
            .doOnNext(new Action1<Boolean>() {
                @Override
                public void call(Boolean aBoolean) {
                    bucketCache.clear();
                }
            });
    }

    @Override
    public Observable<AsyncClusterManager> clusterManager(final String username,
        final String password) {
        return Observable.just(
            (AsyncClusterManager) DefaultAsyncClusterManager.create(
                username, password, connectionString, environment, core
            )
        );
    }

    @Override
    public Observable<ClusterFacade> core() {
        return Observable.just(core);
    }

    /**
     * The error handling logic if the open bucket process failed for some reason.
     *
     * @author Michael Nitschinger
     * @since 2.2.2
     */
    static class OpenBucketErrorHandler implements Func1<Throwable, Observable<AsyncBucket>> {

        private final String name;

        public OpenBucketErrorHandler(final String name) {
            this.name = name;
        }

        @Override
        public Observable<AsyncBucket> call(Throwable throwable) {
            if (throwable instanceof ConfigurationException) {
                if (throwable.getCause() instanceof IllegalStateException
                    && throwable.getCause().getMessage().contains("NOT_EXISTS")) {
                    return Observable.error(new BucketDoesNotExistException("Bucket \""
                        + name + "\" does not exist."));
                } else if (throwable.getCause() instanceof IllegalStateException
                    && throwable.getCause().getMessage().contains("Unauthorized")) {
                    return Observable.error(
                        new InvalidPasswordException("Passwords for bucket \"" + name
                            + "\" do not match.")
                    );
                } else {
                    return Observable.error(throwable);
                }
            } else if (throwable instanceof CouchbaseException) {
                return Observable.error(throwable);
            } else {
                return Observable.error(new CouchbaseException(throwable));
            }
        }
    }
}
