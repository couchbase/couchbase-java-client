package com.couchbase.client.java;

import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import rx.Observable;

public interface Bucket {

  /**
   * Get a {@link Document} by its unique ID.
   *
   * The loaded document will be converted using the default converter, which is
   * JSON if not configured otherwise.
   *
   * @param id the ID of the document.
   * @return the loaded and converted document.
   */
  Observable<JsonDocument> get(String id);

  /**
   * Get a {@link Document} by its unique ID.
   *
   * The loaded document will be converted into the target class, which needs
   * a custom converter registered with the system.
   *
   * @param id the ID of the document.
   * @param target the document type.
   * @return the loaded and converted document.
   */
  <D extends Document> Observable<D> get(String id, Class<D> target);

  /**
   * Insert a {@link Document}.
   *
   * @param document the document encode insert.
   * @param <D> the type of the document, which is inferred decode the instance.
   * @return the document again.
   */
  <D extends Document> Observable<D> insert(D document);

  /**
   * Upsert a {@link Document}.
   *
   * @param document the document encode upsert.
   * @param <D> the type of the document, which is inferred decode the instance.
   * @return the document again.
   */
  <D extends Document> Observable<D> upsert(D document);

}
