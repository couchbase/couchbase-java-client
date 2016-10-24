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

import static org.junit.Assert.*;

import com.couchbase.client.java.error.subdoc.PathInvalidException;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.datastructures.MutationOptionBuilder;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.RequestTooBigException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import org.junit.*;

public class DataStructuresTest {

    private static CouchbaseTestContext ctx;

    @BeforeClass
    public static void connect() throws Exception {
        ctx = CouchbaseTestContext.builder()
                .bucketQuota(100)
                .bucketReplicas(1)
                .bucketType(BucketType.COUCHBASE)
                .build();

        ctx.ignoreIfMissing(CouchbaseFeature.SUBDOC);
    }

    @Before
    public void init() throws Exception {
        ctx.bucket().mapAdd("dsmap", "1", "1");
        ctx.bucket().mapAdd("dsmapFull", "1", "1");
        ctx.bucket().listPush("dslist", "1");
        ctx.bucket().listPush("dslistFull", "1");
        ctx.bucket().setAdd("dsset", 1);
        ctx.bucket().setAdd("dssetFull", 1);
        ctx.bucket().queueAdd("dsqueue", 1);
        ctx.bucket().queueAdd("dsqueueFull", 1);
    }

    @AfterClass
    public static void disconnect() throws InterruptedException {
        ctx.destroyBucketAndDisconnect();
    }

    @After
    public void cleanup() throws Exception {
        ctx.bucket().remove("dsmap");
        ctx.bucket().remove("dsmapFull");
        ctx.bucket().remove("dslist");
        ctx.bucket().remove("dslistFull");
        ctx.bucket().remove("dsset");
        ctx.bucket().remove("dssetFull");
        ctx.bucket().remove("dsqueue");
        ctx.bucket().remove("dsqueueFull");
    }

    @Test
    public void testMap() {
        ctx.bucket().mapAdd("dsmap", "foo", "bar");
        String myval = ctx.bucket().mapGet("dsmap", "foo", String.class);
        assertEquals(myval, "bar");
        boolean result = ctx.bucket().mapRemove("dsmap", "foo");
        assertEquals(result, true);
        result = ctx.bucket().mapRemove("dsmap", "foo");
        assertEquals(result, true);
        int size = ctx.bucket().mapSize("dsmap");
        ctx.bucket().mapAdd("dsmap", "foo", "bar", MutationOptionBuilder.builder().persistTo(PersistTo.MASTER));
        int newSize = ctx.bucket().mapSize("dsmap");
        assert (newSize == size + 1);
        result = ctx.bucket().mapAdd("dsmap", "foo", null);
        assertEquals(result, true);
        result = ctx.bucket().mapAdd("dsmap", "foo", 10);
        assertEquals(result, true);
    }

    @Test(expected = CouchbaseException.class)
    public void testMapGetInvalidKey() {
        ctx.bucket().mapGet("dsmap", "9999", String.class);
    }


    @Test(expected = CASMismatchException.class)
    public void testMapSetCasMismatch() {
        JsonDocument document = ctx.bucket().get("dsmap");
        ctx.bucket().mapAdd("dsmap", "foo", "bar", MutationOptionBuilder.builder().cas(document.cas() + 1));
    }

    @Test(expected = RequestTooBigException.class)
    public void testMapRequestTooBigException() {
        char[] data = new char[5000000];
        String str = new String(data);
        boolean result = ctx.bucket().mapAdd("dsmapFull", "foo", str, MutationOptionBuilder.builder().persistTo(PersistTo.MASTER));
    }

    @Test(expected = RuntimeException.class)
    public void testSyncMapAddTimeout() {
        ctx.bucket().mapAdd("dsmap", "timeout", "timeout", 1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testMapGetOnNonExistentDocument() {
        ctx.bucket().mapGet("dsmapRandom", "foo", String.class);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testMapSizeOnNonExistentDocument() {
        ctx.bucket().mapSize("dsmapRandom");
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testMapExpiry() {
        ctx.bucket().mapAdd("dsmapShortLived", "1", "1", MutationOptionBuilder.builder().expiry(1));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        ctx.bucket().mapSize("dsmapShortLived");
    }

    @Test
    public void testList() {
        ctx.bucket().listPush("dslist", "foo");
        String myval = ctx.bucket().listGet("dslist", 1, String.class);
        assertEquals(myval, "foo");
        ctx.bucket().listShift("dslist", null);
        assertNull(ctx.bucket().listGet("dslist", 0, Object.class));
        ctx.bucket().listSet("dslist", 1, JsonArray.create().add("baz"));
        JsonArray array = ctx.bucket().listGet("dslist", 1, JsonArray.class);
        assertEquals(array.get(0), "baz");
        ctx.bucket().listSet("dslist", 1, JsonObject.create().put("foo", "bar"));
        JsonObject object = ctx.bucket().listGet("dslist", 1, JsonObject.class);
        assertEquals(object.get("foo"), "bar");
        ctx.bucket().listSet("dslist", 3, 10);
        int intVal = ctx.bucket().listGet("dslist", 3, Integer.class);
        assertEquals(intVal, 10);
        int size = ctx.bucket().listSize("dslist");
        assert (size > 0);
        ctx.bucket().listRemove("dslist", 1);
        int newSize = ctx.bucket().listSize("dslist");
        assertEquals(size - 1, newSize);
        while (newSize > 1) {
            ctx.bucket().listRemove("dslist", 1);
            newSize = ctx.bucket().listSize("dslist");
        }
    }

    @Test(expected = PathInvalidException.class)
    public void testListGetInvalidIndex() {
        ctx.bucket().listGet("dslist", -99999, Object.class);
    }

    @Test(expected = CouchbaseException.class)
    public void testListSetInvalidIndex() {
        ctx.bucket().listSet("dslist", -10, "bar");
    }

    @Test(expected = CouchbaseException.class)
    public void testListRemoveInvalidIndex() {
        ctx.bucket().listGet("dslist", -99999, Object.class);
    }


    @Test(expected = CouchbaseException.class)
    public void testListRemoveNonExistentIndex() {
        ctx.bucket().listGet("dslist", 2, Object.class);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testListRemoveOnNonExistentDocument() {
        ctx.bucket().listRemove("dslistRandom", 2);
    }


    @Test(expected = DocumentDoesNotExistException.class)
    public void testListSizeOnNonExistentDocument() {
        ctx.bucket().async().listSize("dslistRandom").toBlocking().single();
    }

    @Test(expected = RequestTooBigException.class)
    public void testListFullException() {
        char[] data = new char[5000000];
        String str = new String(data);
        boolean result = ctx.bucket().listShift("dslistFull", str, MutationOptionBuilder.builder().persistTo(PersistTo.MASTER));
        assertEquals(result, true);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testListExpiry() {
        ctx.bucket().listPush("dslistShortLived", "1", MutationOptionBuilder.builder().expiry(1));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        ctx.bucket().listSize("dslistShortLived");
    }

    @Test
    public void testQueue() {
        Object first = ctx.bucket().queueRemove("dsqueue", Object.class);
        assertNotNull(first);
        boolean result = ctx.bucket().queueAdd("dsqueue", "val1");
        assert (result == true);
        result = ctx.bucket().queueAdd("dsqueue", "val2");
        assert (result == true);
        String val = ctx.bucket().queueRemove("dsqueue", String.class);
        assertEquals(val, "val1");
        val = ctx.bucket().queueRemove("dsqueue", String.class);
        assertEquals(val, "val2");
        ctx.bucket().queueAdd("dsqueue", null);
        assertNull(ctx.bucket().queueRemove("dsqueue", null));
    }

    @Test
    public void testQueueEmptyRemove() {
        int size = ctx.bucket().queueSize("dsqueue");
        while (size > 0) {
            ctx.bucket().queueRemove("dsqueue", Object.class);
            size = ctx.bucket().queueSize("dsqueue");
        }
        assertEquals(ctx.bucket().queueRemove("dsqueue", Object.class), null);
    }

    @Test(expected = CASMismatchException.class)
    public void testQueueAddCasMismatch() {
        JsonArrayDocument document = ctx.bucket().get("dsqueue", JsonArrayDocument.class);
        ctx.bucket().queueAdd("dsqueue", "casElement", MutationOptionBuilder.builder().cas(document.cas() + 1));
    }

    @Test(expected = RequestTooBigException.class)
    public void testQueueFullException() {
        char[] data = new char[5000000];
        String str = new String(data);
        boolean result = ctx.bucket().queueAdd("dsqueueFull", str, MutationOptionBuilder.builder().persistTo(PersistTo.MASTER));
        assertEquals(result, true);
    }

    @Test(expected = RuntimeException.class)
    public void testSyncQueueAddTimeout() {
        ctx.bucket().queueAdd("dsqueue", "timeout", 1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testQueueRemoveOnNonExistentDocument() {
        ctx.bucket().queueRemove("dsqueueRandom", String.class);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testQueueSizeOnNonExistentDocument() {
        ctx.bucket().queueSize("dsqueueRandom");
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testQueueExpiry() {
        ctx.bucket().queueAdd("dsqueueShortLived", "1", MutationOptionBuilder.builder().expiry(1));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        ctx.bucket().queueSize("dsqueueShortLived");
    }

    @Test
    public void testSet() {
        boolean result = ctx.bucket().setAdd("dsset", "foo");
        assertEquals(result, true);
        result = ctx.bucket().setAdd("dsset", "foo");
        assertEquals(result, false);
        String val = ctx.bucket().setRemove("dsset", "foo");
        assertEquals(val, "foo");
        result = ctx.bucket().setAdd("dsset", "foo");
        assertEquals(result, true);
        result = ctx.bucket().setExists("dsset", "foo");
        assertEquals(result, true);
        ctx.bucket().setRemove("dsset", "foo");
        String element = ctx.bucket().setRemove("dsset", "foo");
        assertEquals(element, "foo");
        result = ctx.bucket().setExists("dsset", "foo");
        assertEquals(result, false);
        result = ctx.bucket().setAdd("dsset", null);
        assertEquals(result, true);
        assertEquals(ctx.bucket().setExists("dsset", null), true);
        assertNull(ctx.bucket().setRemove("dsset", null));
        result = ctx.bucket().setAdd("dsset", 2);
        assertEquals(result, true);
        result = ctx.bucket().setAdd("dsset", 2);
        assertEquals(result, false);
        ctx.bucket().setRemove("dsset", 2);
    }

    @Test
    public void testSetEmptyRemove() {
        assertEquals(ctx.bucket().setRemove("dsqueue", "1"), "1");
        assertEquals(ctx.bucket().setRemove("dsqueue", "2"), "2");
    }

    @Test(expected = CASMismatchException.class)
    public void testSetAddCasMismatch() {
        JsonArrayDocument document = ctx.bucket().get("dsset", JsonArrayDocument.class);
        ctx.bucket().setAdd("dsset", "casElement", MutationOptionBuilder.builder().cas(document.cas() + 1));
    }

    @Test(expected = RequestTooBigException.class)
    public void testSetFullException() {
        char[] data = new char[5000000];
        String str = new String(data);
        boolean result = ctx.bucket().setAdd("dssetFull", str, MutationOptionBuilder.builder().persistTo(PersistTo.MASTER));
        assertEquals(result, true);
    }

    @Test(expected = RuntimeException.class)
    public void testSyncSetAddTimeout() {
        ctx.bucket().setAdd("dsset", "timeout", 1, TimeUnit.NANOSECONDS);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testSetExpiry() {
        ctx.bucket().setAdd("dssetShortLived", "1", MutationOptionBuilder.builder().expiry(1));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        ctx.bucket().setSize("dssetShortLived");
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testSetRemoveOnNonExistentDocument() {
        ctx.bucket().setRemove("dssetRandom", "foo");
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void testSetSizeOnNonExistentDocument() {
        ctx.bucket().setSize("dssetRandom");
    }
}