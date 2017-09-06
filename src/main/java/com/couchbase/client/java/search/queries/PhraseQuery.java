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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A FTS query that matches several terms (a "phrase") as is. The order of the terms mater and no
 * further processing is applied to them, so they must appear in the index exactly as provided.
 * Usually for debugging purposes, prefer {@link MatchPhraseQuery}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class PhraseQuery extends AbstractFtsQuery {

    private final List<String> terms;
    private String field;

    public PhraseQuery(String... terms) {
        super();
        this.terms = new ArrayList<String>();
        Collections.addAll(this.terms, terms);
    }

    public PhraseQuery field(String field) {
        this.field = field;
        return this;
    }

    @Override
    public PhraseQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    @Override
    protected void injectParams(JsonObject input) {
        if (terms.isEmpty()) {
            throw new IllegalArgumentException("Phrase query must at least have one term");
        }
        JsonArray terms = JsonArray.from(this.terms);
        input.put("terms", terms);

        if (field != null) {
            input.put("field", this.field);
        }
    }
}
