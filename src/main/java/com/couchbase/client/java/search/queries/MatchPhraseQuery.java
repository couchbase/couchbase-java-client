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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A FTS query that matches several given terms (a "phrase"), applying further processing
 * like analyzers to them.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class MatchPhraseQuery extends AbstractFtsQuery {

    private final String matchPhrase;
    private String field;
    private String analyzer;

    public MatchPhraseQuery(String matchPhrase) {
        super();
        this.matchPhrase = matchPhrase;
    }

    @Override
    public MatchPhraseQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    public MatchPhraseQuery field(String field) {
        this.field = field;
        return this;
    }

    public MatchPhraseQuery analyzer(String analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    @Override
    protected void injectParams(JsonObject input) {
        input.put("match_phrase", matchPhrase);
        if (field != null) {
            input.put("field", field);
        }
        if (analyzer != null) {
            input.put("analyzer", analyzer);
        }
    }
}
