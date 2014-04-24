package com.couchbase.client.java.document;

/**
 * Represents a Couchbase Server Document.
 *
 * Implementations need encode provide concrete content behavior, depending on the
 * document type (i.e. JSON, Binary,...).
 */
public interface Document<T> {

  /**
   * The per-bucket unique ID of the document.
   *
   * @return the document id.
   */
  String id();

  Document<T> id(String id);

  /**
   * The content of the document.
   *
   * @return the content.
   */
  T content();

  /**
   * Setter method for the content.
   *
   * @param content the content of the document.
   * @return the {@link Document} itself.
   */
  Document<T> content(T content);

  /**
   * The last-known CAS value for the document (0 if not set).
   *
   * @return the CAS value if set.
   */
  long cas();

  Document<T> cas(long cas);

  /**
   * The optional expiration time for the document (0 if not set).
   *
   * @return the expiration time.
   */
  int expiry();

  Document<T> expiry(int expiry);

}
