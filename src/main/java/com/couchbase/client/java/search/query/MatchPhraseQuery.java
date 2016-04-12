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
 * {@link MatchPhraseQuery} creates a new Query object for matching phrases in the index.
 *
 * An analyzer is chosen based on the field. Input text is analyzed
 * using this analyzer. Token terms resulting from this analysis
 * are used to build a search phrase.
 *
 * Result documents must match this phrase.
 *
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class MatchPhraseQuery extends SearchQuery {
    private final String matchPhrase;
    private final String field;
    private final String analyzer;

    protected MatchPhraseQuery(Builder builder) {
        super(builder);
        matchPhrase = builder.matchPhrase;
        field = builder.field;
        analyzer = builder.analyzer;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public String match() {
        return matchPhrase;
    }

    public String field() {
        return field;
    }

    public String analyzer() {
        return analyzer;
    }

    @Override
    public JsonObject queryJson() {
        return JsonObject.create()
                .put("match_phrase", matchPhrase)
                .put("field", field)
                .put("analyzer", analyzer);
    }

    public static class Builder extends SearchQuery.Builder {
        private String matchPhrase;
        private String field;
        private String analyzer;

        protected Builder(String index) {
            super(index);
        }

        public MatchPhraseQuery build() {
            return new MatchPhraseQuery(this);
        }

        public Builder matchPhrase(String matchPhrase) {
            this.matchPhrase = matchPhrase;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder analyzer(String analyzer) {
            this.analyzer = analyzer;
            return this;
        }
    }
}