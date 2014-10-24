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
