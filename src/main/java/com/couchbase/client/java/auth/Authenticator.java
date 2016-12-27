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

import java.util.List;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * An Authenticator abstracts credential management for various couchbase operations
 * (all of which fall into one {@link CredentialContext}). The interface allows SDK
 * classes to retrieve credentials corresponding to both a context and a specific
 * (wich can be optional for some contexts).
 *
 * @author Simon Baslé
 * @since 2.3
 */
@InterfaceStability.Committed
@InterfaceAudience.Private
public interface Authenticator {

    /**
     * Retrieve the credentials store by this {@link Authenticator} for the given {@link CredentialContext}
     * and optional specific. If no corresponding credential can be found, an empty list is returned. If the
     * context / specific cannot be processed by this Authenticator, throws an {@link IllegalArgumentException}.
     *
     * @param context the context for which the credential(s) will be used.
     * @param specific a more restrictive sub-context specific to the context.
     * @return a list of credentials that can be used for the context/operation, or empty list if none was set for
     * this context+specific combination.
     * @throws IllegalArgumentException when the context+specific combination is not supported by an Authenticator
     * implementation.
     */
    List<Credential> getCredentials(CredentialContext context, String specific);

    /**
     * @return true if this {@link Authenticator} doesn't have any credentials set.
     */
    boolean isEmpty();
}
