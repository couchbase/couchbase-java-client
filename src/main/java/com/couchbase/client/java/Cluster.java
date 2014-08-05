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
