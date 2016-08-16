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
package com.couchbase.client.java.error;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.auth.Authenticator;
import com.couchbase.client.java.auth.CredentialContext;

/**
 * This exception is commonly raised when an attempt to retrieve a credential in an
 * {@link Authenticator} is made, but cannot be fulfilled. The exception allows to
 * retrieve the {@link CredentialContext} and specific for which the request was made.
 *
 * @author Simon Baslé
 * @since 2.3
 */
public class AuthenticatorException extends CouchbaseException {

    private final CredentialContext context;
    private final String specific;
    private final int foundCredentials;

    public AuthenticatorException(String message, CredentialContext context, String specific, int found) {
        super(message + " [" + context + "/" + specific + ", " + found + " found]");
        this.context = context;
        this.specific = specific;
        this.foundCredentials = found;
    }

    /**
     * @return the context enum in which the credential was requested.
     */
    public CredentialContext context() {
        return context;
    }

    /**
     * @return the specific sub-context for which the credential was request (can be null).
     */
    public String specific() {
        return specific;
    }

    /**
     * @return the number of credentials found for the failing request.
     */
    public int foundCredentials() {
        return 0;
    }
}
