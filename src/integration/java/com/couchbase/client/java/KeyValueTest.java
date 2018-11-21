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

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.LegacyDocument;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.document.StringDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.error.RequestTooBigException;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.util.CouchbaseTestContext;
import junit.framework.TestCase;
import org.junit.Assume;
import org.junit.Test;
import rx.functions.Action0;
import rx.functions.Action1;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class KeyValueTest extends ClusterDependentTest {

    @Test
    public void shouldGetNonexistentWithDefault() {
        JsonDocument jsonDocument = bucket().get("i-dont-exist");
        assertNull(jsonDocument);
    }

    @Test(expected = DocumentAlreadyExistsException.class)
    public void shouldErrorOnDoubleInsert() {
        String id = "double-insert";
        JsonObject content = JsonObject.empty().put("hello", "world");
        final JsonDocument doc = JsonDocument.create(id, content);
        bucket().insert(doc);
        bucket().insert(doc);
    }

    @Test
    public void shouldInsertAndGet() {
        JsonObject content = JsonObject.empty().put("hello", "world");
        final JsonDocument doc = JsonDocument.create("insert", content);

        bucket().insert(doc);
        JsonDocument response = bucket().get("insert");
        assertEquals(content.getString("hello"), response.content().getString("hello"));
    }

    @Test
    public void shouldUpsertAndGetAndRemove() {
        JsonObject content = JsonObject.empty().put("hello", "world");
        final JsonDocument doc = JsonDocument.create("upsert", content);

        bucket().upsert(doc);
        JsonDocument response = bucket().get("upsert");
        assertEquals(content.getString("hello"), response.content().getString("hello"));

        assertTrue(bucket().exists("upsert"));

        JsonDocument removed = bucket().remove(doc);
        assertEquals(doc.id(), removed.id());
        assertNull(removed.content());
        assertEquals(0, removed.expiry());
        if (!CouchbaseTestContext.isMockEnabled()) {
            // a coming mock version will fix this
            assertTrue(removed.cas() != 0);
        }

        assertFalse(bucket().exists("upsert"));
        assertNull(bucket().get("upsert"));
    }

    @Test
    public void shouldRespectCASOnRemove() {
        String id = "removeWithCAS";
        JsonObject content = JsonObject.empty().put("hello", "world");
        final JsonDocument doc = JsonDocument.create(id, content);

        bucket().upsert(doc);
        JsonDocument response = bucket().get(id);
        assertEquals(content.getString("hello"), response.content().getString("hello"));

        try {
            bucket().remove(JsonDocument.create(id, null, 1231435L));
            assertTrue(false);
        } catch(CASMismatchException ex) {
            assertTrue(true);
        }

        response = bucket().get(id);
        assertEquals(content.getString("hello"), response.content().getString("hello"));

        JsonDocument removed = bucket().remove(response);
        assertEquals(removed.id(), response.id());
        assertNull(removed.content());
        if (!CouchbaseTestContext.isMockEnabled()) {
            // will be fixed in a future mock version
            assertTrue(removed.cas() != 0);
        }
        assertNotEquals(response.cas(), removed.cas());

        assertNull(bucket().get(id));
    }

  @Test
  public void shouldUpsertAndReplace() {
    JsonObject content = JsonObject.empty().put("hello", "world");
    final JsonDocument doc = JsonDocument.create("upsert-r", content);
    bucket().upsert(doc);
    JsonDocument response = bucket().get("upsert-r");
    assertEquals(content.getString("hello"), response.content().getString("hello"));

    JsonDocument updated = JsonDocument.from(response, JsonObject.empty().put("hello", "replaced"));
    bucket().replace(updated);
    response = bucket().get("upsert-r");
    assertEquals("replaced", response.content().getString("hello"));
  }

    @Test
    public void shouldIncrementFromCounter() throws Exception {
        JsonLongDocument doc1 = bucket().counter("incr-key", 10, 0, 0);
        assertEquals(0L, (long) doc1.content());

        JsonLongDocument doc2 = bucket().counter("incr-key", 10, 0, 0);
        assertEquals(10L, (long) doc2.content());

        JsonLongDocument doc3 = bucket().counter("incr-key", 10, 0, 0);
        assertEquals(20L, (long) doc3.content());

        assertNotEquals(doc1.cas(), doc2.cas());
        assertNotEquals(doc1.cas(), doc3.cas());
        assertNotEquals(doc2.cas(), doc3.cas());

        JsonLongDocument doc4 = bucket().get("incr-key", JsonLongDocument.class);
        assertEquals(20L, (long) doc4.content());
    }

    @Test
    public void shouldDecrementFromCounter() throws Exception {
        JsonLongDocument doc1 = bucket().counter("decr-key", -10, 100, 0);
        assertEquals(100L, (long) doc1.content());

        JsonLongDocument doc2 = bucket().counter("decr-key", -10, 0, 0);
        assertEquals(90L, (long) doc2.content());

        JsonLongDocument doc3 = bucket().counter("decr-key", -10, 0, 0);
        assertEquals(80L, (long) doc3.content());

        assertNotEquals(doc1.cas(), doc2.cas());
        assertNotEquals(doc1.cas(), doc3.cas());
        assertNotEquals(doc2.cas(), doc3.cas());

        JsonLongDocument doc4 = bucket().get("decr-key", JsonLongDocument.class);
        assertEquals(80L, (long) doc4.content());
    }

    @Test
    public void shouldIncrAndDecrAfterInitialUpsert() throws Exception {
        String id = "incrdecr-key";
        JsonLongDocument doc1 = bucket().upsert(JsonLongDocument.create(id, 30L));
        assertEquals(30L, (long) doc1.content());

        JsonLongDocument doc2 = bucket().get(id, JsonLongDocument.class);
        assertEquals(30L, (long) doc2.content());
        assertEquals(doc1.cas(), doc2.cas());

        JsonLongDocument doc3 = bucket().counter(id, 10);
        assertEquals(40L, (long) doc3.content());
        assertNotEquals(doc3.cas(), doc2.cas());

        JsonLongDocument doc4 = bucket().get(id, JsonLongDocument.class);
        assertEquals(40L, (long) doc4.content());
        assertEquals(doc4.cas(), doc3.cas());

        JsonLongDocument doc5 = bucket().counter(id, -20);
        assertEquals(20L, (long) doc5.content());
        assertNotEquals(doc5.cas(), doc4.cas());

        JsonLongDocument doc6 = bucket().get(id, JsonLongDocument.class);
        assertEquals(20L, (long) doc6.content());
        assertEquals(doc6.cas(), doc5.cas());
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void shouldThrowIfNotExistsIncrementing() throws Exception {
        JsonLongDocument doc1 = bucket().counter("defincr-key", 10);
        assertEquals(0, (long) doc1.content());
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void shouldThrowIfNotExistsDecrementing() throws Exception {
        JsonLongDocument doc3 = bucket().counter("defdecr-key", -10);
        assertEquals(0, (long) doc3.content());
    }

    @Test
    public void shouldGetAndTouch() throws Exception {
        String id = "get-and-touch";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(id, 3, JsonObject.empty().put("k", "v")));
        assertNotNull(upsert);
        assertEquals(id, upsert.id());

        Thread.sleep(2000);

        JsonDocument touched = bucket().getAndTouch(id, 3);
        assertEquals("v", touched.content().getString("k"));

        Thread.sleep(2000);

        touched = bucket().get(id);
        assertEquals("v", touched.content().getString("k"));
    }

    @Test
    public void shouldGetAndLock() throws Exception {
        String id = "get-and-lock";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(id, JsonObject.empty().put("k", "v")));
        assertNotNull(upsert);
        assertEquals(id, upsert.id());

        JsonDocument locked = bucket().getAndLock(id, 2);
        assertEquals("v", locked.content().getString("k"));

        try {
            bucket().upsert(JsonDocument.create(id, JsonObject.empty().put("k", "v")));
            assertTrue(false);
        } catch(CASMismatchException ex) {
            assertTrue(true);
        }

        Thread.sleep(3000);

        bucket().upsert(JsonDocument.create(id, JsonObject.empty().put("k", "v")));
    }

    @Test
    public void shouldUnlock() throws Exception {
        String key = "unlock";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")));
        assertNotNull(upsert);
        assertEquals(key, upsert.id());

        JsonDocument locked = bucket().getAndLock(key, 15);
        assertEquals("v", locked.content().getString("k"));

        try {
            bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")));
            assertTrue(false);
        } catch(CASMismatchException ex) {
            assertTrue(true);
        }

        boolean unlocked = bucket().unlock(key, locked.cas());
        assertTrue(unlocked);

        bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")));
    }

    @Test(expected = TemporaryLockFailureException.class)
    public void shouldFailUnlockWithInvalidCAS() throws Exception {
        String key = "unlockfail";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")));
        assertNotNull(upsert);
        assertEquals(key, upsert.id());

        JsonDocument locked = bucket().getAndLock(key, 15);
        assertEquals("v", locked.content().getString("k"));

        bucket().unlock(key, locked.cas() + 1);
    }

    @Test(expected = TemporaryLockFailureException.class)
    public void shouldFailDoubleLocking() throws Exception {
        String key = "doubleLockFail";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")));
        assertNotNull(upsert);
        assertEquals(key, upsert.id());

        JsonDocument locked = bucket().getAndLock(key, 15);
        assertEquals("v", locked.content().getString("k"));

        bucket().getAndLock(key, 5);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void shouldFailUnlockWhenDocDoesNotExist() throws Exception {
        bucket().unlock("thisDocDoesNotExist", 1234);
    }

    @Test
    public void shouldTouch() throws Exception {
        String key = "touch";

        bucket().upsert(JsonDocument.create(key, 3, JsonObject.empty().put("k", "v")));

        Thread.sleep(2000);

        Boolean touched = bucket().touch(key, 3);
        assertTrue(touched);

        Thread.sleep(2000);

        JsonDocument loaded = bucket().get(key);
        assertEquals("v", loaded.content().getString("k"));
    }

    @Test
    public void shouldFailTouchWhenNotExist() {
        String key = "touchFail";
        JsonDocument touchDoc = JsonDocument.create(key);

        try {
            bucket().touch(key, 3);
            fail("touch(key, expiry) with unknown key expected to fail");
        } catch (DocumentDoesNotExistException e) {
            //expected
        }

        try {
            bucket().touch(touchDoc);
            fail("touch(document) with unknown key expected to fail");
        } catch (DocumentDoesNotExistException e) {
            //expected
        }
    }

    @Test
    public void shouldPersistToMaster() {
        String key = "persist-to-master";

        bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")),
            PersistTo.MASTER, ReplicateTo.NONE);
    }

    /**
     * This is a regression test for JCBC-1223
     */
    @Test
    public void shouldPersistAsyncOnDefaultTimeout() {
        String key ="persist-to-master-json-doc";

        // First do an insert
        JsonDocument result = bucket().async().insert(JsonDocument.create(key, JsonObject.empty().put("k", "v")), PersistTo.MASTER).toBlocking().single();
        assertEquals(key, result.id());
        assertTrue(result.cas() != 0);

        // Then an upsert
        result = bucket().async().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")), PersistTo.MASTER).toBlocking().single();
        assertEquals(key, result.id());
        assertTrue(result.cas() != 0);

        // Then a replace
        result = bucket().async().replace(JsonDocument.create(key, JsonObject.empty().put("k", "v")), PersistTo.MASTER).toBlocking().single();
        assertEquals(key, result.id());
        if (!CouchbaseTestContext.isMockEnabled()) {
            // a coming mock version will fix this
            assertTrue(result.cas() != 0);
        }

        // Then a remove
        result = bucket().async().remove(key, PersistTo.MASTER).toBlocking().single();
        assertEquals(key, result.id());
        if (!CouchbaseTestContext.isMockEnabled()) {
            // will be fixed in a future mock version
            assertTrue(result.cas() != 0);
        }

        // Test with counter
        key = "persist-to-master-counter-doc";

        JsonLongDocument cresult = bucket().async().counter(key, 1, 1, PersistTo.MASTER).toBlocking().single();
        assertEquals(1, (long) cresult.content());
        assertTrue(cresult.cas() != 0);

        // Test with append and prepend
        key = "persist-to-master-string-doc";

        bucket().insert(StringDocument.create(key, "init"));

        StringDocument aresult = bucket().async().append(StringDocument.create(key, "foo"), PersistTo.MASTER).toBlocking().single();
        assertTrue(aresult.cas() != 0);

        aresult = bucket().async().prepend(StringDocument.create(key, "foo"), PersistTo.MASTER).toBlocking().single();
        assertTrue(aresult.cas() != 0);
    }

    @Test
    public void shouldRemoveFromMaster() {
        String key = "remove-from-master";

        bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")),
            PersistTo.MASTER, ReplicateTo.NONE);

        bucket().remove(key, PersistTo.MASTER, ReplicateTo.NONE);
    }

    @Test
    public void shouldUpsertLegacyObjectDocument() {
        String id = "legacy-upsert";
        User user = new User("Michael");
        LegacyDocument doc = LegacyDocument.create(id, user);
        bucket().upsert(doc);

        LegacyDocument found = bucket().get(id, LegacyDocument.class);
        assertEquals(found.content().getClass(), user.getClass());
        assertEquals("Michael", ((User) found.content()).getFirstname());
    }

    @Test
    public void shouldAppendString() {
        String id ="append1";
        String value = "foo";

        LegacyDocument doc = LegacyDocument.create(id, value);
        bucket().upsert(doc);

        LegacyDocument stored = bucket().append(LegacyDocument.create(id, "bar"));
        assertEquals(id, stored.id());
        assertNull(stored.content());
        assertTrue(stored.cas() != 0);
        assertTrue(stored.expiry() == 0);

        LegacyDocument found = bucket().get(id, LegacyDocument.class);
        assertEquals("foobar", found.content());
    }

    @Test
    public void shouldPrependString() {
        String id ="prepend1";
        String value = "bar";

        LegacyDocument doc = LegacyDocument.create(id, value);
        bucket().upsert(doc);

        LegacyDocument stored = bucket().prepend(LegacyDocument.create(id, "foo"));
        assertEquals(id, stored.id());
        assertNull(stored.content());
        assertTrue(stored.cas() != 0);
        assertTrue(stored.expiry() == 0);

        LegacyDocument found = bucket().get(id, LegacyDocument.class);
        assertEquals("foobar", found.content());
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void shouldFailOnNonExistingAppend() {
        LegacyDocument doc = LegacyDocument.create("appendfail", "fail");
        bucket().append(doc);
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void shouldFailOnNonExistingPrepend() {
        LegacyDocument doc = LegacyDocument.create("prependfail", "fail");
        bucket().prepend(doc);
    }

    @Test
    public void shouldStoreAndLoadRawJsonDocument() {
        String id = "jsonRaw";
        String content = "{\"foo\": 1234}";

        bucket().insert(RawJsonDocument.create(id, content));

        RawJsonDocument foundRaw = bucket().get(id, RawJsonDocument.class);
        assertEquals(content, foundRaw.content());

        JsonDocument foundParsed = bucket().get(id);
        assertEquals(1234, (int) foundParsed.content().getInt("foo"));
    }

    @Test(expected = RequestTooBigException.class)
    public void shouldFailStoringLargeDoc() {
        int size = 21000000;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < size; i++) {
            buffer.append('l');
        }

        bucket().upsert(RawJsonDocument.create("tooLong", buffer.toString()));
    }

    @Test(expected = RequestTooBigException.class)
    public void shouldFailAppendWhenTooLarge() {
        String id = "longAppend";

        int size = 5000000;
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < size; i++) {
            chunk.append('l');
        }

        bucket().upsert(StringDocument.create(id, "a"));

        for(int i = 0; i < 5; i++) {
            bucket().append(StringDocument.create(id,chunk.toString()));
        }
    }

    @Test(expected = RequestTooBigException.class)
    public void shouldFailPrependWhenTooLarge() {
        String id = "longPrepend";

        int size = 5000000;
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < size; i++) {
            chunk.append('l');
        }

        bucket().upsert(StringDocument.create(id, "a"));

        for(int i = 0; i < 5; i++) {
            bucket().prepend(StringDocument.create(id, chunk.toString()));
        }
    }

    @Test(expected = DurabilityException.class)
    public void shouldFailUpsertWithDurabilityException() {
        int replicaCount = bucketManager().info().replicaCount();
        Assume.assumeTrue(replicaCount < 3);

        bucket().upsert(JsonDocument.create("upsertSome", JsonObject.create()), ReplicateTo.THREE);
    }

    @Test(expected = DurabilityException.class)
    public void shouldFailInsertWithDurabilityException() {
        int replicaCount = bucketManager().info().replicaCount();
        Assume.assumeTrue(replicaCount < 3);

        bucket().insert(JsonDocument.create("insertSome", JsonObject.create()), ReplicateTo.THREE);
    }

    @Test(expected = DurabilityException.class)
    public void shouldFailReplaceWithDurabilityException() {
        int replicaCount = bucketManager().info().replicaCount();
        Assume.assumeTrue(replicaCount < 3);

        bucket().upsert(JsonDocument.create("replaceSome", JsonObject.create()));
        bucket().replace(JsonDocument.create("replaceSome", JsonObject.create()), ReplicateTo.THREE);
    }

    @Test(expected = DurabilityException.class)
    public void shouldFailRemoveWithDurabilityException() {
        int replicaCount = bucketManager().info().replicaCount();
        Assume.assumeTrue(replicaCount < 3);

        bucket().upsert(JsonDocument.create("removeSome", JsonObject.create()));
        bucket().replace(JsonDocument.create("removeSome", JsonObject.create()), ReplicateTo.THREE);
    }

    @Test(expected = CASMismatchException.class)
    public void shouldFailWithInvalidCASOnAppend() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        StringDocument stored = bucket().upsert(StringDocument.create("appendCasMismatch", "foo"));
        bucket().append(StringDocument.from(stored, stored.cas() + 1));
    }

    @Test(expected = CASMismatchException.class)
    public void shouldFailWithInvalidCASOnPrepend() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        StringDocument stored = bucket().upsert(StringDocument.create("prependCasMismatch", "foo"));
        bucket().prepend(StringDocument.from(stored, stored.cas() + 1));
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void shouldFailOnRemoveWhenNotExists() {
        bucket().remove("thisDocumentDoesNotExist");
    }

    static class User implements Serializable {

        private final String firstname;

        User(String firstname) {
            this.firstname = firstname;
        }

        public String getFirstname() {
            return firstname;
        }
    }

    @Test(expected = CASMismatchException.class)
    public void shouldHonorCASOnRemoveWithDurability() {
        JsonDocument toStore = JsonDocument.create("casWithDurability", JsonObject.create().put("initial", true));
        assertTrue(toStore.cas() == 0);

        JsonDocument stored = bucket().insert(toStore, PersistTo.MASTER);
        assertTrue(stored.cas() != 0);

        bucket().remove(JsonDocument.from(stored, stored.cas() + 1), PersistTo.MASTER);
    }

    @Test
    public void shouldStoreAndGetKeyWithUtf16Char() {
        String key = "utf16\uD834\uDD1E"; //a nice little musical key
        JsonDocument toStore = JsonDocument.create(key, JsonObject.create().put("UTF8", true).put("hasUTF16", true));

        JsonDocument inserted = bucket().upsert(toStore);
        JsonDocument retrieved = bucket().get(key);

        assertNotNull(inserted);
        assertNotNull(retrieved);
        assertEquals(key, inserted.id());
        assertEquals(key, retrieved.id());
        assertEquals(inserted.content(), retrieved.content());
    }

    @Test
    public void shouldStoreAndGetBigInteger() {
        BigInteger bigint = new BigInteger("12345678901234567890432423432324");
        JsonObject original = JsonObject
            .create()
            .put("value", bigint);

        bucket().upsert(JsonDocument.create("bigIntegerDoc", original));

        RawJsonDocument rawLoaded = bucket().get("bigIntegerDoc", RawJsonDocument.class);
        assertEquals("{\"value\":12345678901234567890432423432324}", rawLoaded.content());

        JsonDocument loaded = bucket().get("bigIntegerDoc");
        assertEquals(bigint, loaded.content().getBigInteger("value"));
    }

    @Test
    public void shouldStoreAndGetBigDecimal() {
        assumeFalse(CouchbaseTestContext.isCi());

        BigDecimal bigdec = new BigDecimal("12345.678901234567890432423432324");
        JsonObject original = JsonObject
            .create()
            .put("value", bigdec);

        bucket().upsert(JsonDocument.create("bigDecimalDoc", original));

        RawJsonDocument rawLoaded = bucket().get("bigDecimalDoc", RawJsonDocument.class);
        assertEquals("{\"value\":12345.678901234567890432423432324}", rawLoaded.content());

        // Precision loss through double storage, use com.couchbase.json.decimalForFloat for higher
        // precision.
        JsonDocument loaded = bucket().get("bigDecimalDoc");
        assertEquals(
            new BigDecimal("12345.678901234567092615179717540740966796875"),
            loaded.content().getBigDecimal("value")
        );
        assertEquals(12345.678901234567, loaded.content().getDouble("value"), 0);
    }

    @Test
    public void shouldNotSendTimedOutSyncOperations() {
        String key = "TimedOutSync";
        int incrementCount = 6000;
        bucket().upsert(JsonLongDocument.create(key, 0L));
        for (int i=0; i< incrementCount; i++) {
            try {
                bucket().counter(key, 1, 1, TimeUnit.MICROSECONDS);
            } catch(RuntimeException ex) {
                //ignore
            }
        }
        try {
            Thread.sleep(1000);
        } catch(InterruptedException ex) {
            //ignore
        }
        assert(bucket().get(key, JsonLongDocument.class).content() < incrementCount);
    }

    @Test
    public void shouldNotSendTimedOutAsyncOperations() {
        String key = "TimedOutAsync";
        int incrementCount = 6000;
        bucket().upsert(JsonLongDocument.create(key, 0L));
        final CountDownLatch latch = new CountDownLatch(incrementCount);
        for (int i=0; i< incrementCount; i++) {
            try {
                bucket().async().counter(key, 1)
                        .timeout(0, TimeUnit.SECONDS)
                        .subscribe(
                            new Action1<JsonLongDocument>() {
                                @Override
                                public void call(JsonLongDocument doc) {
                                    //ignore
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable err) {
                                   latch.countDown();
                                }
                            }, new Action0() {
                                @Override
                                public void call() {
                                    latch.countDown();
                                }
                            });
            } catch(RuntimeException ex) {
                //ignore
            }
        }
        try {
            latch.await();
            Thread.sleep(1000);
        } catch(InterruptedException ex) {
            //ignore
        }
        assert(bucket().get(key, JsonLongDocument.class).content() < incrementCount);
    }

}
