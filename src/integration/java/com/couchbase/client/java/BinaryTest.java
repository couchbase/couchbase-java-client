package com.couchbase.client.java;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.LongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

import java.util.Iterator;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class BinaryTest extends ClusterDependentTest {

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

        touched = bucket().getAndTouch(key, 3).toBlocking().single();
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

}
