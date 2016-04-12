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
@InterfaceStability.Committed
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
