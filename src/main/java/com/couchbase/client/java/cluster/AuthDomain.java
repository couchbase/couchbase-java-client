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

/**
 * The possible authentication domains that can be used when managing users with RBAC.
 *
 * @author Michael Nitschinger
 * @since 2.5.0
 */
public enum AuthDomain {
    LOCAL("local"),
    EXTERNAL("external");

    private final String alias;

    AuthDomain(String alias) {
        this.alias = alias;
    }

    public String alias() {
        return alias;
    }

    public static AuthDomain fromAlias(final String alias) {
        if (alias.equalsIgnoreCase("local")) {
            return LOCAL;
        } else if (alias.equalsIgnoreCase("external")) {
            return EXTERNAL;
        } else {
            throw new IllegalStateException("unknown alias:" + alias);
        }
    }
}
