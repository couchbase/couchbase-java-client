/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
