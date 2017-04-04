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
package com.couchbase.client.java.search.sort;

import com.couchbase.client.java.document.json.JsonObject;

/**
 * Sort by a field in the hits.
 *
 * @author Michael Nitschinger
 * @since 2.4.5
 */
public class SearchSortField extends SearchSort {

    private final String field;

    private FieldType type;
    private FieldMode mode;
    private FieldMissing missing;

    public SearchSortField(String field) {
        this.field = field;
    }

    @Override
    public SearchSortField descending(boolean descending) {
        super.descending(descending);
        return this;
    }

    public SearchSortField type(FieldType type) {
        this.type = type;
        return this;
    }

    public SearchSortField mode(FieldMode mode) {
        this.mode = mode;
        return this;
    }

    public SearchSortField missing(FieldMissing missing) {
        this.missing = missing;
        return this;
    }

    @Override
    protected String identifier() {
        return "field";
    }

    @Override
    public void injectParams(JsonObject queryJson) {
        super.injectParams(queryJson);

        queryJson.put("field", field);

        if (type != null) {
            queryJson.put("type", type.value());
        }
        if (mode != null) {
            queryJson.put("mode", mode.value());
        }
        if (missing != null) {
            queryJson.put("missing", missing.value());
        }
    }
}
