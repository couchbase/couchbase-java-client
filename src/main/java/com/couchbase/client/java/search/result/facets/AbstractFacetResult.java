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
package com.couchbase.client.java.search.result.facets;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

@InterfaceStability.Experimental
@InterfaceAudience.Private
public abstract class AbstractFacetResult implements FacetResult {

    protected final String name;
    protected final String field;
    protected final long total;
    protected final long missing;
    protected final long other;

    protected AbstractFacetResult(String name, String field, long total, long missing, long other) {
        this.name = name;
        this.field = field;
        this.total = total;
        this.missing = missing;
        this.other = other;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String field() {
        return this.field;
    }

    @Override
    public long missing() {
        return this.missing;
    }

    @Override
    public long other() {
        return this.other;
    }

    @Override
    public long total() {
        return this.total;
    }
}
