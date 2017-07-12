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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import static org.junit.Assert.*;

public class CouchbaseArrayListTest {

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
    public void size() throws Exception {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket());
        assertEquals(0, list.size());
    }

    @Test
    public void isEmpty() throws Exception {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket());
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldAdd() throws Exception {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket(), Collections.<Object>emptyList());
        assertFalse(list.contains("foobar"));
        list.add("foobar");
        assertTrue(list.contains("foobar"));
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
    }

    @Test
    public void shouldRemoveByValue() throws Exception {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket(), "foo", "bar");
        assertTrue(list.contains("foo"));
        assertTrue(list.contains("bar"));
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());

        assertFalse(list.contains("blarb"));
        assertFalse(list.remove("blarb"));
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());

        assertTrue(list.remove("foo"));
        assertFalse(list.contains("foo"));
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());

        assertTrue(list.remove("bar"));
        assertFalse(list.contains("bar"));
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void shouldRemoveByIndex() {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket(), "foo", "bar", "baz", true);

        assertEquals(4, list.size());
        list.remove(1);
        assertEquals(3, list.size());
        assertEquals("baz", list.get(1));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldThrowOnOutOfBoundsRemove() {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket(), "foo", "bar");

        list.remove(14334324);
    }

    @Test
    public void shouldReturnIterator() {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket(), "foo", "bar");

        Iterator<Object> iter = list.iterator();
        int i = 0;
        while(iter.hasNext()) {
            Object obj = iter.next();
            assertTrue(obj instanceof String);
            switch(i) {
                case 0:
                    assertEquals("foo", obj);
                    break;
                case 1:
                    assertEquals("bar", obj);
                    break;
            }
            i++;
        }
        assertEquals(2, i);
    }

    @Test
    public void shouldClear() {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket(), "foo", "bar");
        assertEquals(2, list.size());

        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    //since clear is optimized, also test the iterator removal of the whole list
    @Test
    public void shouldClearViaIterator() {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket(), "foo", "bar");
        assertEquals(2, list.size());

        Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            assertNotNull(next);
            iterator.remove();
        }

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void shouldIteratorRemoveInReverseOrder() {
        List<String> list = new CouchbaseArrayList<String>(uuid, ctx.bucket(), "keep", "foo", "bar", "baz", "foobar");
        assertEquals(5, list.size());

        ListIterator<String> iterator = list.listIterator();
        while (iterator.hasNext()) iterator.next();
        //at "foobar"
        iterator.remove(); //-foobar
        assertEquals("baz", iterator.previous()); //at "baz"
        assertEquals("bar", iterator.previous()); //at "bar"
        iterator.remove(); //-bar
        assertEquals("foo", iterator.previous()); //at "foo"
        iterator.remove(); //-foo
        assertEquals("baz", iterator.next()); //at "baz"
        iterator.remove(); //-baz

        assertEquals(1, list.size());
        assertEquals("keep", list.get(0));
    }

    @Test
    public void shouldGet() {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket());
        list.add("foo");
        list.add("bar");

        assertEquals("foo", list.get(0));
        assertEquals("bar", list.get(1));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldFailOnOutOfBoundsGet() {
        List<Object> list = new CouchbaseArrayList(uuid, ctx.bucket());
        list.get(4234324);
    }

    @Test
    public void testConstructorWithPreExistingDocument() {
        JsonArrayDocument preExisting = JsonArrayDocument.create(uuid, JsonArray.from("test"));
        ctx.bucket().upsert(preExisting);

        List<String> list = new CouchbaseArrayList(uuid, ctx.bucket());

        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void testConstructorWithPreExistingDocumentOfWrongTypeFails() {
        JsonDocument preExisting = JsonDocument.create(uuid, JsonObject.create().put("test", "value"));
        ctx.bucket().upsert(preExisting);

        List<String> list = new CouchbaseArrayList(uuid, ctx.bucket());
        try {
            list.size();
            fail("Expected TranscodingException");
        } catch (TranscodingException e) {
            //expected
        }
    }

    @Test
    public void testConstructorWithVarargDataOverwrites() {
        JsonDocument preExisting = JsonDocument.create(uuid, JsonObject.create().put("test", "value"));
        ctx.bucket().upsert(preExisting);

        List<String> list = new CouchbaseArrayList(uuid, ctx.bucket(), "foo");

        assertEquals(1, list.size());
        assertEquals("foo", list.get(0));
    }

    @Test
    public void testConstructorWithCollectionDataOverwrites() {
        JsonDocument preExisting = JsonDocument.create(uuid, JsonObject.create().put("test", "value"));
        ctx.bucket().upsert(preExisting);

        List<String> list = new CouchbaseArrayList(uuid, ctx.bucket(), Collections.singletonList("foo"));

        assertEquals(1, list.size());
        assertEquals("foo", list.get(0));
    }

    @Test
    public void testConstructorWithEmptyCollectionOverwrites() {
        JsonDocument preExisting = JsonDocument.create(uuid, JsonObject.create().put("test", "value"));
        ctx.bucket().upsert(preExisting);

        List<String> list = new CouchbaseArrayList(uuid, ctx.bucket(), Collections.emptyList());

        assertEquals(0, list.size());
    }

    @Test
    public void shouldAcceptAllJsonValueCompatibleTypes() {
        List<Object> list = new CouchbaseArrayList<Object>(uuid, ctx.bucket());

        JsonObject sub1 = JsonObject.create().put("foo", "bar").put("value", 4);
        JsonArray sub2 = JsonArray.from("A", "B", 5);

        list.add("someString");
        list.add(123);
        list.add(4.56);
        list.add(null);
        list.add(true);
        list.add(sub1);
        list.add(sub2);

        assertEquals(7, list.size());
        assertTrue(list.contains(sub1));
        assertTrue(list.contains(sub2));
        assertTrue(list.contains(null));
    }

}
