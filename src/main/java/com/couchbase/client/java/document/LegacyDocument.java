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

/**
 * This document is fully compatible with Java SDK 1.* stored documents.
 *
 * It is not compatible with other SDKs. It should be used to interact with legacy documents and code, but it is
 * recommended to switch to the unifying document types (Json* and String) if possible to guarantee better
 * interoperability in the future.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class LegacyDocument extends AbstractDocument<Object> {

    /**
     * Creates a {@link LegacyDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id) {
        return new LegacyDocument(id, 0, null, 0, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id and JSON content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, Object content) {
        return new LegacyDocument(id, 0, content, 0, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id, JSON content and the CAS value.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, Object content, long cas) {
        return new LegacyDocument(id, 0, content, cas, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id, JSON content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param expiry the expiration time of the document.
     * @param content the content of the document.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, int expiry, Object content) {
        return new LegacyDocument(id, expiry, content, 0, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id, JSON content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param expiry the expiration time of the document.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, int expiry, Object content, long cas) {
        return new LegacyDocument(id, expiry, content, cas, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id, JSON content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param expiry the expiration time of the document.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, int expiry, Object content, long cas, MutationToken mutationToken) {
        return new LegacyDocument(id, expiry, content, cas, mutationToken);
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the document ID.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param id the per-bucket unique document id.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, String id) {
        return LegacyDocument.create(id, doc.expiry(), doc.content(), doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the content.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param content the content of the document.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, Object content) {
        return LegacyDocument.create(doc.id(), doc.expiry(), content, doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the document ID and content.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, String id, Object content) {
        return LegacyDocument.create(id, doc.expiry(), content, doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the CAS value.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, long cas) {
        return LegacyDocument.create(doc.id(), doc.expiry(), doc.content(), cas, doc.mutationToken());
    }

    private LegacyDocument(String id, int expiry, Object content, long cas, MutationToken mutationToken) {
        super(id, expiry, content, cas, mutationToken);
    }
}
