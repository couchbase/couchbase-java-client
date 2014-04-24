package com.couchbase.client.java;

import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.TestProperties;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class BinaryTest {

  private static final String seedNode = TestProperties.seedNode();
  private static final String bucketName = TestProperties.bucket();
  private static final String password = TestProperties.password();

  private static Bucket bucket;

  @BeforeClass
  public static void connect() {
    CouchbaseCluster cluster = new CouchbaseCluster(seedNode);
    bucket = cluster
      .openBucket(bucketName, password)
      .toBlockingObservable()
      .single();
  }

  @Test
  public void shouldInsertAndGet() {
    JsonObject content = JsonObject.empty().put("hello", "world");
    final JsonDocument doc = new JsonDocument("key", content);
    JsonDocument response = bucket
      .insert(doc)
      .flatMap(new Func1<JsonDocument, Observable<JsonDocument>>() {
        @Override
        public Observable<JsonDocument> call(JsonDocument document) {
          return bucket.get("key");
        }
      })
      .toBlockingObservable()
      .single();
    assertEquals(content.getString("hello"), response.content().getString("hello"));
  }

  @Test
  public void shouldUpsertAndGet() {
    JsonObject content = JsonObject.empty().put("hello", "world");
    final JsonDocument doc = new JsonDocument("key", content);
    JsonDocument response = bucket.upsert(doc)
      .flatMap(new Func1<JsonDocument, Observable<JsonDocument>>() {
        @Override
        public Observable<JsonDocument> call(JsonDocument document) {
          return bucket.get("key");
        }
      })
      .toBlockingObservable()
      .single();
    assertEquals(content.getString("hello"), response.content().getString("hello"));
  }

  @Test
  public void shouldLoadMultipleDocuments() throws Exception {
    BlockingObservable<JsonDocument> observable = Observable
      .from("doc1", "doc2", "doc3")
      .flatMap(new Func1<String, Observable<JsonDocument>>() {
        @Override
        public Observable<JsonDocument> call(String id) {
          return bucket.get(id);
        }
      })
      .toBlockingObservable();

    Iterator<JsonDocument> iterator = observable.getIterator();
    while (iterator.hasNext()) {
      Document doc = iterator.next();
      assertEquals(null, doc.content());
    }
  }

}
