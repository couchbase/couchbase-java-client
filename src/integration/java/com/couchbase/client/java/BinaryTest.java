/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.LegacyDocument;
import com.couchbase.client.java.document.LongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class BinaryTest extends ClusterDependentTest {

    @Test(expected = NoSuchElementException.class)
    public void shouldGetNonexistentAndFail() {
        bucket().get("i-dont-exist").toBlocking().single();
    }

    @Test
    public void shouldGetNonexistentWithDefault() {
        JsonDocument jsonDocument = bucket().get("i-dont-exist").toBlocking().singleOrDefault(null);
        assertNull(jsonDocument);
    }

    @Test(expected = DocumentAlreadyExistsException.class)
    public void shouldErrorOnDoubleInsert() {
        String id = "double-insert";
        JsonObject content = JsonObject.empty().put("hello", "world");
        final JsonDocument doc = JsonDocument.create(id, content);
        bucket().insert(doc).toBlocking().single();
        bucket().insert(doc).toBlocking().single();
    }

    @Test
    public void shouldInsertAndGet() {
        JsonObject content = JsonObject.empty().put("hello", "world");
        final JsonDocument doc = JsonDocument.create("insert", content);
        JsonDocument response = bucket()
            .insert(doc)
            .flatMap(new Func1<JsonDocument, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(JsonDocument document) {
                    return bucket().get("insert");
                }
            })
            .toBlocking()
            .single();
        assertEquals(content.getString("hello"), response.content().getString("hello"));
    }

  @Test
  public void shouldUpsertAndGet() {
    JsonObject content = JsonObject.empty().put("hello", "world");
    final JsonDocument doc = JsonDocument.create("upsert", content);
    JsonDocument response = bucket().upsert(doc)
      .flatMap(new Func1<JsonDocument, Observable<JsonDocument>>() {
        @Override
        public Observable<JsonDocument> call(JsonDocument document) {
          return bucket().get("upsert");
        }
      })
      .toBlocking()
      .single();
    assertEquals(content.getString("hello"), response.content().getString("hello"));
  }

  @Test
  public void shouldUpsertAndReplace() {
    JsonObject content = JsonObject.empty().put("hello", "world");
    final JsonDocument doc = JsonDocument.create("upsert-r", content);
    JsonDocument response = bucket().upsert(doc)
      .flatMap(new Func1<JsonDocument, Observable<JsonDocument>>() {
        @Override
        public Observable<JsonDocument> call(JsonDocument document) {
          return bucket().get("upsert-r");
        }
      })
      .toBlocking()
      .single();
    assertEquals(content.getString("hello"), response.content().getString("hello"));

    JsonDocument updated = JsonDocument.from(response, JsonObject.empty().put("hello", "replaced"));
    response = bucket().replace(updated)
      .flatMap(new Func1<JsonDocument, Observable<JsonDocument>>() {
        @Override
        public Observable<JsonDocument> call(JsonDocument document) {
          return bucket().get("upsert-r");
        }
      })
      .toBlocking()
      .single();
    assertEquals("replaced", response.content().getString("hello"));
  }

    @Test
    public void shouldLoadMultipleDocuments() throws Exception {
        BlockingObservable<JsonDocument> observable = Observable
          .just("doc1", "doc2", "doc3")
          .flatMap(new Func1<String, Observable<JsonDocument>>() {
              @Override
              public Observable<JsonDocument> call(String id) {
                  return bucket().get(id);
              }
          }).toBlocking();


        final AtomicInteger counter = new AtomicInteger();
        observable.forEach(new Action1<JsonDocument>() {
            @Override
            public void call(JsonDocument document) {
                counter.incrementAndGet();
            }
        });
        assertEquals(0, counter.get());
    }

    @Test
    public void shouldIncrementFromCounter() throws Exception {
        LongDocument doc1 = bucket().counter("incr-key", 10, 0, 0).toBlocking().single();
        assertEquals(0L, (long) doc1.content());

        LongDocument doc2 = bucket().counter("incr-key", 10, 0, 0).toBlocking().single();
        assertEquals(10L, (long) doc2.content());

        LongDocument doc3 = bucket().counter("incr-key", 10, 0, 0).toBlocking().single();
        assertEquals(20L, (long) doc3.content());

        assertTrue(doc1.cas() != doc2.cas());
        assertTrue(doc2.cas() != doc1.cas());
    }

    @Test
    public void shouldDecrementFromCounter() throws Exception {
        LongDocument doc1 = bucket().counter("decr-key", -10, 100, 0).toBlocking().single();
        assertEquals(100L, (long) doc1.content());

        LongDocument doc2 = bucket().counter("decr-key", -10, 0, 0).toBlocking().single();
        assertEquals(90L, (long) doc2.content());

        LongDocument doc3 = bucket().counter("decr-key", -10, 0, 0).toBlocking().single();
        assertEquals(80L, (long) doc3.content());

        assertTrue(doc1.cas() != doc2.cas());
        assertTrue(doc2.cas() != doc1.cas());
    }

    @Test
    public void shouldGetAndTouch() throws Exception {
        String id = "get-and-touch";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(id, JsonObject.empty().put("k", "v"), 3))
            .toBlocking().single();
        assertNotNull(upsert);
        assertEquals(id, upsert.id());

        Thread.sleep(2000);

        JsonDocument touched = bucket().getAndTouch(id, 3).toBlocking().single();
        assertEquals("v", touched.content().getString("k"));

        Thread.sleep(2000);

        touched = bucket().get(id).toBlocking().single();
        assertEquals("v", touched.content().getString("k"));
    }

    @Test
    public void shouldGetAndLock() throws Exception {
        String id = "get-and-lock";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(id, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
        assertNotNull(upsert);
        assertEquals(id, upsert.id());

        JsonDocument locked = bucket().getAndLock(id, 2).toBlocking().single();
        assertEquals("v", locked.content().getString("k"));

        try {
            bucket().upsert(JsonDocument.create(id, JsonObject.empty().put("k", "v")))
                .toBlocking().single();
            assertTrue(false);
        } catch(CASMismatchException ex) {
            assertTrue(true);
        }

        Thread.sleep(3000);

        bucket().upsert(JsonDocument.create(id, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
    }

    @Test
    public void shouldUnlock() throws Exception {
        String key = "unlock";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
        assertNotNull(upsert);
        assertEquals(key, upsert.id());

        JsonDocument locked = bucket().getAndLock(key, 15).toBlocking().single();
        assertEquals("v", locked.content().getString("k"));

        try {
            bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v"))).toBlocking().single();
            assertTrue(false);
        } catch(CASMismatchException ex) {
            assertTrue(true);
        }

        boolean unlocked = bucket().unlock(key, locked.cas()).toBlocking().single();
        assertTrue(unlocked);

        bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
    }

    @Test
    public void shouldTouch() throws Exception {
        String key = "touch";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v"), 3))
            .toBlocking().single();

        Thread.sleep(2000);

        Boolean touched = bucket().touch(key, 3).toBlocking().single();
        assertTrue(touched);

        Thread.sleep(2000);

        JsonDocument loaded = bucket().get(key).toBlocking().single();
        assertEquals("v", loaded.content().getString("k"));
    }

    @Test
    public void shouldPersistToMaster() {
        String key = "persist-to-master";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")),
            PersistTo.MASTER, ReplicateTo.NONE).toBlocking().single();
    }

    @Test
    public void shouldRemoveFromMaster() {
        String key = "remove-from-master";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")),
            PersistTo.MASTER, ReplicateTo.NONE).toBlocking().single();

        JsonDocument remove = bucket().remove(key, PersistTo.MASTER, ReplicateTo.NONE).toBlocking().single();
    }

    @Test
    public void shouldUpsertLegacyObjectDocument() {
        String id = "legacy-upsert";
        User user = new User("Michael");
        LegacyDocument doc = LegacyDocument.create(id, user);
        LegacyDocument stored = bucket().upsert(doc).toBlocking().single();

        LegacyDocument found = bucket().get(id, LegacyDocument.class).toBlocking().single();
        assertEquals(found.content().getClass(), user.getClass());
        assertEquals("Michael", ((User) found.content()).getFirstname());
    }

    @Test
    public void shouldAppendString() {
        String id ="append1";
        String value = "foo";

        LegacyDocument doc = LegacyDocument.create(id, value);
        LegacyDocument stored = bucket().upsert(doc).toBlocking().single();

        stored = bucket().append(LegacyDocument.create(id, "bar")).toBlocking().single();

        LegacyDocument found = bucket().get(id, LegacyDocument.class).toBlocking().single();
        assertEquals("foobar", found.content());
    }

    @Test
    public void shouldPrependString() {
        String id ="prepend1";
        String value = "bar";

        LegacyDocument doc = LegacyDocument.create(id, value);
        LegacyDocument stored = bucket().upsert(doc).toBlocking().single();

        stored = bucket().prepend(LegacyDocument.create(id, "foo")).toBlocking().single();

        LegacyDocument found = bucket().get(id, LegacyDocument.class).toBlocking().single();
        assertEquals("foobar", found.content());
    }

    @Test(expected = DocumentDoesNotExistException.class)
    public void shouldFailOnNonExistingAppend() {
        LegacyDocument doc = LegacyDocument.create("appendfail", "fail");
        bucket().append(doc).toBlocking().single();
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

}
