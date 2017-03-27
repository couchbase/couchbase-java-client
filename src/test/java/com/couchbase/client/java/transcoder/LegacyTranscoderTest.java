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
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.LegacyDocument;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of the {@link LegacyTranscoder}.
 *
 * @author Michael Nitschinger
 * @since 2.4.4
 */
public class LegacyTranscoderTest {

    private LegacyTranscoder converter;

    @Before
    public void setup() {
        converter = new LegacyTranscoder();
    }

    @Test
    public void shouldEncodeJsonString() {
        LegacyDocument document = LegacyDocument.create("id", "{\"test:\":true}");
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals("{\"test:\":true}", encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(0, (long) encoded.value2());
    }

    @Test
    public void shouldNotCompressLongJsonString() {
        String input = loadFileIntoString("/data/legacy/large.json");

        LegacyDocument document = LegacyDocument.create("id", input);
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertEquals(input, encoded.value1().toString(CharsetUtil.UTF_8));
        assertEquals(0, (long) encoded.value2());
    }

    @Test
    public void shouldCompressLongNonJsonString() {
        String input = loadFileIntoString("/data/legacy/large_nonjson.txt");

        LegacyDocument document = LegacyDocument.create("id", input);
        Tuple2<ByteBuf, Integer> encoded = converter.encode(document);

        assertNotEquals(input, encoded.value1().toString(CharsetUtil.UTF_8));
        assertTrue(input.length() > encoded.value1().readableBytes());
        assertEquals(LegacyTranscoder.COMPRESSED, (long) encoded.value2());
    }

    private static String loadFileIntoString(String path) {
        InputStream stream = LegacyTranscoderTest.class.getResourceAsStream(path);
        java.util.Scanner s = new java.util.Scanner(stream).useDelimiter("\\A");
        String input = s.next();
        s.close();
        return input;
    }

}
