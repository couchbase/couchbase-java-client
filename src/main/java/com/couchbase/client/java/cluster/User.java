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

package com.couchbase.client.java.cluster;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Rbac user in couchbase
 *
 * @author Subhashni Balakrishnan
 * @since 2.4.4
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class User {
    final private String name;
    final private String userId;
    final private AuthDomain domain;
    final private UserRole[] roles;

    protected User(String name, String userId, AuthDomain domain, UserRole[] roles) {
        this.name = name;
        this.userId = userId;
        this.domain = domain;
        this.roles = roles;
    }

    /**
     * Get name of the user
     * @return username
     */
    public String name() {
        return this.name;
    }

    /**
     * Get user id
     * @return id
     */
    public String userId() {
        return this.userId;
    }

    /**
     * Get user domain local or ldap
     * @return domain
     */
    public AuthDomain domain() {
        return this.domain;
    }

    /**
     * Get user roles
     * @return roles array of user roles
     */
    public UserRole[] roles() {
        return this.roles;
    }
}