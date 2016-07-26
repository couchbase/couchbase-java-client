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
package com.couchbase.client.java.query.dsl.functions;

import static com.couchbase.client.java.query.dsl.Expression.x;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the Object category.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class ObjectFunctions {

    private ObjectFunctions() {}

    /**
     * Returned expression results in the number of name-value pairs in the object.
     */
    public static Expression objectLength(Expression expression) {
        return x("OBJECT_LENGTH(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the number of name-value pairs in the object.
     */
    public static Expression objectLength(String expression) {
        return objectLength(x(expression));
    }

    /**
     * Returned expression results in the number of name-value pairs in the object.
     */
    public static Expression objectLength(JsonObject value) {
        return objectLength(x(value));
    }

    /**
     * Returned expression results in an array containing the attribute names of the object, in N1QL collation order.
     */
    public static Expression objectNames(Expression expression) {
        return x("OBJECT_NAMES(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in an array containing the attribute names of the object, in N1QL collation order.
     */
    public static Expression objectNames(String expression) {
        return objectNames(x(expression));
    }

    /**
     * Returned expression results in an array containing the attribute names of the object, in N1QL collation order.
     */
    public static Expression objectNames(JsonObject value) {
        return objectNames(x(value));
    }

    /**
     * Returned expression results in an array containing the attribute name and value pairs of the object,
     * in N1QL collation order of the names.
     */
    public static Expression objectPairs(Expression expression) {
        return x("OBJECT_PAIRS(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in an array containing the attribute name and value pairs of the object,
     * in N1QL collation order of the names.
     */
    public static Expression objectPairs(String expression) {
        return objectPairs(x(expression));
    }

    /**
     * Returned expression results in an array containing the attribute name and value pairs of the object,
     * in N1QL collation order of the names.
     */
    public static Expression objectPairs(JsonObject value) {
        return objectPairs(x(value));
    }

    /**
     * Returned expression results in an array containing the attribute values of the object,
     * in N1QL collation order of the corresponding names.
     */
    public static Expression objectValues(Expression expression) {
        return x("OBJECT_VALUES(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in an array containing the attribute values of the object,
     * in N1QL collation order of the corresponding names.
     */
    public static Expression objectValues(String expression) {
        return objectValues(x(expression));
    }

    /**
     * Returned expression results in an array containing the attribute values of the object,
     * in N1QL collation order of the corresponding names.
     */
    public static Expression objectValues(JsonObject value) {
        return objectValues(x(value));
    }
}
