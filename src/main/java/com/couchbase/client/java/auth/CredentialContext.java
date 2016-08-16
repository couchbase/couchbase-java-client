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
package com.couchbase.client.java.auth;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Enum of the contexts that can be used to retrieve an implicit credential from an
 * {@link Authenticator}. Note that not all Authenticator implementations will support
 * all these contexts, and some contexts may also require additional information (a
 * "specific").
 *
 * @author Simon Baslé
 * @since 2.3
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public enum CredentialContext {

    BUCKET_KV, BUCKET_VIEW, BUCKET_N1QL, BUCKET_FTS,
    CLUSTER_N1QL, CLUSTER_FTS,
    BUCKET_MANAGEMENT, CLUSTER_MANAGEMENT;

}
