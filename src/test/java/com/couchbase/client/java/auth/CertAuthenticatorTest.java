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

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Verifies the functionality of the {@link CertAuthenticator}.
 *
 * @author Michael Nitschinger
 * @since 1.6.0
 */
public class CertAuthenticatorTest {

    @Test
    public void getCredentials() {
        List<Credential> creds = CertAuthenticator.INSTANCE.getCredentials(CredentialContext.BUCKET_KV, null);
        assertEquals(1, creds.size());
        assertNull(creds.get(0).login());
        assertNull(creds.get(0).password());
    }

    @Test
    public void isEmpty() {
        assertFalse(CertAuthenticator.INSTANCE.isEmpty());
    }
}