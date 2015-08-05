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

import com.couchbase.client.core.message.kv.MutationToken;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Stores a properly encoded JSON scalar quoted string as the toplevel type.
 *
 * This document works exactly like {@link JsonDocument}, but it accepts a different toplevel type. This document
 * is interoperable with other SDKs.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonStringDocument extends AbstractDocument<String> implements Serializable {

    private static final long serialVersionUID = -2404431009274846282L;

    /**
     * Creates a {@link JsonStringDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link JsonStringDocument}.
     */
    public static JsonStringDocument create(String id) {
        return new JsonStringDocument(id, 0, null, 0, null);
    }

    /**
     * Creates a {@link JsonStringDocument} which the document id and content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link JsonStringDocument}.
     */
    public static JsonStringDocument create(String id, String content) {
        return new JsonStringDocument(id, 0, content, 0, null);
    }

    /**
     * Creates a {@link JsonStringDocument} which the document id, content and the CAS value.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link JsonStringDocument}.
     */
    public static JsonStringDocument create(String id, String content, long cas) {
        return new JsonStringDocument(id, 0, content, cas, null);
    }

    /**
     * Creates a {@link JsonStringDocument} which the document id, content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param expiry the expiration time of the document.
     * @return a {@link JsonStringDocument}.
     */
    public static JsonStringDocument create(String id, int expiry, String content) {
        return new JsonStringDocument(id, expiry, content, 0, null);
    }

    /**
     * Creates a {@link JsonStringDocument} which the document id, content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link JsonStringDocument}.
     */
    public static JsonStringDocument create(String id, int expiry, String content, long cas) {
        return new JsonStringDocument(id, expiry, content, cas, null);
    }

    /**
     * Creates a {@link JsonStringDocument} which the document id, content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link JsonStringDocument}.
     */
    public static JsonStringDocument create(String id, int expiry, String content, long cas, MutationToken mutationToken) {
        return new JsonStringDocument(id, expiry, content, cas, mutationToken);
    }

    /**
     * Creates a copy from a different {@link JsonStringDocument}, but changes the document ID and content.
     *
     * @param doc the original {@link JsonStringDocument} to copy.
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a copied {@link JsonStringDocument} with the changed properties.
     */
    public static JsonStringDocument from(JsonStringDocument doc, String id, String content) {
        return JsonStringDocument.create(id, doc.expiry(), content, doc.cas(), doc.mutationToken());
    }

    /**
     * Creates a copy from a different {@link JsonStringDocument}, but changes the CAS value.
     *
     * @param doc the original {@link JsonStringDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link JsonStringDocument} with the changed properties.
     */
    public static JsonStringDocument from(JsonStringDocument doc, long cas) {
        return JsonStringDocument.create(doc.id(), doc.expiry(), doc.content(), cas, doc.mutationToken());
    }

    /**
     * Private constructor which is called by the static factory methods eventually.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     */
    private JsonStringDocument(String id, int expiry, String content, long cas, MutationToken mutationToken) {
        super(id, expiry, content, cas, mutationToken);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        writeToSerializedStream(stream);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        readFromSerializedStream(stream);
    }
}
