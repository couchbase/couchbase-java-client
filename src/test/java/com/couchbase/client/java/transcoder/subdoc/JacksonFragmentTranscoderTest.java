/*
 * Copyright (C) 2016 Couchbase, Inc.
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

package com.couchbase.client.java.transcoder.subdoc;

import static org.junit.Assert.assertEquals;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.deps.io.netty.util.ReferenceCountUtil;
import com.couchbase.client.java.subdoc.MultiValue;
import org.junit.Test;

public class JacksonFragmentTranscoderTest {

    private static JacksonFragmentTranscoder transcoder = new JacksonFragmentTranscoder(new ObjectMapper());

    @Test
    public void testDoEncodeSingleSimpleValue() throws Exception {
        ByteBuf buf = ReferenceCountUtil.releaseLater(transcoder.doEncodeSingle(123, "test"));

        assertEquals("123", buf.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testDoEncodeSingleArbitraryObject() throws Exception {
        ByteBuf buf = ReferenceCountUtil.releaseLater(transcoder.doEncodeSingle(new Item("foo", false, 123, 45.6), "test"));

        assertEquals("{\"s\":\"foo\",\"b\":false,\"i\":123,\"d\":45.6}", buf.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testDoEncodeMultiSimpleValues() throws Exception {
        MultiValue<?> single = new MultiValue<Object>("single");
        MultiValue<?> multi = new MultiValue<Object>(123, "ABC", true, null);

        ByteBuf singleBuf = ReferenceCountUtil.releaseLater(transcoder.doEncodeMulti(single, "test"));
        ByteBuf multiBuf = ReferenceCountUtil.releaseLater(transcoder.doEncodeMulti(multi, "test"));

        assertEquals("\"single\"", singleBuf.toString(CharsetUtil.UTF_8));
        assertEquals("123,\"ABC\",true,null", multiBuf.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testDoEncodeMultiObjectValues() throws Exception {
        MultiValue<Item> multi = new MultiValue<Item>(null,
                new Item("v1", true, 123, 45.6),
                new Item("v2", false, 456, 321.0));

        ByteBuf multiBuf = ReferenceCountUtil.releaseLater(transcoder.doEncodeMulti(multi, "test"));

        assertEquals("null,{\"s\":\"v1\",\"b\":true,\"i\":123,\"d\":45.6}," +
                "{\"s\":\"v2\",\"b\":false,\"i\":456,\"d\":321.0}", multiBuf.toString(CharsetUtil.UTF_8));
    }

    private class Item {
        private String s;
        private boolean b;
        private int i;
        private double d;

        public Item(String s, boolean b, int i, double d) {
            this.s = s;
            this.b = b;
            this.i = i;
            this.d = d;
        }

        public String getS() {
            return s;
        }

        public boolean isB() {
            return b;
        }

        public int getI() {
            return i;
        }

        public double getD() {
            return d;
        }

        public void setS(String s) {
            this.s = s;
        }

        public void setB(boolean b) {
            this.b = b;
        }

        public void setI(int i) {
            this.i = i;
        }

        public void setD(double d) {
            this.d = d;
        }
    }
}