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
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.BinaryDocument;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BinaryTranscoderTest {

    private BinaryTranscoder converter;

    @Before
    public void setup() {
        converter = new BinaryTranscoder();
    }

    @Test
    public void shouldEncodeBinary() {
        BinaryDocument document = BinaryDocument.create("id", Unpooled.copiedBuffer("value", CharsetUtil.UTF_8));
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("value", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.BINARY_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeCommonBinary() {
        final String id = "id";
        final String value = "value";
        // encode document
        BinaryDocument document = BinaryDocument.create(id, Unpooled.copiedBuffer(value, CharsetUtil.UTF_8));
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        // decode document
        BinaryDocument decodedDocument = converter.decode(id, encoded.value1(), 0, 0, TranscoderUtils.BINARY_COMMON_FLAGS, null);
        ByteBuf content = decodedDocument.content();
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);

        assertEquals(value, new String(bytes, CharsetUtil.UTF_8));
    }

    @Test
    public void shouldDecodeLegacyBinary() {
        String id = "id";
        String value = "value";
        // encode document
        BinaryDocument document = BinaryDocument.create(id, Unpooled.copiedBuffer(value, CharsetUtil.UTF_8));
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        // decode document
        BinaryDocument decodedDocument = converter.decode(id, encoded.value1(), 0, 0, TranscoderUtils.BINARY_COMPAT_FLAGS, null);
        ByteBuf content = decodedDocument.content();
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);

        assertEquals(value, new String(bytes, CharsetUtil.UTF_8));
    }

}
