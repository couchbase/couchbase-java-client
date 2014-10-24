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
 * Base {@link Transcoder} which should be extended for compatibility.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 * @since 2.0
 */
public abstract class AbstractTranscoder<D extends Document<T>, T> implements Transcoder<D, T> {

    @Override
    public D decode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status) {
        try {
            D result = doDecode(id, content, cas, expiry, flags, status);
            if (content != null && shouldAutoReleaseOnDecode()) {
                content.release();
            }
            return result;
        } catch(Exception ex) {
            if (content != null && shouldAutoReleaseOnError()) {
                content.release();
            }

            if (ex instanceof TranscodingException) {
                throw (TranscodingException) ex;
            } else {
                throw new TranscodingException("Could not decode document with ID " + id, ex);
            }
        }
    }

    @Override
    public Tuple2<ByteBuf, Integer> encode(D document) {
        try {
            return doEncode(document);
        } catch(Exception ex) {
            if (ex instanceof TranscodingException) {
                throw (TranscodingException) ex;
            } else {
                throw new TranscodingException("Could not encode document with ID " + document.id(), ex);
            }
        }
    }

    /**
     * Perform the decoding of the received response.
     *
     * @param id the id of the document.
     * @param content the encoded content of the document.
     * @param cas the cas value of the document.
     * @param expiry the expiration time of the document.
     * @param flags the flags set on the document.
     * @param status the response status.
     *
     * @return the decoded document.
     * @throws Exception if something goes wrong during the decode process.
     */
    protected abstract D doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception;

    /**
     * Perform the encoding of the request document.
     *
     * @param document the document to encode.
     * @return A tuple consisting of the encoded content and the flags to set.
     * @throws Exception if something goes wrong during the encode process.
     */
    protected abstract Tuple2<ByteBuf, Integer> doEncode(D document)
        throws Exception;

    /**
     * Flag method to auto release decoded buffers. Override to change default behaviour (true).
     *
     * @return true if the {@link ByteBuf} passed to
     * {@link #decode(String, ByteBuf, long, int, int, ResponseStatus) decode}
     * method is to be released automatically on success (default behaviour)
     */
    protected boolean shouldAutoReleaseOnDecode() {
        return true;
    }

    /**
     * Flag method to auto release buffers on decoding error. Override to change default behaviour (true).
     *
     * @return true if the {@link ByteBuf} passed to
     * {@link #decode(String, ByteBuf, long, int, int, ResponseStatus) decode}
     * method is to be released automatically in case of error (default behaviour)
     */
    protected boolean shouldAutoReleaseOnError() {
        return true;
    }
}
