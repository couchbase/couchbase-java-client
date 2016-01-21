/*
 * Copyright (C) 2015 Couchbase, Inc.
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

package com.couchbase.client.java.bucket;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.x;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.error.IndexAlreadyExistsException;
import com.couchbase.client.java.error.IndexDoesNotExistException;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.dsl.path.index.IndexType;
import com.couchbase.client.java.query.util.IndexInfo;
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
        indexedBucketName = indexBucketNamePrefix + ++count;
        clusterManager.insertBucket(DefaultBucketSettings.builder()
                .name(indexedBucketName)
                .quota(100));
        indexedBucket = cluster.openBucket(indexedBucketName);
        LOGGER.info(indexedBucket + " created and opened");
        List<IndexInfo> initialIndexes = indexedBucket.bucketManager().listIndexes();
        assertEquals("Newly created bucket unexpectedly has indexes: " + initialIndexes, 0, initialIndexes.size());
    }

    @After
    public void removeBucket() {
        indexedBucket.close();
        clusterManager.removeBucket(indexedBucketName);
    }

    @AfterClass
    public static void disconnectCluster() {
        cluster.disconnect();
    }

    @Test
    public void testListIndexes() throws Exception {
        List<IndexInfo> indexes = indexedBucket.bucketManager().listIndexes();

        assertEquals(indexes.toString(), 0, indexes.size());

        indexedBucket.query(N1qlQuery.simple("CREATE PRIMARY INDEX ON `" + indexedBucketName + "`"));
        indexes = indexedBucket.bucketManager().listIndexes();

        assertEquals(indexes.toString(), 1, indexes.size());
        IndexInfo first = indexes.get(0);
        assertTrue(first.isPrimary());
        assertEquals(IndexInfo.PRIMARY_DEFAULT_NAME, first.name());
        assertEquals("gsi", first.rawType());
        assertNotNull(first.type());
        assertEquals(IndexType.GSI, first.type());
        assertEquals("online", first.state());
        assertEquals(JsonArray.empty(), first.indexKey());

        indexedBucket.query(N1qlQuery.simple("CREATE INDEX `aIndex` ON `" + indexedBucketName + "` (tata, toto)"));
        indexedBucket.query(N1qlQuery.simple("CREATE INDEX `bIndex` ON `" + indexedBucketName + "` (tata) USING VIEW"));
        indexes = indexedBucket.bucketManager().listIndexes();

        assertEquals(indexes.toString(), 3, indexes.size());
        assertEquals(first, indexes.get(0)); //primary is still listed first
        IndexInfo second = indexes.get(1);
        IndexInfo third = indexes.get(2);
        assertEquals("aIndex", second.name());
        assertEquals("bIndex", third.name()); //indexes are then listed alphabetically
        assertEquals(JsonArray.create().add("`tata`").add("`toto`"), second.indexKey());
        assertEquals(JsonArray.create().add("`tata`"), third.indexKey());
        assertEquals("online", second.state());
        assertEquals("online", third.state());
        assertFalse(second.isPrimary());
        assertFalse(third.isPrimary());
        assertEquals(IndexType.GSI, second.type());
        assertEquals(IndexType.VIEW, third.type());
    }

    @Test
    public void testCreateIndex() {
        final String index = "secondaryIndex";
        indexedBucket.bucketManager().createIndex(index, false, false, "toto", x("tata"));
        List<IndexInfo> indexes = indexedBucket.bucketManager().listIndexes();

        assertEquals(1, indexes.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateIndexFailsIfBadField() {
        indexedBucket.bucketManager().createIndex("secondaryIndex", false, false, "toto", x("tata"), 128L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateIndexEmptyFieldsFails() {
        indexedBucket.bucketManager().createIndex("secondaryIndex", false, false);
    }

    @Test
    public void testCreateExistingIndexSeveralTimes() {
        String index = "secondaryIndex";
        boolean firstAttempt = indexedBucket.bucketManager().createIndex(index, false, false, "toto");
        try {
            indexedBucket.bucketManager().createIndex(index, false, false, "tata");
            fail("expected failure of second index creation");
        } catch (IndexAlreadyExistsException e) {
            //OK
        }
        boolean existingAttempt = indexedBucket.bucketManager().createIndex(index, true, false, "titi");

        assertTrue(firstAttempt);
        assertFalse(existingAttempt);
        List<IndexInfo> indexes = indexedBucket.bucketManager().listIndexes();
        assertEquals(1, indexes.size());
        assertNotNull(indexes.get(0).indexKey());
        assertEquals(1, indexes.get(0).indexKey().size());
        assertEquals(i("toto").toString(), indexes.get(0).indexKey().getString(0));
    }

    @Test
    public void testCreatePrimaryIndex() {
        indexedBucket.bucketManager().createPrimaryIndex(false, false);
        List<IndexInfo> indexes = indexedBucket.bucketManager().listIndexes();

        assertEquals(1, indexes.size());
    }

    @Test
    public void testCreatePrimaryIndexSeveralTimes() {
        boolean firstAttempt = indexedBucket.bucketManager().createPrimaryIndex(false, false);
        try {
            indexedBucket.bucketManager().createPrimaryIndex(false, false);
            fail("expected failure of second index creation");
        } catch (IndexAlreadyExistsException e) {
            //OK
        }
        boolean existingAttempt = indexedBucket.bucketManager().createPrimaryIndex(true, false);

        assertTrue(firstAttempt);
        assertFalse(existingAttempt);
        List<IndexInfo> indexes = indexedBucket.bucketManager().listIndexes();
        assertEquals(1, indexes.size());
        assertNotNull(indexes.get(0).indexKey());
        assertTrue(indexes.get(0).isPrimary());
    }

    @Test
    public void testDropIndex() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createIndex("toDrop", true, false, "field");
        assertEquals(1, mgr.listIndexes().size());

        mgr.dropIndex("toDrop", false);
        assertEquals(0, mgr.listIndexes().size());
    }

    @Test(expected = IndexDoesNotExistException.class)
    public void testDropIndexThatDoesntExistFails() {
        indexedBucket.bucketManager().dropIndex("blabla", false);
    }

    @Test
    public void testDropIgnoreIndexThatDoesntExistSucceeds() {
        boolean dropped = indexedBucket.bucketManager().dropIndex("blabla", true);
        assertFalse(dropped);
    }


    @Test
    public void testDropPrimaryIndex() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createPrimaryIndex(true, false);
        assertEquals(1, mgr.listIndexes().size());

        mgr.dropPrimaryIndex(false);
        assertEquals(0, mgr.listIndexes().size());
    }

    @Test(expected = IndexDoesNotExistException.class)
    public void testDropPrimaryIndexThatDoesntExistFails() {
        indexedBucket.bucketManager().dropPrimaryIndex(false);
    }

    @Test
    public void testDropIgnorePrimaryIndexThatDoesntExistSucceeds() {
        boolean dropped = indexedBucket.bucketManager().dropPrimaryIndex(true);
        assertFalse(dropped);
    }

    @Test
    public void testDeferredBuildTriggersAllPending() throws InterruptedException {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createIndex("first", false, false, "first");
        List<String> triggered = mgr.buildDeferredIndexes();
        assertEquals(0, triggered.size());

        mgr.createPrimaryIndex(false, true);
        mgr.createIndex("toto", false, true, "toto");
        mgr.createIndex("tata", false, true, "tata");
        mgr.createIndex("titi", false, false, "titi");

        String[] watch = new String[]{"toto", "tata", Index.PRIMARY_NAME};
        triggered = mgr.buildDeferredIndexes();
        assertEquals(3, triggered.size());
        assertTrue("Unexpected triggered list " + triggered + ", expected " + watch, triggered.containsAll(Arrays.asList(watch)));
    }

    @Test
    public void testWatchDeferredWaitsForAllPending() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createIndex("first", false, false, "first");
        List<String> triggered = mgr.buildDeferredIndexes();
        assertEquals(0, triggered.size());

        mgr.createPrimaryIndex(false, true);
        mgr.createIndex("toto", false, true, "toto");
        mgr.createIndex("tata", false, true, "tata");
        mgr.createIndex("titi", false, false, "titi");

        List<String> watch = Arrays.asList("toto", "tata", Index.PRIMARY_NAME);
        List<IndexInfo> readyInMilliseconds = mgr.watchIndexes(watch, false, 10, TimeUnit.MILLISECONDS);
        assertEquals(0, readyInMilliseconds.size());

        triggered = mgr.buildDeferredIndexes();
        assertEquals(3, triggered.size());
        assertTrue("Unexpected triggered list " + triggered + ", expected " + watch, triggered.containsAll(watch));

        List<IndexInfo> readyInSeconds = mgr.async().watchIndexes(watch, false, 10, TimeUnit.SECONDS)
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

    @Test
    public void testWatchIndexesWithPrimaryTrueAddsPrimaryToWatchList() {
        BucketManager mgr = indexedBucket.bucketManager();
        mgr.createPrimaryIndex(true, false);

        final List<IndexInfo> indexInfos = mgr.watchIndexes(Collections.<String>emptyList(), true, 3, TimeUnit.SECONDS);
        assertEquals("Expected primary index to be added to watch list", 1, indexInfos.size());
        assertEquals(Index.PRIMARY_NAME, indexInfos.get(0).name());
    }
}