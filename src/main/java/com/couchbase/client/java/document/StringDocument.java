package com.couchbase.client.java.document;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class StringDocument extends AbstractDocument<String> {

    /**
     * Creates a empty {@link StringDocument}.
     *
     * @return a empty {@link StringDocument}.
     */
    public static StringDocument empty() {
        return new StringDocument(null, 0, null, 0);
    }

    /**
     * Creates a {@link StringDocument} which the document id.
     *
     * @param id the per-bucket unique document id.
     * @return a {@link StringDocument}.
     */
    public static StringDocument create(String id) {
        return new StringDocument(id, 0, null, 0);
    }

    /**
     * Creates a {@link StringDocument} which the document id and content.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a {@link StringDocument}.
     */
    public static StringDocument create(String id, String content) {
        return new StringDocument(id, 0, content, 0);
    }

    /**
     * Creates a {@link StringDocument} which the document id, content and the CAS value.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a {@link StringDocument}.
     */
    public static StringDocument create(String id, String content, long cas) {
        return new StringDocument(id, 0, content, cas);
    }

    /**
     * Creates a {@link StringDocument} which the document id, content and the expiration time.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param expiry the expiration time of the document.
     * @return a {@link StringDocument}.
     */
    public static StringDocument create(String id, int expiry, String content) {
        return new StringDocument(id, expiry, content, 0);
    }

    /**
     * Creates a {@link StringDocument} which the document id, content, CAS value, expiration time and status code.
     *
     * This factory method is normally only called within the client library when a response is analyzed and a document
     * is returned which is enriched with the status code. It does not make sense to pre populate the status field from
     * the user level code.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     * @return a {@link StringDocument}.
     */
    public static StringDocument create(String id, int expiry, String content, long cas) {
        return new StringDocument(id, expiry, content, cas);
    }

    /**
     * Creates a copy from a different {@link StringDocument}, but changes the document ID and content.
     *
     * @param doc the original {@link StringDocument} to copy.
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @return a copied {@link StringDocument} with the changed properties.
     */
    public static StringDocument from(StringDocument doc, String id, String content) {
        return StringDocument.create(id, doc.expiry(), content, doc.cas());
    }

    /**
     * Creates a copy from a different {@link StringDocument}, but changes the CAS value.
     *
     * @param doc the original {@link StringDocument} to copy.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @return a copied {@link StringDocument} with the changed properties.
     */
    public static StringDocument from(StringDocument doc, long cas) {
        return StringDocument.create(doc.id(), doc.expiry(), doc.content(), cas);
    }

    /**
     * Private constructor which is called by the static factory methods eventually.
     *
     * @param id the per-bucket unique document id.
     * @param content the content of the document.
     * @param cas the CAS (compare and swap) value for optimistic concurrency.
     * @param expiry the expiration time of the document.
     */
    private StringDocument(String id, int expiry, String content, long cas) {
        super(id, expiry, content, cas);
    }


}
