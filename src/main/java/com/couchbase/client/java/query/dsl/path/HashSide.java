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

package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Hash side for hash based join
 *
 * @author Subhashni Balakrishnan
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public enum HashSide {

    /**
     * The PROBE side will use that table to find matches and perform the join
     * */
    PROBE("PROBE"),

    /**
     * The BUILD side of the join will be used to create an in-memory hash table
     * */
    BUILD("BUILD");

    private final String value;

    HashSide(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}