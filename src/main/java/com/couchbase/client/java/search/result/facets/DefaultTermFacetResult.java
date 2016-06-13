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
package com.couchbase.client.java.search.result.facets;

import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Implementation of a {@link TermFacetResult}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DefaultTermFacetResult extends AbstractFacetResult implements TermFacetResult {

    private final List<TermRange> terms;

    public DefaultTermFacetResult(String name, String field, long total, long missing, long other,
            List<TermRange> terms) {
        super(name, field, total, missing, other);
        this.terms = terms;
    }

    @Override
    public List<TermRange> terms() {
        return this.terms;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TermFacetResult{")
                .append("name='").append(name).append('\'')
                .append(", field='").append(field).append('\'')
                .append(", total=").append(total)
                .append(", missing=").append(missing)
                .append(", other=").append(other)
                .append(", terms=").append(terms)
                .append('}');
        return sb.toString();
    }
}
