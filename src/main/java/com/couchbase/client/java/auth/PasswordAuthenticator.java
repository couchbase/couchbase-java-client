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
package com.couchbase.client.java.auth;

import java.util.Collections;
import java.util.List;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * {@link Authenticator} for RBAC users in Couchbase
 *
 * @author Subhashni Balakrishnan
 * @since 2.4.4
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class PasswordAuthenticator implements Authenticator {
    final private String username;
    final private String password;

    public PasswordAuthenticator(String password) {
        this.username = null;
        this.password = password;
    }

    public PasswordAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public List<Credential> getCredentials(CredentialContext context, String specific) {
        return Collections.singletonList(new Credential(username, password));
    }

    public boolean isEmpty() {
        return false;
    }

    public String username() {
        return this.username;
    }

    public String password() {
        return this.password;
    }
}
