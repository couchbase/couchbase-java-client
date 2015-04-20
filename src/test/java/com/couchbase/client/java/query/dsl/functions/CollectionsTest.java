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
import static org.junit.Assert.assertEquals;

import java.util.Collection;

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class CollectionsTest {

    @Test
    public void testMultipleInAndWithin() {
        Expression expression = Collections.anyIn("a", x("b")).in("c", x("d")).within("e", x("f")).end("TEST", x("z"));
        assertEquals("ANY a IN b, c IN d, e WITHIN f TEST z END", expression.toString());

        expression = Collections.arrayWithin(x("arr"), "a", x("b")).in("c", x("d")).within("e", x("f")).end("TEST",
                x("z"));
        assertEquals("ARRAY arr FOR a WITHIN b, c IN d, e WITHIN f TEST z END", expression.toString());
    }

    @Test
    public void testDirectEnd() {
        Expression expression = Collections.anyIn("a", x("b")).end();
        assertEquals("ANY a IN b END", expression.toString());

        expression = Collections.arrayIn(x("arr"), "a", x("b")).end();
        assertEquals("ARRAY arr FOR a IN b END", expression.toString());
    }

    @Test
    public void testAnyIn() throws Exception {
        Expression comprehension = Collections.anyIn("child", x("tutorial.children"))
                   .satisfies(x("child.age").gt(10));
        assertEquals("ANY child IN tutorial.children SATISFIES child.age > 10 END", comprehension.toString());
    }

    @Test
    public void testAnyWithin() throws Exception {
        Expression comprehension = Collections.anyWithin("child", x("tutorial.children"))
                                              .satisfies(x("child.age").gt(10));
        assertEquals("ANY child WITHIN tutorial.children SATISFIES child.age > 10 END", comprehension.toString());
    }

    @Test
    public void testEveryIn() throws Exception {
        Expression comprehension = Collections.everyIn("child", x("tutorial.children"))
                                              .satisfies(x("child.age").gt(10));
        assertEquals("EVERY child IN tutorial.children SATISFIES child.age > 10 END", comprehension.toString());
    }

    @Test
    public void testEveryWithin() throws Exception {
        Expression comprehension = Collections.everyWithin("child", x("tutorial.children"))
                                              .satisfies(x("child.age").gt(10));
        assertEquals("EVERY child WITHIN tutorial.children SATISFIES child.age > 10 END", comprehension.toString());
    }

    @Test
    public void testArrayIn() throws Exception {
        Expression comprehension = Collections.arrayIn(x("child.fname"), "child", x("tutorial.children"))
                                              .when(x("child.age").gt(10));
        assertEquals("ARRAY child.fname FOR child IN tutorial.children WHEN child.age > 10 END", comprehension.toString());
    }

    @Test
    public void testArrayWithin() throws Exception {
        Expression comprehension = Collections.arrayWithin(x("child.fname"), "child", x("tutorial.children"))
                                              .when(x("child.age").gt(10));
        assertEquals("ARRAY child.fname FOR child WITHIN tutorial.children WHEN child.age > 10 END", comprehension.toString());
    }

    @Test
    public void testFirstIn() throws Exception {
        Expression comprehension = Collections.firstIn(x("child.fname"), "child", x("tutorial.children"))
                                              .when(x("child.age").gt(10));
        assertEquals("FIRST child.fname FOR child IN tutorial.children WHEN child.age > 10 END", comprehension.toString());
    }

    @Test
    public void testFirstWithin() throws Exception {
        Expression comprehension = Collections.firstWithin(x("child.fname"), "child", x("tutorial.children"))
                                              .when(x("child.age").gt(10));
        assertEquals("FIRST child.fname FOR child WITHIN tutorial.children WHEN child.age > 10 END", comprehension.toString());
    }
}