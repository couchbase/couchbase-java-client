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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.bucket.BucketType;

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

}
