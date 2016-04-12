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
import com.couchbase.client.java.document.StringDocument;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode {@link StringDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class StringTranscoder extends AbstractTranscoder<StringDocument, String> {

    @Override
    protected StringDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags,
        ResponseStatus status) throws Exception {
        if (!TranscoderUtils.hasStringFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-String document for "
                + "id " + id + ", could not decode.");
        }
        return newDocument(id, expiry, content.toString(CharsetUtil.UTF_8), cas);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(StringDocument document) throws Exception {
        return Tuple.create(
            TranscoderUtils.encodeStringAsUtf8(document.content()),
            TranscoderUtils.STRING_COMMON_FLAGS
        );
    }

    @Override
    public StringDocument newDocument(String id, int expiry, String content, long cas) {
        return StringDocument.create(id, expiry, content, cas);
    }

    @Override
    public StringDocument newDocument(String id, int expiry, String content, long cas,
        MutationToken mutationToken) {
        return StringDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<StringDocument> documentType() {
        return StringDocument.class;
    }
}
