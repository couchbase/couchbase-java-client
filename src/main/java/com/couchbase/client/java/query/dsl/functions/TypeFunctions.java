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
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the Type category.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class TypeFunctions {

    //===== TYPE CHECKING FUNCTIONS =====

    /**
     * Returned expression results in True if expression is an array, otherwise returns MISSING, NULL or false.
     */
    public static Expression isArray(Expression expression) {
        return x("ISARRAY(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in True if expression is an array, otherwise returns MISSING, NULL or false.
     */
    public static Expression isArray(String expression) {
        return isArray(x(expression));
    }

    /**
     * Returned expression results in True if expression is a Boolean, number, or string,
     * otherwise returns MISSING, NULL or false.
     */
    public static Expression isAtom(Expression expression) {
        return x("ISATOM(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in True if expression is a Boolean, number, or string,
     * otherwise returns MISSING, NULL or false.
     */
    public static Expression isAtom(String expression) {
        return isAtom(x(expression));
    }

    /**
     * Returned expression results in True if expression is a Boolean, otherwise returns MISSING, NULL or false.
     */
    public static Expression isBoolean(Expression expression) {
        return x("ISBOOLEAN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in True if expression is a Boolean, otherwise returns MISSING, NULL or false.
     */
    public static Expression isBoolean(String expression) {
        return isBoolean(x(expression));
    }

    /**
     * Returned expression results in True if expression is a number, otherwise returns MISSING, NULL or false.
     */
    public static Expression isNumber(Expression expression) {
        return x("ISNUMBER(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in True if expression is a number, otherwise returns MISSING, NULL or false.
     */
    public static Expression isNumber(String expression) {
        return isNumber(x(expression));
    }

    /**
     * Returned expression results in True if expression is an object, otherwise returns MISSING, NULL or false.
     */
    public static Expression isObject(Expression expression) {
        return x("ISOBJECT(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in True if expression is an object, otherwise returns MISSING, NULL or false.
     */
    public static Expression isObject(String expression) {
        return isObject(x(expression));
    }

    /**
     * Returned expression results in True if expression is a string, otherwise returns MISSING, NULL or false.
     */
    public static Expression isString(Expression expression) {
        return x("ISSTRING(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in True if expression is a string, otherwise returns MISSING, NULL or false.
     */
    public static Expression isString(String expression) {
        return isString(x(expression));
    }

    /**
     * Returned expression results in one of the following strings, based on the value of expression:
     *
     *  - "missing"
     *  - "null"
     *  - "boolean"
     *  - "number"
     *  - "string"
     *  - "array"
     *  - "object"
     *  - "binary"
     */
    public static Expression type(Expression expression) {
        return x("TYPE(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in one of the following strings, based on the value of expression:
     *
     *  - "missing"
     *  - "null"
     *  - "boolean"
     *  - "number"
     *  - "string"
     *  - "array"
     *  - "object"
     *  - "binary"
     */
    public static Expression type(String expression) {
        return type(x(expression));
    }


    //===== TYPE CONVERSION FUNCTIONS =====

    /**
     * Returned expression results in an array as follows:
     *
     *  - MISSING is MISSING.
     *  - NULL is NULL.
     *  - Arrays are themselves.
     *  - All other values are wrapped in an array.
     *  - TOATOM(expression)
     */
    public static Expression toArray(Expression expression) {
        return x("TOARRAY(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in an array as follows:
     *
     *  - MISSING is MISSING.
     *  - NULL is NULL.
     *  - Arrays are themselves.
     *  - All other values are wrapped in an array.
     *  - TOATOM(expression)
     */
    public static Expression toArray(String expression) {
        return toArray(x(expression));
    }

    /**
     * Returned expression results in an atomic value as follows:
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - Arrays of length 1 are the result of TOATOM() on their single element.
     * - Objects of length 1 are the result of TOATOM() on their single value.
     * - Booleans, numbers, and strings are themselves.
     * - All other values are NULL.
     */
    public static Expression toAtom(Expression expression) {
        return x("TOATOM(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in an atomic value as follows:
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - Arrays of length 1 are the result of TOATOM() on their single element.
     * - Objects of length 1 are the result of TOATOM() on their single value.
     * - Booleans, numbers, and strings are themselves.
     * - All other values are NULL.
     */
    public static Expression toAtom(String expression) {
        return toAtom(x(expression));
    }

    /**
     * Returned expression results in a Boolean as follows:
     *
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - False is false.
     * - Numbers +0, -0, and NaN are false.
     * - Empty strings, arrays, and objects are false.
     * - All other values are true.
     */
    public static Expression toBoolean(Expression expression) {
        return x("TOBOOLEAN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in a Boolean as follows:
     *
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - False is false.
     * - Numbers +0, -0, and NaN are false.
     * - Empty strings, arrays, and objects are false.
     * - All other values are true.
     */
    public static Expression toBoolean(String expression) {
        return toBoolean(x(expression));
    }


    /**
     * Returned expression results in a number as follows:
     *
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - False is 0.
     * - True is 1.
     * - Numbers are themselves.
     * - Strings that parse as numbers are those numbers.
     * - All other values are NULL.
     */
    public static Expression toNumber(Expression expression) {
        return x("TONUMBER(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in a number as follows:
     *
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - False is 0.
     * - True is 1.
     * - Numbers are themselves.
     * - Strings that parse as numbers are those numbers.
     * - All other values are NULL.
     */
    public static Expression toNumber(String expression) {
        return toNumber(x(expression));
    }

    /**
     * Returned expression results in an object as follows:
     *
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - Objects are themselves.
     * - All other values are the empty object.
     */
    public static Expression toObject(Expression expression) {
        return x("TOOBJECT(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in an object as follows:
     *
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - Objects are themselves.
     * - All other values are the empty object.
     */
    public static Expression toObject(String expression) {
        return toObject(x(expression));
    }

    /**
     * Returned expression results in a string as follows:
     *
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - False is "false".
     * - True is "true".
     * - Numbers are their string representation.
     * - Strings are themselves.
     * - All other values are NULL.
     */
    public static Expression toString(Expression expression) {
        return x("TOSTRING(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in a string as follows:
     *
     * - MISSING is MISSING.
     * - NULL is NULL.
     * - False is "false".
     * - True is "true".
     * - Numbers are their string representation.
     * - Strings are themselves.
     * - All other values are NULL.
     */
    public static Expression toString(String expression) {
        return toString(x(expression));
    }
}
