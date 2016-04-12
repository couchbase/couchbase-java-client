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
import static com.couchbase.client.java.query.dsl.functions.MetaFunctions.base64;
import static com.couchbase.client.java.query.dsl.functions.MetaFunctions.meta;
import static com.couchbase.client.java.query.dsl.functions.MetaFunctions.uuid;
import static org.junit.Assert.*;

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class MetaFunctionsTest {

    @Test
    public void testMeta() throws Exception {
        Expression e1 = meta(x(1));
        Expression e2 = meta("1");

        assertEquals(e1.toString(), e2.toString());
        assertEquals("META(1)", e1.toString());
    }

    @Test
    public void testBase64() throws Exception {
        Expression e1 = base64(x(1));
        Expression e2 = base64("1" );

        assertEquals(e1.toString(), e2.toString());
        assertEquals("BASE64(1)", e1.toString());
    }

    @Test
    public void testUuid() throws Exception {
        assertEquals("UUID()", uuid().toString());
    }
}