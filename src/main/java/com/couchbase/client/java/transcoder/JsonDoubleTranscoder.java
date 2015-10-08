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

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonDoubleDocument;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode {@link JsonDoubleDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonDoubleTranscoder extends AbstractTranscoder<JsonDoubleDocument, Double> {

    @Override
    protected JsonDoubleDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception {
        double decoded;

        if (TranscoderUtils.hasCommonFlags(flags) && flags == TranscoderUtils.JSON_COMMON_FLAGS) {
            String val = content.toString(CharsetUtil.UTF_8);
            decoded = JacksonTransformers.MAPPER.readValue(val, Double.class);
        } else if (flags == 6 << 8 || flags == 7 << 8) {
            long rv = 0;
            int readable = content.readableBytes();
            for (int i = 0; i < readable; i++) {
                byte b = content.readByte();
                rv = (rv << 8) | (b < 0 ? 256 + b : b);
            }
            if (flags == 6 << 8) {
                decoded = Float.intBitsToFloat((int) rv);
            } else {
                decoded = Double.longBitsToDouble(rv);
            }
        } else {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non " +
                "JsonDoubleDocument id " + id + ", could not decode.");
        }

        return newDocument(id, expiry, decoded, cas);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(final JsonDoubleDocument document) throws Exception {
        return Tuple.create(
            TranscoderUtils.encodeStringAsUtf8(
                JacksonTransformers.MAPPER.writeValueAsString(document.content())
            ),
            TranscoderUtils.DOUBLE_COMPAT_FLAGS
        );
    }

    @Override
    public JsonDoubleDocument newDocument(String id, int expiry, Double content, long cas) {
        return JsonDoubleDocument.create(id, expiry, content, cas);
    }

    @Override
    public JsonDoubleDocument newDocument(String id, int expiry, Double content, long cas,
        MutationToken mutationToken) {
        return JsonDoubleDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<JsonDoubleDocument> documentType() {
        return JsonDoubleDocument.class;
    }
}
