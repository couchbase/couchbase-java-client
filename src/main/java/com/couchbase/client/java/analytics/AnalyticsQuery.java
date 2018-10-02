/*
 * Copyright (c) 2017 Couchbase, Inc.
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
package com.couchbase.client.java.analytics;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

import java.io.Serializable;

/**
 * Base class for an analytics query.
 *
 * @author Michael Nitschinger
 * @since 2.4.3
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public abstract class AnalyticsQuery implements Serializable {

    private static final long serialVersionUID = 3758113456237959730L;

    /**
     * Returns the statement from this query.
     */
    public abstract String statement();

    /**
     * Returns the params from this query.
     */
    public abstract AnalyticsParams params();

    /**
     * Returns the full query.
     */
    public abstract JsonObject query();

    /**
     * Creates an {@link AnalyticsQuery} from just an analytics statement.
     *
     * @param statement the statement to send.
     * @return a {@link AnalyticsQuery}.
     */
    public static SimpleAnalyticsQuery simple(final String statement) {
        return simple(statement, null);
    }

    /**
     * Creates an {@link AnalyticsQuery} from an analytics statement and custom parameters.
     *
     * @param statement the statement to send.
     * @param params the parameters to provide to the server in addition.
     * @return a {@link AnalyticsQuery}.
     */
    public static SimpleAnalyticsQuery simple(final String statement, final AnalyticsParams params) {
        return new SimpleAnalyticsQuery(statement, params);
    }

    /**
     * Creates an {@link AnalyticsQuery} with positional parameters as part of the query.
     *
     * @param statement the statement to send.
     * @param positionalParams the positional parameters which will be put in for the placeholders.
     * @return a {@link AnalyticsQuery}.
     */
    public static ParameterizedAnalyticsQuery parameterized(final String statement,
        final JsonArray positionalParams) {
        return new ParameterizedAnalyticsQuery(statement, positionalParams, null, null);
    }

    /**
     * Creates an {@link AnalyticsQuery} with positional parameters as part of the query.
     *
     * @param statement the statement to send.
     * @param positionalParams the positional parameters which will be put in for the placeholders.
     * @param params the parameters to provide to the server in addition.
     * @return a {@link AnalyticsQuery}.
     */
    public static ParameterizedAnalyticsQuery parameterized(final String statement,
        final JsonArray positionalParams, final AnalyticsParams params) {
        return new ParameterizedAnalyticsQuery(statement, positionalParams, null, params);
    }

    /**
     * Creates an {@link AnalyticsQuery} with named parameters as part of the query.
     *
     * @param statement the statement to send.
     * @param namedParams the named parameters which will be put in for the placeholders.
     * @return a {@link AnalyticsQuery}.
     */
    public static ParameterizedAnalyticsQuery parameterized(final String statement,
        final JsonObject namedParams) {
        return new ParameterizedAnalyticsQuery(statement, null, namedParams, null);
    }

    /**
     * Creates an {@link AnalyticsQuery} with named parameters as part of the query.
     *
     * @param statement the statement to send.
     * @param namedParams the named parameters which will be put in for the placeholders.
     * @param params the parameters to provide to the server in addition.
     * @return a {@link AnalyticsQuery}.
     */
    public static ParameterizedAnalyticsQuery parameterized(final String statement,
        final JsonObject namedParams, final AnalyticsParams params) {
        return new ParameterizedAnalyticsQuery(statement, null, namedParams, params);
    }
}
