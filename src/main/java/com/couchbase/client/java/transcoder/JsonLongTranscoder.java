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
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.document.JsonDoubleDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode {@link JsonDoubleDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonLongTranscoder extends AbstractTranscoder<JsonLongDocument, Long> {

    @Override
    protected JsonLongDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception {
        long decoded;

        if (TranscoderUtils.hasCommonFlags(flags) && flags == TranscoderUtils.JSON_COMMON_FLAGS) {
            String val = content.toString(CharsetUtil.UTF_8);
            decoded = Long.valueOf(val);
        } else if (flags == 3 << 8 || flags == 2 << 8) {
            long rv = 0;
            int readable = content.readableBytes();
            for (int i = 0; i < readable; i++) {
                byte b = content.readByte();
                rv = (rv << 8) | (b < 0 ? 256 + b : b);
            }
            decoded = rv;
        } else if (flags == 0) {
            String val = content.toString(CharsetUtil.UTF_8);
            decoded = Long.valueOf(val);
        } else {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non " +
                "JsonLongDocument id " + id + ", could not decode.");
        }

        return newDocument(id, expiry, decoded, cas);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(final JsonLongDocument document) throws Exception {
        return Tuple.create(
            TranscoderUtils.encodeStringAsUtf8(String.valueOf(document.content())),
            TranscoderUtils.LONG_COMPAT_FLAGS
        );
    }

    @Override
    public JsonLongDocument newDocument(String id, int expiry, Long content, long cas) {
        return JsonLongDocument.create(id, expiry, content, cas);
    }

    @Override
    public JsonLongDocument newDocument(String id, int expiry, Long content, long cas,
        MutationToken mutationToken) {
        return JsonLongDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<JsonLongDocument> documentType() {
        return JsonLongDocument.class;
    }
}
