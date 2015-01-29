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
package com.couchbase.client.java.bucket;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonObject;

import java.net.InetAddress;
import java.util.List;

/**
 * Provides information about a {@link Bucket}.
 *
 * Selected bucket properties are available through explicit getters, the full (raw JSON) response from the server
 * is accessible through the {@link #raw()} method. Note that the response is subject to change across server
 * versions and therefore should be properly checked before being used.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface BucketInfo {

    /**
     * The name of the bucket.
     *
     * @return the bucket name.
     */
    String name();

    /**
     * The type of the bucket.
     *
     * @return the bucket type.
     */
    BucketType type();

    /**
     * The number of nodes on the bucket.
     *
     * @return number of nodes.
     */
    int nodeCount();

    /**
     * The number of replicas configured.
     *
     * @return number of replicas configured.
     */
    int replicaCount();

    /**
     * Raw JSON server response for advanced analysis.
     *
     * @return the raw JSON bucket info.
     */
    JsonObject raw();

    /**
     * Returns a list of nodes that is interacting with the bucket.
     *
     * @return the list of nodes.
     */
    List<InetAddress> nodeList();

}
