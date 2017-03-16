/*
 * Copyright (c) 2017 Couchbase, Inc.
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
import com.couchbase.client.java.auth.PasswordAuthenticator;

/**
 * This exception is commonly raised when the {@link PasswordAuthenticator}
 * containing RBAC user credentials and bucket credentials are used together
 *
 * @author Subhashni Balakrishnan
 * @since 2.4.4
 */
public class MixedAuthenticationException extends CouchbaseException {

    public MixedAuthenticationException() {
        super();
    }

    public MixedAuthenticationException(String message) {
        super(message);
    }

    public MixedAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MixedAuthenticationException(Throwable cause) {
        super(cause);
    }
}
