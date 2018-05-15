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
package com.couchbase.client.java.auth;

import com.couchbase.client.java.env.CouchbaseEnvironment;

import java.util.Collections;
import java.util.List;

/**
 * This {@link Authenticator} enables client certificate based
 * authentication.
 *
 * Note that it doesn't actually handles any credentials at this
 * point since the username is part of the certificate passed
 * into the {@link CouchbaseEnvironment}.
 *
 * @author Michael Nitschinger
 * @since 1.6.0
 */
public class CertAuthenticator implements Authenticator {

    public static CertAuthenticator INSTANCE = new CertAuthenticator();

    private static Credential CREDENTIAL = new Credential(null, null);

    private CertAuthenticator() { }

    @Override
    public List<Credential> getCredentials(CredentialContext context, String specific) {
        return Collections.singletonList(CREDENTIAL);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

}
