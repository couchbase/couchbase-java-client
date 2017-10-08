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

import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of {@link TranscoderUtils}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class TranscoderUtilsTest {

    @Test
    public void testHasCommonFlags() {
        assertFalse(TranscoderUtils.hasCommonFlags(4));
        assertTrue(TranscoderUtils.hasCommonFlags(4 << 24));
    }

    @Test
    public void testExtractCommonFlags() {
        assertEquals(4, TranscoderUtils.extractCommonFlags(4 << 24));
    }

    @Test
    public void testCreateCommonFlags() {
        assertEquals(2 << 24, TranscoderUtils.createCommonFlags(2));
    }

    @Test
    public void testHasJsonFlags() {
        assertTrue(TranscoderUtils.hasJsonFlags(0));
        assertTrue(TranscoderUtils.hasJsonFlags(2 << 24));
        assertFalse(TranscoderUtils.hasJsonFlags(1));
        assertFalse(TranscoderUtils.hasJsonFlags(4 << 24));
    }

    @Test
    public void testHasBinaryFlags() {
        //new flag only
        assertTrue(TranscoderUtils.hasBinaryFlags(TranscoderUtils.BINARY_COMMON_FLAGS));
        //new flag + legacy flag (what a client should write)
        assertTrue(TranscoderUtils.hasBinaryFlags(TranscoderUtils.BINARY_COMPAT_FLAGS));
        //legacy flag only
        assertTrue(TranscoderUtils.hasBinaryFlags((8 << 8)));
    }

    @Test
    public void shouldEncodeUtf8() {
        String simple = "simpleValue";
        String rune = "ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ";
        String greek = "Τη γλώσσα μου έδωσαν ελληνική";
        String russian = "На берегу пустынных волн";

        assertEquals(
            Unpooled.wrappedBuffer(simple.getBytes(CharsetUtil.UTF_8)),
            TranscoderUtils.encodeStringAsUtf8(simple)
        );

        assertEquals(
            Unpooled.wrappedBuffer(rune.getBytes(CharsetUtil.UTF_8)),
            TranscoderUtils.encodeStringAsUtf8(rune)
        );

        assertEquals(
            Unpooled.wrappedBuffer(greek.getBytes(CharsetUtil.UTF_8)),
            TranscoderUtils.encodeStringAsUtf8(greek)
        );

        assertEquals(
            Unpooled.wrappedBuffer(russian.getBytes(CharsetUtil.UTF_8)),
            TranscoderUtils.encodeStringAsUtf8(russian)
        );
    }

    @Test
    public void shouldDecodeToClassFromDirectBuffer() throws Exception {
        ByteBuf input = Unpooled.directBuffer();
        input.writeBytes("{\"hello\": \"world\", \"direct\": true}".getBytes(CharsetUtil.UTF_8));

        JsonObject result = TranscoderUtils.byteBufToClass(input, JsonObject.class, JacksonTransformers.MAPPER);
        assertEquals(JsonObject.create().put("hello", "world").put("direct", true), result);
        assertEquals(input.refCnt(), 1);
        input.release();
    }

    @Test
    public void shouldDecodeToClassFromHeapBuffer() throws Exception {
        ByteBuf input = Unpooled.buffer();
        input.writeBytes("{\"hello\": \"world\", \"direct\": true}".getBytes(CharsetUtil.UTF_8));

        JsonObject result = TranscoderUtils.byteBufToClass(input, JsonObject.class, JacksonTransformers.MAPPER);
        assertEquals(JsonObject.create().put("hello", "world").put("direct", true), result);
        assertEquals(input.refCnt(), 1);
        input.release();
    }

}