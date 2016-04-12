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
