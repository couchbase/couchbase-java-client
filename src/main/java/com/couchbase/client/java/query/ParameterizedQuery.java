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

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;

/**
 * Represent a N1QL {@link} with an optionally parameterized statement (in which case the
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
public class ParameterizedQuery extends AbstractQuery {

    private final JsonValue statementParams;
    private final boolean positional;

    /* package */ ParameterizedQuery(Statement statement, JsonArray positionalParams, QueryParams params) {
        super(statement, params);
        this.statementParams = positionalParams;
        this.positional = true;
    }

    /* package */ ParameterizedQuery(Statement statement, JsonObject namedParams, QueryParams params) {
        super(statement, params);
        this.statementParams = namedParams;
        this.positional = false;
    }

    @Override
    protected String statementType() {
        return "statement";
    }

    @Override
    protected Object statementValue() {
        return statement().toString();
    }

    @Override
    public JsonValue statementParameters() {
        return statementParams;
    }

    public boolean isPositional() {
        return positional;
    }
}
