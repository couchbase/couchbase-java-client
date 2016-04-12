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
 * Represents a {@link Document} that contains a already encoded JSON document.
 *
 * The {@link RawJsonDocument} can be used if a custom JSON library is already in place and the content should just
 * be passed through and properly flagged as JSON on the server side. The only transcoding that is happening internally
 * is the conversion into bytes from the provided JSON string.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class RawJsonDocument extends AbstractDocument<String> implements Serializable {

    private static final long serialVersionUID = 375731014642624274L;

    /**
     * Creates a {@link RawJsonDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link RawJsonDocument}.
     */
    public static RawJsonDocument create(String id) {
        return new RawJsonDocument(id, 0, null, 0, null);
    }

    /**
     * Creates a {@link RawJsonDocument} which the document id and JSON content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link RawJsonDocument}.
     */
    public static RawJsonDocument create(String id, String content) {
        return new RawJsonDocument(id, 0, content, 0, null);
    }

    /**
     * Creates a {@link RawJsonDocument} which the document id, JSON content and the CAS value.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link RawJsonDocument}.
     */
    public static RawJsonDocument create(String id, String content, long cas) {
        return new RawJsonDocument(id, 0, content, cas, null);
    }

    /**
     * Creates a {@link RawJsonDocument} which the document id, JSON content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param expiry the expiration time of the document.
     * @return a {@link RawJsonDocument}.
     */
    public static RawJsonDocument create(String id, int expiry, String content) {
        return new RawJsonDocument(id, expiry, content, 0, null);
    }

    /**
     * Creates a {@link RawJsonDocument} which the document id, JSON content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link RawJsonDocument}.
     */
    public static RawJsonDocument create(String id, int expiry, String content, long cas) {
        return new RawJsonDocument(id, expiry, content, cas, null);
    }

    /**
     * Creates a {@link RawJsonDocument} which the document id, JSON content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link RawJsonDocument}.
     */
    public static RawJsonDocument create(String id, int expiry, String content, long cas, MutationToken mutationToken) {
        return new RawJsonDocument(id, expiry, content, cas, mutationToken);
    }

    /**
     * Creates a copy from a different {@link RawJsonDocument}, but changes the document ID and content.
     *
     * @param doc the original {@link RawJsonDocument} to copy.
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a copied {@link RawJsonDocument} with the changed properties.
     */
    public static RawJsonDocument from(RawJsonDocument doc, String id, String content) {
        return RawJsonDocument.create(id, doc.expiry(), content, doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link RawJsonDocument}, but changes the CAS value.
     *
     * @param doc the original {@link RawJsonDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link RawJsonDocument} with the changed properties.
     */
    public static RawJsonDocument from(RawJsonDocument doc, long cas) {
        return RawJsonDocument.create(doc.id(), doc.expiry(), doc.content(), cas, doc.mutationToken());
    }

    /**
     * Private constructor which is called by the static factory methods eventually.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     */
    private RawJsonDocument(String id, int expiry, String content, long cas, MutationToken mutationToken) {
        super(id, expiry, content, cas, mutationToken);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        writeToSerializedStream(stream);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        readFromSerializedStream(stream);
    }

}
