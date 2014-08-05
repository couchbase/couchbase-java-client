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
import rx.Observable;
import rx.functions.Func1;

public class CouchbaseCluster implements Cluster {

    private static final String DEFAULT_BUCKET = "default";

    private final ClusterFacade core;

    public CouchbaseCluster(final String... hostnames) {
        core = new CouchbaseCore();
        SeedNodesRequest request = hostnames.length == 0 ? new SeedNodesRequest() : new SeedNodesRequest(hostnames);
        core.send(request).toBlocking().single();
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
}
