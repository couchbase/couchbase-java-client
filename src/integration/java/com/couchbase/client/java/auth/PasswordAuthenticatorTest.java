/*
 * Copyright (c) 2017 Couchbase, Inc.
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

package com.couchbase.client.java.auth;

import java.util.ArrayList;
import java.util.Arrays;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.AuthDomain;
import com.couchbase.client.java.cluster.UserRole;
import com.couchbase.client.java.cluster.UserSettings;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.error.MixedAuthenticationException;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.Version;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assume.assumeFalse;

/**
 * @author Subhashni Balakrishnan
 */
public class PasswordAuthenticatorTest {
    private static CouchbaseTestContext ctx;
    private static String username = "testUser";
    private static String password = "password";

    /**
     * The mock does not support RBAC, so let's disable this test.
     */
    @Before
    public void checkMock() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());
    }

    @BeforeClass
    public static void setup() throws Exception {
        ctx = CouchbaseTestContext.builder()
                .build()
                .ignoreIfClusterUnder(Version.parseVersion("5.0.0"));

        ctx.clusterManager().upsertUser(AuthDomain.LOCAL, username, UserSettings.build().password(password)
                .roles(Arrays.asList(new UserRole("bucket_full_access", "*"))));
        Thread.sleep(100); //sleep a bit for the user to be async updated to memcached before opening bucket
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (ctx != null) {
            ctx.clusterManager().removeUser(AuthDomain.LOCAL, username);
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void shouldOpenBucketWithCorrectCredentials() {
        Cluster cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, password));
        cluster.openBucket(ctx.bucketName());
        cluster.disconnect();
    }

    @Test
    public void shouldOpenBucketWithShortcutOverload() {
        Cluster cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(username, password);
        cluster.openBucket(ctx.bucketName());
        cluster.disconnect();
    }

    @Test(expected = InvalidPasswordException.class)
    public void shouldNotOpenBucketWithInCorrectCredentials() {
        Cluster cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, "x"));
        cluster.openBucket(ctx.bucketName());
    }

    @Test(expected = InvalidPasswordException.class)
    public void shouldNotOpenBucketWithInCorrectCredentialsOverload() {
        Cluster cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, "x"));
        cluster.openBucket(ctx.bucketName(), new ArrayList<Transcoder<? extends Document, ?>>());
    }

    @Test(expected = MixedAuthenticationException.class)
    public void shouldNotAcceptMixedAuthenticationWithBucketCredentials() {
        Cluster cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, password));
        cluster.openBucket(ctx.bucketName(), "x");
    }

    @Test(expected = MixedAuthenticationException.class)
    public void shouldNotAcceptMixedAuthenticationWithBucketCredentialsOverload() {
        Cluster cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, password));
        cluster.openBucket(ctx.bucketName(), "x", null);
    }

    @Test(expected = MixedAuthenticationException.class)
    public void shouldNotAcceptMixedAuthenticationWithClassicAuthenticatorOverload() {
        Cluster cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, password));
        cluster.authenticate(new ClassicAuthenticator());
    }

    @Test
    public void shouldWorkWithUserNameInConnectionString() {
        Cluster cluster = CouchbaseCluster.fromConnectionString("couchbase://" + username + "@" + ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(password));
        cluster.openBucket(ctx.bucketName());
        cluster.disconnect();
    }

    @Test
    public void shouldWorkWithUserNameInConnectionStringWithCorrectPriority() {
        Cluster cluster = CouchbaseCluster.fromConnectionString("couchbase://" + "blah" + "@" + ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, password));
        cluster.openBucket(ctx.bucketName());
        cluster.disconnect();
    }
}