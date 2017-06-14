/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.cluster;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.utils.Blocking;
import com.couchbase.client.java.cluster.api.AsyncClusterApiClient;
import com.couchbase.client.core.utils.ConnectionString;
import com.couchbase.client.java.cluster.api.ClusterApiClient;
import com.couchbase.client.java.env.CouchbaseEnvironment;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultClusterManager implements ClusterManager {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final DefaultAsyncClusterManager asyncClusterManager;
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

    @Override
    public Boolean upsertUser(AuthDomain domain, String username, UserSettings settings) {
        return upsertUser(domain, username, settings, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean upsertUser(AuthDomain domain, String username, UserSettings settings, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.upsertUser(domain, username, settings).single(), timeout, timeUnit);
    }

    @Override
    public Boolean removeUser(AuthDomain domain, String username) {
        return removeUser(domain, username, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean removeUser(AuthDomain domain, String username, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.removeUser(domain, username).single(), timeout, timeUnit);
    }

    @Override
    public List<User> getUsers(AuthDomain domain) {
        return getUsers(domain, timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<User> getUsers(AuthDomain domain, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.getUsers(domain).toList().single(), timeout, timeUnit);
    }

    @Override
    public User getUser(AuthDomain domain, String userid) {
        return Blocking.blockForSingle(asyncClusterManager.getUser(domain, userid).single(), timeout, TIMEOUT_UNIT);
    }

    @Override
    public User getUser(AuthDomain domain, String userid, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncClusterManager.getUser(domain, userid).single(), timeout, timeUnit);
    }

    @Override
    @InterfaceStability.Experimental
    public ClusterApiClient apiClient() {
        return new ClusterApiClient(asyncClusterManager.username, asyncClusterManager.password, asyncClusterManager.core,
                this.timeout, TIMEOUT_UNIT); //uses the management timeout as default for API calls as well
    }
}