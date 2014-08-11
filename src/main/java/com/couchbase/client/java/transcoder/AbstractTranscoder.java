package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.Document;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public abstract class AbstractTranscoder<D extends Document<T>, T> implements Transcoder<D, T> {

    @Override
    public D decode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status) {
        try {
            return doDecode(id, content, cas, expiry, flags, status);
        } catch(Exception ex) {
            throw new TranscodingException("Could not decode document with ID " + id, ex);
        }
    }

    @Override
    public Tuple2<ByteBuf, Integer> encode(D document) {
        try {
            return doEncode(document);
        } catch(Exception ex) {
            throw new TranscodingException("Could not encode document with ID " + document.id(), ex);
        }
    }

    protected abstract D doDecode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status)
        throws Exception;
    protected abstract Tuple2<ByteBuf, Integer> doEncode(D document)
        throws Exception;
}
