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
 * Represent a N1QL query with an optionally parameterized statement (in which case the
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
public class ParameterizedN1qlQuery extends AbstractN1qlQuery {

    private final JsonValue statementParams;
    private final boolean positional;

    /* package */ ParameterizedN1qlQuery(Statement statement, JsonArray positionalParams, N1qlParams params) {
        super(statement, params);
        this.statementParams = positionalParams;
        this.positional = true;
    }

    /* package */ ParameterizedN1qlQuery(Statement statement, JsonObject namedParams, N1qlParams params) {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ParameterizedN1qlQuery{");
        sb.append("statement=").append(statement().toString());
        if (statementParameters() != null) {
            sb.append(", params=").append(statementParameters().toString());
        }
        sb.append('}');
        return sb.toString();
    }
}
