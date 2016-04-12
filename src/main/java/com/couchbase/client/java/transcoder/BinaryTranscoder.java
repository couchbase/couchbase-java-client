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
import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.error.TranscodingException;

public class BinaryTranscoder extends AbstractTranscoder<BinaryDocument, ByteBuf> {

    @Override
    protected BinaryDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags,
        ResponseStatus status) throws Exception {
        if (!TranscoderUtils.hasBinaryFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-binary " +
                "document for id " + id + ", could not decode.");
        }
        return BinaryDocument.create(id, expiry, content, cas);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(BinaryDocument document) throws Exception {
        return Tuple.create(document.content(), TranscoderUtils.BINARY_COMPAT_FLAGS);
    }

    @Override
    public BinaryDocument newDocument(String id, int expiry, ByteBuf content, long cas) {
        return BinaryDocument.create(id, expiry, content, cas);
    }

    @Override
    public BinaryDocument newDocument(String id, int expiry, ByteBuf content, long cas,
        MutationToken mutationToken) {
        return BinaryDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<BinaryDocument> documentType() {
        return BinaryDocument.class;
    }

    @Override
    protected boolean shouldAutoReleaseOnDecode() {
        return false;
    }

    @Override
    protected boolean shouldAutoReleaseOnError() {
        return true;
    }
}
