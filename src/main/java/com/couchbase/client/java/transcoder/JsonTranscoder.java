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

import java.util.List;
import java.util.Map;

import com.couchbase.client.core.endpoint.util.WhitespaceSkipper;
import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode {@link JsonDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonTranscoder extends AbstractTranscoder<JsonDocument, JsonObject> {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(JsonTranscoder.class);

    public JsonTranscoder() {
    }

    @Override
    public Class<JsonDocument> documentType() {
        return JsonDocument.class;
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(final JsonDocument document) throws Exception {
        return Tuple.create(jsonObjectToByteBuf(document.content()), TranscoderUtils.JSON_COMPAT_FLAGS);
    }

    @Override
    protected JsonDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception {
        if (!TranscoderUtils.hasJsonFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-JSON document for "
                + "id " + id + ", could not decode.");
        }
        return newDocument(id, expiry, byteBufToJsonObject(content), cas);
    }

    @Override
    public JsonDocument newDocument(String id, int expiry, JsonObject content, long cas) {
        return JsonDocument.create(id, expiry, content, cas);
    }

    public String jsonObjectToString(JsonObject input) throws Exception {
        return JacksonTransformers.MAPPER.writeValueAsString(input);
    }

    public ByteBuf jsonObjectToByteBuf(JsonObject input) throws Exception {
        return Unpooled.wrappedBuffer(JacksonTransformers.MAPPER.writeValueAsBytes(input));
    }

    public JsonObject stringToJsonObject(String input) throws Exception {
        return JacksonTransformers.MAPPER.readValue(input, JsonObject.class);
    }

    private <T> T byteBufToClass(ByteBuf input, Class<? extends T> clazz) throws Exception {
        byte[] inputBytes;
        int offset = 0;
        int length = input.readableBytes();
        if (input.hasArray()) {
            inputBytes = input.array();
            offset = input.arrayOffset() + input.readerIndex();
        } else {
            inputBytes = new byte[length];
            input.getBytes(input.readerIndex(), inputBytes);
        }
        return JacksonTransformers.MAPPER.readValue(inputBytes, offset, length, clazz);
    }

    /**
     * Converts a {@link ByteBuf} to a {@link JsonObject}, <b>without releasing the buffer</b>
     *
     * @param input the buffer to convert. It won't be cleared (contrary to {@link #doDecode(String, ByteBuf, long, int, int, ResponseStatus) classical decode})
     * @return a JsonObject decoded from the buffer
     * @throws Exception
     */
    public JsonObject byteBufToJsonObject(ByteBuf input) throws Exception {
        return byteBufToClass(input, JsonObject.class);
    }

    /**
     * Converts a {@link ByteBuf} representing a valid JSON entity to a generic {@link Object},
     * <b>without releasing the buffer</b>. The entity can either be a JSON object, array or scalar value, potentially with leading whitespace (which gets ignored).
     *
     * Detection of JSON objects and arrays is attempted in order not to incur an
     * additional conversion step (JSON to Map to JsonObject for example), but if a
     * Map or List is produced, it will be transformed to {@link JsonObject} or
     * {@link JsonArray} (with a warning logged).
     *
     * @param input the buffer to convert. It won't be cleared (contrary to
     * {@link #doDecode(String, ByteBuf, long, int, int, ResponseStatus) classical decode})
     * @return a Object decoded from the buffer
     * @throws Exception
     */
    public Object byteBufJsonValueToObject(ByteBuf input) throws Exception {
        //skip leading whitespaces
        int toSkip = input.forEachByte(new WhitespaceSkipper());
        if (toSkip > 0) {
            input.skipBytes(toSkip);
        }
        //peek into the buffer for quick detection of objects and arrays
        input.markReaderIndex();
        byte first = input.readByte();
        input.resetReaderIndex();

        switch (first) {
            case '{':
                return byteBufToJsonObject(input);
            case '[':
                return byteBufToClass(input, JsonArray.class);
        }

        //we couldn't fast detect the type, we'll have to unmarshall to object and make sure maps and lists
        //are converted to JsonObject/JsonArray.
        Object value = byteBufToClass(input, Object.class);
        if (value instanceof Map) {
            LOGGER.warn("A JSON object could not be fast detected (first byte '" + (char) first + "')");
            return JsonObject.from((Map<String, ?>) value);
        } else if (value instanceof List) {
            LOGGER.warn("A JSON array could not be fast detected (first byte '" + (char) first + "')");
            return JsonArray.from((List<?>) value);
        } else {
            return value;
        }
    }

}
