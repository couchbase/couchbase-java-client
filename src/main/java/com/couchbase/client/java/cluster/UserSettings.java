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

import java.util.List;

/**
 * Setting for the user - full name, password, roles
 *
 * @author Subhashni Balakrishnan
 * @since 2.4.4
 */
public class UserSettings {

    private String name;

    private String password;

    private List<UserRole> roles;

    private UserSettings() {}

    public static UserSettings build() {
        return new UserSettings();
    }

    /**
     * Set the full name of the user
     */
    public UserSettings name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the password for the user
     */
    public UserSettings password(String password) {
        this.password = password;
        return this;
    }

    /**
     * Set the roles for the user {@link UserRole}
     */
    public UserSettings roles(List<UserRole> roles) {
        this.roles = roles;
        return this;
    }

    /**
     * Get the full name for the user
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the password for the user
     */
    public String password() {
        return this.password;
    }

    /**
     * Get roles for the user {@link UserRole}
     *
     * @return list of user roles
     */
    public List<UserRole> roles() {
        return this.roles;
    }
}
