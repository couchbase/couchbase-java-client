package com.couchbase.client.java.util;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import rx.Observer;
import rx.Subscriber;
import rx.observables.BlockingObservable;

/**
 * Base test class for tests that need a working cluster reference.
 *
 * @author Michael Nitschinger
 */
public class ClusterDependentTest {

    private static final String seedNode = TestProperties.seedNode();
    private static final String bucketName = TestProperties.bucket();
    private static final String password = TestProperties.password();

    private static Cluster cluster;
    private static Bucket bucket;

    @BeforeClass
    public static void connect() {
        cluster = new CouchbaseCluster(seedNode);
        bucket = cluster.openBucket(bucketName, password).toBlocking().single();
        bucket.flush().toBlocking().single();
    }

    @AfterClass
    public static void disconnect() throws InterruptedException {
        cluster.disconnect().toBlocking().single();
    }

    public static String password() {
        return password;
    }

    public static Cluster cluster() {
        return cluster;
    }

    public static Bucket bucket() {
        return bucket;
    }

    public static String bucketName() {
        return bucketName;
    }
}