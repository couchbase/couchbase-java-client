/**
 * Copyright (C) 2014 Couchbase, Inc.
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

import java.util.Map;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;

/**
 * Represent a N1QL {@link} with an optionally parametrized statement (in which case the
 * values must be passed according to the type and number of placeholders).
 *
 * Positional placeholders (in the form of either "$1" "$2" or just simple "?") are filled
 * by the values taken from a {@link JsonArray}.
 *
 * Named placeholders (in the form of "$param1", "$myOtherParam", etc...) are filled by
 * the values taken from a {@link JsonObject}. If in this JsonObject attributes don't have the $
 * prefix, it is added upon building the query.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class ParametrizedQuery implements Query {

    private final Statement statement;
    private final JsonValue params;

    /**
     * Create a new query with positionalParameters. Note that the {@link JsonArray}
     * should not be mutated until {@link #toN1QL()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the {@link Statement} to execute (containing positional placeholders)
     * @param positionalParams the values for the positional placeholders in statement
     */
    public ParametrizedQuery(Statement statement, JsonArray positionalParams) {
        this.statement = statement;
        this.params = positionalParams;
    }

    /**
     * Create a new query with named parameters. Note that the {@link JsonObject}
     * should not be mutated until {@link #toN1QL()} is called since it backs the
     * creation of the query string.
     *
     * @param statement the {@link Statement} to execute (containing named placeholders)
     * @param namedParams the values for the named placeholders in statement
     */
    public ParametrizedQuery(Statement statement, JsonObject namedParams) {
        this.statement = statement;
        this.params = namedParams;
    }

    protected String statementType() {
        return "statement";
    }

    @Override
    public Statement statement() {
        return this.statement;
    }

    @Override
    public String toN1QL() {
        JsonObject query = JsonObject.create()
                .put(statementType(), statement.toString());
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
        return query.toString(); //return json-escaped string
    }

}
