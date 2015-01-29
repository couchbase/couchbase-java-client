package com.couchbase.client.java;

import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class BucketInfoTest extends ClusterDependentTest  {

    @Test
    public void shouldLoadBucketInfo() {
        BucketInfo info = bucket().bucketManager().info();

        assertEquals(BucketType.COUCHBASE, info.type());
        assertEquals(bucketName(), info.name());
        assertTrue(info.nodeCount() > 0);
        assertEquals(info.nodeCount(), info.nodeList().size());
        assertNotNull(info.raw());
    }

}
