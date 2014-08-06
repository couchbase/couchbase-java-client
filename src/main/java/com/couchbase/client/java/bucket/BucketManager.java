package com.couchbase.client.java.bucket;

import rx.Observable;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface BucketManager {

    Observable<Boolean> flush();

}
