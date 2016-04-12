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

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode {@link JsonDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonArrayTranscoder extends AbstractTranscoder<JsonArrayDocument, JsonArray> {

    @Override
    public Class<JsonArrayDocument> documentType() {
        return JsonArrayDocument.class;
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(final JsonArrayDocument document) throws Exception {
        return Tuple.create(jsonArrayToByteBuf(document.content()), TranscoderUtils.JSON_COMMON_FLAGS);
    }

    @Override
    protected JsonArrayDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception {
        if (!TranscoderUtils.hasJsonFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-JSON array document for "
                + "id " + id + ", could not decode.");
        }
        return newDocument(id, expiry, byteBufToJsonArray(content), cas);
    }

    @Override
    public JsonArrayDocument newDocument(String id, int expiry, JsonArray content, long cas) {
        return JsonArrayDocument.create(id, expiry, content, cas);
    }

    @Override
    public JsonArrayDocument newDocument(String id, int expiry, JsonArray content, long cas,
        MutationToken mutationToken) {
        return JsonArrayDocument.create(id, expiry, content, cas, mutationToken);
    }

    public String jsonArrayToString(JsonArray input) throws Exception {
        return JacksonTransformers.MAPPER.writeValueAsString(input);
    }

    public ByteBuf jsonArrayToByteBuf(JsonArray input) throws Exception {
        return Unpooled.wrappedBuffer(JacksonTransformers.MAPPER.writeValueAsBytes(input));
    }

    public JsonArray stringToJsonArray(String input) throws Exception {
        return JacksonTransformers.MAPPER.readValue(input, JsonArray.class);
    }

    public JsonArray byteBufToJsonArray(ByteBuf input) throws Exception {
        return TranscoderUtils.byteBufToClass(input, JsonArray.class, JacksonTransformers.MAPPER);
    }

}
