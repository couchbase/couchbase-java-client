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
package com.couchbase.client.java.rbac;

import java.util.Arrays;
import java.util.List;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.auth.PasswordAuthenticator;
import com.couchbase.client.java.cluster.UserRole;
import com.couchbase.client.java.cluster.UserSettings;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.Version;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.View;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Subhashni Balakrishnan
 */
public class BucketAndClusterManagerUserTest {
    private static CouchbaseTestContext ctx;
    private static String username = "roAdminUser";
    private static String password = "password";
    private static Cluster cluster;
    private static Bucket bucket;

    @BeforeClass
    public static void setup() throws Exception {
        ctx = CouchbaseTestContext.builder()
                .build()
                .ignoreIfClusterUnder(Version.parseVersion("5.0.0"));

        ctx.clusterManager().upsertUser(username, UserSettings.build().password(password)
                .roles(Arrays.asList(new UserRole("ro_admin", ""))));
        cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, password));
        Thread.sleep(100); //sleep a bit for the user to be async updated to memcached before opening bucket
        bucket = cluster.openBucket(ctx.bucketName());
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (ctx != null) {
            cluster.disconnect();
            ctx.clusterManager().removeUser(username);
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void testGetReadOnlyClusterInfoAuth() {
        cluster.clusterManager().info();
        cluster.clusterManager().getBuckets();
        cluster.clusterManager().getUsers();
    }

    @Test(expected = CouchbaseException.class)
    public void shouldFailOnUpdatingCluster() {
        cluster.clusterManager().upsertUser("testUser", UserSettings.build().password("password"));
    }

    @Test
    public void testGetReadOnlyBucketInfoAuth() {
        bucket.bucketManager().info();
        bucket.bucketManager().getDesignDocuments();
        bucket.bucketManager().listN1qlIndexes();
    }

    @Test(expected = CouchbaseException.class)
    public void shouldFailOnUpdatingBucket() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}"));
        DesignDocument designDocument = DesignDocument.create("testDesgin", views);
        bucket.bucketManager().upsertDesignDocument(designDocument);
    }
}