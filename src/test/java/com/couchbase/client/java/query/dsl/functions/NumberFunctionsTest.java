/*
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
import static com.couchbase.client.java.query.dsl.functions.NumberFunctions.*;
import static org.junit.Assert.*;

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class NumberFunctionsTest {

    @Test
    public void testAbs() throws Exception {
        Expression e1 = abs(x(1));
        Expression e2 = abs(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ABS(1)", e1.toString());
    }

    @Test
    public void testAcos() throws Exception {
        Expression e1 = acos(x(1));
        Expression e2 = acos(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ACOS(1)", e1.toString());
    }

    @Test
    public void testAsin() throws Exception {
        Expression e1 = asin(x(1));
        Expression e2 = asin(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ASIN(1)", e1.toString());
    }

    @Test
    public void testAtan() throws Exception {
        Expression e1 = atan(x(1));
        Expression e2 = atan(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ATAN(1)", e1.toString());
    }

    @Test
    public void testAtanExpression() throws Exception {
        Expression e1 = atan(x("ab"), x(1));

        assertEquals("ATAN(ab, 1)", e1.toString());
    }

    @Test
    public void testCeil() throws Exception {
        Expression e1 = ceil(x(1));
        Expression e2 = ceil(1 );

        assertEquals(e1.toString(), e2.toString());
        assertEquals("CEIL(1)", e1.toString());
    }

    @Test
    public void testCos() throws Exception {
        Expression e1 = cos(x(1));
        Expression e2 = cos(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("COS(1)", e1.toString());
    }

    @Test
    public void testDegrees() throws Exception {
        Expression e1 = degrees(x(1));
        Expression e2 = degrees(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("DEGREES(1)", e1.toString());
    }

    @Test
    public void testE() throws Exception {
        assertEquals("E()", e().toString());
    }

    @Test
    public void testExp() throws Exception {
        Expression e1 = exp(x(1));
        Expression e2 = exp(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("EXP(1)", e1.toString());
    }

    @Test
    public void testLn() throws Exception {
        Expression e1 = ln(x(1));
        Expression e2 = ln(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("LN(1)", e1.toString());
    }

    @Test
    public void testLog() throws Exception {
        Expression e1 = log(x(1));
        Expression e2 = log(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("LOG(1)", e1.toString());
    }

    @Test
    public void testFloor() throws Exception {
        Expression e1 = floor(x(1));
        Expression e2 = floor(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("FLOOR(1)", e1.toString());
    }

    @Test
    public void testPi() throws Exception {
        assertEquals("PI()", pi().toString());
    }

    @Test
    public void testPower() throws Exception {
        Expression e1 = power(x(1), x(2));
        Expression e2 = power(1 , 2);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("POWER(1, 2)", e1.toString());
    }

    @Test
    public void testRadians() throws Exception {
        Expression e1 = radians(x(1));
        Expression e2 = radians(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("RADIANS(1)", e1.toString());
    }

    @Test
    public void testRandom() throws Exception {
        Expression e1 = random(x(1));
        Expression e2 = random(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("RANDOM(1)", e1.toString());
    }

    @Test
    public void testRandomWithoutSeed() throws Exception {
        assertEquals("RANDOM()", random().toString());
    }

    @Test
    public void testRound() throws Exception {
        Expression e1 = round(x(1.1234567), 5);
        Expression e2 = round(1.1234567, 5);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ROUND(1.1234567, 5)", e1.toString());
    }

    @Test
    public void testRoundToZeroDigits() throws Exception {
        Expression e1 = round(x(1.1234567));
        Expression e2 = round(1.1234567);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ROUND(1.1234567)", e1.toString());
    }

    @Test
    public void testSign() throws Exception {
        Expression e1 = sign(x(1));
        Expression e2 = sign(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("SIGN(1)", e1.toString());
    }

    @Test
    public void testSin() throws Exception {
        Expression e1 = sin(x(1));
        Expression e2 = sin(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("SIN(1)", e1.toString());
    }

    @Test
    public void testSquareRoot() throws Exception {
        Expression e1 = squareRoot(x(1));
        Expression e2 = squareRoot(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("SQRT(1)", e1.toString());
    }

    @Test
    public void testTan() throws Exception {
        Expression e1 = tan(x(1));
        Expression e2 = tan(1);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TAN(1)", e1.toString());
    }

    @Test
    public void testTrunc() throws Exception {
        Expression e1 = trunc(x(1.1234567), 5);
        Expression e2 = trunc(1.1234567, 5);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TRUNC(1.1234567, 5)", e1.toString());
    }

    @Test
    public void testTruncToZeroDigits() throws Exception {
        Expression e1 = trunc(x(1.1234567));
        Expression e2 = trunc(1.1234567);

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TRUNC(1.1234567)", e1.toString());
    }
}