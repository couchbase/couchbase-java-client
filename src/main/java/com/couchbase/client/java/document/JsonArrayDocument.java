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

import com.couchbase.client.java.document.json.JsonArray;

/**
 * Stores a properly encoded JSON array as the toplevel type.
 *
 * This document works exactly like {@link JsonDocument}, but it accepts a different toplevel type. This document
 * is interoperable with other SDKs.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonArrayDocument extends AbstractDocument<JsonArray> {

    /**
     * Creates a {@link JsonDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link JsonDocument}.
     */
    public static JsonArrayDocument create(String id) {
        return new JsonArrayDocument(id, 0, null, 0);
    }

    /**
     * Creates a {@link JsonDocument} which the document id and JSON content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link JsonDocument}.
     */
    public static JsonArrayDocument create(String id, JsonArray content) {
        return new JsonArrayDocument(id, 0, content, 0);
    }

    /**
     * Creates a {@link JsonDocument} which the document id, JSON content and the CAS value.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link JsonDocument}.
     */
    public static JsonArrayDocument create(String id, JsonArray content, long cas) {
        return new JsonArrayDocument(id, 0, content, cas);
    }

    /**
     * Creates a {@link JsonDocument} which the document id, JSON content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param expiry the expiration time of the document.
     * @return a {@link JsonArrayDocument}.
     */
    public static JsonArrayDocument create(String id, int expiry, JsonArray content) {
        return new JsonArrayDocument(id, expiry, content, 0);
    }

    /**
     * Creates a {@link JsonDocument} which the document id, JSON content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link JsonArrayDocument}.
     */
    public static JsonArrayDocument create(String id, int expiry, JsonArray content, long cas) {
        return new JsonArrayDocument(id, expiry, content, cas);
    }

    /**
     * Creates a copy from a different {@link JsonArrayDocument}, but changes the document ID.
     *
     * @param doc the original {@link JsonArrayDocument} to copy.
     * @param id the per-bucket unique document id.
     * @return a copied {@link JsonArrayDocument} with the changed properties.
     */
    public static JsonArrayDocument from(JsonArrayDocument doc, String id) {
        return JsonArrayDocument.create(id, doc.expiry(), doc.content(), doc.cas());
    }

    /**
     * Creates a copy from a different {@link JsonDocument}, but changes the content.
     *
     * @param doc the original {@link JsonDocument} to copy.
     * @param content the content of the document.
     * @return a copied {@link JsonDocument} with the changed properties.
     */
    public static JsonArrayDocument from(JsonArrayDocument doc, JsonArray content) {
        return JsonArrayDocument.create(doc.id(), doc.expiry(), content, doc.cas());
    }

    /**
     * Creates a copy from a different {@link JsonDocument}, but changes the document ID and content.
     *
     * @param doc the original {@link JsonDocument} to copy.
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a copied {@link JsonDocument} with the changed properties.
     */
    public static JsonArrayDocument from(JsonArrayDocument doc, String id, JsonArray content) {
        return JsonArrayDocument.create(id, doc.expiry(), content, doc.cas());
    }

    /**
     * Creates a copy from a different {@link JsonDocument}, but changes the CAS value.
     *
     * @param doc the original {@link JsonDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link JsonDocument} with the changed properties.
     */
    public static JsonArrayDocument from(JsonArrayDocument doc, long cas) {
        return JsonArrayDocument.create(doc.id(), doc.expiry(), doc.content(), cas);
    }

    /**
     * Private constructor which is called by the static factory methods eventually.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     */
    private JsonArrayDocument(String id, int expiry, JsonArray content, long cas) {
        super(id, expiry, content, cas);
    }
}
