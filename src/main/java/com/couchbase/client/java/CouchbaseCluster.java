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
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.java.cluster.AsyncClusterManager;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultClusterManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.util.Blocking;
import rx.functions.Action1;
import rx.functions.Func1;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Main synchronous entry point to a Couchbase Cluster.
 *
 * The {@link CouchbaseCluster} object is the main entry point when connecting to a remote
 * Couchbase Server cluster. It will either create a bundled stateful environment or accept
 * one passed in, in case the application needs to connect to more clusters at the same time.
 *
 * It provides cluster level management facilities through the {@link ClusterManager} class,
 * but mainly provides facilities to open {@link Bucket}s where the actual CRUD and query
 * operations are performed against.
 *
 * The simplest way to initialize a {@link CouchbaseCluster} is by using the {@link #create()}
 * factory method. This is only recommended during development, since it will connect to a Cluster
 * residing on `127.0.0.1`.
 *
 * ```java
 * Cluster cluster = CouchbaseCluster.create();
 * ```
 *
 * In production, it is recommended that at least two or three hosts are passed in, so in case one
 * fails the SDK is able to bootstrap through alternative options.
 *
 * ```java
 * Cluster cluster = CouchbaseCluster.create(
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
 * Cluster cluster = CouchbaseCluster.create(environment, "192.168.56.101",
 *   "192.168.56.102");
 * Bucket bucket = cluster.openBucket("travel-sample");
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
public class CouchbaseCluster implements Cluster {

    private static final CouchbaseLogger LOGGER =
        CouchbaseLoggerFactory.getInstance(CouchbaseCluster.class);

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

    private final CouchbaseAsyncCluster couchbaseAsyncCluster;
    private final CouchbaseEnvironment environment;
    private final ConnectionString connectionString;

    private final Map<String, Bucket> bucketCache;

    /**
     * Creates a new {@link CouchbaseCluster} reference against the
     * {@link CouchbaseAsyncCluster#DEFAULT_HOST}.
     *
     * **Note:** It is recommended to use this method only during development, since it does not
     * allow you to pass in hostnames when connecting to a remote cluster. Please use
     * {@link #create(String...)} or similar instead.
     *
     * The {@link CouchbaseEnvironment} will be automatically created and its lifecycle managed.
     *
     * @return a new {@link CouchbaseCluster} reference.
     */
    public static CouchbaseCluster create() {
        return create(CouchbaseAsyncCluster.DEFAULT_HOST);
    }

    /**
     * Creates a new {@link CouchbaseCluster} reference against the
     * {@link CouchbaseAsyncCluster#DEFAULT_HOST}.
     *
     * **Note:** It is recommended to use this method only during development, since it does not
     * allow you to pass in hostnames when connecting to a remote cluster. Please use
     * {@link #create(String...)} or similar instead.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @return a new {@link CouchbaseCluster} reference.
     */
    public static CouchbaseCluster create(final CouchbaseEnvironment environment) {
        return create(environment, CouchbaseAsyncCluster.DEFAULT_HOST);
    }

    /**
     * Creates a new {@link CouchbaseCluster} reference against the nodes passed in.
     *
     * The {@link CouchbaseEnvironment} will be automatically created and its lifecycle managed.
     *
     * @param nodes the list of nodes to use when connecting to the cluster reference.
     * @return a new {@link CouchbaseCluster} reference.
     */
    public static CouchbaseCluster create(final String... nodes) {
        return create(Arrays.asList(nodes));
    }

    /**
     * Creates a new {@link CouchbaseCluster} reference against the nodes passed in.
     *
     * The {@link CouchbaseEnvironment} will be automatically created and its lifecycle managed.
     *
     * @param nodes the list of nodes to use when connecting to the cluster reference.
     * @return a new {@link CouchbaseCluster} reference.
     */
    public static CouchbaseCluster create(final List<String> nodes) {
        return new CouchbaseCluster(
            DefaultCouchbaseEnvironment.create(),
            ConnectionString.fromHostnames(nodes),
            false
        );
    }

    /**
     * Creates a new {@link CouchbaseCluster} reference against the nodes passed in.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @param nodes the list of nodes to use when connecting to the cluster reference.
     * @return a new {@link CouchbaseCluster} reference.
     */
    public static CouchbaseCluster create(final CouchbaseEnvironment environment,
        final String... nodes) {
        return create(environment, Arrays.asList(nodes));
    }

    /**
     * Creates a new {@link CouchbaseCluster} reference against the nodes passed in.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @param nodes the list of nodes to use when connecting to the cluster reference.
     * @return a new {@link CouchbaseCluster} reference.
     */
    public static CouchbaseCluster create(final CouchbaseEnvironment environment,
        final List<String> nodes) {
        return new CouchbaseCluster(environment, ConnectionString.fromHostnames(nodes), true);
    }

    /**
     * Creates a new {@link CouchbaseCluster} reference using the connection string.
     *
     * The {@link CouchbaseEnvironment} will be automatically created and its lifecycle managed.
     *
     * @param connectionString the connection string to identify the remote cluster.
     * @return a new {@link CouchbaseCluster} reference.
     */
    public static CouchbaseCluster fromConnectionString(final String connectionString) {
        return new CouchbaseCluster(
            DefaultCouchbaseEnvironment.create(),
            ConnectionString.create(connectionString),
            false
        );
    }

    /**
     * Creates a new {@link CouchbaseCluster} reference using the connection string.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @param connectionString the connection string to identify the remote cluster.
     * @return a new {@link CouchbaseCluster} reference.
     */
    public static CouchbaseCluster fromConnectionString(final CouchbaseEnvironment environment,
        final String connectionString) {
        return new CouchbaseCluster(environment, ConnectionString.create(connectionString), true);
    }

    /**
     * Package private constructor to create the {@link CouchbaseCluster}.
     *
     * This method should not be called directly, but rather through the many factory methods
     * available on this class.
     *
     * @param environment the custom environment to use for this cluster reference.
     * @param connectionString the connection string to identify the remote cluster.
     * @param sharedEnvironment if the environment is managed by this class or not.
     */
    CouchbaseCluster(final CouchbaseEnvironment environment,
        final ConnectionString connectionString, final boolean sharedEnvironment) {
        couchbaseAsyncCluster = new CouchbaseAsyncCluster(
            environment,
            connectionString,
            sharedEnvironment
        );
        this.environment = environment;
        this.connectionString = connectionString;
        this.bucketCache = new ConcurrentHashMap<String, Bucket>();
    }

    @Override
    public Bucket openBucket() {
        return openBucket(CouchbaseAsyncCluster.DEFAULT_BUCKET);
    }

    @Override
    public Bucket openBucket(long timeout, TimeUnit timeUnit) {
        return openBucket(CouchbaseAsyncCluster.DEFAULT_BUCKET, timeout, timeUnit);
    }

    @Override
    public Bucket openBucket(String name) {
        return openBucket(name, null);
    }

    @Override
    public Bucket openBucket(String name, long timeout, TimeUnit timeUnit) {
        return openBucket(name, null, timeout, timeUnit);
    }

    @Override
    public Bucket openBucket(String name, String password) {
        return openBucket(name, password, null);
    }

    @Override
    public Bucket openBucket(String name, String password, long timeout, TimeUnit timeUnit) {
        return openBucket(name, password, null, timeout, timeUnit);
    }

    @Override
    public Bucket openBucket(String name, String password,
        List<Transcoder<? extends Document, ?>> transcoders) {
        return openBucket(name, password, transcoders, environment.connectTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public Bucket openBucket(final String name, final String password,
        final List<Transcoder<? extends Document, ?>> transcoders,
        long timeout, TimeUnit timeUnit) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Bucket name is not allowed to be null or empty.");
        }

        Bucket cachedBucket = getCachedBucket(name);
        if (cachedBucket != null) {
            return cachedBucket;
        }

        final List<Transcoder<? extends Document, ?>> trans = transcoders == null
            ? new ArrayList<Transcoder<? extends Document, ?>>() : transcoders;

        return Blocking.blockForSingle(couchbaseAsyncCluster
            .openBucket(name, password, transcoders)
            .map(new Func1<AsyncBucket, Bucket>() {
                @Override
                public Bucket call(AsyncBucket asyncBucket) {
                    CouchbaseBucket bucket = new CouchbaseBucket(environment, core(), name,
                        password, trans);
                    bucketCache.put(name, bucket);
                    return bucket;
                }
            }).single(), timeout, timeUnit);
    }

    /**
     * Helper method to get a bucket instead of opening it if it is cached already.
     *
     * @param name the name of the bucket
     * @return the cached bucket if found, null if not.
     */
    private Bucket getCachedBucket(final String name) {
        Bucket cachedBucket = bucketCache.get(name);

        if(cachedBucket != null) {
            if (cachedBucket.isClosed()) {
                LOGGER.debug("Not returning cached bucket \"{}\", because it is closed.", name);
                bucketCache.remove(name);
            } else {
                LOGGER.debug("Returning still open, cached bucket \"{}\"", name);
                return cachedBucket;
            }
        }

        return null;
    }

    @Override
    public ClusterManager clusterManager(final String username, final String password) {
        return couchbaseAsyncCluster
            .clusterManager(username, password)
            .map(new Func1<AsyncClusterManager, ClusterManager>() {
                @Override
                public ClusterManager call(AsyncClusterManager asyncClusterManager) {
                    return DefaultClusterManager.create(username, password, connectionString,
                        environment, core());
                }
            })
            .toBlocking()
            .single();
    }

    @Override
    public Boolean disconnect() {
        return disconnect(environment.disconnectTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public Boolean disconnect(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            couchbaseAsyncCluster
                .disconnect()
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        bucketCache.clear();
                    }
                }),
            timeout,
            timeUnit
        );
    }

    @Override
    public ClusterFacade core() {
        return couchbaseAsyncCluster.core().toBlocking().single();
    }
}
