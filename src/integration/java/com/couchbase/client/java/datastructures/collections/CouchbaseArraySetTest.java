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
import java.util.Set;
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

public class CouchbaseArraySetTest {

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
    public void shouldRefuseAddingJsonObjectToSet() {
        CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, ctx.bucket(), null);

        try {
            set.add(JsonObject.create());
        } catch (ClassCastException e) {
            assertTrue(e.getMessage().contains("CouchbaseArraySet"));
            //success
        }
    }

    @Test
    public void shouldRefuseCreatingSetWithJsonObject() {
        Set<Object> initial = Collections.<Object>singleton(JsonObject.create());

        try {
            CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, ctx.bucket(), initial);
        } catch (ClassCastException e) {
            assertTrue(e.getMessage().contains("CouchbaseArraySet"));
            //success
        }
    }

    @Test
    public void shouldRefuseAddingJsonArrayToSet() {
        CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, ctx.bucket(), null);

        try {
            set.add(JsonArray.create());
        } catch (ClassCastException e) {
            assertTrue(e.getMessage().contains("CouchbaseArraySet"));
            //success
        }
    }

    @Test
    public void shouldRefuseCreatingSetWithJsonArray() {
        Set<Object> initial = Collections.<Object>singleton(JsonArray.create());

        try {
            CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, ctx.bucket(), initial);
        } catch (ClassCastException e) {
            assertTrue(e.getMessage().contains("CouchbaseArraySet"));
            //success
        }
    }

    @Test
    public void shouldAddCloseValuesDifferentTypes() {
        CouchbaseArraySet<Object> set = new CouchbaseArraySet<Object>(uuid, ctx.bucket());

        set.add("1");
        set.add(1);
        set.add(1.0);

        assertEquals(3, set.size());
    }

    @Test
    public void testConstructorWithPreExistingDocument() {
        JsonArrayDocument preExisting = JsonArrayDocument.create(uuid, JsonArray.from("test"));
        ctx.bucket().upsert(preExisting);

        Set<String> set = new CouchbaseArraySet(uuid, ctx.bucket());

        assertEquals(1, set.size());
        assertTrue(set.contains("test"));
    }

    @Test
    public void testConstructorWithPreExistingDocumentOfWrongTypeFails() {
        JsonDocument preExisting = JsonDocument.create(uuid, JsonObject.create().put("test", "value"));
        ctx.bucket().upsert(preExisting);

        Set<String> set = new CouchbaseArraySet(uuid, ctx.bucket());
        try {
            set.size();
            fail("Expected TranscodingException");
        } catch (TranscodingException e) {
            //expected
        }
    }

    @Test
    public void testConstructorWithCollectionDataOverwrites() {
        JsonDocument preExisting = JsonDocument.create(uuid, JsonObject.create().put("test", "value"));
        ctx.bucket().upsert(preExisting);

        Set<String> set = new CouchbaseArraySet(uuid, ctx.bucket(), Collections.singleton("foo"));

        assertEquals(1, set.size());
        assertTrue(set.contains("foo"));
    }

    @Test
    public void testConstructorWithEmptyCollectionOverwrites() {
        JsonDocument preExisting = JsonDocument.create(uuid, JsonObject.create().put("test", "value"));
        ctx.bucket().upsert(preExisting);

        Set<String> set = new CouchbaseArraySet(uuid, ctx.bucket(), Collections.emptySet());

        assertEquals(0, set.size());
    }
}