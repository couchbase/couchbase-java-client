package com.couchbase.client.java;

import java.util.Iterator;

import com.couchbase.client.java.analytics.AnalyticsDeferredResultHandle;
import com.couchbase.client.java.analytics.AnalyticsParams;
import com.couchbase.client.java.analytics.AnalyticsQuery;
import com.couchbase.client.java.analytics.AnalyticsQueryResult;
import com.couchbase.client.java.analytics.AnalyticsQueryRow;

/**
 * Stand alone test for now as it is experimental
 */
public class AnalyticsDeferredQueryTest {

    public static void main(String... args) throws Exception {
        Cluster cluster = CouchbaseCluster.create();
        cluster.authenticate("Administrator", "password");
        Bucket bucket = cluster.openBucket("default");

        AnalyticsQueryResult result = bucket.query(AnalyticsQuery.simple("SELECT 1=1;", AnalyticsParams.build().deferred(true)));

        byte[] serialized = bucket.exportAnalyticsDeferredResultHandle(result.handle());
        AnalyticsDeferredResultHandle handle = bucket.importAnalyticsDeferredResultHandle(serialized);

        while(!handle.status().equalsIgnoreCase("success")) {
            Thread.sleep(100);
            handle.status();
        }
        Iterator<AnalyticsQueryRow> it = handle.rows();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }
}
