package com.couchbase.client.java;

import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.util.TestProperties;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by michael on 21/05/14.
 */
public class QueryTest {
    private static final String seedNode = TestProperties.seedNode();
    private static final String bucketName = TestProperties.bucket();
    private static final String password = TestProperties.password();

    private static Bucket bucket;

    @BeforeClass
    public static void connect() {
        System.setProperty("com.couchbase.client.queryEnabled", "true");
        CouchbaseCluster cluster = new CouchbaseCluster(seedNode);
        bucket = cluster
            .openBucket(bucketName, password)
            .toBlockingObservable()
            .single();
    }

    @Test
    public void shouldQueryView() throws Exception {
        System.out.println(bucket.query(Query.raw("select * from default limit 5")).toList().toBlockingObservable().single());
    }
}
