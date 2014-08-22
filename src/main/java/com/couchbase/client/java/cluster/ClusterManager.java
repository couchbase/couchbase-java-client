package com.couchbase.client.java.cluster;

import rx.Observable;

public interface ClusterManager {

    Observable<ClusterInfo> info();

    Observable<ClusterBucketSettings> getBuckets();
    Observable<ClusterBucketSettings> getBucket(String name);
    Observable<Boolean> hasBucket(String name);
    Observable<ClusterBucketSettings> insertBucket(ClusterBucketSettings settings);
    Observable<ClusterBucketSettings> updateBucket(ClusterBucketSettings settings);
    Observable<Boolean> removeBucket(String name);

}
