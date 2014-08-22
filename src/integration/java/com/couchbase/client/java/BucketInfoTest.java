package com.couchbase.client.java;

import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BucketInfoTest extends ClusterDependentTest  {

    @Test
    public void shouldLoadBucketInfo() {
        BucketInfo info = bucket().info().toBlocking().single();

        assertEquals(BucketType.COUCHBASE, info.type());
        assertEquals(bucketName(), info.name());
        assertTrue(info.nodeCount() > 0);
        assertTrue(info.replicaCount() > 0);
        assertNotNull(info.raw());
        assertTrue(info.raw() instanceof JsonObject);
    }

}
