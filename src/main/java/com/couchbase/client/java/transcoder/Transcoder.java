package com.couchbase.client.java.transcoder;

import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.Document;

public interface Transcoder<D extends Document<T>, T> {

    D decode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status);

    Tuple2<ByteBuf, Integer> encode(D document);

    D newDocument(String id, int expiry, T content, long cas);

    Class<D> documentType();
}
