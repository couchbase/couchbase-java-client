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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.lang.Tuple2;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.Document;

/**
 *
 * @param <D>
 * @param <T>
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface Transcoder<D extends Document<T>, T> {

    D decode(String id, ByteBuf content, long cas, int expiry, int flags, ResponseStatus status);

    Tuple2<ByteBuf, Integer> encode(D document);

    /**
     * Creates a new Document with the passed in information.
     *
     * Use the one with the mutation token instead
     * ({@link #newDocument(String, int, Object, long, MutationToken)}).
     *
     * @param id the id of the document.
     * @param expiry the document expiration.
     * @param content the document content.
     * @param cas the documents cas value.
     * @return the created document.
     */
    @Deprecated
    D newDocument(String id, int expiry, T content, long cas);

    /**
     * Creates a new Document with the passed in information.
     *
     * @param id the id of the document.
     * @param expiry the document expiration.
     * @param content the document content.
     * @param cas the documents cas value.
     * @param mutationToken the documents mutation token.
     * @return the created document.
     */
    D newDocument(String id, int expiry, T content, long cas, MutationToken mutationToken);


    Class<D> documentType();
}
