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

package com.couchbase.client.java.search.query;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class WildcardQuery extends SearchQuery {
    private final String wildcard;
    private final String field;

    protected WildcardQuery(Builder builder) {
        super(builder);
        wildcard = builder.wildcard;
        field = builder.field;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public String wildcard() {
        return wildcard;
    }
    public String field() {
        return field;
    }

    @Override
    public JsonObject queryJson() {
        return JsonObject.create()
                .put("wildcard", wildcard)
                .put("field", field);
    }

    public static class Builder extends SearchQuery.Builder {
        private String wildcard;
        private String field;

        protected Builder(String index) {
            super(index);
        }

        public WildcardQuery build() {
            return new WildcardQuery(this);
        }

        public Builder wildcard(String wildcard) {
            this.wildcard = wildcard;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }
    }
}