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
package com.couchbase.client.java.document;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;

/**
 * Represents raw {@link ByteBuf} content in a document.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class BinaryDocument extends AbstractDocument<ByteBuf> {

    /**
     * Creates a {@link BinaryDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link BinaryDocument}.
     */
    public static BinaryDocument create(String id) {
        return new BinaryDocument(id, 0, null, 0, null);
    }

    /**
     * Creates a {@link BinaryDocument} which the document id and content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link BinaryDocument}.
     */
    public static BinaryDocument create(String id, ByteBuf content) {
        return new BinaryDocument(id, 0, content, 0, null);
    }

    /**
     * Creates a {@link BinaryDocument} which the document id, content and the CAS value.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link BinaryDocument}.
     */
    public static BinaryDocument create(String id, ByteBuf content, long cas) {
        return new BinaryDocument(id, 0, content, cas, null);
    }

    /**
     * Creates a {@link BinaryDocument} which the document id, content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param expiry the expiration time of the document.
     * @return a {@link BinaryDocument}.
     */
    public static BinaryDocument create(String id, int expiry, ByteBuf content) {
        return new BinaryDocument(id, expiry, content, 0, null);
    }

    /**
     * Creates a {@link BinaryDocument} which the document id, content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link BinaryDocument}.
     */
    public static BinaryDocument create(String id, int expiry, ByteBuf content, long cas) {
        return new BinaryDocument(id, expiry, content, cas, null);
    }

    /**
     * Creates a {@link BinaryDocument} which the document id, content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @param mutationToken the optional mutation token of the document.
     * @return a {@link BinaryDocument}.
     */
    public static BinaryDocument create(String id, int expiry, ByteBuf content, long cas, MutationToken mutationToken) {
        return new BinaryDocument(id, expiry, content, cas, mutationToken);
    }

    /**
     * Creates a copy from a different {@link BinaryDocument}, but changes the document ID and content.
     *
     * @param doc the original {@link BinaryDocument} to copy.
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a copied {@link BinaryDocument} with the changed properties.
     */
    public static BinaryDocument from(BinaryDocument doc, String id, ByteBuf content) {
        return BinaryDocument.create(id, doc.expiry(), content, doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link BinaryDocument}, but changes the CAS value.
     *
     * @param doc the original {@link BinaryDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link BinaryDocument} with the changed properties.
     */
    public static BinaryDocument from(BinaryDocument doc, long cas) {
        return BinaryDocument.create(doc.id(), doc.expiry(), doc.content(), cas, doc.mutationToken());
    }

    /**
     * Private constructor which is called by the static factory methods eventually.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     */
    private BinaryDocument(String id, int expiry, ByteBuf content, long cas, MutationToken mutationToken) {
        super(id, expiry, content, cas, mutationToken);
    }

}
