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

package com.couchbase.client.java.query;

import com.couchbase.client.java.SerializationHelper;
import org.junit.Test;

import static com.couchbase.client.java.query.Select.select;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StatementSerializationTest {

    @Test
    public void rawStatementShouldBeSerializable() throws Exception {
        N1qlQuery.RawStatement st = new N1qlQuery.RawStatement("test");

        byte[] bytes = SerializationHelper.serializeToBytes(st);
        assertNotNull(bytes);

        N1qlQuery.RawStatement deserialized = SerializationHelper.deserializeFromBytes(bytes,
                N1qlQuery.RawStatement.class);
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
        assertTrue(deserialized.toString().startsWith(PrepareStatement.PREPARE_PREFIX));
        assertTrue(deserialized.toString().endsWith(toPrepare.toString()));
    }
    @Test
    public void preparedPayloadShouldBeSerializable() throws Exception {
        PreparedPayload plan = new PreparedPayload(select("*"), "planName", "plan1234");

        byte[] bytes = SerializationHelper.serializeToBytes(plan);
        assertNotNull(bytes);

        PreparedPayload deserialized = SerializationHelper.deserializeFromBytes(bytes,
                PreparedPayload.class);
        assertNotNull(deserialized);
        assertNotNull(deserialized.preparedName());
        assertNotNull(deserialized.originalStatement());
        assertEquals(plan.originalStatement().toString(), deserialized.originalStatement().toString());
        assertEquals(plan.preparedName(), deserialized.preparedName());
        assertEquals(plan.toString(), deserialized.toString());
    }



}