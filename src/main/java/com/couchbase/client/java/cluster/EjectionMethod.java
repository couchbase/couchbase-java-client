/*
 * Copyright (c) 2018 Couchbase, Inc.
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

/**
 * The ejection method used on the bucket.
 *
 * Value Ejection needs more system memory, but provides the
 * best performance. Full Ejection reduces the memory overhead
 * requirement.
 */
public enum EjectionMethod {
    /**
     * Value ejection - for couchbase buckets only.
     *
     * <p>During ejection, only the value will be ejected (key and metadata will remain in memory).</p>
     */
    VALUE,

    /**
     * Full ejection - for couchbase buckets only.
     *
     * <p>During ejection, everything (including key, metadata, and value) will be ejected.</p>
     */
    FULL,

    /**
     * No ejection at all - for ephemeral buckets only.
     *
     * <p>Specifying "NO ejection" means that the bucket will not evict items from the cache if the cache is full:
     * this type of eviction policy should be used for in-memory database use-cases.</p>
     */
    NONE,

    /**
     * Not recently used ejection - for ephemeral buckets only.
     *
     * <p>Specifying "NRU ejection" means that items not recently used will be evicted from memory, when all memory in
     * the bucket is used: this type of eviction policy should be used for caching use-cases.</p>
     */
    NRU
}
