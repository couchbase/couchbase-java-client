package com.couchbase.client.java;

import com.couchbase.client.java.cluster.AsyncClusterManager;
import com.couchbase.client.java.cluster.BucketSettings;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.util.TestProperties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;

import static org.junit.Assert.*;

public class ClusterManagerTest {

    private static AsyncClusterManager clusterManager;

    @BeforeClass
    public static void setup() {
        clusterManager = CouchbaseAsyncCluster
            .create(TestProperties.seedNode())
            .clusterManager(TestProperties.adminName(), TestProperties.adminPassword())
            .toBlocking()
            .single();
    }

    @Before
    public void clearBuckets() {
        clusterManager
            .getBuckets()
            .flatMap(new Func1<BucketSettings, Observable<?>>() {
                @Override
                public Observable<?> call(BucketSettings bucketSettings) {
                    return clusterManager.removeBucket(bucketSettings.name());
                }
            }).toBlocking().lastOrDefault(null);
    }

    @Test
    public void shouldLoadInfo() {
        ClusterInfo info = clusterManager.info().toBlocking().single();

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

        clusterManager.insertBucket(settings).toBlocking().single();
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
                    return clusterManager.insertBucket(settings);
                }
            }).toBlocking().last();


        List<BucketSettings> settings = clusterManager.getBuckets().toList().toBlocking().single();
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

        clusterManager.insertBucket(settings).toBlocking().single();

        assertEquals(1, clusterManager.getBuckets().toList().toBlocking().single().size());
        assertTrue(clusterManager.removeBucket("removeBucket").toBlocking().single());
        assertEquals(0, clusterManager.getBuckets().toList().toBlocking().single().size());
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

        clusterManager.insertBucket(settings).toBlocking().single();

        settings = DefaultBucketSettings
            .builder()
            .name("updateBucket")
            .password("password")
            .quota(256)
            .build();

        clusterManager.updateBucket(settings).toBlocking().single();
        assertEquals(256, clusterManager.getBucket("updateBucket").toBlocking().single().quota());
    }

}
