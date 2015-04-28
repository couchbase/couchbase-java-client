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
