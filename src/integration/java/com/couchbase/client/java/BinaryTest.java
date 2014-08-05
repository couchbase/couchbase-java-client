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

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.LongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
    assertEquals(ResponseStatus.SUCCESS, response.status());
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
      .from("doc1", "doc2", "doc3")
      .flatMap(new Func1<String, Observable<JsonDocument>>() {
        @Override
        public Observable<JsonDocument> call(String id) {
          return bucket().get(id);
        }
      }).toBlocking();

    Iterator<JsonDocument> iterator = observable.getIterator();
    while (iterator.hasNext()) {
      JsonDocument doc = iterator.next();
      assertNull(doc.content());
      assertEquals(ResponseStatus.NOT_EXISTS, doc.status());
    }
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
        String key = "get-and-touch";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v"), 3))
            .toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsert.status());

        Thread.sleep(2000);

        JsonDocument touched = bucket().getAndTouch(key, 3).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, touched.status());
        assertEquals("v", touched.content().getString("k"));

        Thread.sleep(2000);

        touched = bucket().get(key).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, touched.status());
        assertEquals("v", touched.content().getString("k"));
    }

    @Test
    public void shouldGetAndLock() throws Exception {
        String key = "get-and-lock";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsert.status());

        JsonDocument locked = bucket().getAndLock(key, 2).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, locked.status());
        assertEquals("v", locked.content().getString("k"));

        upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
        assertEquals(ResponseStatus.EXISTS, upsert.status());

        Thread.sleep(3000);

        upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsert.status());
    }

    @Test
    public void shouldUnlock() throws Exception {
        String key = "unlock";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsert.status());

        JsonDocument locked = bucket().getAndLock(key, 15).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, locked.status());
        assertEquals("v", locked.content().getString("k"));

        upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
        assertEquals(ResponseStatus.EXISTS, upsert.status());

        boolean unlocked = bucket().unlock(key, locked.cas()).toBlocking().single();
        assertTrue(unlocked);

        upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")))
            .toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsert.status());
    }

    @Test
    public void shouldTouch() throws Exception {
        String key = "touch";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v"), 3))
            .toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsert.status());

        Thread.sleep(2000);

        Boolean touched = bucket().touch(key, 3).toBlocking().single();
        assertTrue(touched);

        Thread.sleep(2000);

        JsonDocument loaded = bucket().get(key).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, loaded.status());
        assertEquals("v", loaded.content().getString("k"));
    }

    @Test
    public void shouldPersistToMaster() {
        String key = "persist-to-master";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")),
            PersistTo.MASTER, ReplicateTo.NONE).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsert.status());
    }

    @Test
    public void shouldRemoveFromMaster() {
        String key = "remove-from-master";

        JsonDocument upsert = bucket().upsert(JsonDocument.create(key, JsonObject.empty().put("k", "v")),
            PersistTo.MASTER, ReplicateTo.NONE).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, upsert.status());

        JsonDocument remove = bucket().remove(key, PersistTo.MASTER, ReplicateTo.NONE).toBlocking().single();
        assertEquals(ResponseStatus.SUCCESS, remove.status());

    }

}
