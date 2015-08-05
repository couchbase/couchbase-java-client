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
