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

import com.couchbase.client.java.document.json.JsonValue;

/**
 * The simplest form of N1QL {@link N1qlQuery} with a plain un-parameterized {@link Statement}.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public class SimpleN1qlQuery extends AbstractN1qlQuery {

    /* package */ SimpleN1qlQuery(Statement statement, N1qlParams params) {
        super(statement, params);
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
    protected JsonValue statementParameters() {
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SimpleN1qlQuery{");
        sb.append("statement=").append(statement().toString());
        sb.append('}');
        return sb.toString();
    }
}
