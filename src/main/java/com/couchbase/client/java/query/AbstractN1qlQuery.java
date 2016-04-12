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
package com.couchbase.client.java.query;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;

/**
 * An abstract base for N1QL {@link N1qlQuery}.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public abstract class AbstractN1qlQuery extends N1qlQuery {

    private N1qlParams queryParameters;
    private SerializableStatement statement;

    /** The type of the statement, used as JSON name in the final JSON form of the query */
    protected abstract String statementType();

    /** The JSON representation for the underlying {@link Statement} in the final JSON form of the query */
    protected abstract Object statementValue();

    /** The parameters to inject in the query, null or empty to ignore. */
    protected abstract JsonValue statementParameters();

    protected AbstractN1qlQuery(Statement statement, N1qlParams params) {
        this.statement = (statement instanceof SerializableStatement)
                ? (SerializableStatement) statement
                : new RawStatement(statement.toString());
        this.queryParameters = params == null ? N1qlParams.build() : params;
    }

    @Override
    public N1qlParams params() {
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
