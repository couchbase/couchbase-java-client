package com.couchbase.client.java;

import com.couchbase.client.core.message.CouchbaseResponse;
import com.couchbase.client.core.message.cluster.OpenBucketRequest;
import com.couchbase.client.core.message.cluster.SeedNodesRequest;
import rx.Observable;
import rx.functions.Func1;

public class CouchbaseCluster implements Cluster {

    private final com.couchbase.client.core.cluster.Cluster core;

    public CouchbaseCluster(final String... hostnames) {
        core = new com.couchbase.client.core.cluster.CouchbaseCluster();
        core.send(new SeedNodesRequest(hostnames)).toBlockingObservable().single();
    }

    @Override
    public Observable<Bucket> openBucket() {
        return openBucket("default");
    }

    @Override
    public Observable<Bucket> openBucket(final String name) {
        return openBucket(name, null);
    }

    @Override
    public Observable<Bucket> openBucket(final String name, final String pass) {
        final String password = pass == null ? "" : pass;
        return core.send(new OpenBucketRequest(name, password)).map(new Func1<CouchbaseResponse, Bucket>() {
        @Override
        public Bucket call(CouchbaseResponse response) {
            return new CouchbaseBucket(core, name, password);
        }
        });
    }

}
