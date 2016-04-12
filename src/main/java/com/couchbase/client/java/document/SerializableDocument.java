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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Handles content which implements {@link Serializable}.
 *
 * This document is not interoperable with other SDKs, since java object serialization is not convertible
 * into other programming languages. It is compatible with the legacy object serialization from the 1.*
 * SDK series.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class SerializableDocument extends AbstractDocument<Serializable> implements Serializable {

    private static final long serialVersionUID = 2153366534711753989L;

    /**
     * Creates a {@link SerializableDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link SerializableDocument}.
     */
    public static SerializableDocument create(String id) {
        return new SerializableDocument(id, 0, null, 0, null);
    }

    /**
     * Creates a {@link SerializableDocument} which the document id and content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link SerializableDocument}.
     */
    public static SerializableDocument create(String id, Serializable content) {
        return new SerializableDocument(id, 0, content, 0, null);
    }

    /**
     * Creates a {@link SerializableDocument} which the document id, content and the CAS value.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link SerializableDocument}.
     */
    public static SerializableDocument create(String id, Serializable content, long cas) {
        return new SerializableDocument(id, 0, content, cas, null);
    }

    /**
     * Creates a {@link SerializableDocument} which the document id, content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param expiry the expiration time of the document.
     * @return a {@link SerializableDocument}.
     */
    public static SerializableDocument create(String id, int expiry, Serializable content) {
        return new SerializableDocument(id, expiry, content, 0, null);
    }

    /**
     * Creates a {@link SerializableDocument} which the document id, content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link SerializableDocument}.
     */
    public static SerializableDocument create(String id, int expiry, Serializable content, long cas) {
        return new SerializableDocument(id, expiry, content, cas, null);
    }

    /**
     * Creates a {@link SerializableDocument} which the document id, content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link SerializableDocument}.
     */
    public static SerializableDocument create(String id, int expiry, Serializable content, long cas, MutationToken mutationToken) {
        return new SerializableDocument(id, expiry, content, cas, mutationToken);
    }

    /**
     * Creates a copy from a different {@link SerializableDocument}, but changes the document ID.
     *
     * @param doc the original {@link SerializableDocument} to copy.
     * @param id the per-bucket unique document id.
     * @return a copied {@link SerializableDocument} with the changed properties.
     */
    public static SerializableDocument from(SerializableDocument doc, String id) {
        return SerializableDocument.create(id, doc.expiry(), doc.content(), doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link SerializableDocument}, but changes the content.
     *
     * @param doc the original {@link SerializableDocument} to copy.
     * @param content the content of the document.
     * @return a copied {@link SerializableDocument} with the changed properties.
     */
    public static SerializableDocument from(SerializableDocument doc, Long content) {
        return SerializableDocument.create(doc.id(), doc.expiry(), content, doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link SerializableDocument}, but changes the document ID and content.
     *
     * @param doc the original {@link SerializableDocument} to copy.
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a copied {@link SerializableDocument} with the changed properties.
     */
    public static SerializableDocument from(SerializableDocument doc, String id, Long content) {
        return SerializableDocument.create(id, doc.expiry(), content, doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link SerializableDocument}, but changes the CAS value.
     *
     * @param doc the original {@link SerializableDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link SerializableDocument} with the changed properties.
     */
    public static SerializableDocument from(SerializableDocument doc, long cas) {
        return SerializableDocument.create(doc.id(), doc.expiry(), doc.content(), cas, doc.mutationToken());
    }

    /**
     * Private constructor which is called by the static factory methods eventually.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     */
    private SerializableDocument(String id, int expiry, Serializable content, long cas, MutationToken mutationToken) {
        super(id, expiry, content, cas, mutationToken);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        writeToSerializedStream(stream);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        readFromSerializedStream(stream);
    }
}
