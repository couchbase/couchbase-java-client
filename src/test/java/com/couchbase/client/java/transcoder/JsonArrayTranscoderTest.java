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
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests which verify the functionality for the {@link JsonTranscoder}.
 */
public class JsonArrayTranscoderTest {

    private static final ObjectMapper CONTROL_MAPPER = new ObjectMapper();
    private JsonArrayTranscoder converter;

    private static List<Object> readJsonIntoList(final ByteBuf raw) throws Exception {
        return CONTROL_MAPPER.readValue(
            raw.toString(CharsetUtil.UTF_8),
            CONTROL_MAPPER.getTypeFactory().constructCollectionType(ArrayList.class, Object.class)
        );
    }

    @Before
    public void setup() {
        converter = new JsonArrayTranscoder();
    }

    @Test
    public void shouldEncodeEmptyJsonArray() {
        JsonArray array = JsonArray.empty();

        Tuple2<ByteBuf, Integer> encoded = converter.encode(JsonArrayDocument.create("id", array));
        assertEquals("[]", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.JSON_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeEmptyJsonArray() {
        ByteBuf content = Unpooled.copiedBuffer("[]", CharsetUtil.UTF_8);
        JsonArrayDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);
        assertTrue(decoded.content().isEmpty());
    }

    @Test(expected = TranscodingException.class)
    public void shouldFailToDecodeWithWrongOldFlags() {
        ByteBuf content = Unpooled.copiedBuffer("[]", CharsetUtil.UTF_8);
        int wrongFlags = 1234;
        converter.decode("id", content, 0, 0, wrongFlags, ResponseStatus.SUCCESS);
    }

    @Test(expected = TranscodingException.class)
    public void shouldFailToDecodeWithWrongCommonFlags() {
        ByteBuf content = Unpooled.copiedBuffer("[]", CharsetUtil.UTF_8);
        int wrongFlags = TranscoderUtils.BINARY_COMMON_FLAGS;
        converter.decode("id", content, 0, 0, wrongFlags, ResponseStatus.SUCCESS);
    }

    @Test
    public void shouldEncodeArrayWithEmptyObject() {
        JsonArray array = JsonArray.create();
        array.add(JsonObject.create());

        Tuple2<ByteBuf, Integer> encoded = converter.encode(JsonArrayDocument.create("id", array));
        assertEquals("[{}]", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.JSON_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeObjectWithEmptyArray() {
        ByteBuf content = Unpooled.copiedBuffer("[{}]", CharsetUtil.UTF_8);
        JsonArrayDocument decoded = converter.decode("id", content, 0, 0,
            TranscoderUtils.JSON_COMMON_FLAGS, ResponseStatus.SUCCESS);

        assertFalse(decoded.content().isEmpty());
        assertEquals(1, decoded.content().size());
        assertTrue(decoded.content().getObject(0).isEmpty());
    }

    @Test
    public void shouldEncodeMixedJsonValues() throws Exception {
        JsonArray object = JsonArray.create();
        object.add("Hello World");
        object.add(1);
        object.add(Long.MAX_VALUE);
        object.add(11.3322);
        object.add(true);

        Tuple2<ByteBuf, Integer> encoded = converter.encode(JsonArrayDocument.create("id", object));
        List<Object> control = readJsonIntoList(encoded.value1());

        assertEquals(5, control.size());
        assertEquals("Hello World", control.get(0));
        assertEquals(1, control.get(1));
        assertEquals(Long.MAX_VALUE, control.get(2));
        assertEquals(11.3322, control.get(3));
        assertEquals(true, control.get(4));
        assertEquals(TranscoderUtils.JSON_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeMixedJsonValues() throws Exception {
        ByteBuf content = Unpooled.copiedBuffer("[\"Hello World\",1,9223372036854775807,11.3322,true]",
            CharsetUtil.UTF_8);
        JsonArrayDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);
        JsonArray found = decoded.content();

        assertFalse(found.isEmpty());
        assertEquals(5, found.size());
        assertEquals(true, found.getBoolean(4));
        assertEquals(1, (int) found.getInt(1));
        assertEquals("Hello World", found.getString(0));
        assertEquals(11.3322, found.getDouble(3), 0);
        assertEquals(Long.MAX_VALUE, (long) found.getLong(2));
    }


    @Test
    public void shouldReleaseBufferWhenDecoded() {
        ByteBuf content = Unpooled.copiedBuffer("[]", CharsetUtil.UTF_8);
        JsonArrayDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);
        assertEquals(0, content.refCnt());
    }

    @Test(expected = TranscodingException.class)
    public void shouldReleaseBufferWhenError() {
        ByteBuf content = Unpooled.copiedBuffer("[]", CharsetUtil.UTF_8);
        int wrongFlags = 1234;
        try {
            converter.decode("id", content, 0, 0, wrongFlags,
                ResponseStatus.SUCCESS);
        } finally {
            assertEquals(0, content.refCnt());
        }
    }

    @Test
    public void shouldEncodeAndDecodeSubJson() {
        JsonObject sub1 = JsonObject.create().put("item1", 1).put("item2", 2);
        JsonArray sub2 = JsonArray.create().add("item3");
        JsonArray arr = JsonArray.create().add(sub1).add(sub2);
        JsonArrayDocument doc = JsonArrayDocument.create("test", arr);

        Tuple2<ByteBuf, Integer> encoded = converter.encode(doc);
        JsonArrayDocument decoded = converter.decode("test", encoded.value1(), 0, 0, encoded.value2(),
                ResponseStatus.SUCCESS);

        assertNotNull(decoded.content());
        assertEquals(2, decoded.content().size());
        assertEquals(sub1, decoded.content().get(0));
        assertEquals(sub2, decoded.content().get(1));
    }
}