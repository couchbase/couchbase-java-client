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

import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonBooleanDocument;
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

}
