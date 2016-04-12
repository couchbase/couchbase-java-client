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
import com.couchbase.client.java.document.JsonStringDocument;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode {@link JsonStringDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonStringTranscoder extends AbstractTranscoder<JsonStringDocument, String> {

    @Override
    protected JsonStringDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags,
        ResponseStatus status) throws Exception {
        String decoded = content.toString(CharsetUtil.UTF_8);
        if (TranscoderUtils.hasCommonFlags(flags) && flags == TranscoderUtils.JSON_COMMON_FLAGS) {
            decoded = decoded.substring(1, decoded.length() - 1);
        } else if (flags == 0) {
            if (decoded.startsWith("\"") && decoded.endsWith("\"")) {
                decoded = decoded.substring(1, decoded.length() - 1);
            }
        } else {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non " +
                "JsonStringDocument id " + id + ", could not decode.");
        }

        return newDocument(id, expiry, decoded, cas);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(JsonStringDocument document) throws Exception {
        return Tuple.create(
            TranscoderUtils.encodeStringAsUtf8("\"" + document.content() + "\""),
            TranscoderUtils.JSON_COMPAT_FLAGS
        );
    }

    @Override
    public JsonStringDocument newDocument(String id, int expiry, String content, long cas) {
        return JsonStringDocument.create(id, expiry, content, cas);
    }

    @Override
    public JsonStringDocument newDocument(String id, int expiry, String content, long cas,
        MutationToken mutationToken) {
        return JsonStringDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<JsonStringDocument> documentType() {
        return JsonStringDocument.class;
    }
}
