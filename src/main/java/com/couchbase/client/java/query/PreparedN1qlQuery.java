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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * Represent a N1QL query, with a parameterized prepared statement plan (for which the
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
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class PreparedN1qlQuery extends ParameterizedN1qlQuery {

    private boolean encodedPlanEnabled = true;

    public PreparedN1qlQuery(PreparedPayload plan, JsonArray positionalParams, N1qlParams params) {
        super(plan, positionalParams, params);
    }

    public PreparedN1qlQuery(PreparedPayload plan, JsonObject namedParams, N1qlParams params) {
       super(plan, namedParams, params);
    }

    public PreparedN1qlQuery(PreparedPayload plan, N1qlParams params) {
        super(plan, (JsonArray) null, params);
    }

    @Override
    protected String statementType() {
        return "prepared";
    }

    @Override
    protected Object statementValue() {
        return statement().payload();
    }

    @Override
    public PreparedPayload statement() {
        return (PreparedPayload) super.statement();
    }

    /**
     * Toggle whether or not the encodedPlan part of the payload should be made part of the N1QL statement.
     *
     * @param enabled true to activate encodedPlan in the N1QL statement, false to avoid including it.
     */
    public void setEncodedPlanEnabled(boolean enabled) {
        this.encodedPlanEnabled = enabled;
    }

    /**
     * Returns whether or not the encodedPlan part of the payload will be made part of the N1QL statement.
     *
     * @return true if encodedPlan is to be used in statement, false otherwise.
     */
    public boolean isEncodedPlanEnabled() {
        return this.encodedPlanEnabled;
    }

    @Override
    public JsonObject n1ql() {
        JsonObject n1ql = super.n1ql();
        String encodedPlan = statement().encodedPlan();
        if (encodedPlan != null && encodedPlanEnabled) {
            n1ql.put("encoded_plan", encodedPlan);
        }
        return n1ql;
    }
}
