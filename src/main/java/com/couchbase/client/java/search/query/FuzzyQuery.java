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