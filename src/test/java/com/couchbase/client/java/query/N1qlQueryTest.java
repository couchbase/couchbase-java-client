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
package com.couchbase.client.java.query;

import com.couchbase.client.java.SerializationHelper;
import com.couchbase.client.java.document.json.JsonArray;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.query.Select.select;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies the functionality of the {@link N1qlQuery} class.
 *
 * @author Michael Nitschinger
 * @since 2.1.1
 */
public class N1qlQueryTest {

    private final N1qlParams params = N1qlParams.build().serverSideTimeout(1, TimeUnit.SECONDS);

    @Test
    public void simpleShouldSupportSerialization() throws Exception {
        N1qlQuery source = N1qlQuery.simple("select * from default", params);

        byte[] serialized = SerializationHelper.serializeToBytes(source);
        assertNotNull(serialized);

        N1qlQuery deserialized = SerializationHelper.deserializeFromBytes(serialized, N1qlQuery.class);
        assertSerialization(source, deserialized);
    }

    @Test
    public void parameterizedShouldSupportSerialization() throws Exception {
        N1qlQuery source = N1qlQuery.parameterized(select("*").from("default"), JsonArray.from("a", "b"), params);

        byte[] serialized = SerializationHelper.serializeToBytes(source);
        assertNotNull(serialized);

        N1qlQuery deserialized = SerializationHelper.deserializeFromBytes(serialized, N1qlQuery.class);
        assertSerialization(source, deserialized);
    }

    @Test
    public void preparedShouldSupportSerialization() throws Exception {
        PreparedPayload plan = new PreparedPayload(select("*"), "planName", "somePlan434324");
        N1qlQuery source = new PreparedN1qlQuery(plan, JsonArray.from("a", "b"), params);

        byte[] serialized = SerializationHelper.serializeToBytes(source);
        assertNotNull(serialized);

        N1qlQuery deserialized = SerializationHelper.deserializeFromBytes(serialized, N1qlQuery.class);
        assertSerialization(source, deserialized);
    }

    private static void assertSerialization(N1qlQuery left, N1qlQuery right) {
        assertEquals(left.n1ql(), right.n1ql());
        assertEquals(left.params(), right.params());
        assertEquals(left.statement().toString(), right.statement().toString());
    }

}
