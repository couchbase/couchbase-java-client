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
import java.io.OutputStream;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.transcoder.TranscoderUtils;

/**
 * A Jackson-based implementation of a {@link FragmentTranscoder}, based on {@link AbstractByteArrayFragmentTranscoder}.
 *
 * This implementation changes the {@link #decodeWithMessage(ByteBuf, Class, String) decodeWithMessage} behavior
 * compared to the parent strategy by attempting to deserialize JSON arrays into {@link JsonArray} and JSON
 * dictionaries into {@link JsonObject}.
 *
 * Care should be taken to not use Jackson specific annotations if you want to be able to
 * easily swap this for another SubdocumentTranscoder implementation at a later time.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class JacksonFragmentTranscoder extends AbstractByteArrayFragmentTranscoder {

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
    protected Object byteArrayToGenericObject(byte[] byteArray, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("byteArrayToGenericObject is unused by custom decodeWithMessage");
    }

    @Override
    protected <T> T byteArrayToClass(byte[] byteArray, int offset, int length, Class<? extends T> clazz) throws IOException {
        throw new UnsupportedOperationException("byteArrayToClass is unused by custom decodeWithMessage");
    }

    @Override
    protected <T> byte[] writeValueAsBytes(T value) throws IOException {
        return mapper.writeValueAsBytes(value);
    }

    @Override
    protected void writeValueIntoStream(OutputStream out, Object o) throws IOException {
        mapper.writeValue(out, o);
    }
}
