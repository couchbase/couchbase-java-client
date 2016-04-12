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