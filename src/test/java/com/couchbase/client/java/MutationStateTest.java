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
package com.couchbase.client.java;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.subdoc.SubdocOperationResult;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of the {@link MutationState} class.
 *
 * @author Michael Nitschinger
 * @since 2.3.0
 */
public class MutationStateTest {

    @Test
    public void shouldInitializeFromDocument() {
        MutationToken token = mock(MutationToken.class);
        when(token.vbucketID()).thenReturn(1L);
        JsonDocument document = JsonDocument.create("id", 0, JsonObject.empty(), 0, token);

        MutationState state = MutationState.from(document);
        int count = 0;
        for (MutationToken mt : state) {
            count++;
            assertEquals(1L, mt.vbucketID());
        }
        assertEquals(1, count);
    }

    @Test
    public void shouldInitializeFromDocuments() {
        MutationToken token1 = mock(MutationToken.class);
        when(token1.vbucketID()).thenReturn(1L);
        JsonDocument document1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);
        MutationToken token2 = mock(MutationToken.class);
        when(token2.vbucketID()).thenReturn(2L);
        JsonDocument document2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        MutationState state = MutationState.from(document1, document2);
        int count = 0;
        for (MutationToken mt : state) {
            count++;
            assertTrue(mt.vbucketID() == 1L || mt.vbucketID() == 2L);
        }
        assertEquals(2, count);
    }

    @Test
    public void shouldInitializeFromFragment() {
        MutationToken token = mock(MutationToken.class);
        when(token.vbucketID()).thenReturn(1L);
        DocumentFragment<Mutation> fragment =
            new DocumentFragment<Mutation>("id", 0, token, Collections.<SubdocOperationResult<Mutation>>emptyList());

        MutationState state = MutationState.from(fragment);
        int count = 0;
        for (MutationToken mt : state) {
            count++;
            assertEquals(1L, mt.vbucketID());
        }
        assertEquals(1, count);
    }

    @Test
    public void shouldInitializeFromFragments() {
        MutationToken token1 = mock(MutationToken.class);
        when(token1.vbucketID()).thenReturn(1L);
        DocumentFragment<Mutation> fragment1 =
            new DocumentFragment<Mutation>("id", 0, token1, Collections.<SubdocOperationResult<Mutation>>emptyList());
        MutationToken token2 = mock(MutationToken.class);
        when(token2.vbucketID()).thenReturn(2L);
        DocumentFragment<Mutation> fragment2 =
            new DocumentFragment<Mutation>("id", 0, token2, Collections.<SubdocOperationResult<Mutation>>emptyList());

        MutationState state = MutationState.from(fragment1, fragment2);
        int count = 0;
        for (MutationToken mt : state) {
            count++;
            assertTrue(mt.vbucketID() == 1L || mt.vbucketID() == 2L);
        }
        assertEquals(2, count);
    }

    @Test
    public void shouldAddDocuments() {
        MutationToken token = mock(MutationToken.class);
        when(token.vbucketID()).thenReturn(1L);
        JsonDocument document = JsonDocument.create("id", 0, JsonObject.empty(), 0, token);

        MutationState state = MutationState.from(document);

        MutationToken token2 = mock(MutationToken.class);
        when(token2.vbucketID()).thenReturn(2L);
        JsonDocument document2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        state.add(document2);

        int count = 0;
        for (MutationToken mt : state) {
            count++;
            assertTrue(mt.vbucketID() == 1L || mt.vbucketID() == 2L);
        }
        assertEquals(2, count);
    }

    @Test
    public void shouldAddFragments() {
        MutationToken token1 = mock(MutationToken.class);
        when(token1.vbucketID()).thenReturn(1L);
        DocumentFragment<Mutation> fragment1 =
            new DocumentFragment<Mutation>("id", 0, token1, Collections.<SubdocOperationResult<Mutation>>emptyList());

        MutationState state = MutationState.from(fragment1);

        MutationToken token2 = mock(MutationToken.class);
        when(token2.vbucketID()).thenReturn(2L);
        DocumentFragment<Mutation> fragment2 =
            new DocumentFragment<Mutation>("id", 0, token2, Collections.<SubdocOperationResult<Mutation>>emptyList());

        state.add(fragment2);

        int count = 0;
        for (MutationToken mt : state) {
            count++;
            assertTrue(mt.vbucketID() == 1L || mt.vbucketID() == 2L);
        }
        assertEquals(2, count);
    }

    @Test
    public void shouldAddOtherMutationState() {
        MutationToken token1 = mock(MutationToken.class);
        when(token1.vbucketID()).thenReturn(1L);
        JsonDocument document1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);

        MutationState state1 = MutationState.from(document1);

        MutationToken token2 = mock(MutationToken.class);
        when(token2.vbucketID()).thenReturn(2L);
        DocumentFragment<Mutation> fragment1 =
            new DocumentFragment<Mutation>("id", 0, token2, Collections.<SubdocOperationResult<Mutation>>emptyList());

        MutationState state2 = MutationState.from(fragment1);

        state1.add(state2);
        int count = 0;
        for (MutationToken mt : state1) {
            count++;
            assertTrue(mt.vbucketID() == 1L || mt.vbucketID() == 2L);
        }
        assertEquals(2, count);
    }

    @Test
    public void shouldExport() {
        MutationToken token = new MutationToken(1, 1234, 5678, "bucket1");
        JsonDocument document = JsonDocument.create("id", 0, JsonObject.empty(), 0, token);

        MutationState state = MutationState.from(document);

        MutationToken token2 = new MutationToken(2, 8888, 9999, "bucket2");
        JsonDocument document2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        state.add(document2);

        JsonObject result = state.export();
        JsonObject expected = JsonObject.create()
            .put("bucket1", JsonObject.create().put("1", JsonArray.from(5678L, "1234")))
            .put("bucket2", JsonObject.create().put("2", JsonArray.from(9999L, "8888")));
        assertEquals(expected, result);
    }

    @Test
    public void shouldIgnoreTokenWithLowerSequence() {
        MutationToken token1 = new MutationToken(1, 1234, 1000, "bucket1");
        JsonDocument document1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);

        MutationToken token2 = new MutationToken(1, 1234, 500, "bucket1");
        JsonDocument document2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        MutationState state = MutationState.from(document1, document2);
        int count = 0;
        for (MutationToken mt : state) {
            count++;
            assertEquals(1000, mt.sequenceNumber());
        }
        assertEquals(1, count);
    }

    @Test
    public void shouldOverrideTokenWithHigherSequence() {
        MutationToken token1 = new MutationToken(1, 1234, 1000, "bucket1");
        JsonDocument document1 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token1);

        MutationToken token2 = new MutationToken(1, 1234, 2000, "bucket1");
        JsonDocument document2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        MutationState state = MutationState.from(document1, document2);
        int count = 0;
        for (MutationToken mt : state) {
            count++;
            assertEquals(2000, mt.sequenceNumber());
        }
        assertEquals(1, count);
    }

    @Test
    public void shouldImportFromString() {
        String input = "{\n" +
            "  \"default\": {\n" +
            "    \"1\": [1, \"1234\"]\n" +
            "  },\n" +
            "  \"beer-sample\": {\n" +
            "    \"25\": [10, \"5678\"]\n" +
            "  }\n" +
            "}";

        MutationState state = MutationState.from(input);
        int count = 0;
        for (MutationToken token : state) {
            count++;
            if (token.bucket().equals("default")) {
                assertEquals(1, token.vbucketID());
                assertEquals(1, token.sequenceNumber());
                assertEquals(1234, token.vbucketUUID());
            } else if (token.bucket().equals("beer-sample")) {
                assertEquals(25, token.vbucketID());
                assertEquals(10, token.sequenceNumber());
                assertEquals(5678, token.vbucketUUID());
            } else {
                assertTrue("Found not expected bucket name when importing MutationState", false);
            }
        }
        assertEquals(2, count);
    }

    @Test
    public void shouldImportFromJsonObject() {
        MutationToken token = new MutationToken(1, 1234, 5678, "bucket1");
        JsonDocument document = JsonDocument.create("id", 0, JsonObject.empty(), 0, token);

        MutationState state = MutationState.from(document);

        MutationToken token2 = new MutationToken(2, 8888, 9999, "bucket2");
        JsonDocument document2 = JsonDocument.create("id", 0, JsonObject.empty(), 0, token2);

        state.add(document2);

        JsonObject exported = state.export();
        assertEquals(state, MutationState.from(exported));
    }

}