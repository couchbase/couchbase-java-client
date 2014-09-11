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

import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.error.TranscodingException;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public abstract class AbstractTranscoder<D extends Document<T>, T> implements Transcoder<D, T> {

    @Override
    public D decode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status) {
        try {
            return doDecode(id, content, cas, expiry, flags, status);
        } catch(Exception ex) {
            throw new TranscodingException("Could not decode document with ID " + id, ex);
        }
    }

    @Override
    public Tuple2<ByteBuf, Integer> encode(D document) {
        try {
            return doEncode(document);
        } catch(Exception ex) {
            throw new TranscodingException("Could not encode document with ID " + document.id(), ex);
        }
    }

    protected abstract D doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception;
    protected abstract Tuple2<ByteBuf, Integer> doEncode(D document)
        throws Exception;
}
