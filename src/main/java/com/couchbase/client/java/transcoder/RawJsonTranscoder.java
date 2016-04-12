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
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode a {@link RawJsonDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class RawJsonTranscoder extends AbstractTranscoder<RawJsonDocument, String> {

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(RawJsonDocument document) throws Exception {
        return Tuple.create(
            TranscoderUtils.encodeStringAsUtf8(document.content()),
            TranscoderUtils.JSON_COMPAT_FLAGS
        );
    }

    @Override
    protected RawJsonDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags,
        ResponseStatus status) throws Exception {
        if (!TranscoderUtils.hasJsonFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-JSON document for "
                + "id " + id + ", could not decode.");
        }

        String converted = content.toString(CharsetUtil.UTF_8);
        return newDocument(id, expiry, converted, cas);
    }

    @Override
    public RawJsonDocument newDocument(String id, int expiry, String content, long cas) {
        return RawJsonDocument.create(id, expiry, content, cas);
    }

    @Override
    public RawJsonDocument newDocument(String id, int expiry, String content, long cas,
        MutationToken mutationToken) {
        return RawJsonDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<RawJsonDocument> documentType() {
        return RawJsonDocument.class;
    }
}
