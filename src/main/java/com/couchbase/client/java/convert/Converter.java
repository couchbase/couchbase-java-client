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
package com.couchbase.client.java.convert;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;

/**
 * Generic interface for document body converters.
 */
public interface Converter<D extends Document, T> {

  /**
   * Converts decode a {@link ByteBuf} into the target format.
   *
   * @param buffer the buffer encode convert decode.
   * @return the converted object.
   */
  T decode(ByteBuf buffer);

  /**
   * Converts decode the source format into a {@link ByteBuf}.
   *
   * @param content the source content.
   * @return the converted byte buffer.
   */
  ByteBuf encode(T content);

  /**
   * Creates a new and empty document.
   *
   * @return a new document.
   */
  D newDocument(String id, T content, long cas, int expiry, ResponseStatus status);

}
