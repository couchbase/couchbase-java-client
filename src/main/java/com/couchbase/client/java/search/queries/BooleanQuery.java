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
 * A compound FTS query that allows various combinations of sub-queries.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class BooleanQuery extends AbstractFtsQuery {

    private final ConjunctionQuery must;
    private final DisjunctionQuery mustNot;
    private final DisjunctionQuery should;

    public BooleanQuery() {
        super();
        this.must = new ConjunctionQuery();
        this.should = new DisjunctionQuery();
        this.mustNot = new DisjunctionQuery();
    }

    public BooleanQuery shouldMin(int minForShould) {
        this.should.min(minForShould);
        return this;
    }

    public BooleanQuery must(AbstractFtsQuery... mustQueries) {
        must.and(mustQueries);
        return this;
    }

    public BooleanQuery mustNot(AbstractFtsQuery... mustNotQueries) {
        mustNot.or(mustNotQueries);
        return this;
    }
    public BooleanQuery should(AbstractFtsQuery... shouldQueries) {
        should.or(shouldQueries);
        return this;
    }

    @Override
    public BooleanQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    @Override
    protected void injectParams(JsonObject input) {
        boolean mustIsEmpty = must == null || must.childQueries().isEmpty();
        boolean mustNotIsEmpty = mustNot == null || mustNot.childQueries().isEmpty();
        boolean shouldIsEmpty = should == null || should.childQueries().isEmpty();

        if (mustIsEmpty && mustNotIsEmpty && shouldIsEmpty) {
            throw new IllegalArgumentException("Boolean query needs at least one of must, mustNot and should");
        }

        if (!mustIsEmpty) {
            JsonObject jsonMust = JsonObject.create();
            must.injectParamsAndBoost(jsonMust);
            input.put("must", jsonMust);
        }

        if (!mustNotIsEmpty) {
            JsonObject jsonMustNot = JsonObject.create();
            mustNot.injectParamsAndBoost(jsonMustNot);
            input.put("must_not", jsonMustNot);
        }

        if (!shouldIsEmpty) {
            JsonObject jsonShould = JsonObject.create();
            should.injectParamsAndBoost(jsonShould);
            input.put("should", jsonShould);
        }
    }
}
