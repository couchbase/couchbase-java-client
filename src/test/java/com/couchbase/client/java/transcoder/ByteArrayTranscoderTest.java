/*
 * Copyright (c) 2017 Couchbase, Inc.
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
import com.couchbase.client.java.document.ByteArrayDocument;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the functionality of the {@link ByteArrayTranscoder}.
 *
 * @author Michael Nitschinger
 * @since 2.5.2
 */
public class ByteArrayTranscoderTest {

    private ByteArrayTranscoder converter;

    @Before
    public void setup() {
        converter = new ByteArrayTranscoder();
    }

    @Test
    public void shouldEncodeFromByteArray() {
        ByteArrayDocument document = ByteArrayDocument.create("id", "value".getBytes(CharsetUtil.UTF_8));
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("value", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(TranscoderUtils.BINARY_COMPAT_FLAGS, (long) encoded.value2());
    }

    @Test
    public void shouldDecodeIntoByteArray() {
        ByteBuf content = Unpooled.copiedBuffer("value", CharsetUtil.UTF_8);
        ByteArrayDocument decoded = converter.decode("id", content, 0, 0,
            TranscoderUtils.BINARY_COMMON_FLAGS, ResponseStatus.SUCCESS);

        assertEquals("value", new String(decoded.content(), CharsetUtil.UTF_8));
        assertEquals(0, content.refCnt());
    }

}
