package com.couchbase.client.java;

import com.couchbase.client.java.cluster.BucketSettings;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.error.InvalidPasswordException;
import com.couchbase.client.java.util.TestProperties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClusterManagerTest {

    private static ClusterManager clusterManager;
    private static CouchbaseCluster couchbaseCluster;

    @BeforeClass
    public static void setup() {
        couchbaseCluster = CouchbaseCluster.create(TestProperties.seedNode());
        clusterManager = couchbaseCluster
            .clusterManager(TestProperties.adminName(), TestProperties.adminPassword());
    }

    @Before
    public void clearBuckets() {
        clusterManager.async()
            .getBuckets()
            .flatMap(new Func1<BucketSettings, Observable<?>>() {
                @Override
                public Observable<?> call(BucketSettings bucketSettings) {
                    return clusterManager.async().removeBucket(bucketSettings.name());
                }
            }).toBlocking().lastOrDefault(null);
    }

    @Test(expected = InvalidPasswordException.class)
    public void shouldFailOnWrongUser() {
        ClusterManager manager = couchbaseCluster.clusterManager("invalidUser", "");
        manager.getBuckets();
    }

    @Test(expected = InvalidPasswordException.class)
    public void shouldFailOnWrongPassword() {
        ClusterManager manager = couchbaseCluster.clusterManager(TestProperties.adminName(), "foobar3423$$");
        manager.getBuckets();
    }

    @Test
    public void shouldLoadInfo() {
        ClusterInfo info = clusterManager.info();

        assertNotNull(info);
        assertTrue(info.raw().getObject("storageTotals").getObject("ram").getLong("total") > 0);
    }

    @Test
    public void shouldInsertBucket() {
        BucketSettings settings = DefaultBucketSettings
            .builder()
            .name("insertBucket")
            .password("password")
            .quota(128)
            .build();

        clusterManager.insertBucket(settings);
    }

    @Test
    public void shouldGetBuckets() {
        Observable
            .just("bucket1", "bucket2")
            .map(new Func1<String, BucketSettings>() {
                @Override
                public BucketSettings call(final String name) {
                    return DefaultBucketSettings
                        .builder()
                        .name(name)
                        .password("password")
                        .quota(128)
                        .build();
                }
            })
            .flatMap(new Func1<BucketSettings, Observable<?>>() {
                @Override
                public Observable<?> call(BucketSettings settings) {
                    return clusterManager.async().insertBucket(settings);
                }
            }).toBlocking().last();


        List<BucketSettings> settings = clusterManager.async().getBuckets().toList().toBlocking().single();
        assertEquals(2, settings.size());
        for (BucketSettings bucket : settings) {
            assertTrue(bucket.name().equals("bucket1") || bucket.name().equals("bucket2"));
        }
    }

    @Test
    public void shouldRemoveBucket() {
        BucketSettings settings = DefaultBucketSettings
            .builder()
            .name("removeBucket")
            .password("password")
            .quota(128)
            .build();

        clusterManager.insertBucket(settings);

        assertEquals(1, clusterManager.async().getBuckets().toList().toBlocking().single().size());
        assertTrue(clusterManager.removeBucket("removeBucket"));
        assertEquals(0, clusterManager.async().getBuckets().toList().toBlocking().single().size());
    }

    @Test
    @Ignore
    public void shouldUpdateBucket() {
        BucketSettings settings = DefaultBucketSettings
            .builder()
            .name("updateBucket")
            .password("password")
            .quota(128)
            .build();

        clusterManager.insertBucket(settings);

        settings = DefaultBucketSettings
            .builder()
            .name("updateBucket")
            .password("password")
            .quota(256)
            .build();

        clusterManager.updateBucket(settings);
        assertEquals(256, clusterManager.getBucket("updateBucket").quota());
    }

}
