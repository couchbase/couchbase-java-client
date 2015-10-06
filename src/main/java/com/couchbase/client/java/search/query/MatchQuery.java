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