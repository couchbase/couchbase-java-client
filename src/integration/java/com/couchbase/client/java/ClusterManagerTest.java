package com.couchbase.client.java;

import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClusterManagerTest extends ClusterDependentTest {

    @Test
    public void shouldLoadInfo() {
        ClusterManager manager = cluster().clusterManager("Administrator", "password").toBlocking().single();
        ClusterInfo info = manager.info().toBlocking().single();

        assertNotNull(info);
        assertTrue(info.raw().getObject("storageTotals").getObject("ram").getLong("total") > 0);
    }

}
