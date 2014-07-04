package com.couchbase.client.java;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

}
