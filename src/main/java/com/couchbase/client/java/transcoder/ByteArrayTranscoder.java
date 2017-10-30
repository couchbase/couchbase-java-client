/*
 * Copyright (c) 2017 Couchbase, Inc.
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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.lang.Tuple;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.document.ByteArrayDocument;
import com.couchbase.client.java.error.TranscodingException;

/**
 * A transcoder to encode and decode {@link ByteArrayDocument}s.
 *
 * This transcoder makes it much easier to work with raw binary data than {@link BinaryTranscoder} since
 * on retry and unsubscribe, buffers don't need to be freed and managed seperately. As a result, for all
 * but special purpose cases it should be preferred over {@link BinaryTranscoder}.
 *
 * @author Michael Nitschinger
 * @since 2.5.2
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class ByteArrayTranscoder extends AbstractTranscoder<ByteArrayDocument, byte[]> {

    @Override
    protected ByteArrayDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags,
        ResponseStatus status) throws Exception {
        if (!TranscoderUtils.hasBinaryFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-binary " +
                "document for id " + id + ", could not decode.");
        }
        byte[] data = new byte[content.readableBytes()];
        content.readBytes(data);
        return newDocument(id, expiry, data, cas);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(ByteArrayDocument document) throws Exception {
        return Tuple.create(
            Unpooled.wrappedBuffer(document.content()),
            TranscoderUtils.BINARY_COMPAT_FLAGS
        );
    }

    @Override
    public ByteArrayDocument newDocument(String id, int expiry, byte[] content, long cas) {
        return ByteArrayDocument.create(id, expiry, content, cas);
    }

    @Override
    public ByteArrayDocument newDocument(String id, int expiry, byte[] content, long cas,
        MutationToken mutationToken) {
        return ByteArrayDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<ByteArrayDocument> documentType() {
        return ByteArrayDocument.class;
    }
}
