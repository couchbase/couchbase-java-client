package com.couchbase.client.java.cluster;

import rx.Observable;

public interface ClusterManager {

    Observable<ClusterInfo> info();

/*
    Observable<ClusterBucketSettings> getBuckets();
    Observable<ClusterBucketSettings> getBucket(String name);
    Observable<ClusterBucketSettings> removeBucket(String name);
    Observable<ClusterBucketSettings> insertBucket(ClusterBucketSettings bucketSettings);
    Observable<ClusterBucketSettings> updateBucket(ClusterBucketSettings bucketSettings);
*/
}
