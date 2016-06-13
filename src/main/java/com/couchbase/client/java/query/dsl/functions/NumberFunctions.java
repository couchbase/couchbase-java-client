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
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the Numbers category.
 *
 * Number functions are functions that are performed on a numeric field.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class NumberFunctions {

    private NumberFunctions() {}

    /**
     * Returned expression results in the absolute value of the number.
     */
    public static Expression abs(Expression expression) {
        return x("ABS(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the absolute value of the number.
     */
    public static Expression abs(Number value) {
        return abs(x(value));
    }

    /**
     * Returned expression results in the arccosine in radians.
     */
    public static Expression acos(Expression expression) {
        return x("ACOS(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the arccosine in radians.
     */
    public static Expression acos(Number value) {
        return acos(x(value));
    }

    /**
     * Returned expression results in the arcsine in radians.
     */
    public static Expression asin(Expression expression) {
        return x("ASIN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the arcsine in radians.
     */
    public static Expression asin(Number value) {
        return asin(x(value));
    }

    /**
     * Returned expression results in the arctangent in radians.
     */
    public static Expression atan(Expression expression) {
        return x("ATAN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the arctangent in radians.
     */
    public static Expression atan(Number value) {
        return atan(x(value));
    }

    /**
     * Returned expression results in the arctangent of expression2/expression1.
     */
    public static Expression atan(Expression expression1, Expression expression2) {
        return x("ATAN(" + expression1.toString() + ", " + expression2.toString() + ")");
    }

    /**
     * Returned expression results in the arctangent of expression2/expression1.
     */
    public static Expression atan(String expression1, String expression2) {
        return atan(x(expression1), x(expression2));
    }

    /**
     * Returned expression results in the smallest integer not less than the number.
     */
    public static Expression ceil(Expression expression) {
        return x("CEIL(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the smallest integer not less than the number.
     */
    public static Expression ceil(Number value) {
        return ceil(x(value));
    }

    /**
     * Returned expression results in the cosine.
     */
    public static Expression cos(Expression expression) {
        return x("COS(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the cosine.
     */
    public static Expression cos(Number value) {
        return cos(x(value));
    }

    /**
     * Returned expression results in the conversion of radians to degrees.
     */
    public static Expression degrees(Expression expression) {
        return x("DEGREES(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the conversion of radians to degrees.
     */
    public static Expression degrees(Number value) {
        return degrees(x(value));
    }

    /**
     * Returned expression results in the base of natural logarithms.
     */
    public static Expression e() {
        return x("E()");
    }

    /**
     * Returned expression results in the exponential of expression.
     */
    public static Expression exp(Expression expression) {
        return x("EXP(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the exponential of expression.
     */
    public static Expression exp(Number value) {
        return exp(x(value));
    }

    /**
     * Returned expression results in the log base e.
     */
    public static Expression ln(Expression expression) {
        return x("LN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the log base e.
     */
    public static Expression ln(Number value) {
        return ln(x(value));
    }

    /**
     * Returned expression results in the log base 10.
     */
    public static Expression log(Expression expression) {
        return x("LOG(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the log base 10.
     */
    public static Expression log(Number value) {
        return log(x(value));
    }

    /**
     * Returned expression results in the largest integer not greater than the number.
     */
    public static Expression floor(Expression expression) {
        return x("FLOOR(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the largest integer not greater than the number.
     */
    public static Expression floor(Number value) {
        return floor(x(value));
    }

    /**
     * Returned expression results in Pi.
     */
    public static Expression pi() {
        return x("PI()");
    }

    /**
     * Returned expression results in expression1 to the power of expression2.
     */
    public static Expression power(Expression expression1, Expression expression2) {
        return x("POWER(" + expression1.toString() + ", " + expression2.toString() + ")");
    }

    /**
     * Returned expression results in value1 to the power of value2.
     */
    public static Expression power(Number value1, Number value2) {
        return power(x(value1), x(value2));
    }

    /**
     * Returned expression results in the conversion of degrees to radians.
     */
    public static Expression radians(Expression expression) {
        return x("RADIANS(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the conversion of degrees to radians.
     */
    public static Expression radians(Number value) {
        return radians(x(value));
    }

    /**
     * Returned expression results in a pseudo-random number with optional seed.
     */
    public static Expression random(Expression seed) {
        return x("RANDOM(" + seed.toString() + ")");
    }

    /**
     * Returned expression results in a pseudo-random number with optional seed.
     */
    public static Expression random(Number seed) {
        return random(x(seed));
    }

    /**
     * Returned expression results in a pseudo-random number with default seed.
     */
    public static Expression random() {
        return x("RANDOM()");
    }

    /**
     * Returned expression results in the value rounded to 0 digits to the right of the decimal point.
     */
    public static Expression round(Expression expression) {
        return x("ROUND(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the value rounded to the given number of integer digits to the right
     * of the decimal point (left if digits is negative).
     */
    public static Expression round(Expression expression, int digits) {
        return x("ROUND(" + expression.toString() + ", " + digits + ")");
    }

    /**
     * Returned expression results in the value rounded to 0 digits to the right of the decimal point.
     */
    public static Expression round(Number expression) {
        return round(x(expression));
    }

    /**
     * Returned expression results in the value rounded to the given number of integer digits to the right
     * of the decimal point (left if digits is negative).
     */
    public static Expression round(Number expression, int digits) {
        return round(x(expression), digits);
    }

    /**
     * Returned expression results in the sign of the numerical expression,
     * represented as -1, 0, or 1 for negative, zero, or positive numbers respectively.
     */
    public static Expression sign(Expression expression) {
        return x("SIGN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the sign of the numerical expression,
     * represented as -1, 0, or 1 for negative, zero, or positive numbers respectively.
     */
    public static Expression sign(Number value) {
        return sign(x(value));
    }

    /**
     * Returned expression results in the sine.
     */
    public static Expression sin(Expression expression) {
        return x("SIN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the sine.
     */
    public static Expression sin(Number value) {
        return sin(x(value));
    }

    /**
     * Returned expression results in the square root.
     */
    public static Expression squareRoot(Expression expression) {
        return x("SQRT(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the square root.
     */
    public static Expression squareRoot(Number value) {
        return squareRoot(x(value));
    }

    /**
     * Returned expression results in the tangent.
     */
    public static Expression tan(Expression expression) {
        return x("TAN(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in the tangent.
     */
    public static Expression tan(Number value) {
        return tan(x(value));
    }

    /**
     * Returned expression results in a truncation of the number to the given number of integer digits
     * to the right of the decimal point (left if digits is negative).
     */
    public static Expression trunc(Expression expression, int digits) {
        return x("TRUNC(" + expression.toString() + ", " + digits + ")");
    }

    /**
     * Returned expression results in a truncation of the number to the given number of integer digits
     * to the right of the decimal point (left if digits is negative).
     */
    public static Expression trunc(Number value, int digits) {
        return trunc(x(value), digits);
    }

    /**
     * Returned expression results in a truncation of the number to 0 digits to the right of the decimal point.
     */
    public static Expression trunc(Expression expression) {
        return x("TRUNC(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in a truncation of the number to 0 digits to the right of the decimal point.
     */
    public static Expression trunc(Number value) {
        return trunc(x(value));
    }

}
