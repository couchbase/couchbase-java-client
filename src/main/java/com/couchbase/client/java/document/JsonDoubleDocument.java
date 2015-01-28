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

/**
 * Stores a properly encoded JSON scalar double or float as the toplevel type.
 *
 * This document works exactly like {@link JsonDocument}, but it accepts a different toplevel type. This document
 * is interoperable with other SDKs.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonDoubleDocument extends AbstractDocument<Double> {

    /**
     * Creates a {@link JsonDoubleDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link JsonDoubleDocument}.
     */
    public static JsonDoubleDocument create(String id) {
        return new JsonDoubleDocument(id, 0, null, 0);
    }

    /**
     * Creates a {@link JsonDoubleDocument} which the document id and content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link JsonDoubleDocument}.
     */
    public static JsonDoubleDocument create(String id, Double content) {
        return new JsonDoubleDocument(id, 0, content, 0);
    }

    /**
     * Creates a {@link JsonDoubleDocument} which the document id, content and the CAS value.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link JsonDoubleDocument}.
     */
    public static JsonDoubleDocument create(String id, Double content, long cas) {
        return new JsonDoubleDocument(id, 0, content, cas);
    }

    /**
     * Creates a {@link JsonDoubleDocument} which the document id, content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param expiry the expiration time of the document.
     * @return a {@link JsonDoubleDocument}.
     */
    public static JsonDoubleDocument create(String id, int expiry, Double content) {
        return new JsonDoubleDocument(id, expiry, content, 0);
    }

    /**
     * Creates a {@link JsonDoubleDocument} which the document id, content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link JsonDoubleDocument}.
     */
    public static JsonDoubleDocument create(String id, int expiry, Double content, long cas) {
        return new JsonDoubleDocument(id, expiry, content, cas);
    }

    /**
     * Creates a copy from a different {@link JsonDoubleDocument}, but changes the document ID.
     *
     * @param doc the original {@link JsonDoubleDocument} to copy.
     * @param id the per-bucket unique document id.
     * @return a copied {@link JsonDoubleDocument} with the changed properties.
     */
    public static JsonDoubleDocument from(JsonDoubleDocument doc, String id) {
        return JsonDoubleDocument.create(id, doc.expiry(), doc.content(), doc.cas());
    }

    /**
     * Creates a copy from a different {@link JsonDoubleDocument}, but changes the content.
     *
     * @param doc the original {@link JsonDoubleDocument} to copy.
     * @param content the content of the document.
     * @return a copied {@link JsonDoubleDocument} with the changed properties.
     */
    public static JsonDoubleDocument from(JsonDoubleDocument doc, Double content) {
        return JsonDoubleDocument.create(doc.id(), doc.expiry(), content, doc.cas());
    }

    /**
     * Creates a copy from a different {@link JsonDoubleDocument}, but changes the document ID and content.
     *
     * @param doc the original {@link JsonDoubleDocument} to copy.
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a copied {@link JsonDoubleDocument} with the changed properties.
     */
    public static JsonDoubleDocument from(JsonDoubleDocument doc, String id, Double content) {
        return JsonDoubleDocument.create(id, doc.expiry(), content, doc.cas());
    }

    /**
     * Creates a copy from a different {@link JsonDoubleDocument}, but changes the CAS value.
     *
     * @param doc the original {@link JsonDoubleDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link JsonDoubleDocument} with the changed properties.
     */
    public static JsonDoubleDocument from(JsonDoubleDocument doc, long cas) {
        return JsonDoubleDocument.create(doc.id(), doc.expiry(), doc.content(), cas);
    }

    /**
     * Private constructor which is called by the static factory methods eventually.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     */
    private JsonDoubleDocument(String id, int expiry, Double content, long cas) {
        super(id, expiry, content, cas);
    }
}
