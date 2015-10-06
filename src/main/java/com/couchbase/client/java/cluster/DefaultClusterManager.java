package com.couchbase.client.java.cluster;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.ConnectionString;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.util.Blocking;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultClusterManager implements ClusterManager {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncClusterManager asyncClusterManager;
    private final long timeout;

    DefaultClusterManager(final String username, final String password, final ConnectionString connectionString,
                          final CouchbaseEnvironment environment, final ClusterFacade core) {
        asyncClusterManager = DefaultAsyncClusterManager.create(username, password, connectionString, environment,
            core);
        this.timeout = environment.managementTimeout();
    }

    public static DefaultClusterManager create(final String username, final String password,
                                               final ConnectionString connectionString, final CouchbaseEnvironment environment, final ClusterFacade core) {
        return new DefaultClusterManager(username, password, connectionString, environment, core);
    }

    @Override
    public AsyncClusterManager async() {
        return asyncClusterManager;
    }

    @Override
    public ClusterInfo info() {
        return info(timeout, TIMEOUT_UNIT);
    }

    @Override
    public ClusterInfo info(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.info().single(), timeout, timeUnit);
    }

    @Override
    public List<BucketSettings> getBuckets() {
        return getBuckets(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<BucketSettings> getBuckets(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.getBuckets().toList(), timeout, timeUnit);
    }

    @Override
    public BucketSettings getBucket(String name) {
        return getBucket(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public BucketSettings getBucket(String name, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.getBucket(name).singleOrDefault(null), timeout, timeUnit);
    }

    @Override
    public Boolean hasBucket(String name) {
        return hasBucket(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean hasBucket(String name, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.hasBucket(name).single(), timeout, timeUnit);
    }

    @Override
    public BucketSettings insertBucket(BucketSettings settings) {
        return insertBucket(settings, timeout, TIMEOUT_UNIT);
    }

    @Override
    public BucketSettings insertBucket(BucketSettings settings, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.insertBucket(settings).single(), timeout, timeUnit);
    }

    @Override
    public BucketSettings updateBucket(BucketSettings settings) {
        return updateBucket(settings, timeout, TIMEOUT_UNIT);
    }

    @Override
    public BucketSettings updateBucket(BucketSettings settings, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.updateBucket(settings).single(), timeout, timeUnit);
    }

    @Override
    public Boolean removeBucket(String name) {
        return removeBucket(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean removeBucket(String name, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.removeBucket(name).single(), timeout, timeUnit);
    }
}