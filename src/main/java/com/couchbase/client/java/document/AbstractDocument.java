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
package com.couchbase.client.java.document;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * Common parent implementation of a {@link Document}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public abstract class AbstractDocument<T> implements Document<T> {

  private final String id;
  private final long cas;
  private final int expiry;
  private final T content;
  private final ResponseStatus status;

  protected AbstractDocument(String id, T content, long cas, int expiry, ResponseStatus status) {
    this.id = id;
    this.cas = cas;
    this.expiry = expiry;
    this.content = content;
    this.status = status;
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
  public ResponseStatus status() {
    return status;
  }

}
