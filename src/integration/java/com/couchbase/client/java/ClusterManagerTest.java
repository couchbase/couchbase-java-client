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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClusterManagerTest {

    private static ClusterManager clusterManager;
    private static CouchbaseCluster couchbaseCluster;

    private static final String INSERT_BUCKET = "insertBucket";
    private static final String BUCKET_1 = "bucket1";
    private static final String BUCKET_2 = "bucket2";
    private static final String REMOVE_BUCKET = "removeBucket";
    private static final String UPDATE_BUCKET = "updateBucket";

    private static final Set<String> BUCKETS = new HashSet<String>(5);
    static {
        BUCKETS.add(INSERT_BUCKET);
        BUCKETS.add(BUCKET_1);
        BUCKETS.add(BUCKET_2);
        BUCKETS.add(REMOVE_BUCKET);
        BUCKETS.add(UPDATE_BUCKET);
    }

    private int bucketsRemaining = 0;

    @BeforeClass
    public static void setup() {
        couchbaseCluster = CouchbaseCluster.create(TestProperties.seedNode());
        clusterManager = couchbaseCluster
            .clusterManager(TestProperties.adminName(), TestProperties.adminPassword());
    }

    @Before
    public void clearBuckets() {
        //Only clear the buckets relevant to this integration test
        Observable.from(BUCKETS)
                .flatMap(new Func1<String, Observable<?>>() {
                    @Override
                    public Observable<?> call(String bucket) {
                        return clusterManager.async().removeBucket(bucket);
                    }
                }).toBlocking().lastOrDefault(null);

        //also find out how many buckets remain
        this.bucketsRemaining = clusterManager.async().getBuckets().count().toBlocking().single();
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
            .name(INSERT_BUCKET)
            .password("password")
            .quota(128)
            .build();

        clusterManager.insertBucket(settings);
    }

    @Test
    public void shouldGetBuckets() {
        Observable
            .just(BUCKET_1, BUCKET_2)
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
        assertEquals(bucketsRemaining + 2, settings.size());
        int others = 0;
        for (BucketSettings bucket : settings) {
            if (BUCKETS.contains(bucket.name())) {
                assertTrue(bucket.name().equals(BUCKET_1) || bucket.name().equals(BUCKET_2));
            } else {
                others++;
            }
        }
        assertEquals(others, bucketsRemaining);
    }

    @Test
    public void shouldRemoveBucket() {
        BucketSettings settings = DefaultBucketSettings
            .builder()
            .name(REMOVE_BUCKET)
            .password("password")
            .quota(128)
            .build();

        clusterManager.insertBucket(settings);

        assertEquals(bucketsRemaining + 1, clusterManager.async().getBuckets().toList().toBlocking().single().size());
        assertTrue(clusterManager.removeBucket(REMOVE_BUCKET));
        assertEquals(bucketsRemaining, clusterManager.async().getBuckets().toList().toBlocking().single().size());
    }

    @Test
    public void shouldUpdateBucket() {
        BucketSettings settings = DefaultBucketSettings
            .builder()
            .name(UPDATE_BUCKET)
            .password("password")
            .quota(128)
            .build();

        clusterManager.insertBucket(settings);

        settings = DefaultBucketSettings
            .builder()
            .name(UPDATE_BUCKET)
            .password("password")
            .quota(256)
            .build();

        clusterManager.updateBucket(settings);
        int size = clusterManager.getBucket(UPDATE_BUCKET).quota();

        clusterManager.removeBucket(UPDATE_BUCKET);
        assertEquals(256, size);


    }

}
