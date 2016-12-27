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
 * A simple class that internally represents an implicit credential for authenticated operations in the SDK.
 *
 * @author Simon Baslé
 * @since 2.3
 */
@InterfaceStability.Committed
@InterfaceAudience.Private
public class Credential {

    private final String login;
    private final String password;

    public Credential(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String login() {
        return login;
    }

    public String password() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Credential that = (Credential) o;

        if (login != null ? !login.equals(that.login) : that.login != null) {
            return false;
        }
        return password != null ? password.equals(that.password) : that.password == null;

    }

    @Override
    public int hashCode() {
        int result = login != null ? login.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Credential{");
        sb.append("login='").append(login).append('\'');
        if (password != null && password.length() > 0)
        sb.append(", password='•••••'"); //could avoid inadvertently revealing the password or its length, eg. in logs
        sb.append('}');
        return sb.toString();
    }
}
