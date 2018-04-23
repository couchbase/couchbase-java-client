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
package com.couchbase.client.java;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.error.BucketDoesNotExistException;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.error.MixedAuthenticationException;
import com.couchbase.client.java.util.TestProperties;
import com.couchbase.client.java.util.features.Version;
import org.junit.Test;

/**
 * Basic test cases which verify functionality not bound to a {@link Bucket}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class ConnectionTest  {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfBucketIsNull() {
        Cluster cluster = CouchbaseCluster.create(TestProperties.seedNode());
        cluster.openBucket(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfBucketIsEmpty() {
        Cluster cluster = CouchbaseCluster.create(TestProperties.seedNode());
        cluster.openBucket("");
    }

    @Test
    public void shouldThrowBucketDoesNotExistExceptionForWrongBucketName() {
        Cluster cluster = CouchbaseCluster.create(TestProperties.seedNode());
        ClusterManager clusterManager = cluster.clusterManager(TestProperties.adminName(), TestProperties.adminPassword());
        assumeTrue("Server after 4.5.0 throw an IllegalArgumentException, see other test",
                clusterManager.info().getMinVersion().compareTo(new Version(4, 5, 0)) < 0);

        verifyException(cluster, BucketDoesNotExistException.class).openBucket("someWrongBucketName");
    }

    @Test
    public void shouldThrowInvalidPasswordExceptionForWrongBucketNameAfterWatson() {
        Cluster cluster = CouchbaseCluster.create(TestProperties.seedNode());
        ClusterManager clusterManager = cluster.clusterManager(TestProperties.adminName(), TestProperties.adminPassword());
        assumeTrue("Server before 4.5.0 throw a BucketDoesNotExistException, see other test",
                clusterManager.info().getMinVersion().compareTo(new Version(4, 5, 0)) >= 0);

        verifyException(cluster, InvalidPasswordException.class).openBucket("someWrongBucketName");
    }

    @Test(expected = InvalidPasswordException.class)
    public void shouldThrowConfigurationExceptionForWrongBucketPassword() {
        Cluster cluster = CouchbaseCluster.create(TestProperties.seedNode());
        cluster.openBucket(TestProperties.bucket(), "completelyWrongPassword");
    }

    @Test
    public void shouldBootstrapWithBadHost() {
        Cluster cluster = CouchbaseCluster.create("badnode", TestProperties.seedNode());
        try {
            cluster.openBucket(TestProperties.bucket(), TestProperties.password());
        } catch (InvalidPasswordException ex) {
            cluster.authenticate(TestProperties.adminName(), TestProperties.adminPassword());
            cluster.openBucket(TestProperties.bucket());
        }
    }

    @Test
    public void shouldProvideClusterInfoWithBadHostInBootstrapList() {
        Cluster cluster = CouchbaseCluster.create("1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4", TestProperties.seedNode());
        cluster.authenticate(TestProperties.adminName(), TestProperties.adminPassword());
        cluster.clusterManager().info();
    }

    @Test
    public void shouldCacheBucketReference() {
        Cluster cluster = CouchbaseCluster.create(TestProperties.seedNode());
        Bucket bucket1;
        Bucket bucket2;

        try {
            bucket1 = cluster.openBucket(TestProperties.bucket(), TestProperties.password());
            bucket2 = cluster.openBucket(TestProperties.bucket(), TestProperties.password());
        } catch (InvalidPasswordException ex) {
            // rbac hack
            cluster.authenticate(TestProperties.adminName(), TestProperties.adminPassword());
            bucket1 = cluster.openBucket(TestProperties.bucket());
            bucket2 = cluster.openBucket(TestProperties.bucket());
        }

        assertEquals(bucket1.hashCode(), bucket2.hashCode());

        assertFalse(bucket1.isClosed());
        assertFalse(bucket2.isClosed());
        bucket1.close();
        assertTrue(bucket1.isClosed());
        assertTrue(bucket2.isClosed());

        Bucket bucket3;
        try {
            bucket3 = cluster.openBucket(TestProperties.bucket(), TestProperties.password());
        } catch(MixedAuthenticationException ex) {
            // rbac hack
            bucket3 = cluster.openBucket(TestProperties.bucket());
        }

        assertNotEquals(bucket1.hashCode(), bucket3.hashCode());
        assertFalse(bucket3.isClosed());
    }
}
