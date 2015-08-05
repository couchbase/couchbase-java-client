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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.AsyncBucket;

/**
 * Represents a Couchbase Server {@link Document} which is stored in and retrieved from a {@link AsyncBucket}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface Document<T> {

   /**
    * The per-bucket unique ID of the {@link Document}.
    *
    * @return the document id.
    */
    String id();

   /**
    * The content of the {@link Document}.
    *
    * @return the content.
    */
    T content();

   /**
    * The last-known CAS value for the {@link Document} (0 if not set).
    *
    * @return the CAS value if set.
    */
    long cas();

   /**
    * The optional expiration time for the {@link Document} (0 if not set).
    *
    * @return the expiration time.
    */
    int expiry();

    /**
     * The optional, opaque mutation token set after a successful mutation and if enabled on
     * the environment.
     *
     * Note that the mutation token is always null, unless they are explicitly enabled on the
     * environment, the server version is supported (>= 4.0.0) and the mutation operation succeeded.
     *
     * If set, it can be used for enhanced durability requirements, as well as optimized consistency
     * for N1QL queries.
     *
     * @return the mutation token if set, otherwise null.
     */
    MutationToken mutationToken();

}
