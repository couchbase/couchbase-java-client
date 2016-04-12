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
import com.couchbase.client.java.document.SerializableDocument;
import com.couchbase.client.java.error.TranscodingException;

import java.io.Serializable;

/**
 * A transcoder to encode and decode {@link SerializableDocument}s.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class SerializableTranscoder extends AbstractTranscoder<SerializableDocument, Serializable> {

    @Override
    protected SerializableDocument doDecode(String id, ByteBuf content, long cas, int expiry, int flags,
        ResponseStatus status) throws Exception {
        if (!TranscoderUtils.hasSerializableFlags(flags)) {
            throw new TranscodingException("Flags (0x" + Integer.toHexString(flags) + ") indicate non-serialized " +
                "document for id " + id + ", could not decode.");
        }

        Serializable deserialized = TranscoderUtils.deserialize(content);
        return newDocument(id, expiry, deserialized, cas);
    }

    @Override
    protected Tuple2<ByteBuf, Integer> doEncode(final SerializableDocument document) throws Exception {
        return Tuple.create(TranscoderUtils.serialize(document.content()), TranscoderUtils.SERIALIZED_COMPAT_FLAGS);
    }

    @Override
    public SerializableDocument newDocument(String id, int expiry, Serializable content, long cas) {
        return SerializableDocument.create(id, expiry, content, cas);
    }

    @Override
    public SerializableDocument newDocument(String id, int expiry, Serializable content, long cas,
        MutationToken mutationToken) {
        return SerializableDocument.create(id, expiry, content, cas, mutationToken);
    }

    @Override
    public Class<SerializableDocument> documentType() {
        return SerializableDocument.class;
    }
}
