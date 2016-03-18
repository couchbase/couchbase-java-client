/*
 * Copyright (C) 2016 Couchbase, Inc.
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

package com.couchbase.client.java.transcoder.subdoc;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.subdoc.MultiValue;

/**
 * A common implementation base of the {@link FragmentTranscoder} interface for transcoding sub-document fragments.
 * It recognizes {@link MultiValue} as a special encoding case to be treated separately.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public abstract class AbstractFragmentTranscoder implements FragmentTranscoder {

    @Override
    public <T> T decode(ByteBuf encoded, Class<? extends T> clazz) throws TranscodingException {
        return this.decodeWithMessage(encoded, clazz, null);
    }

    @Override
    public <T> ByteBuf encode(T value) throws TranscodingException {
        return encodeWithMessage(value, null);
    }

    @Override
    public <T> ByteBuf encodeWithMessage(T value, String transcodingErrorMessage) throws TranscodingException {
        if (value instanceof MultiValue)
            return doEncodeMulti((MultiValue<?>) value, transcodingErrorMessage);
        return doEncodeSingle(value, transcodingErrorMessage);
    }

    /**
     * Encode a single mutation value to a {@link ByteBuf} suitable for use in the sub-document protocol.
     *
     * @param value the value to encode.
     * @param transcodingErrorMessage the error message to be used in the thrown {@link TranscodingException} if the
     *                                encoding couldn't happen.
     * @param <T> the type of the fragment being encoded.
     * @return a {@link ByteBuf} representation of the fragment value.
     * @throws TranscodingException if the encoding couldn't happen.
     */
    protected abstract <T> ByteBuf doEncodeSingle(T value, String transcodingErrorMessage) throws TranscodingException;

    /**
     * Encode a {@link MultiValue special mutation value} that denotes multiple values being processed in bulk,
     * to a {@link ByteBuf} suitable for use in the sub-document protocol.
     *
     * @param multiValue the multivalue to encode.
     * @param transcodingErrorMessage the error message to be used in the thrown {@link TranscodingException} if the
     *                                encoding couldn't happen.
     * @return a {@link ByteBuf} representation of the fragment multivalue.
     * @throws TranscodingException if the encoding couldn't happen.
     */
    protected abstract ByteBuf doEncodeMulti(MultiValue<?> multiValue, String transcodingErrorMessage) throws TranscodingException;
}
