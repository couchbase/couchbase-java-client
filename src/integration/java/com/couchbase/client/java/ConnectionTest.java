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
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import com.couchbase.client.java.auth.Authenticator;
import com.couchbase.client.java.auth.CertAuthenticator;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.AuthenticationException;
import com.couchbase.client.java.error.BucketDoesNotExistException;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.error.MixedAuthenticationException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.TestProperties;
import com.couchbase.client.java.util.features.Version;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic test cases which verify functionality not bound to a {@link Bucket}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class ConnectionTest  {

    @Before
    public void checkMock() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());
    }

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
        Cluster cluster = CouchbaseCluster.create("x.y.z", TestProperties.seedNode());
        cluster.authenticate(TestProperties.adminName(), TestProperties.adminPassword());
        cluster.clusterManager().info();
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCheckReverseLookupWhenDNSSRVEnabled() throws Exception {
        Cluster cluster = CouchbaseCluster.create(DefaultCouchbaseEnvironment.builder().dnsSrvEnabled(true).build(), "x.y.z");
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

    @Test(expected = AuthenticationException.class)
    public void shouldNotAcceptCertAuthWithoutCertAuthEnabled() {
        Cluster cluster = null;
        try {
            cluster = CouchbaseCluster.create(TestProperties.seedNode());
            cluster.authenticate(CertAuthenticator.INSTANCE);
        } finally {
            if (cluster != null) {
                cluster.disconnect();
            }
        }
    }

    @Test(expected = AuthenticationException.class)
    public void shouldNotAcceptCertAuthWithoutKeystoreOrTruststore() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
            .sslEnabled(true)
            .certAuthEnabled(true)
            .build();
        Cluster cluster = null;
        try {
            cluster = CouchbaseCluster.create(env, TestProperties.seedNode());
            cluster.authenticate(CertAuthenticator.INSTANCE);
        } finally {
            if (cluster != null) {
                cluster.disconnect();
            }
            env.shutdown();
        }
    }

    @Test(expected = AuthenticationException.class)
    public void shouldNotAcceptOtherAuthenticatorIfCertEnabled() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
            .sslEnabled(true)
            .certAuthEnabled(true)
            .build();
        Cluster cluster = null;
        try {
            cluster = CouchbaseCluster.create(env, TestProperties.seedNode());
            cluster.authenticate("foo", "bar");
        } finally {
            if (cluster != null) {
                cluster.disconnect();
            }
            env.shutdown();
        }
    }

    @Test(expected = AuthenticationException.class)
    public void shouldNotAcceptCertMixedAuth() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
            .sslEnabled(true)
            .certAuthEnabled(true)
            .sslTruststoreFile("some/file/path")
            .build();
        Cluster cluster = null;
        try {
            cluster = CouchbaseCluster.create(env, TestProperties.seedNode());
            cluster.authenticate(CertAuthenticator.INSTANCE);
            cluster.openBucket(TestProperties.bucket(), "password");
        } finally {
            if (cluster != null) {
                cluster.disconnect();
            }
            env.shutdown();
        }
    }

    @Test(expected = AuthenticationException.class)
    public void shouldNotOpenBucketIfCertNotSetProperly() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
            .sslEnabled(true)
            .certAuthEnabled(true)
            .build();
        Cluster cluster = null;
        try {
            cluster = CouchbaseCluster.create(env, TestProperties.seedNode());
            cluster.openBucket(TestProperties.bucket());
        } finally {
            if (cluster != null) {
                cluster.disconnect();
            }
            env.shutdown();
        }
    }
}
