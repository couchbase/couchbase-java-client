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

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import java.util.Arrays;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.auth.PasswordAuthenticator;
import com.couchbase.client.java.cluster.AuthDomain;
import com.couchbase.client.java.cluster.UserRole;
import com.couchbase.client.java.cluster.UserSettings;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.Version;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Subhashni Balakrishnan
 */
public class QueryServiceUserTest {
    private static CouchbaseTestContext ctx;
    private static String username = "queryServiceUser";
    private static String password = "password";
    private static Bucket bucket;
    private static Cluster cluster;

    @BeforeClass
    public static void setup() throws Exception {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
                .build()
                .ignoreIfClusterUnder(Version.parseVersion("5.0.0"))
                .ignoreIfNoN1ql()
                .ensurePrimaryIndex();

        ctx.clusterManager().upsertUser(AuthDomain.LOCAL, username, UserSettings.build().password(password)
                .roles(Arrays.asList(new UserRole("query_select", ctx.bucketName()),
                        new UserRole("data_reader", ctx.bucketName()))));//see MB-23475, needs a data role
        cluster = CouchbaseCluster.create(ctx.seedNode());
        cluster.authenticate(new PasswordAuthenticator(username, password));
        Thread.sleep(100); //sleep a bit for the user to be async updated to memcached before opening bucket
        bucket = cluster.openBucket(ctx.bucketName());
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (ctx != null) {
            cluster.disconnect();
            ctx.clusterManager().removeUser(AuthDomain.LOCAL, username);
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void testN1qlSelectAuth() {
        N1qlQueryResult result = bucket.query(N1qlQuery.simple("select * from " + ctx.bucketName() + " limit 1"));
        assertEquals("N1ql select should be successful", true, result.finalSuccess());
    }

    @Test
    @Ignore //fails currently
    public void testN1qlInsertAuthFail() {
        N1qlQueryResult result = bucket.query(N1qlQuery.simple("INSERT INTO "+ ctx.bucketName() +" (KEY, VALUE) VALUES (\"foo\", \n" +
                "      { \"bar\": \"baz\" }) RETURNING * \n "));
        assertEquals("N1ql insert should not be successful", 1, result.errors().size());
    }
}