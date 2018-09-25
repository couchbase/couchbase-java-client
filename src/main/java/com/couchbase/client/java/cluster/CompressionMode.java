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
 * The compression mode used for bucket settings.
 */
public enum CompressionMode {

    /**
     * Compressed documents are accepted but actively decompressed
     * for storage in memory and for streaming. Not advised!
     */
    OFF,

    /**
     * Compressed documents can be stored and streamed from the server,
     * but the server does not try to actively compress documents (client-initiated)
     */
    PASSIVE,

    /**
     * The server will try to actively compress documents in memory
     */
    ACTIVE
}
