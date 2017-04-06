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

    @Test
    public void testAnyAndEveryIn() throws Exception {
        Expression comprehension = Collections.anyAndEveryIn("child", x("tutorial.children")).satisfies(x("child.age").gt(10));
        assertEquals("ANY AND EVERY child IN tutorial.children SATISFIES child.age > 10 END", comprehension.toString());
    }
}