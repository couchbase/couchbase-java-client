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
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonBooleanDocument;
import com.couchbase.client.java.error.TranscodingException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonBooleanTranscoderTest {

    private JsonBooleanTranscoder converter;

    @Before
    public void setup() {
        converter = new JsonBooleanTranscoder();
    }

    @Test
    public void shouldEncodeTrue() {
        JsonBooleanDocument document = JsonBooleanDocument.create("id", true);
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("true", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.JSON_COMMON_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldEncodeFalse() {
        JsonBooleanDocument document = JsonBooleanDocument.create("id", false);
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("false", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.JSON_COMMON_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeTrueFromLegacy() {
        ByteBuf content = Unpooled.buffer().writeChar('1');
        JsonBooleanDocument decoded = converter.decode("id", content, 0, 0, 1 << 8,
            ResponseStatus.SUCCESS);
        assertTrue(decoded.content());
    }

    @Test
    public void shouldDecodeFalseFromLegacy() {
        ByteBuf content = Unpooled.buffer().writeChar('0');
        JsonBooleanDocument decoded = converter.decode("id", content, 0, 0, 1 << 8,
            ResponseStatus.SUCCESS);
        assertFalse(decoded.content());
    }

    @Test
    public void shouldDecodeTrueFromCommonFlags() {
        ByteBuf content = Unpooled.copiedBuffer("true", CharsetUtil.UTF_8);
        JsonBooleanDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);
        assertTrue(decoded.content());
    }

    @Test
    public void shouldDecodeFalseFromCommonFlags() {
        ByteBuf content = Unpooled.copiedBuffer("false", CharsetUtil.UTF_8);
        JsonBooleanDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);
        assertFalse(decoded.content());
    }

    @Test
    public void shouldReleaseBufferWhenDecoded() {
        ByteBuf content = Unpooled.copiedBuffer("false", CharsetUtil.UTF_8);
        JsonBooleanDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);
        assertEquals(0, content.refCnt());
    }

    @Test(expected = TranscodingException.class)
    public void shouldReleaseBufferWhenError() {
        ByteBuf content = Unpooled.copiedBuffer("false", CharsetUtil.UTF_8);
        int wrongFlags = 1234;
        try {
            converter.decode("id", content, 0, 0, wrongFlags, ResponseStatus.SUCCESS);
        } finally {
            assertEquals(0, content.refCnt());
        }
    }

}
