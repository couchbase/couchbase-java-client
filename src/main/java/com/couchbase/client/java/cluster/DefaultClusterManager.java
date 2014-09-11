package com.couchbase.client.java.cluster;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.ConnectionString;
import com.couchbase.client.java.env.CouchbaseEnvironment;

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
        return asyncClusterManager
            .info()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public List<BucketSettings> getBuckets() {
        return getBuckets(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<BucketSettings> getBuckets(long timeout, TimeUnit timeUnit) {
        return asyncClusterManager
            .getBuckets()
            .toList()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public BucketSettings getBucket(String name) {
        return getBucket(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public BucketSettings getBucket(String name, long timeout, TimeUnit timeUnit) {
        return asyncClusterManager
            .getBucket(name)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public Boolean hasBucket(String name) {
        return hasBucket(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean hasBucket(String name, long timeout, TimeUnit timeUnit) {
        return asyncClusterManager
            .hasBucket(name)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public BucketSettings insertBucket(BucketSettings settings) {
        return insertBucket(settings, timeout, TIMEOUT_UNIT);
    }

    @Override
    public BucketSettings insertBucket(BucketSettings settings, long timeout, TimeUnit timeUnit) {
        return asyncClusterManager
            .insertBucket(settings)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public BucketSettings updateBucket(BucketSettings settings) {
        return updateBucket(settings, timeout, TIMEOUT_UNIT);
    }

    @Override
    public BucketSettings updateBucket(BucketSettings settings, long timeout, TimeUnit timeUnit) {
        return asyncClusterManager
            .updateBucket(settings)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public Boolean removeBucket(String name) {
        return removeBucket(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean removeBucket(String name, long timeout, TimeUnit timeUnit) {
        return asyncClusterManager
            .removeBucket(name)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }
}
