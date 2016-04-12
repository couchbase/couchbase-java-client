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
import com.couchbase.client.java.document.JsonDoubleDocument;
import com.couchbase.client.java.error.TranscodingException;

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

    @Test
    public void shouldReleaseBufferWhenDecoded() {
        ByteBuf content = Unpooled.copiedBuffer("1.7976931348623157E308", CharsetUtil.UTF_8);
        JsonDoubleDocument decoded = converter.decode("id", content, 0, 0, TranscoderUtils.JSON_COMMON_FLAGS,
            ResponseStatus.SUCCESS);
        assertEquals(0, content.refCnt());
    }

    @Test(expected = TranscodingException.class)
    public void shouldReleaseBufferWhenError() {
        ByteBuf content = Unpooled.copiedBuffer("1.7976931348623157E308", CharsetUtil.UTF_8);
        int wrongFlags = 1234;
        try {
            converter.decode("id", content, 0, 0, wrongFlags, ResponseStatus.SUCCESS);
        } finally {
            assertEquals(0, content.refCnt());
        }
    }
}