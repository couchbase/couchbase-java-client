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
package com.couchbase.client.java.util;

import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import com.couchbase.client.java.util.features.Version;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;

/**
 * Base test class for tests that need a working cluster reference.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 */
public class ClusterDependentTest {

    private static final String seedNode = TestProperties.seedNode();
    private static final String adminName = TestProperties.adminName();
    private static final String adminPassword = TestProperties.adminPassword();

    private static Cluster cluster;
    private static Bucket bucket;
    private static ClusterManager clusterManager;
    private static BucketManager bucketManager;

    @BeforeClass
    public static void connect() throws Exception {
        cluster = CouchbaseCluster.create(seedNode);
        clusterManager = cluster.clusterManager(adminName, adminPassword);
        boolean exists = clusterManager.hasBucket(bucketName());

        if (!exists) {
            clusterManager.insertBucket(DefaultBucketSettings
                .builder()
                .name(bucketName())
                .quota(256)
                .password(password())
                .enableFlush(true)
                .type(BucketType.COUCHBASE)
                .build());
        }

        bucket = cluster.openBucket(bucketName(), password());
        bucketManager = bucket.bucketManager();
        bucketManager.flush();
    }

    @AfterClass
    public static void disconnect() throws InterruptedException {
        cluster.disconnect();
    }

    /**
     * By calling this in @BeforeClass with a {@link CouchbaseFeature},
     * tests will be skipped is said feature is not available on the cluster.
     *
     * @param feature the feature to check for.
     */
    public static void ignoreIfMissing(CouchbaseFeature feature) {
        Assume.assumeTrue(clusterManager().info().checkAvailable(feature));
    }

    /**
     * By calling this in @BeforeClass with a {@link Version},
     * tests will be skipped is all nodes in the cluster are not above
     * or at that version.
     *
     * @param minimumVersion the required version to check for.
     */
    public static void ignoreIfClusterUnder(Version minimumVersion) {
        Assume.assumeTrue(clusterManager().info().getMinVersion().compareTo(minimumVersion) >= 0);
    }

    public static String password() {
        return  TestProperties.password();
    }

    public static Cluster cluster() {
        return cluster;
    }

    public static Bucket bucket() {
        return bucket;
    }

    public static String bucketName() {
        return TestProperties.bucket();
    }

    public static ClusterManager clusterManager() {
        return clusterManager;
    }

    public static BucketManager bucketManager() {
        return bucketManager;
    }
}