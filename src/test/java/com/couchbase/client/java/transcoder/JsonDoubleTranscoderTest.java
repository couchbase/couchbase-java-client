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
import com.couchbase.client.java.document.JsonDoubleDocument;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonDoubleTranscoderTest {

    private JsonDoubleTranscoder converter;

    @Before
    public void setup() {
        converter = new JsonDoubleTranscoder();
    }

    @Test
    public void shouldEncodeDouble() {
        JsonDoubleDocument document = JsonDoubleDocument.create("id", Double.MAX_VALUE);
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("1.7976931348623157E308", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.JSON_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeCommonFlagsDouble() {
        ByteBuf content = Unpooled.copiedBuffer("1.7976931348623157E308", CharsetUtil.UTF_8);
        JsonDoubleDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);

        assertEquals(Double.MAX_VALUE, decoded.content(), 0);
    }

    @Test
    public void shouldDecodeLegacyDouble() {
        byte[] bytes = LegacyTranscoder.encodeNum(Double.doubleToRawLongBits(Double.MAX_VALUE), 8);
        ByteBuf content = Unpooled.buffer().writeBytes(bytes);
        JsonDoubleDocument decoded = converter.decode("id", content, 0, 0, 7 << 8,
            ResponseStatus.SUCCESS);

        assertEquals(Double.MAX_VALUE, decoded.content(), 0);
    }

    @Test
    public void shouldDecodeLegacyFloat() {
        byte[] bytes = LegacyTranscoder.encodeNum(Float.floatToRawIntBits(Float.MAX_VALUE), 4);
        ByteBuf content = Unpooled.buffer().writeBytes(bytes);
        JsonDoubleDocument decoded = converter.decode("id", content, 0, 0, 6 << 8,
            ResponseStatus.SUCCESS);

        assertEquals(Float.MAX_VALUE, decoded.content(), 0);
    }

}