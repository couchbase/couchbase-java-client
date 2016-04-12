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
 * {@link MatchQuery} creates a Query for matching text.
 *
 * An analyzer is chosen based on the field. Input text is analyzed
 * using this analyzer. Token terms resulting from this analysis
 * are used to perform term searches.
 *
 * Result documents must satisfy at least one of these term searches.
 *
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class MatchQuery extends SearchQuery {
    private static final int PREFIX_LENGTH = 0;
    private static final int FUZZINESS = 2;

    private final String match;
    private final String field;
    private final String analyzer;
    private final int prefixLength;
    private final int fuzziness;

    protected MatchQuery(Builder builder) {
        super(builder);
        match = builder.match;
        field = builder.field;
        analyzer = builder.analyzer;
        prefixLength = builder.prefixLength;
        fuzziness = builder.fuzziness;
    }

    public static Builder on(String index) {
        return new Builder(index);
    }

    public String match() {
        return match;
    }

    public String field() {
        return field;
    }

    public String analyzer() {
        return analyzer;
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
                .put("match", match)
                .put("field", field)
                .put("analyzer", analyzer)
                .put("prefix_length", prefixLength)
                .put("fuzziness", fuzziness);
    }

    public static class Builder extends SearchQuery.Builder {
        private String match;
        private String field;
        private String analyzer;
        private int prefixLength = PREFIX_LENGTH;
        private int fuzziness = FUZZINESS;

        protected Builder(String index) {
            super(index);
        }

        public MatchQuery build() {
            return new MatchQuery(this);
        }

        public Builder match(String match) {
            this.match = match;
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

        public Builder fuzziness(int fuzziness) {
            this.fuzziness = fuzziness;
            return this;
        }

        public Builder prefixLength(int prefixLength) {
            this.prefixLength = prefixLength;
            return this;
        }
    }
}