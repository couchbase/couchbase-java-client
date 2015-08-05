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
package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.Document;

/**
 *
 * @param <D>
 * @param <T>
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface Transcoder<D extends Document<T>, T> {

    D decode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status);

    Tuple2<ByteBuf, Integer> encode(D document);

    /**
     * Creates a new Document with the passed in information.
     *
     * Use the one with the mutation token instead
     * ({@link #newDocument(String, int, Object, long, MutationToken)}).
     *
     * @param id the id of the document.
     * @param expiry the document expiration.
     * @param content the document content.
     * @param cas the documents cas value.
     * @return the created document.
     */
    @Deprecated
    D newDocument(String id, int expiry, T content, long cas);

    /**
     * Creates a new Document with the passed in information.
     *
     * @param id the id of the document.
     * @param expiry the document expiration.
     * @param content the document content.
     * @param cas the documents cas value.
     * @param mutationToken the documents mutation token.
     * @return the created document.
     */
    D newDocument(String id, int expiry, T content, long cas, MutationToken mutationToken);


    Class<D> documentType();
}
