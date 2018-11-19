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

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.auth.PasswordAuthenticator;
import com.couchbase.client.java.cluster.AuthDomain;
import com.couchbase.client.java.cluster.UserRole;
import com.couchbase.client.java.cluster.UserSettings;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DesignDocumentDoesNotExistException;
import com.couchbase.client.java.subdoc.SubdocOptionsBuilder;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.Version;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.ViewQuery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assume.assumeFalse;

/**
 * @author Subhashni Balakrishnan
 */
public class DataServiceUserTest {
    private static CouchbaseTestContext ctx;
    private static String username = "dataServiceUser";
    private static String password = "password";
    private static String roUsername = "dataServiceRoUser";
    private static Cluster cluster;
    private static Bucket bucket;
    private static Cluster roCluster;
    private static Bucket roBucket;

    @BeforeClass
    public static void setup() throws Exception {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
                .build()
                .ignoreIfClusterUnder(Version.parseVersion("5.0.0"));

        ctx.clusterManager().upsertUser(AuthDomain.LOCAL, username, UserSettings.build().password(password)
                .roles(Arrays.asList(new UserRole("data_writer", ctx.bucketName()),new UserRole("data_reader", ctx.bucketName()))));
        ctx.clusterManager().upsertUser(AuthDomain.LOCAL, roUsername, UserSettings.build().password(password)
                .roles(Arrays.asList(new UserRole("data_reader", ctx.bucketName()))));
        Thread.sleep(100); //sleep a bit for the user to be async updated to memcached before opening bucket

        cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, password));
        bucket = cluster.openBucket(ctx.bucketName());

        roCluster = CouchbaseCluster.create(ctx.seedNode());
        roCluster.authenticate(new PasswordAuthenticator(roUsername, password));
        roBucket = roCluster.openBucket(ctx.bucketName());

        bucket.insert(JsonDocument.create("dataServiceUserTestDoc"));

        DesignDocument designDoc = DesignDocument.create(
                "all",
                Arrays.asList(
                        DefaultView.create("all", "function (doc, meta)"+
                                "{ emit(doc.name, null); }"))
        );

        try {
            DesignDocument stored = ctx.bucketManager().getDesignDocument("all");
            if (!stored.equals(designDoc)) {
                ctx.bucketManager().upsertDesignDocument(designDoc);
            }
        } catch (DesignDocumentDoesNotExistException ex) {
            ctx.bucketManager().upsertDesignDocument(designDoc);
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (ctx != null) {
            cluster.disconnect();
            roCluster.disconnect();
            ctx.clusterManager().removeUser(AuthDomain.LOCAL, username);
            ctx.clusterManager().removeUser(AuthDomain.LOCAL, roUsername);
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void testSimpleKeyValueOperationsAuth() {
        bucket.insert(JsonDocument.create("fooa"));
        bucket.get(JsonDocument.create("fooa"));
        bucket.remove(JsonDocument.create("fooa"));
    }

    @Test
    public void testSimpleSubdocOperationsAuth() {
        bucket.insert(JsonDocument.create("foob", JsonObject.create()));
        bucket.mutateIn("foob").insert("bar", "baz").execute();
        bucket.remove(JsonDocument.create("foob"));
    }

    @Test
    public void testSimpleXattrOperationsAuth() {
        bucket.insert(JsonDocument.create("fooc"));
        bucket.mutateIn("fooc").upsert("bar", "baz", new SubdocOptionsBuilder().xattr(true)).execute();
        bucket.remove(JsonDocument.create("fooc"));
    }

    @Test
    public void testViewQueryOperationAuth() {
        roBucket.query(ViewQuery.from("all", "all"));
    }

    @Test
    public void testReadOnlyUserAuth() {
        roBucket.get("dataServiceUserTestDoc");
    }

    @Test(expected = CouchbaseException.class)
    public void shouldFailWriteForReadOnlyUser() {
        roBucket.upsert(JsonDocument.create("dataServiceUserTestDoc"));
    }
}