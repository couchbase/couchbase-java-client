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

import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.path.DefaultSelectPath;
import com.couchbase.client.java.query.dsl.path.FromPath;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class Select {

    private Select() {}

    public static FromPath select(Expression... expressions) {
        return new DefaultSelectPath(null).select(expressions);
    }

    public static FromPath select(String... expressions) {
        return new DefaultSelectPath(null).select(expressions);
    }

    public static FromPath selectAll(Expression... expressions) {
        return new DefaultSelectPath(null).selectAll(expressions);
    }

    public static FromPath selectAll(String... expressions) {
        return new DefaultSelectPath(null).selectAll(expressions);
    }

    public static FromPath selectDistinct(Expression... expressions) {
        return new DefaultSelectPath(null).selectDistinct(expressions);
    }

    public static FromPath selectDistinct(String... expressions) {
        return new DefaultSelectPath(null).selectDistinct(expressions);
    }

    public static FromPath selectRaw(Expression expression) {
        return new DefaultSelectPath(null).selectRaw(expression);
    }

    public static FromPath selectRaw(String expression) {
        return new DefaultSelectPath(null).selectRaw(expression);
    }

    public static FromPath selectDistinctRaw(Expression expression) {
        return new DefaultSelectPath(null).selectDistinctRaw(expression);
    }

    public static FromPath selectDistinctRaw(String expression) {
        return new DefaultSelectPath(null).selectDistinctRaw(expression);
    }
}
