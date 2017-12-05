/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.error.TranscodingException;

import static com.couchbase.client.core.logging.RedactableArgument.user;

/**
 * Base {@link Transcoder} which should be extended for compatibility.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 * @since 2.0
 */
public abstract class AbstractTranscoder<D extends Document<T>, T> implements Transcoder<D, T> {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(AbstractTranscoder.class);

    @Override
    public D decode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status) {
        try {
            D result = doDecode(id, content, cas, expiry, flags, status);
            if (content != null && shouldAutoReleaseOnDecode()) {
                content.release();
            }
            return result;
        } catch(Exception ex) {
            LOGGER.warn("Decoding of document with {} failed. exception: {}, id: \"{}\", cas: {}, expiry: {}, flags: {}, status: {}"
                + ", content size: {} bytes, content: \"{}\"", this.getClass().getSimpleName(), ex.getMessage(), user(id),
                user(Long.toString(cas)), user(Integer.toString(expiry)), "0x" + user(Integer.toHexString(flags)), status, user(Integer.toString(content == null ? 0 : content.readableBytes())),
                user(content == null ? "null" : content.toString(CharsetUtil.UTF_8)));

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

    /**
     * Default implementation for backwards compatibility.
     */
    @Override
    public D newDocument(String id, int expiry, T content, long cas, MutationToken mutationToken) {
        LOGGER.warn("This transcoder ({}) does not support mutation tokens - this method is a " +
            "stub and needs to be implemented on custom transcoders.", this.getClass().getSimpleName());
        return newDocument(id, expiry, content, cas);
    }
}
