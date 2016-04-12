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

import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonBooleanDocument;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode {@link JsonBooleanDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonBooleanTranscoder extends AbstractTranscoder<JsonBooleanDocument, Boolean> {

    private static final ByteBuf TRUE = Unpooled.unreleasableBuffer(
        Unpooled.wrappedBuffer("true".getBytes(CharsetUtil.UTF_8))
    );
    private static final ByteBuf FALSE = Unpooled.unreleasableBuffer(
        Unpooled.wrappedBuffer("false".getBytes(CharsetUtil.UTF_8))
    );

    @Override
    protected JsonBooleanDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception {
        boolean decoded;

        if (TranscoderUtils.hasCommonFlags(flags) && flags == TranscoderUtils.JSON_COMMON_FLAGS) {
            String val = content.toString(CharsetUtil.UTF_8);
            decoded = val.equals("true");
        } else if (flags == 1 << 8) {
            char val = content.getChar(0);
            decoded = val == '1';
        } else {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non " +
                "JsonBooleanDocument id " + id + ", could not decode.");
        }
        return newDocument(id, expiry, decoded, cas);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(final JsonBooleanDocument document) throws Exception {
        return Tuple.create(
            document.content() ? TRUE : FALSE,
            TranscoderUtils.BOOLEAN_COMPAT_FLAGS
        );
    }

    @Override
    public JsonBooleanDocument newDocument(String id, int expiry, Boolean content, long cas) {
        return JsonBooleanDocument.create(id, expiry, content, cas);
    }

    @Override
    public JsonBooleanDocument newDocument(String id, int expiry, Boolean content, long cas,
        MutationToken mutationToken) {
        return JsonBooleanDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<JsonBooleanDocument> documentType() {
        return JsonBooleanDocument.class;
    }
}
