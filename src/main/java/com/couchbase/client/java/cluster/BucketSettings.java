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
package com.couchbase.client.java.cluster;

import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * {@link BucketSettings} represent changeable properties for a {@link Bucket}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface BucketSettings {

    /**
     * The name of the bucket.
     *
     * @return name of the bucket.
     */
    String name();

    /**
     * The type of the bucket.
     *
     * @return type of the bucket.
     */
    BucketType type();

    /**
     * The bucket quota.
     *
     * @return bucket quota.
     */
    int quota();

    /**
     * The optional proxy port.
     *
     * @return proxy port.
     */
    int port();

    /**
     * The password of the bucket.
     *
     * @return password.
     */
    String password();

    /**
     * Number of replicas.
     *
     * @return number of replicas.
     */
    int replicas();

    /**
     * If replicas are indexed.
     *
     * @return indexing replicas.
     */
    boolean indexReplicas();

    /**
     * If flush is enabled.
     *
     * @return flush enabled.
     */
    boolean enableFlush();

    /**
     * The different compression modes for the bucket.
     *
     * @return the compression mode selected.
     */
    CompressionMode compressionMode();

    /**
     * The ejection method available for the bucket.
     *
     * @return the ejection method selected.
     */
    EjectionMethod ejectionMethod();

    /**
     * A map of map of advanced settings that are not covered by the native methods of the object
     * but still need to be set when configuring a bucket.
     *
     * To get a full raw representation of an existing bucket's configuration, see {@link #raw()}
     * instead.
     *
     * @return the map of custom advanced settings to use when configuring the bucket.
     */
    Map<String, Object> customSettings();

    /**
     * A raw representation of the bucket settings when acquired from the server. This can be used
     * to get any missing information about the bucket that isn't covered by native methods.
     *
     * To configure a bucket and add settings that are not covered by native methods either, you
     * should instead see {@link #customSettings()}.
     *
     * @return the raw representation of the whole bucket settings, as returned by the server, or
     * an empty {@link JsonObject} if not applicable.
     */
    JsonObject raw();

}
