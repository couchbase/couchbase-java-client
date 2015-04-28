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
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.isArray;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.isAtom;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.isBoolean;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.isNumber;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.isObject;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.isString;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.toArray;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.toAtom;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.toBoolean;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.toNumber;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.toObject;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.toString;
import static com.couchbase.client.java.query.dsl.functions.TypeFunctions.type;
import static org.junit.Assert.*;

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class TypeFunctionsTest {

    @Test
    public void testIsArray() throws Exception {
        Expression e1 = isArray(x("field"));
        Expression e2 = isArray("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ISARRAY(field)", e1.toString());
    }

    @Test
    public void testIsAtom() throws Exception {
        Expression e1 = isAtom(x("field"));
        Expression e2 = isAtom("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ISATOM(field)", e1.toString());
    }

    @Test
    public void testIsBoolean() throws Exception {
        Expression e1 = isBoolean(x("field"));
        Expression e2 = isBoolean("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ISBOOLEAN(field)", e1.toString());
    }

    @Test
    public void testIsNumber() throws Exception {
        Expression e1 = isNumber(x("field"));
        Expression e2 = isNumber("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ISNUMBER(field)", e1.toString());
    }

    @Test
    public void testIsObject() throws Exception {
        Expression e1 = isObject(x("field"));
        Expression e2 = isObject("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ISOBJECT(field)", e1.toString());
    }

    @Test
    public void testIsString() throws Exception {
        Expression e1 = isString(x("field"));
        Expression e2 = isString("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("ISSTRING(field)", e1.toString());
    }

    @Test
    public void testType() throws Exception {
        Expression e1 = type(x("field"));
        Expression e2 = type("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TYPE(field)", e1.toString());
    }

    @Test
    public void testToArray() throws Exception {
        Expression e1 = toArray(x("field"));
        Expression e2 = toArray("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TOARRAY(field)", e1.toString());
    }

    @Test
    public void testToAtom() throws Exception {
        Expression e1 = toAtom(x("field"));
        Expression e2 = toAtom("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TOATOM(field)", e1.toString());
    }

    @Test
    public void testToBoolean() throws Exception {
        Expression e1 = toBoolean(x("field"));
        Expression e2 = toBoolean("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TOBOOLEAN(field)", e1.toString());
    }

    @Test
    public void testToNumber() throws Exception {
        Expression e1 = toNumber(x("field"));
        Expression e2 = toNumber("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TONUMBER(field)", e1.toString());
    }

    @Test
    public void testToObject() throws Exception {
        Expression e1 = toObject(x("field"));
        Expression e2 = toObject("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TOOBJECT(field)", e1.toString());
    }

    @Test
    public void testToString() throws Exception {
        Expression e1 = TypeFunctions.toString(x("field"));
        Expression e2 = TypeFunctions.toString("field");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("TOSTRING(field)", e1.toString());
    }
}