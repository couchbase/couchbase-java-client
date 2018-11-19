package com.couchbase.client.java.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.TestProperties;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests around the usage of a {@link ClassicAuthenticator}.
 */
public class ClassicAuthenticatorTesst {

    private CouchbaseCluster cluster;
    private ClassicAuthenticator authenticator;

    @Before
    public void init() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        this.authenticator = new ClassicAuthenticator();
        this.cluster = CouchbaseCluster.create(TestProperties.seedNode())
                .authenticate(authenticator);
    }

    @After
    public void tearDown() {
        if (this.cluster != null) {
            this.cluster.disconnect();
        }
    }

    @Test
    public void testClusterManagementWithGoodCreds() {
        authenticator.cluster(TestProperties.adminName(), TestProperties.adminPassword());

        ClusterManager manager = cluster.clusterManager();

        assertThat(manager).isNotNull();
        assertThat(manager.info()).isNotNull();
    }

    @Test
    public void testClusterManagementWithBadCreds() {
        authenticator.cluster(TestProperties.adminName(), TestProperties.adminPassword() + "bad");

        ClusterManager manager = cluster.clusterManager();
        try {
            manager.info();
            fail("Expected InvalidPasswordException");
        } catch (InvalidPasswordException e) {
            //success
        }
    }

    @Test
    public void testOpenBucketWithBucketNameAndWrongCreds() {
        authenticator.bucket(TestProperties.bucket(), TestProperties.password() + "bad");

        try {
            cluster.openBucket(TestProperties.bucket());
            fail("Expected InvalidPasswordException");
        } catch (InvalidPasswordException e) {
            //success
        }

        try {
            cluster.openBucket(TestProperties.bucket(), 5, TimeUnit.SECONDS);
            fail("Expected InvalidPasswordException");
        } catch (InvalidPasswordException e) {
            //success
        }
    }

    @Test
    public void testOpenBucketWithBucketNameAndGoodCreds() {
        authenticator.bucket(TestProperties.bucket(), TestProperties.password());

        Bucket b = cluster.openBucket(TestProperties.bucket());
        assertThat(b).isNotNull();
        b.close();
        assertThat(cluster.openBucket(TestProperties.bucket(), 5, TimeUnit.SECONDS))
                .isNotNull()
                .isNotSameAs(b);
    }
    @Test
    public void testOpenBucketDefaultDoesntUseAuthenticator() throws InterruptedException {
        authenticator.bucket(TestProperties.bucket(), TestProperties.password());

        Bucket b1 = cluster.openBucket(TestProperties.bucket());
        b1.close();
        Thread.sleep(1000);
        Bucket b2 = cluster.openBucket(TestProperties.bucket(), 5, TimeUnit.SECONDS);

        assertThat(b1).isNotNull();
        assertThat(b2).isNotNull().isNotSameAs(b1);
    }

    @Test
    public void testOpenBucketWithExplicitCredsDoesntOverwriteAuthenticator() {
        authenticator.bucket(TestProperties.bucket(), TestProperties.password());

        try {
            cluster.openBucket(TestProperties.bucket(), "bad");
            fail("Expected InvalidPasswordException, usage of explicit password");
        } catch (InvalidPasswordException e) {
            //success
        }
        assertThat(authenticator.getCredentials(CredentialContext.BUCKET_KV, TestProperties.bucket()))
                .containsOnly(new Credential(TestProperties.bucket(), TestProperties.password()));
    }

    @Test
    public void testClusterManagerWithExplicitCredsDoesntOverwriteAuthenticator() {
        authenticator.cluster(TestProperties.adminName(), TestProperties.adminPassword());

        ClusterManager manager = cluster.clusterManager(TestProperties.adminName(), "bad");
        try {
            manager.info();
            fail("Expected InvalidPasswordException, usage of explicit password");
        } catch (InvalidPasswordException e) {
            //success
        }
        assertThat(authenticator.getCredentials(CredentialContext.CLUSTER_MANAGEMENT, TestProperties.adminName()))
                .containsOnly(new Credential(TestProperties.adminName(), TestProperties.adminPassword()));
    }

    @Test
    public void shouldAllowToResetAuthenticator() {
        Authenticator auth1 = cluster.authenticator();
        Authenticator auth2 = new ClassicAuthenticator();

        Assertions.assertThat(cluster.authenticator()).isSameAs(auth1);

        cluster.authenticate(auth2);

        Assertions.assertThat(cluster.authenticator())
                .isNotSameAs(auth1)
                .isSameAs(auth2);
    }
}