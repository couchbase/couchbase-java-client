package com.couchbase.client.java.document;

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
        return new LegacyDocument(null, 0, null, 0);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id) {
        return new LegacyDocument(id, 0, null, 0);
    }

    /**
     * Creates a {@link LegacyDocument} which the document id and JSON content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link LegacyDocument}.
     */
    public static LegacyDocument create(String id, Object content) {
        return new LegacyDocument(id, 0, content, 0);
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
        return new LegacyDocument(id, 0, content, cas);
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
        return new LegacyDocument(id, expiry, content, 0);
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
        return new LegacyDocument(id, expiry, content, cas);
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the document ID.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param id the per-bucket unique document id.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, String id) {
        return LegacyDocument.create(id, doc.expiry(), doc.content(), doc.cas());
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the content.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param content the content of the document.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, Object content) {
        return LegacyDocument.create(doc.id(), doc.expiry(), content, doc.cas());
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
        return LegacyDocument.create(id, doc.expiry(), content, doc.cas());
    }

    /**
     * Creates a copy from a different {@link LegacyDocument}, but changes the CAS value.
     *
     * @param doc the original {@link LegacyDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link LegacyDocument} with the changed properties.
     */
    public static LegacyDocument from(LegacyDocument doc, long cas) {
        return LegacyDocument.create(doc.id(), doc.expiry(), doc.content(), cas);
    }

    private LegacyDocument(String id, int expiry, Object content, long cas) {
        super(id, expiry, content, cas);
    }
}
