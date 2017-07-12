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
package com.couchbase.client.java.datastructures.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CouchbaseMapTest {

    private static CouchbaseTestContext ctx;

    @BeforeClass
    public static void setup() {
        ctx = CouchbaseTestContext.builder()
            .bucketQuota(100)
            .bucketReplicas(1)
            .bucketType(BucketType.COUCHBASE)
            .build();

        ctx.ignoreIfMissing(CouchbaseFeature.SUBDOC);
    }

    @AfterClass
    public static void teardown() {
        ctx.destroyBucketAndDisconnect();
    }

    private String uuid;

    @Before
    public void generateId() {
        uuid = uuid();
    }

    @After
    public void deleteDoc() {
        try {
            ctx.bucket().remove(uuid);
        } catch (DocumentDoesNotExistException e) {
            //ignore
        }
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void testConstructorWithPreExistingDocument() {
        JsonDocument preExisting = JsonDocument.create(uuid, JsonObject.create().put("test", 123).put("foo", "bar"));
        ctx.bucket().upsert(preExisting);

        Map<String, Object> map = new CouchbaseMap<Object>(uuid, ctx.bucket());

        assertEquals(2, map.size());
        assertTrue(map.containsKey("foo"));
        assertTrue(map.containsValue(123));
    }

    @Test
    public void testConstructorWithPreExistingDocumentOfWrongTypeFails() {
        JsonArrayDocument preExisting = JsonArrayDocument.create(uuid, JsonArray.from("test"));
        ctx.bucket().upsert(preExisting);

        Map<String, Object> map = new CouchbaseMap<Object>(uuid, ctx.bucket());
        try {
            map.size();
            fail("Expected TranscodingException");
        } catch (TranscodingException e) {
            //expected
        }
    }

    @Test
    public void testConstructorWithCollectionDataOverwrites() {
        JsonArrayDocument preExisting = JsonArrayDocument.create(uuid, JsonArray.from("test", "test2"));
        ctx.bucket().upsert(preExisting);

        Map<String, Object> map = new CouchbaseMap<Object>(uuid, ctx.bucket(), Collections.singletonMap("foo", "bar"));

        assertEquals(1, map.size());
        assertTrue(map.containsKey("foo"));
        assertEquals("bar", map.get("foo"));
    }

    @Test
    public void testConstructorWithEmptyCollectionOverwrites() {
        JsonArrayDocument preExisting = JsonArrayDocument.create(uuid, JsonArray.from("test", "test2"));
        ctx.bucket().upsert(preExisting);

        Map<String, Object> map = new CouchbaseMap<Object>(uuid, ctx.bucket(), Collections.<String, Object>emptyMap());

        assertEquals(0, map.size());
    }
}