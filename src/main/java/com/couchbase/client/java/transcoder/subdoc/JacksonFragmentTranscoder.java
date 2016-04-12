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

import java.io.IOException;
import java.util.Iterator;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.ByteBufOutputStream;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.subdoc.MultiValue;
import com.couchbase.client.java.transcoder.TranscoderUtils;

/**
 * A Jackson-based implementation of a {@link FragmentTranscoder}, based on {@link AbstractFragmentTranscoder}.
 *
 * Care should be taken to not use Jackson specific annotations if you want to be able to
 * easily swap this for another SubdocumentTranscoder implementation at a later time.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class JacksonFragmentTranscoder extends AbstractFragmentTranscoder {

    private final ObjectMapper mapper;

    public JacksonFragmentTranscoder(ObjectMapper mapper) {
        this.mapper = mapper;
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
    protected <T> ByteBuf doEncodeSingle(T value, String transcodingErrorMessage) throws TranscodingException {
        try {
            return Unpooled.wrappedBuffer(mapper.writeValueAsBytes(value));
        } catch (JsonProcessingException e) {
            throw new TranscodingException(transcodingErrorMessage, e);
        }
    }

    @Override
    protected ByteBuf doEncodeMulti(MultiValue<?> multiValue, String transcodingErrorMessage) throws TranscodingException {
        //initial capacity is very roughly and arbitrarily initialized (4 bytes on average per value)
        final ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer(4 * multiValue.size()));
        //Note this OutputStream implementation doesn't implement flush() nor close(), so they are left out.
        try {
            for (Iterator<?> iterator = multiValue.iterator(); iterator.hasNext(); ) {
                Object o = iterator.next();
                mapper.writeValue(out, o);
                if (iterator.hasNext()) {
                   out.writeBytes(",");
                }
            }
            return out.buffer();
        } catch (IOException e) {
            throw new TranscodingException(transcodingErrorMessage, e);
        }
        //changing the OutputStream concrete implementation would probably require to close() in a finally block
    }
}
