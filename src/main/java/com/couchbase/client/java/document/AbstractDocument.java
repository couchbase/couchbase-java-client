package com.couchbase.client.java.document;

/**
 * Common implementation of a {@link Document}.
 */
public abstract class AbstractDocument<T> implements Document<T> {

  private String id;
  private long cas;
  private int expiry;
  private T content;

  protected AbstractDocument() {
    this(null, null, 0, 0);
  }

  protected AbstractDocument(String id) {
    this(id, null, 0, 0);
  }

  protected AbstractDocument(String id, T content) {
    this(id, content, 0, 0);
  }

  protected AbstractDocument(String id, T content, int expiry) {
    this(id, content, 0, expiry);
  }

  protected AbstractDocument(String id, T content, long cas) {
    this(id, content, cas, 0);
  }

  protected AbstractDocument(String id, T content, long cas, int expiry) {
    this.id = id;
    this.cas = cas;
    this.expiry = expiry;
    this.content = content;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public long cas() {
    return cas;
  }

  @Override
  public int expiry() {
    return expiry;
  }

  @Override
  public T content() {
    return content;
  }

  @Override
  public Document<T> content(T content) {
    this.content = content;
    return this;
  }

  @Override
  public Document<T> id(String id) {
    this.id = id;
    return this;
  }

  @Override
  public Document<T> cas(long cas) {
    this.cas = cas;
    return this;
  }

  @Override
  public Document<T> expiry(int expiry) {
    this.expiry = expiry;
    return this;
  }
}
