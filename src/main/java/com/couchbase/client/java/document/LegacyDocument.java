package com.couchbase.client.java.document;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class LegacyDocument extends AbstractDocument<Object> {


    /**
     * Creates a empty {@link LegacyDocument}.
     *
     * @return a empty {@link LegacyDocument}.
     */
    public static LegacyDocument empty() {
        return new LegacyDocument(null, null, 0, 0, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id) {
        return new LegacyDocument(id, null, 0, 0, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id and JSON content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, Object content) {
        return new LegacyDocument(id, content, 0, 0, null);
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
        return new LegacyDocument(id, content, cas, 0, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id, JSON content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param expiry the expiration time of the document.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, Object content, int expiry) {
        return new LegacyDocument(id, content, 0, expiry, null);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id, JSON content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @param status the response status as returned by the underlying infrastructure.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, Object content, long cas, int expiry, ResponseStatus status) {
        return new LegacyDocument(id, content, cas, expiry, status);
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the document ID.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param id the per-bucket unique document id.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, String id) {
        return LegacyDocument.create(id, doc.content(), doc.cas(), doc.expiry(), doc.status());
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the content.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param content the content of the document.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, Object content) {
        return LegacyDocument.create(doc.id(), content, doc.cas(), doc.expiry(), doc.status());
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
        return LegacyDocument.create(id, content, doc.cas(), doc.expiry(), doc.status());
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the CAS value.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, long cas) {
        return LegacyDocument.create(doc.id(), doc.content(), cas, doc.expiry(), doc.status());
    }

    private LegacyDocument(String id, Object content, long cas, int expiry, ResponseStatus status) {
        super(id, content, cas, expiry, status);
    }
}
