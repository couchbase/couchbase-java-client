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

import java.io.IOException;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.transcoder.TranscoderUtils;

/**
 * A Jackson-based implementation of a {@link FragmentTranscoder}.
 *
 * Care should be taken to not use Jackson specific annotations if you want to be able to
 * easily swap this for another SubdocumentTranscoder implementation at a later time.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Private
public class JacksonFragmentTranscoder implements FragmentTranscoder {

    private final ObjectMapper mapper;

    public JacksonFragmentTranscoder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T decode(ByteBuf encoded, Class<? extends T> clazz) throws TranscodingException {
        return this.decodeWithMessage(encoded, clazz, null);
    }

    @Override
    public <T> T decodeWithMessage(ByteBuf encoded, Class<? extends T> clazz, String transcodingErrorMessage) throws TranscodingException {
        try {
            if (Object.class.equals(clazz)) {
                //generic path that will transform dictionaries to JsonObject and arrays to JsonArray
                return (T) TranscoderUtils.byteBufToGenericObject(encoded, mapper);
            } else {
                return TranscoderUtils.byteBufToClass(encoded, clazz, mapper);
            }
        } catch (IOException e) {
            throw new TranscodingException(transcodingErrorMessage, e);
        }
    }

    @Override
    public <T> ByteBuf encode(T value) throws TranscodingException {
        return encodeWithMessage(value, null);
    }

    @Override
    public <T> ByteBuf encodeWithMessage(T value, String transcodingErrorMessage) throws TranscodingException {
        try {
            return Unpooled.wrappedBuffer(mapper.writeValueAsBytes(value));
        } catch (JsonProcessingException e) {
            throw new TranscodingException(transcodingErrorMessage, e);
        }
    }
}
