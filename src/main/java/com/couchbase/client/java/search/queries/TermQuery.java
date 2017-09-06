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
 * A FTS query that matches terms (without further analysis). Usually for debugging purposes,
 * prefer using {@link MatchQuery}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class TermQuery extends AbstractFtsQuery {

    private final String term;
    private String field;
    private int fuzziness;
    private int prefixLength;

    public TermQuery(String term) {
        super();
        this.term = term;
    }

    public TermQuery field(String fieldName) {
        this.field = fieldName;
        return this;
    }

    public TermQuery fuzziness(int fuzziness) {
        this.fuzziness = fuzziness;
        return this;
    }

    public TermQuery prefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
        return this;
    }

    @Override
    public TermQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    @Override
    protected void injectParams(JsonObject input) {
        input.put("term", term);
        if (field != null) {
            input.put("field", field);
        }
        if (fuzziness > 0) {
            input.put("fuzziness", fuzziness);
            if (prefixLength > 0) {
                input.put("prefix_length", prefixLength);
            }
        }
    }
}
