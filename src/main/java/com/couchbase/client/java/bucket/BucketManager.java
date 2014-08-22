package com.couchbase.client.java.bucket;

import com.couchbase.client.java.view.DesignDocument;
import rx.Observable;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface BucketManager {

    Observable<BucketInfo> info();

    Observable<Boolean> flush();

    Observable<DesignDocument> getDesignDocuments();
    Observable<DesignDocument> getDesignDocuments(boolean development);

    Observable<DesignDocument> getDesignDocument(String name);
    Observable<DesignDocument> getDesignDocument(String name, boolean development);

    Observable<DesignDocument> insertDesignDocument(DesignDocument designDocument);
    Observable<DesignDocument> insertDesignDocument(DesignDocument designDocument, boolean development);

    Observable<DesignDocument> upsertDesignDocument(DesignDocument designDocument);
    Observable<DesignDocument> upsertDesignDocument(DesignDocument designDocument, boolean development);

    Observable<Boolean> removeDesignDocument(String name);
    Observable<Boolean> removeDesignDocument(String name, boolean development);

    Observable<DesignDocument> publishDesignDocument(String name);
    Observable<DesignDocument> publishDesignDocument(String name, boolean overwrite);
}
