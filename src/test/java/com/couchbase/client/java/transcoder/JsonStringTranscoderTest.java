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
import com.couchbase.client.java.document.JsonStringDocument;
import com.couchbase.client.java.error.TranscodingException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonStringTranscoderTest {

    private JsonStringTranscoder converter;

    @Before
    public void setup() {
        converter = new JsonStringTranscoder();
    }

    @Test
    public void shouldEncodeStringWithoutQuotes() {
        JsonStringDocument document = JsonStringDocument.create("id", "value");
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("\"value\"", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.JSON_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeCommonString() {
        ByteBuf content = Unpooled.copiedBuffer("\"value\"", CharsetUtil.UTF_8);
        JsonStringDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMPAT_FLAGS,
            ResponseStatus.SUCCESS);

        assertEquals("value", decoded.content());
    }

    @Test
    public void shouldDecodeEmptyCommonString() {
        ByteBuf content = Unpooled.copiedBuffer("\"\"", CharsetUtil.UTF_8);
        JsonStringDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMPAT_FLAGS,
            ResponseStatus.SUCCESS);

        assertEquals("", decoded.content());
    }

    @Test
    public void shouldDecodeLegacyStringWithQuotes() {
        ByteBuf content = Unpooled.copiedBuffer("\"value\"", CharsetUtil.UTF_8);
        JsonStringDocument decoded = converter.decode("id", content, 0, 0, 0,
            ResponseStatus.SUCCESS);

        assertEquals("value", decoded.content());
    }

    @Test
    public void shouldDecodeLegacyStringWithoutQuotes() {
        ByteBuf content = Unpooled.copiedBuffer("value", CharsetUtil.UTF_8);
        JsonStringDocument decoded = converter.decode("id", content, 0, 0, 0,
            ResponseStatus.SUCCESS);

        assertEquals("value", decoded.content());
    }

    @Test
    public void shouldReleaseBufferWhenDecoded() {
        ByteBuf content = Unpooled.copiedBuffer("\"value\"", CharsetUtil.UTF_8);
        JsonStringDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMPAT_FLAGS,
            ResponseStatus.SUCCESS);
        assertEquals(0, content.refCnt());
    }

    @Test(expected = TranscodingException.class)
    public void shouldReleaseBufferWhenError() {
        ByteBuf content = Unpooled.copiedBuffer("\"value\"", CharsetUtil.UTF_8);
        int wrongFlags = 1234;
        try {
            converter.decode("id", content, 0, 0, wrongFlags, ResponseStatus.SUCCESS);
        } finally {
            assertEquals(0, content.refCnt());
        }
    }
}
