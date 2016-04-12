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
package com.couchbase.client.java.document;

import com.couchbase.client.java.SerializationHelper;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies functionality of the {@link JsonDocument}.
 *
 * @author Michael Nitschinger
 * @since 2.0.2
 */
public class JsonDocumentTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowForNullId() {
        JsonDocument.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowForEmptyId() {
        JsonDocument.create("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowForNegativeExpiry() {
        JsonDocument.create("id", -1, JsonObject.create());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowLongerDocumentID() {
        String id = "thisIsCertainlyATooLongDocumentIdToStoreInCouchbaseWhoWouldUseItLikeThisAnyway?"
            + "thisIsCertainlyATooLongDocumentIdToStoreInCouchbaseWhoWouldUseItLikeThisAnyway?"
            + "thisIsCertainlyATooLongDocumentIdToStoreInCouchbaseWhoWouldUseItLikeThisAnyway?"
            + "thisIsCertainlyATooLongDocumentIdToStoreInCouchbaseWhoWouldUseItLikeThisAnyway?";
        JsonDocument.create(id);
    }

    @Test
    public void shouldSupportSerialization() throws Exception {
        JsonDocument original = JsonDocument.create("Ķěƴ", JsonObject.create().put("foo", "bar"));

        byte[] serialized = SerializationHelper.serializeToBytes(original);
        assertNotNull(serialized);

        JsonDocument deserialized = SerializationHelper.deserializeFromBytes(serialized, JsonDocument.class);
        assertEquals(original, deserialized);
    }
}
