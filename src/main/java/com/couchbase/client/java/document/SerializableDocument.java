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
package com.couchbase.client.java.document;

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
        return new SerializableDocument(id, 0, null, 0);
    }

    /**
     * Creates a {@link SerializableDocument} which the document id and content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link SerializableDocument}.
     */
    public static SerializableDocument create(String id, Serializable content) {
        return new SerializableDocument(id, 0, content, 0);
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
        return new SerializableDocument(id, 0, content, cas);
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
        return new SerializableDocument(id, expiry, content, 0);
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
        return new SerializableDocument(id, expiry, content, cas);
    }

    /**
     * Creates a copy from a different {@link SerializableDocument}, but changes the document ID.
     *
     * @param doc the original {@link SerializableDocument} to copy.
     * @param id the per-bucket unique document id.
     * @return a copied {@link SerializableDocument} with the changed properties.
     */
    public static SerializableDocument from(SerializableDocument doc, String id) {
        return SerializableDocument.create(id, doc.expiry(), doc.content(), doc.cas());
    }

    /**
     * Creates a copy from a different {@link SerializableDocument}, but changes the content.
     *
     * @param doc the original {@link SerializableDocument} to copy.
     * @param content the content of the document.
     * @return a copied {@link SerializableDocument} with the changed properties.
     */
    public static SerializableDocument from(SerializableDocument doc, Long content) {
        return SerializableDocument.create(doc.id(), doc.expiry(), content, doc.cas());
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
        return SerializableDocument.create(id, doc.expiry(), content, doc.cas());
    }

    /**
     * Creates a copy from a different {@link SerializableDocument}, but changes the CAS value.
     *
     * @param doc the original {@link SerializableDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link SerializableDocument} with the changed properties.
     */
    public static SerializableDocument from(SerializableDocument doc, long cas) {
        return SerializableDocument.create(doc.id(), doc.expiry(), doc.content(), cas);
    }

    /**
     * Private constructor which is called by the static factory methods eventually.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     */
    private SerializableDocument(String id, int expiry, Serializable content, long cas) {
        super(id, expiry, content, cas);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        writeToSerializedStream(stream);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        readFromSerializedStream(stream);
    }
}
