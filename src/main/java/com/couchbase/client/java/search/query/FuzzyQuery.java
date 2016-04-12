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
 * {@link FuzzyQuery} creates a new Query which finds documents
 * containing terms within a specific fuzziness of the specified
 * term. The default fuzziness is 2.
 *
 * The current implementation uses Leveshtein edit distance as
 * the fuzziness metric.
 *
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class FuzzyQuery extends SearchQuery {
    private static final int PREFIX_LENGTH = 0;
    private static final int FUZZINESS = 2;

    private final String term;
    private final String field;
    private final int prefixLength;
    private final int fuzziness;

    protected FuzzyQuery(Builder builder) {
        super(builder);
        term = builder.term;
        prefixLength = builder.prefixLength;
        fuzziness = builder.fuzziness;
        field = builder.field;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public String term() {
        return term;
    }

    public String field() {
        return field;
    }

    public int prefixLength() {
        return prefixLength;
    }

    public int fuzziness() {
        return fuzziness;
    }

    @Override
    public JsonObject queryJson() {
        return JsonObject.create()
                .put("term", term)
                .put("field", field)
                .put("prefix_length", prefixLength)
                .put("fuzziness", fuzziness);
    }

    public static class Builder extends SearchQuery.Builder {
        private String term;
        private String field;
        private int prefixLength = PREFIX_LENGTH;
        private int fuzziness = FUZZINESS;

        protected Builder(String index) {
            super(index);
        }

        public FuzzyQuery build() {
            return new FuzzyQuery(this);
        }

        public Builder fuzziness(int fuzziness) {
            this.fuzziness = fuzziness;
            return this;
        }

        public Builder term(String term) {
            this.term = term;
            return this;
        }

        public Builder prefixLength(int prefixLength) {
            this.prefixLength = prefixLength;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }
    }
}