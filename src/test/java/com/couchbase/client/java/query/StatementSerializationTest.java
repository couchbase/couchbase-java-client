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

package com.couchbase.client.java.query;

import static com.couchbase.client.java.query.Select.select;
import static org.junit.Assert.*;

import com.couchbase.client.java.SerializationHelper;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

public class StatementSerializationTest {

    @Test
    public void rawStatementShouldBeSerializable() throws Exception {
        Query.RawStatement st = new Query.RawStatement("test");

        byte[] bytes = SerializationHelper.serializeToBytes(st);
        assertNotNull(bytes);

        Query.RawStatement deserialized = SerializationHelper.deserializeFromBytes(bytes,
                Query.RawStatement.class);
        assertEquals(st.toString(), deserialized.toString());
    }
    @Test
    public void prepareStatementShouldBeSerializable() throws Exception {
        Statement toPrepare = select("*");
        PrepareStatement st = PrepareStatement.prepare(toPrepare);

        byte[] bytes = SerializationHelper.serializeToBytes(st);
        assertNotNull(bytes);

        PrepareStatement deserialized = SerializationHelper.deserializeFromBytes(bytes,
                PrepareStatement.class);
        assertEquals(st.toString(), deserialized.toString());
        assertEquals(PrepareStatement.PREPARE_PREFIX + toPrepare.toString(), deserialized.toString());
    }
    @Test
    public void queryPlanShouldBeSerializable() throws Exception {
        JsonObject internalPlan = JsonObject.create().put("plan", "test");
        QueryPlan plan = new QueryPlan(internalPlan);

        byte[] bytes = SerializationHelper.serializeToBytes(plan);
        assertNotNull(bytes);

        QueryPlan deserialized = SerializationHelper.deserializeFromBytes(bytes,
                QueryPlan.class);
        assertNotNull(deserialized);
        assertNotNull(deserialized.plan());
        assertEquals(plan.plan(), deserialized.plan());
        assertEquals(plan.toString(), deserialized.toString());
    }



}