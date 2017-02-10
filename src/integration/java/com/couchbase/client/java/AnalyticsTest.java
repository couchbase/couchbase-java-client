package com.couchbase.client.java;

import com.couchbase.client.java.analytics.AnalyticsQuery;
import com.couchbase.client.java.analytics.AnalyticsQueryResult;

/**
 * Still manual simple test, to be expanded going forward once properly integrated
 * into the server.
 *
 * @author Michael Nitschinger
 * @since 2.4.3
 */
public class AnalyticsTest {

    public static void main(String... args) throws Exception {

        System.setProperty("com.couchbase.analyticsEnabled", "true");

        Cluster cluster = CouchbaseCluster.create();
        Bucket bucket = cluster.openBucket();

        AnalyticsQueryResult result = bucket.query(AnalyticsQuery.simple("SELECT 1=1;"));
        System.out.println(result);
    }
}
