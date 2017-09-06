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
package com.couchbase.client.java.search.queries;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A FTS query that matches on Couchbase document IDs. Useful to restrict the search space to a list of keys
 * (by using this in a {@link AbstractCompoundQuery compound query}).
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class DocIdQuery extends AbstractFtsQuery {

    private List<String> docIds = new LinkedList<String>();

    public DocIdQuery(String... docIds) {
        super();
        Collections.addAll(this.docIds, docIds);
    }

    public DocIdQuery docIds(String... docIds) {
        Collections.addAll(this.docIds, docIds);
        return this;
    }

    @Override
    public DocIdQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    @Override
    protected void injectParams(JsonObject input) {
        if (docIds.isEmpty()) {
            throw new IllegalArgumentException("DocID query needs at least one document ID");
        }

        JsonArray ids = JsonArray.from(docIds);
        input.put("ids", ids);
    }
}
