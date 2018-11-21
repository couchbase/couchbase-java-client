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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.auth.Authenticator;
import com.couchbase.client.java.auth.CredentialContext;
import com.couchbase.client.java.auth.ClassicAuthenticator;
import com.couchbase.client.java.auth.PasswordAuthenticator;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.AsyncN1qlQueryRow;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.observers.TestSubscriber;

/**
 * Verifies the functionality of various N1QL queries in a Cluster-Level configuration
 *
 * @author Simon Baslé
 * @since 2.3
 */
public class N1qlClusterLevelTest {

    private static CouchbaseTestContext ctx;
    private static CouchbaseTestContext ctx2;
    private static CouchbaseTestContext ctx3;


    @BeforeClass
    public static void init() throws Exception {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
                .bucketName("N1qlCluster")
                .bucketPassword("protected")
                .adhoc(true)
                .bucketQuota(100)
                .build()
                .ignoreIfNoN1ql()
                .ensurePrimaryIndex();

        ctx2 = CouchbaseTestContext.builder()
                .bucketName("N1qlCluster2")
                .bucketPassword("protected")
                .adhoc(true)
                .bucketQuota(100)
                .build()
                .ignoreIfNoN1ql()
                .ensurePrimaryIndex();

        ctx3 = CouchbaseTestContext.builder()
                .bucketName("N1qlCluster3")
                .bucketPassword("safe")
                .adhoc(true)
                .bucketQuota(100)
                .build()
                .ignoreIfNoN1ql()
                .ensurePrimaryIndex();


        JsonObject content1 = JsonObject.create().put("foreignKey", "baz");
        JsonObject content2 = JsonObject.create().put("foo", "bar");
        ctx.bucket().upsert(JsonDocument.create("join", content1));
        ctx2.bucket().upsert(JsonDocument.create("baz", content2));
        ctx3.bucket().upsert(JsonDocument.create("baz", content2));
        if (!ctx.rbacEnabled()) {
            Authenticator authenticator = new ClassicAuthenticator()
                .bucket(ctx.bucketName(), "protected")
                .bucket(ctx2.bucketName(), "protected");
            ctx.cluster().authenticate(authenticator);
        }
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
        if (ctx2 != null) {
            ctx2.destroyBucketAndDisconnect();
        }
        if (ctx3 != null) {
            ctx3.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void shouldJoinProtectedBuckets() {
        N1qlQuery query = N1qlQuery.simple(
                "SELECT A.*, B.* FROM `" + ctx.bucketName() + "` AS A" +
                        " JOIN `" + ctx2.bucketName() + "` as B ON KEYS A.foreignKey",
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS));

        N1qlQueryResult result = ctx.cluster().query(query);

        assertThat(result.errors()).isEmpty();
        assertThat(result.finalSuccess()).isTrue();
        List<N1qlQueryRow> allRows = result.allRows();
        assertThat(allRows)
                .isNotEmpty()
                .hasSize(1);
        assertThat(allRows.get(0).value().toMap())
                .containsEntry("foreignKey", "baz")
                .containsEntry("foo", "bar")
                .hasSize(2);
    }

    @Test
    public void shouldJoinProtectedBucketsAsynchronously() {

        Authenticator authenticator = ctx.rbacEnabled() ? new PasswordAuthenticator(ctx.adminName(), ctx.adminPassword()) :
            new ClassicAuthenticator()
                .bucket(ctx.bucketName(), "protected")
                .bucket(ctx2.bucketName(), "protected");

        AsyncCluster asyncCluster = CouchbaseAsyncCluster.create(ctx.env(), ctx.seedNode());
        asyncCluster.authenticate(authenticator);
        asyncCluster.openBucket(ctx.bucketName())
                .mergeWith(asyncCluster.openBucket(ctx2.bucketName()))
                .toBlocking().last();

        N1qlQuery query = N1qlQuery.simple(
                "SELECT A.*, B.* FROM `" + ctx.bucketName() + "` AS A" +
                        " JOIN `" + ctx2.bucketName() + "` as B ON KEYS A.foreignKey",
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS));

        try {
            AsyncN1qlQueryResult result = asyncCluster.query(query).timeout(2, TimeUnit.SECONDS).toBlocking().single();

            TestSubscriber<JsonObject> errorTestSub = new TestSubscriber<JsonObject>();
            TestSubscriber<AsyncN1qlQueryRow> rowsTestSub = new TestSubscriber<AsyncN1qlQueryRow>();
            result.errors().subscribe(errorTestSub);
            result.rows().subscribe(rowsTestSub);

            errorTestSub.awaitTerminalEvent();
            errorTestSub.assertNoErrors();
            errorTestSub.assertNoValues();
            errorTestSub.assertCompleted();

            rowsTestSub.awaitTerminalEvent();
            rowsTestSub.assertNoErrors();
            rowsTestSub.assertValueCount(1);
            rowsTestSub.assertCompleted();
        } finally {
            asyncCluster.disconnect().toBlocking().single();
        }
    }

    @Test
    public void shouldFailJoinWithPartialCredentials() {
        assumeTrue(!ctx2.rbacEnabled());
        N1qlQuery query = N1qlQuery.simple(
                "SELECT A.*, B.* FROM `" + ctx.bucketName() + "` AS A" +
                        " JOIN `" + ctx3.bucketName() + "` as B ON KEYS A.foreignKey",
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS));

        N1qlQueryResult result = ctx.cluster().query(query);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).getInt("code")).isEqualTo(10000);
        assertThat(result.finalSuccess()).isFalse();
    }

    @Test
    public void shouldFailOnClusterWithoutOpenBuckets() {
        Cluster cluster = CouchbaseCluster.create(ctx.env(), ctx.seedNode());
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM " + ctx2.bucketName());

        try {
            cluster.query(query);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            //success
        } finally {
            cluster.disconnect();
        }
    }

    @Test
    public void shouldFailOnClusterWithoutOpenBucketsAsync() {
        AsyncCluster asyncCluster = CouchbaseAsyncCluster.create(ctx.env(), ctx.seedNode());
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM " + ctx2.bucketName());

        try {
            TestSubscriber<AsyncN1qlQueryResult> testSubscriber = new TestSubscriber<AsyncN1qlQueryResult>();
            asyncCluster.query(query).subscribe(testSubscriber);

            testSubscriber.awaitTerminalEvent();
            testSubscriber.assertNoValues();
            testSubscriber.assertError(UnsupportedOperationException.class);
        } finally {
            asyncCluster.disconnect().toBlocking().single();
        }
    }

    @Test
    public void shouldFailOnClusterWithoutCredentials() {
        assumeTrue(!ctx2.rbacEnabled());
        ctx2.cluster().authenticate(new ClassicAuthenticator());

        N1qlQuery query = N1qlQuery.simple("SELECT * FROM " + ctx2.bucketName(),
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS));

        try {
            ctx2.cluster().query(query);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e).hasMessage("CLUSTER_N1QL credentials are required in the Authenticator for cluster level querying");
        }
    }

    @Test
    public void shouldFailOnClusterWhenAuthenticatorDoesntSupportClusterN1QL() {
        assumeTrue(!ctx2.rbacEnabled());
        Authenticator mock = mock(Authenticator.class);
        IllegalArgumentException cause = new IllegalArgumentException();
        when(mock.getCredentials(any(CredentialContext.class), anyString()))
                .thenThrow(cause);

        ctx3.cluster().authenticate(mock);
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM " + ctx3.bucketName());

        try {
            ctx3.cluster().query(query);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e)
                    .hasMessage("Couldn't retrieve credentials for cluster level querying from Authenticator")
                    .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause()).isSameAs(cause);
        }
    }

}
