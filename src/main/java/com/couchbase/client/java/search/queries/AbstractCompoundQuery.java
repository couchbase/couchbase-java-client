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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Base class for FTS queries that are composite, compounding several other {@link AbstractFtsQuery}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public abstract class AbstractCompoundQuery extends AbstractFtsQuery {

    List<AbstractFtsQuery> childQueries = new LinkedList<AbstractFtsQuery>();

    protected AbstractCompoundQuery(AbstractFtsQuery... queries) {
        super();
        addAll(queries);
    }

    protected void addAll(AbstractFtsQuery... queries) {
        Collections.addAll(childQueries, queries);
    }

    public List<AbstractFtsQuery> childQueries() {
        return this.childQueries;
    }
}
