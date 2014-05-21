package com.couchbase.client.java;

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

    private final com.couchbase.client.core.cluster.Cluster core;

    public CouchbaseCluster(final String... hostnames) {
        core = new com.couchbase.client.core.cluster.CouchbaseCluster();
        SeedNodesRequest request = hostnames.length == 0 ? new SeedNodesRequest() : new SeedNodesRequest(hostnames);
        core.send(request).toBlockingObservable().single();
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
