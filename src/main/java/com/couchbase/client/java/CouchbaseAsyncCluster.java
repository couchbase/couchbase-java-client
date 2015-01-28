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
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.cluster.DisconnectRequest;
import com.couchbase.client.core.message.cluster.DisconnectResponse;
import com.couchbase.client.core.message.cluster.OpenBucketRequest;
import com.couchbase.client.core.message.cluster.SeedNodesRequest;
import com.couchbase.client.java.cluster.AsyncClusterManager;
import com.couchbase.client.java.cluster.DefaultAsyncClusterManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.BucketDoesNotExistException;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.transcoder.Transcoder;
import rx.Observable;
import rx.functions.Func1;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CouchbaseAsyncCluster implements AsyncCluster {

    private static final String DEFAULT_BUCKET = "default";
    private static final String DEFAULT_HOST = "127.0.0.1";

    private final ClusterFacade core;
    private final CouchbaseEnvironment environment;
    private final ConnectionString connectionString;

    private final boolean sharedEnvironment;

    public static CouchbaseAsyncCluster create() {
        return create(DEFAULT_HOST);
    }

    public static CouchbaseAsyncCluster create(final CouchbaseEnvironment environment) {
        return create(environment, DEFAULT_HOST);
    }

    public static CouchbaseAsyncCluster create(final String... nodes) {
        return create(Arrays.asList(nodes));
    }

    public static CouchbaseAsyncCluster create(final List<String> nodes) {
        return new CouchbaseAsyncCluster(DefaultCouchbaseEnvironment.create(), ConnectionString.fromHostnames(nodes), false);
    }

    public static CouchbaseAsyncCluster create(final CouchbaseEnvironment environment, final String... nodes) {
        return create(environment, Arrays.asList(nodes));
    }

    public static CouchbaseAsyncCluster create(final CouchbaseEnvironment environment, final List<String> nodes) {
        return new CouchbaseAsyncCluster(environment, ConnectionString.fromHostnames(nodes), true);
    }

    public static CouchbaseAsyncCluster fromConnectionString(final String connectionString) {
        return new CouchbaseAsyncCluster(DefaultCouchbaseEnvironment.create(), ConnectionString.create(connectionString), false);
    }

    public static CouchbaseAsyncCluster fromConnectionString(final CouchbaseEnvironment environment, final String connectionString) {
        return new CouchbaseAsyncCluster(environment, ConnectionString.create(connectionString), true);
    }

    CouchbaseAsyncCluster(final CouchbaseEnvironment environment, final ConnectionString connectionString, final boolean sharedEnvironment) {
        this.sharedEnvironment = sharedEnvironment;
        core = new CouchbaseCore(environment);
        List<String> seedNodes = new ArrayList<String>();
        for (InetSocketAddress node : connectionString.hosts()) {
            seedNodes.add(node.getHostName());
        }
        if (seedNodes.isEmpty()) {
            seedNodes.add(DEFAULT_HOST);
        }
        SeedNodesRequest request = new SeedNodesRequest(seedNodes);
        core.send(request).toBlocking().single();
        this.environment = environment;
        this.connectionString = connectionString;
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
    public Observable<AsyncBucket> openBucket(final String name, final String pass) {
        return openBucket(name, pass, null);
    }

    @Override
    public Observable<AsyncBucket> openBucket(final String name, String pass,
        final List<Transcoder<? extends Document, ?>> transcoders) {
        if (name == null || name.isEmpty()) {
            return Observable.error(new IllegalArgumentException("Bucket name is not allowed to be null or empty."));
        }

        final String password = pass == null ? "" : pass;

        final List<Transcoder<? extends Document, ?>> trans = transcoders == null
            ? new ArrayList<Transcoder<? extends Document, ?>>() : transcoders;
        return core
            .send(new OpenBucketRequest(name, password))
            .map(new Func1<CouchbaseResponse, AsyncBucket>() {
                @Override
                public AsyncBucket call(CouchbaseResponse response) {
                    if (response.status() != ResponseStatus.SUCCESS) {
                        throw new CouchbaseException("Could not open bucket.");
                    }

                    return new CouchbaseAsyncBucket(core, environment, name, password, trans);
                }
            }).onErrorResumeNext(new Func1<Throwable, Observable<AsyncBucket>>() {
                @Override
                public Observable<AsyncBucket> call(final Throwable throwable) {
                    if (throwable instanceof ConfigurationException) {
                        if (throwable.getCause() instanceof IllegalStateException
                            && throwable.getCause().getMessage().contains("NOT_EXISTS")) {
                            return Observable.error(new BucketDoesNotExistException("Bucket \"" + name
                                + "\" does not exist."));
                        } else if (throwable.getCause() instanceof IllegalStateException
                            && throwable.getCause().getMessage().contains("Unauthorized")) {
                            return Observable.error(new InvalidPasswordException("Passwords for bucket \"" + name
                                +"\" do not match."));
                        } else {
                            return Observable.error(throwable);
                        }
                    } else if (throwable instanceof CouchbaseException) {
                        return Observable.error(throwable);
                    } else {
                        return Observable.error(new CouchbaseException(throwable));
                    }
                }
            });
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
            });
    }

    @Override
    public Observable<AsyncClusterManager> clusterManager(final String username, final String password) {
        return Observable.just((AsyncClusterManager) DefaultAsyncClusterManager.create(username, password, connectionString,
            environment, core));
    }

    @Override
    public Observable<ClusterFacade> core() {
        return Observable.just(core);
    }
}
