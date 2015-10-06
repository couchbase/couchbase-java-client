/**
 * Copyright (C) 2015 Couchbase, Inc.
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
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