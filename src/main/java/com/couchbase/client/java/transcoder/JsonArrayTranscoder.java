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

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
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
        String content = jsonArrayToString(document.content());
        return Tuple.create(Unpooled.copiedBuffer(content, CharsetUtil.UTF_8), TranscoderUtils.JSON_COMMON_FLAGS);
    }

    @Override
    protected JsonArrayDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception {
        if (!TranscoderUtils.hasJsonFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-JSON array document for "
                + "id " + id + ", could not decode.");
        }

        JsonArray converted = stringToJsonArray(content.toString(CharsetUtil.UTF_8));
        return newDocument(id, expiry, converted, cas);
    }

    @Override
    public JsonArrayDocument newDocument(String id, int expiry, JsonArray content, long cas) {
        return JsonArrayDocument.create(id, expiry, content, cas);
    }

    public String jsonArrayToString(JsonArray input) throws Exception {
        return JacksonTransformers.MAPPER.writeValueAsString(input);
    }

    public JsonArray stringToJsonArray(String input) throws Exception {
        return JacksonTransformers.MAPPER.readValue(input, JsonArray.class);
    }

}
