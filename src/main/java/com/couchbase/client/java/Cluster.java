package com.couchbase.client.java;

import rx.Observable;

/**
 * Represents a Couchbase Server cluster.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public interface Cluster {

    /**
     * Open the default {@link Bucket}.
     *
     * @return a {@link Observable} containing the {@link Bucket} reference once open.
     */
    Observable<Bucket> openBucket();

    /**
     * Open the given {@link Bucket} without a password.
     *
     * @return a {@link Observable} containing the {@link Bucket} reference once open.
     */
    Observable<Bucket> openBucket(String name);

    /**
     * Open the given {@link Bucket} with a password.
     *
     * @return a {@link Observable} containing the {@link Bucket} reference once open.
     */
    Observable<Bucket> openBucket(String name, String password);

    /**
     * Disconnects from the {@link Cluster} and closes all open buckets.
     *
     * @return a {@link Observable} containing true if successful.
     */
    Observable<Boolean> disconnect();
}
