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
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.query.Select.select;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    @Test
    public void preparedShouldHaveBothNameAndEncodedPlanButNotStatementInN1ql() {
        PreparedPayload plan = new PreparedPayload(select("*"), "planName", "somePlan1234");
        PreparedN1qlQuery preparedN1qlQuery = new PreparedN1qlQuery(plan, N1qlParams.build());

        assertEquals("prepared", preparedN1qlQuery.statementType());
        assertEquals("planName", preparedN1qlQuery.statementValue());
        JsonObject n1ql = preparedN1qlQuery.n1ql();
        assertEquals("somePlan1234", n1ql.getString("encoded_plan"));
        assertEquals("planName", n1ql.getString("prepared"));
        assertFalse(n1ql.containsKey("statement"));
    }

}
