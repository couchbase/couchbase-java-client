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
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.error.TranscodingException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonLongTranscoderTest {

    private JsonLongTranscoder converter;

    @Before
    public void setup() {
        converter = new JsonLongTranscoder();
    }

    @Test
    public void shouldEncodeLong() {
        JsonLongDocument doc = JsonLongDocument.create("id", Long.MAX_VALUE);
        Tuple2<ByteBuf, Integer> encoded = converter.encode(doc);

        assertEquals("9223372036854775807", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.JSON_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeLegacyLong() {
        byte[] bytes = LegacyTranscoder.encodeNum(Long.MAX_VALUE, 8);
        ByteBuf content = Unpooled.buffer().writeBytes(bytes);
        JsonLongDocument decoded = converter.decode("id", content, 0, 0, 3 << 8,
            ResponseStatus.SUCCESS);

        assertEquals(Long.MAX_VALUE, (long) decoded.content());
    }

    @Test
    public void shouldDecodeLegacyInt() {
        byte[] bytes = LegacyTranscoder.encodeNum(Integer.MAX_VALUE, 4);
        ByteBuf content = Unpooled.buffer().writeBytes(bytes);
        JsonLongDocument decoded = converter.decode("id", content, 0, 0, 2 << 8,
            ResponseStatus.SUCCESS);

        assertEquals(Integer.MAX_VALUE, (long) decoded.content());
    }

    @Test
    public void shouldDecodeCommonFlagsLong() {
        ByteBuf content = Unpooled.copiedBuffer("9223372036854775807", CharsetUtil.UTF_8);
        JsonLongDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMPAT_FLAGS,
            ResponseStatus.SUCCESS);

        assertEquals(Long.MAX_VALUE, (long) decoded.content());
    }

    @Test
    public void shouldDecodeCommonFlagsInt() {
        ByteBuf content = Unpooled.copiedBuffer("2147483647", CharsetUtil.UTF_8);
        JsonLongDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMPAT_FLAGS,
            ResponseStatus.SUCCESS);

        assertEquals(Integer.MAX_VALUE, (long) decoded.content());
    }

    @Test
    public void shouldReleaseBufferWhenDecoded() {
        ByteBuf content = Unpooled.copiedBuffer("9223372036854775807", CharsetUtil.UTF_8);
        JsonLongDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMPAT_FLAGS,
            ResponseStatus.SUCCESS);
        assertEquals(0, content.refCnt());
    }

    @Test(expected = TranscodingException.class)
    public void shouldReleaseBufferWhenError() {
        ByteBuf content = Unpooled.copiedBuffer("9223372036854775807", CharsetUtil.UTF_8);
        int wrongFlags = 1234;
        try {
            converter.decode("id", content, 0, 0, wrongFlags, ResponseStatus.SUCCESS);
        } finally {
            assertEquals(0, content.refCnt());
        }
    }
}
