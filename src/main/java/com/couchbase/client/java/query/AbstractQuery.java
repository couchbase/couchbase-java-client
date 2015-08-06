/**
 * Copyright (C) 2015 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;

/**
 * An abstract base for N1QL {@link Query}.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public abstract class AbstractQuery extends Query {

    private QueryParams queryParameters;
    private SerializableStatement statement;

    /** The type of the statement, used as JSON name in the final JSON form of the query */
    protected abstract String statementType();

    /** The JSON representation for the underlying {@link Statement} in the final JSON form of the query */
    protected abstract Object statementValue();

    /** The parameters to inject in the query, null or empty to ignore. */
    protected abstract JsonValue statementParameters();

    protected AbstractQuery(Statement statement, QueryParams params) {
        this.statement = (statement instanceof SerializableStatement)
                ? (SerializableStatement) statement
                : new RawStatement(statement.toString());
        this.queryParameters = params == null ? QueryParams.build() : params;
    }

    @Override
    public QueryParams params() {
        return this.queryParameters;
    }

    @Override
    public Statement statement() {
        return this.statement;
    }

    @Override
    public JsonObject n1ql() {
        JsonObject query = JsonObject.create().put(statementType(), statementValue());
        populateParameters(query, statementParameters());
        this.queryParameters.injectParams(query);
        return query; //return json-escaped string
    }

    /**
     * Populate a {@link JsonObject} representation of a query with parameters, either positional or named.
     *
     *  - If params is a {@link JsonObject}, named parameters will be used (prefixing the names with '$' if not present).
     *  - If params is a {@link JsonArray}, positional parameters will be used.
     *  - If params is null or an empty json, no parameters are populated in the query object.
     *
     * Note that the {@link JsonValue} should not be mutated until {@link #n1ql()} is called since it backs the
     * creation of the query string.
     *
     * Also, the {@link Statement} is expected to contain the correct placeholders (corresponding names and number).
     *
     * @param query the query JsonObject to populated with parameters.
     * @param params the parameters.
     */
    public static void populateParameters(JsonObject query, JsonValue params) {
        if (params instanceof JsonArray && !((JsonArray) params).isEmpty()) {
            query.put("args", (JsonArray) params);
        } else if (params instanceof JsonObject && !((JsonObject) params).isEmpty()) {
            JsonObject namedParams = (JsonObject) params;
            for (String key : namedParams.getNames()) {
                Object value = namedParams.get(key);
                if (key.charAt(0) != '$') {
                    query.put('$' + key, value);
                } else {
                    query.put(key, value);
                }
            }
        } //else do nothing, as if a simple statement
    }

}
