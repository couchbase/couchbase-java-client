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

package com.couchbase.client.java.search;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;

import java.util.Map;

/**
 * @author Sergey Avseyev
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class SearchControl {
    private final long timeout;
    private final Consistency consistency;

    protected SearchControl(Builder builder) {
        timeout = builder.timeout;
        consistency = builder.consistency;
    }

    public JsonObject json() {
        JsonObject json = JsonObject.create();
        json.put("timeout", timeout);
        if (consistency != null) {
            json.put("consistency", consistency.json());
        }
        return json;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public long timeout;
        public Consistency consistency;

        protected Builder() {
        }

        public SearchControl build() {
            return new SearchControl(this);
        }

        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder consistency(Consistency consistency) {
            this.consistency = consistency;
            return this;
        }
    }

    public static class Consistency {
        private static final String LEVEL = "at_plus"; // "" -> "ok"

        private final String level;
        /**
         * "index" -> { "partition/vbucketUUID" -> seqno }
         */
        private final Map<String, Map<String, Integer>> vectors;

        protected Consistency(Builder builder) {
            level = builder.level;
            vectors = builder.vectors;
        }

        public JsonObject json() {
            JsonObject json = JsonObject.create();
            json.put("level", level);
            if (vectors != null) {
                JsonObject vectorsJson = JsonObject.create();
                for (Map.Entry<String, Map<String, Integer>> entry : vectors.entrySet()) {
                    vectorsJson.put(entry.getKey(), JsonObject.from(entry.getValue()));
                }
                json.put("vectors", vectorsJson);
            }

            return json;
        }

        public Builder builder() {
            return new Builder();
        }

        public static class Builder {
            public String level = LEVEL;
            public Map<String, Map<String, Integer>> vectors;

            protected Builder() {
            }

            public Consistency build() {
                return new Consistency(this);
            }

            public Builder level(String level) {
                this.level = level;
                return this;
            }

            public Builder vectors(Map<String, Map<String, Integer>> vectors) {
                this.vectors = vectors;
                return this;
            }
        }
    }
}
