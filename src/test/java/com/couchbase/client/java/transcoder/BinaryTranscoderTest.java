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
import com.couchbase.client.deps.io.netty.util.ReferenceCountUtil;
import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.error.TranscodingException;

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
        BinaryDocument document = converter.decode("id",
                Unpooled.copiedBuffer("value", CharsetUtil.UTF_8), 0, 0,
                TranscoderUtils.BINARY_COMMON_FLAGS, ResponseStatus.SUCCESS);
        assertEquals("value", document.content().toString(CharsetUtil.UTF_8));
    }

    @Test
    public void shouldDecodeLegacyBinary() {
        BinaryDocument document = converter.decode("id",
                Unpooled.copiedBuffer("value", CharsetUtil.UTF_8), 0, 0,
                TranscoderUtils.BINARY_COMPAT_FLAGS, ResponseStatus.SUCCESS);
        assertEquals("value", document.content().toString(CharsetUtil.UTF_8));
    }

    @Test
    public void shouldNotReleaseBufferWhenDecoded() {
        ByteBuf content = ReferenceCountUtil.releaseLater(Unpooled.copiedBuffer("value", CharsetUtil.UTF_8));
        converter.decode("id", content, 0, 0, TranscoderUtils.BINARY_COMMON_FLAGS,
            ResponseStatus.SUCCESS);
        assertEquals(1, content.refCnt());
    }

    @Test(expected = TranscodingException.class)
    public void shouldReleaseBufferWhenError() {
        ByteBuf content = Unpooled.copiedBuffer("value", CharsetUtil.UTF_8);
        int wrongFlags = 1234;
        try {
            converter.decode("id", content, 0, 0, wrongFlags,
                ResponseStatus.SUCCESS);
        } finally {
            assertEquals(0, content.refCnt());
        }
    }

}
