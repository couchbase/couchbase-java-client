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

import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of {@link TranscoderUtils}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class TranscoderUtilsTest {

    @Test
    public void testHasCommonFlags() {
        assertFalse(TranscoderUtils.hasCommonFlags(4));
        assertTrue(TranscoderUtils.hasCommonFlags(4 << 24));
    }

    @Test
    public void testExtractCommonFlags() {
        assertEquals(4, TranscoderUtils.extractCommonFlags(4 << 24));
    }

    @Test
    public void testCreateCommonFlags() {
        assertEquals(2 << 24, TranscoderUtils.createCommonFlags(2));
    }

    @Test
    public void testHasJsonFlags() {
        assertTrue(TranscoderUtils.hasJsonFlags(0));
        assertTrue(TranscoderUtils.hasJsonFlags(2 << 24));
        assertFalse(TranscoderUtils.hasJsonFlags(1));
        assertFalse(TranscoderUtils.hasJsonFlags(4 << 24));
    }

    @Test
    public void testHasBinaryFlags() {
        //new flag only
        assertTrue(TranscoderUtils.hasBinaryFlags(TranscoderUtils.BINARY_COMMON_FLAGS));
        //new flag + legacy flag (what a client should write)
        assertTrue(TranscoderUtils.hasBinaryFlags(TranscoderUtils.BINARY_COMPAT_FLAGS));
        //legacy flag only
        assertTrue(TranscoderUtils.hasBinaryFlags((8 << 8)));
    }

    @Test
    public void shouldEncodeUtf8() {
        String simple = "simpleValue";
        String rune = "ᚠᛇᚻ᛫ᛒᛦᚦ᛫ᚠᚱᚩᚠᚢᚱ᛫ᚠᛁᚱᚪ᛫ᚷᛖᚻᚹᛦᛚᚳᚢᛗ";
        String greek = "Τη γλώσσα μου έδωσαν ελληνική";
        String russian = "На берегу пустынных волн";

        assertEquals(
            Unpooled.wrappedBuffer(simple.getBytes(CharsetUtil.UTF_8)),
            TranscoderUtils.encodeStringAsUtf8(simple)
        );

        assertEquals(
            Unpooled.wrappedBuffer(rune.getBytes(CharsetUtil.UTF_8)),
            TranscoderUtils.encodeStringAsUtf8(rune)
        );

        assertEquals(
            Unpooled.wrappedBuffer(greek.getBytes(CharsetUtil.UTF_8)),
            TranscoderUtils.encodeStringAsUtf8(greek)
        );

        assertEquals(
            Unpooled.wrappedBuffer(russian.getBytes(CharsetUtil.UTF_8)),
            TranscoderUtils.encodeStringAsUtf8(russian)
        );
    }

}