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

package com.couchbase.client.java.bucket;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.internal.PingReport;
import com.couchbase.client.core.message.internal.PingServiceHealth;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.error.IndexAlreadyExistsException;
import com.couchbase.client.java.error.IndexDoesNotExistException;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.dsl.path.index.IndexType;
import com.couchbase.client.java.query.util.IndexInfo;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.TestProperties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.functions.Action1;

/**
 * Test case for all N1QL index-related method in the {@link BucketManager}.
 *
 * @author Simon Baslé
 */
public class BucketManagerIndexManagementTests {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(
            BucketManagerIndexManagementTests.class);

    private static Cluster cluster;
    private static ClusterManager clusterManager;
    private static final String indexBucketNamePrefix = "testIndexedBucket";
    private static int count = 0;

    private String indexedBucketName;
    private Bucket indexedBucket;

    @BeforeClass
    public static void initCluster() {
        String seedNode = TestProperties.seedNode();
        String adminName = TestProperties.adminName();
        String adminPassword = TestProperties.adminPassword();

        cluster = CouchbaseCluster.create(seedNode);
        clusterManager = cluster.clusterManager(adminName, adminPassword);
    }

    @Before
    public void createBucket() throws InterruptedException {
        assumeFalse(CouchbaseTestContext.isMockEnabled());
        cluster.authenticate(TestProperties.adminName(), TestProperties.adminPassword());
        indexedBucketName = indexBucketNamePrefix + ++count;
        clusterManager.insertBucket(DefaultBucketSettings.builder()
                .name(indexedBucketName)
                .quota(100));
        boolean canUseBucket = false;
        do {
            try {
                indexedBucket = cluster.openBucket(indexedBucketName);
                PingReport pingReport = indexedBucket.ping();
                for (PingServiceHealth health:pingReport.services()) {
                    if (health.state() != PingServiceHealth.PingState.OK) {
                        throw new Exception("Not healthy");
                    }
                }
                indexedBucket.upsert(JsonDocument.create(indexedBucketName + "foo"));
                indexedBucket.remove(indexedBucketName + "foo");
                canUseBucket = true;
            } catch (Exception e) {
                LOGGER.info("Unable to open/use bucket" + e.toString());
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        } while(!canUseBucket);
        LOGGER.info(indexedBucket + " created and opened");
        List<IndexInfo> initialIndexes = indexedBucket.bucketManager().listN1qlIndexes();
        assertEquals("Newly created bucket unexpectedly has indexes: " + initialIndexes, 0, initialIndexes.size());
    }

    @After
    public void removeBucket() {
        if (indexedBucket != null) {
            indexedBucket.close();
            clusterManager.removeBucket(indexedBucketName);
        }
    }

    @AfterClass
    public static void disconnectCluster() {
        cluster.disconnect();
    }

    @Test
    public void testListIndexes() throws Exception {
        List<IndexInfo> indexes = indexedBucket.bucketManager().listN1qlIndexes();

        assertEquals(indexes.toString(), 0, indexes.size());

        indexedBucket.query(N1qlQuery.simple("CREATE PRIMARY INDEX ON `" + indexedBucketName + "`"));
        indexes = indexedBucket.bucketManager().listN1qlIndexes();

        assertEquals(indexes.toString(), 1, indexes.size());
        IndexInfo first = indexes.get(0);
        assertTrue(first.isPrimary());
        assertEquals(IndexInfo.PRIMARY_DEFAULT_NAME, first.name());
        assertEquals("gsi", first.rawType());
        assertNotNull(first.type());
        assertEquals(IndexType.GSI, first.type());
        assertEquals("online", first.state());
        assertEquals(JsonArray.empty(), first.indexKey());

        indexedBucket.query(N1qlQuery.simple("CREATE INDEX `bIndex` ON `" + indexedBucketName + "` (tata, toto)"));
        indexedBucket.query(N1qlQuery.simple("CREATE INDEX `aIndex` ON `" + indexedBucketName + "` (tata)"));
        indexes = indexedBucket.bucketManager().listN1qlIndexes();

        assertEquals(indexes.toString(), 3, indexes.size());
        assertEquals(first, indexes.get(0)); //primary is still listed first
        IndexInfo second = indexes.get(1);
        IndexInfo third = indexes.get(2);
        assertEquals("aIndex", second.name());
        assertEquals("bIndex", third.name()); //indexes are then listed alphabetically
        assertEquals(JsonArray.create().add("`tata`"), second.indexKey());
        assertEquals(JsonArray.create().add("`tata`").add("`toto`"), third.indexKey());
        assertEquals("online", second.state());
        assertEquals("online", third.state());
        assertFalse(second.isPrimary());
        assertFalse(third.isPrimary());
        assertEquals(IndexType.GSI, second.type());
        assertEquals(IndexType.GSI, third.type());
    }

    @Test
    public void testListIndexesIgnoresNonGsi() {
        List<IndexInfo> indexes = indexedBucket.bucketManager().listN1qlIndexes();
        assertEquals(indexes.toString(), 0, indexes.size());

        indexedBucket.query(N1qlQuery.simple("CREATE PRIMARY INDEX ON `" + indexedBucketName + "`"));
        indexedBucket.query(N1qlQuery.simple("CREATE INDEX `ambiguousIndex` ON `" + indexedBucketName + "` (tata, toto)"));
        indexedBucket.query(N1qlQuery.simple("CREATE INDEX `ambiguousIndex` ON `" + indexedBucketName + "` (tata) USING VIEW"));
        indexes = indexedBucket.bucketManager().listN1qlIndexes();

        assertEquals(indexes.toString(), 2, indexes.size());
        IndexInfo first = indexes.get(0);
        IndexInfo second = indexes.get(1);

        //make sure the view index was created
        N1qlQueryResult countResult = indexedBucket.query(N1qlQuery
                .simple("SELECT COUNT(*) AS cnt FROM system:indexes WHERE keyspace_id = '" + indexedBucketName + "'"));
        List<N1qlQueryRow> rows = countResult.allRows();
        Long count = rows.get(0).value().getLong("cnt");
        assertEquals("" + rows, (Long) 3L, count);

        //assert found indexes
        assertTrue(first.isPrimary());
        assertEquals(IndexInfo.PRIMARY_DEFAULT_NAME, first.name());
        assertEquals("gsi", first.rawType());
        assertNotNull(first.type());
        assertEquals(IndexType.GSI, first.type());
        assertEquals("online", first.state());
        assertEquals(JsonArray.empty(), first.indexKey());

        assertEquals("ambiguousIndex", second.name());
        assertEquals(JsonArray.create().add("`tata`").add("`toto`"), second.indexKey());
        assertEquals("online", second.state());
        assertFalse(second.isPrimary());
        assertEquals(IndexType.GSI, second.type());

    }

    @Test
    public void testCreateIndex() {
        final String index = "secondaryIndex";
        indexedBucket.bucketManager().createN1qlIndex(index, false, false, "toto", x("tata"));
        List<IndexInfo> indexes = indexedBucket.bucketManager().listN1qlIndexes();

        assertEquals(1, indexes.size());
        JsonArray expectedKeys = JsonArray.from("`toto`", "`tata`");
        assertEquals(expectedKeys, indexes.get(0).indexKey());
        assertEquals("", indexes.get(0).condition());
    }

    @Test
    public void testCreateIndexWithWhereClause() {
        final String index = "secondaryConditionalIndex";
        List<Object> fields = Arrays.asList("toto", x("tata"));
        indexedBucket.bucketManager().createN1qlIndex(index, fields,
                x("toto").isNotMissing()
                .and(x("tata").isNotNull()),
                false, false);
        List<IndexInfo> indexes = indexedBucket.bucketManager().listN1qlIndexes();

        assertEquals(1, indexes.size());
        JsonArray expectedKeys = JsonArray.from("`toto`", "`tata`");
        assertEquals(expectedKeys, indexes.get(0).indexKey());
        assertEquals("((`toto` is not missing) and (`tata` is not null))", indexes.get(0).condition());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateIndexFailsIfBadField() {
        indexedBucket.bucketManager().createN1qlIndex("secondaryIndex", false, false, "toto", x("tata"), 128L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateIndexEmptyFieldsFails() {
        indexedBucket.bucketManager().createN1qlIndex("secondaryIndex", false, false);
    }

    @Test
    public void testCreateExistingIndexSeveralTimes() {
        String index = "secondaryIndex";
        boolean firstAttempt = indexedBucket.bucketManager().createN1qlIndex(index, false, false, "toto");
        try {
            indexedBucket.bucketManager().createN1qlIndex(index, false, false, "tata");
            fail("expected failure of second index creation");
        } catch (IndexAlreadyExistsException e) {
            //OK
        }
        boolean existingAttempt = indexedBucket.bucketManager().createN1qlIndex(index, true, false, "titi");

        assertTrue(firstAttempt);
        assertFalse(existingAttempt);
        List<IndexInfo> indexes = indexedBucket.bucketManager().listN1qlIndexes();
        assertEquals(1, indexes.size());
        assertNotNull(indexes.get(0).indexKey());
        assertEquals(1, indexes.get(0).indexKey().size());
        assertEquals(i("toto").toString(), indexes.get(0).indexKey().getString(0));
    }

    @Test
    public void testCreatePrimaryIndex() {
        indexedBucket.bucketManager().createN1qlPrimaryIndex(false, false);
        List<IndexInfo> indexes = indexedBucket.bucketManager().listN1qlIndexes();

        assertEquals(1, indexes.size());
        assertEquals(Index.PRIMARY_NAME, indexes.get(0).name());
        assertTrue(indexes.get(0).isPrimary());
    }

    @Test
    public void testCreatePrimaryIndexWithCustomName() {
        indexedBucket.bucketManager().createN1qlPrimaryIndex("def_primary", false, false);
        List<IndexInfo> indexes = indexedBucket.bucketManager().listN1qlIndexes();

        assertEquals(1, indexes.size());
        assertEquals("def_primary", indexes.get(0).name());
        assertTrue(indexes.get(0).isPrimary());
    }

    @Test
    public void testCreatePrimaryIndexSeveralTimes() {
        boolean firstAttempt = indexedBucket.bucketManager().createN1qlPrimaryIndex(false, false);
        try {
            indexedBucket.bucketManager().createN1qlPrimaryIndex(false, false);
            fail("expected failure of second index creation");
        } catch (IndexAlreadyExistsException e) {
            //OK
        }
        boolean existingAttempt = indexedBucket.bucketManager().createN1qlPrimaryIndex(true, false);

        assertTrue(firstAttempt);
        assertFalse(existingAttempt);
        List<IndexInfo> indexes = indexedBucket.bucketManager().listN1qlIndexes();
        assertEquals(1, indexes.size());
        assertNotNull(indexes.get(0).indexKey());
        assertTrue(indexes.get(0).isPrimary());
    }

    @Test
    public void testCreatePrimaryIndexesWithDifferentNames() {
        boolean namedAttempt1 = indexedBucket.bucketManager().createN1qlPrimaryIndex("def_primary", false, false);
        boolean namedAttempt2 = indexedBucket.bucketManager().createN1qlPrimaryIndex("def_primary2", false, false);
        boolean unamedAttempt = indexedBucket.bucketManager().createN1qlPrimaryIndex(false, false);

        assertTrue(namedAttempt1);
        assertTrue(namedAttempt2);
        assertTrue(unamedAttempt);

        assertEquals(3, indexedBucket.bucketManager().listN1qlIndexes().size());
    }

    @Test
    public void testDropIndex() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createN1qlIndex("toDrop", true, false, "field");
        assertEquals(1, mgr.listN1qlIndexes().size());

        mgr.dropN1qlIndex("toDrop", false);
        assertEquals(0, mgr.listN1qlIndexes().size());
    }

    @Test(expected = IndexDoesNotExistException.class)
    public void testDropIndexThatDoesntExistFails() {
        indexedBucket.bucketManager().dropN1qlIndex("blabla", false);
    }

    @Test
    public void testDropIgnoreIndexThatDoesntExistSucceeds() {
        boolean dropped = indexedBucket.bucketManager().dropN1qlIndex("blabla", true);
        assertFalse(dropped);
    }

    @Test
    public void testDropPrimaryIndex() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createN1qlPrimaryIndex(true, false);
        assertEquals(1, mgr.listN1qlIndexes().size());

        mgr.dropN1qlPrimaryIndex(false);
        assertEquals(0, mgr.listN1qlIndexes().size());
    }

    @Test(expected = IndexDoesNotExistException.class)
    public void testDropPrimaryIndexThatDoesntExistFails() {
        indexedBucket.bucketManager().dropN1qlPrimaryIndex(false);
    }

    @Test
    public void testDropIgnorePrimaryIndexThatDoesntExistSucceeds() {
        boolean dropped = indexedBucket.bucketManager().dropN1qlPrimaryIndex(true);
        assertFalse(dropped);
    }

    @Test
    public void testDropNamedPrimaryIndex() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createN1qlPrimaryIndex("def_primary", true, false);
        assertEquals(1, mgr.listN1qlIndexes().size());

        mgr.dropN1qlPrimaryIndex("def_primary", false);
        assertEquals(0, mgr.listN1qlIndexes().size());
    }

    @Test
    public void testDropPrimaryIndexWithNameFailsIfNameNotProvided() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createN1qlPrimaryIndex("def_primary", true, false);
        assertEquals(1, mgr.listN1qlIndexes().size());

        boolean dropped = mgr.dropN1qlPrimaryIndex(true);
        assertEquals(false, dropped);
        assertEquals(1, mgr.listN1qlIndexes().size());

        try {
            mgr.dropN1qlPrimaryIndex(false);
            fail("Expected IndexDoesNotExistException");
        } catch (IndexDoesNotExistException e) {
            //OK
        }
    }

    @Test(expected = IndexDoesNotExistException.class)
    public void testDropNamedPrimaryIndexThatDoesntExistFails() {
        indexedBucket.bucketManager().dropN1qlPrimaryIndex("invalidPrimaryIndex", false);
    }

    @Test
    public void testDropIgnoreNamedPrimaryIndexThatDoesntExistSucceeds() {
        boolean dropped = indexedBucket.bucketManager().dropN1qlPrimaryIndex("invalidPrimaryIndex", true);
        assertFalse(dropped);
    }

    @Test
    public void testDeferredBuildTriggersAllPending() throws InterruptedException {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createN1qlIndex("first", false, false, "first");
        List<String> triggered = mgr.buildN1qlDeferredIndexes();
        assertEquals(0, triggered.size());

        mgr.createN1qlPrimaryIndex(false, true);
        mgr.createN1qlIndex("toto", false, true, "toto");
        mgr.createN1qlIndex("tata", false, true, "tata");
        mgr.createN1qlIndex("titi", false, false, "titi");

        String[] watch = new String[]{"toto", "tata", Index.PRIMARY_NAME};
        triggered = mgr.buildN1qlDeferredIndexes();
        assertEquals(3, triggered.size());
        assertTrue("Unexpected triggered list " + triggered + ", expected " + watch, triggered.containsAll(Arrays.asList(watch)));
    }

    @Test
    public void testWatchDeferredWaitsForAllPending() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createN1qlIndex("first", false, false, "first");
        List<String> triggered = mgr.buildN1qlDeferredIndexes();
        assertEquals(0, triggered.size());

        mgr.createN1qlPrimaryIndex(false, true);
        mgr.createN1qlIndex("toto", false, true, "toto");
        mgr.createN1qlIndex("tata", false, true, "tata");
        mgr.createN1qlIndex("titi", false, false, "titi");

        List<String> watch = Arrays.asList("toto", "tata", Index.PRIMARY_NAME);
        List<IndexInfo> readyInMilliseconds = mgr.watchN1qlIndexes(watch, 10, TimeUnit.MILLISECONDS);
        assertEquals(0, readyInMilliseconds.size());

        triggered = mgr.buildN1qlDeferredIndexes();
        assertEquals(3, triggered.size());
        assertTrue("Unexpected triggered list " + triggered + ", expected " + watch, triggered.containsAll(watch));

        List<IndexInfo> readyInSeconds = mgr.async().watchN1qlIndexes(watch, 10, TimeUnit.SECONDS)
                .doOnNext(new Action1<IndexInfo>() {
                    @Override
                    public void call(IndexInfo indexInfo) {
                        LOGGER.info(indexInfo.name() + ": " + indexInfo.state());
                    }
                })
                .toList()
                .toBlocking().single();
        assertEquals("Not all indexes are online after 10 seconds", 3, readyInSeconds.size());
    }
}