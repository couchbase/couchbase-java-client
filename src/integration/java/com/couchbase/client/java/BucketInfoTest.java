/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
