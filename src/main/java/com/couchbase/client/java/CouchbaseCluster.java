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
import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.cluster.DisconnectRequest;
import com.couchbase.client.core.message.cluster.DisconnectResponse;
import com.couchbase.client.core.message.cluster.OpenBucketRequest;
import com.couchbase.client.core.message.cluster.SeedNodesRequest;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.CouchbaseClusterManager;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.env.DefaultClusterEnvironment;
import rx.Observable;
import rx.functions.Func1;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CouchbaseCluster implements Cluster {

    private static final String DEFAULT_BUCKET = "default";
    private static final String DEFAULT_HOST = "127.0.0.1";

    private final ClusterFacade core;
    private final ClusterEnvironment environment;
    private final ConnectionString connectionString;

    public static CouchbaseCluster create() {
        return create(DEFAULT_HOST);
    }

    public static CouchbaseCluster create(final String... nodes) {
        return create(Arrays.asList(nodes));
    }

    public static CouchbaseCluster create(final List<String> nodes) {
        return create(DefaultClusterEnvironment.create(), nodes);
    }

    public static CouchbaseCluster create(final ClusterEnvironment environment, final String... nodes) {
        return create(environment, Arrays.asList(nodes));
    }

    public static CouchbaseCluster create(final ClusterEnvironment environment, final List<String> nodes) {
        ConnectionString connectionString;
        if (nodes.size() == 1 && nodes.get(0).contains("://")) {
            connectionString = ConnectionString.create(nodes.get(0));
        } else {
            connectionString = ConnectionString.fromHostnames(nodes);
        }
        return from(environment, connectionString);
    }

    public static CouchbaseCluster from(final ConnectionString connectionString) {
        return from(DefaultClusterEnvironment.create(), connectionString);
    }

    public static CouchbaseCluster from(final ClusterEnvironment environment, final ConnectionString connectionString) {
        return new CouchbaseCluster(environment, connectionString);
    }

    CouchbaseCluster(final ClusterEnvironment environment, final ConnectionString connectionString) {
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
    public Observable<Bucket> openBucket() {
        return openBucket(DEFAULT_BUCKET);
    }

    @Override
    public Observable<Bucket> openBucket(final String name) {
        return openBucket(name, null);
    }

    @Override
    public Observable<Bucket> openBucket(final String name, final String pass) {
        final String password = pass == null ? "" : pass;
        return core
            .send(new OpenBucketRequest(name, password))
            .map(new Func1<CouchbaseResponse, Bucket>() {
                @Override
                public Bucket call(CouchbaseResponse response) {
                    return new CouchbaseBucket(core, name, password);
                }
            });
    }

    @Override
    public Observable<Boolean> disconnect() {
        return core
            .<DisconnectResponse>send(new DisconnectRequest())
            .map(new Func1<DisconnectResponse, Boolean>() {
                     @Override
                     public Boolean call(DisconnectResponse response) {
                         return response.status() == ResponseStatus.SUCCESS;
                     }
                 }
            );
    }

    @Override
    public Observable<ClusterManager> clusterManager(final String username, final String password) {
        return Observable.just((ClusterManager) CouchbaseClusterManager.create(username, password, connectionString,
            environment, core));
    }
}
