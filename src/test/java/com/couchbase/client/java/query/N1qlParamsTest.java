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

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.SerializationHelper;
import com.couchbase.client.java.auth.Credential;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests on {@link N1qlParams}.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.1
 */
public class N1qlParamsTest {

    @Test
    public void shouldInjectCorrectConsistencies() {
        N1qlParams p = N1qlParams.build().consistency(ScanConsistency.NOT_BOUNDED);

        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals("not_bounded", actual.get("scan_consistency"));

        p.consistency(ScanConsistency.REQUEST_PLUS);
        p.injectParams(actual);
        assertEquals(1, actual.size());
        assertEquals("request_plus", actual.getString("scan_consistency"));

        p.consistency(ScanConsistency.STATEMENT_PLUS);
        p.injectParams(actual);
        assertEquals(1, actual.size());
        assertEquals("statement_plus", actual.getString("scan_consistency"));
    }

    @Test
    public void consistencyNotBoundedShouldEraseScanWaitAndVector() {
        N1qlParams p = N1qlParams.build()
            .scanWait(12, TimeUnit.SECONDS)
            .consistency(ScanConsistency.NOT_BOUNDED);

        JsonObject expected = JsonObject.create()
            .put("scan_consistency", "not_bounded");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldIgnoreScanWaitIfConsistencyNotBounded() {
        N1qlParams p = N1qlParams.build()
           .consistency(ScanConsistency.NOT_BOUNDED)
           .scanWait(12, TimeUnit.SECONDS);

        JsonObject expected = JsonObject.create()
            .put("scan_consistency", "not_bounded");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldAllowScanWaitOnlyForCorrectConsistencies() {
        N1qlParams p = N1qlParams.build()
                                   .scanWait(12, TimeUnit.SECONDS)
                                   .consistency(ScanConsistency.REQUEST_PLUS);

        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);
        assertEquals("12s", actual.getString("scan_wait"));

        actual = JsonObject.empty();
        p.injectParams(actual);
        assertEquals("12s", actual.getString("scan_wait"));

        p.consistency(ScanConsistency.STATEMENT_PLUS);

        actual = JsonObject.empty();
        p.injectParams(actual);
        assertEquals("12s", actual.getString("scan_wait"));

        p.consistency(ScanConsistency.NOT_BOUNDED);
        actual = JsonObject.empty();
        assertFalse(actual.containsKey("scan_wait"));

    }

    @Test
    public void shouldInjectClientId() {
        N1qlParams p = N1qlParams.build()
                                   .withContextId("test");

        JsonObject expected = JsonObject.create()
                                        .put("client_context_id", "test");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectTimeoutNanos() {
        N1qlParams p = N1qlParams.build().serverSideTimeout(24, TimeUnit.NANOSECONDS);

        JsonObject expected = JsonObject.create().put("timeout", "24ns");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }
    @Test
    public void shouldInjectTimeoutMicros() {
        N1qlParams p = N1qlParams.build().serverSideTimeout(24, TimeUnit.MICROSECONDS);

        JsonObject expected = JsonObject.create().put("timeout", "24us");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }
    @Test
    public void shouldInjectTimeoutMillis() {
        N1qlParams p = N1qlParams.build().serverSideTimeout(24, TimeUnit.MILLISECONDS);

        JsonObject expected = JsonObject.create().put("timeout", "24ms");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }
    @Test
    public void shouldInjectTimeoutSeconds() {
        N1qlParams p = N1qlParams.build().serverSideTimeout(24, TimeUnit.SECONDS);

        JsonObject expected = JsonObject.create().put("timeout", "24s");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }
    @Test
    public void shouldInjectTimeoutMinutes() {
        N1qlParams p = N1qlParams.build().serverSideTimeout(24, TimeUnit.MINUTES);

        JsonObject expected = JsonObject.create().put("timeout", "24m");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectTimeoutHours() {
        N1qlParams p = N1qlParams.build().serverSideTimeout(24, TimeUnit.HOURS);

        JsonObject expected = JsonObject.create().put("timeout", "24h");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectTimeoutHoursIfDays() {
        N1qlParams p = N1qlParams.build().serverSideTimeout(2, TimeUnit.DAYS);

        JsonObject expected = JsonObject.create().put("timeout", "48h");
        JsonObject actual = JsonObject.empty();
        p.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldDoNothingIfParamsEmpty() {
        N1qlParams p = N1qlParams.build();
        JsonObject empty = JsonObject.empty();
        p.injectParams(empty);

        assertTrue(empty.isEmpty());
    }

    @Test
    public void shouldSupportSerialization() throws Exception {
        N1qlParams source = N1qlParams
            .build()
            .serverSideTimeout(1, TimeUnit.DAYS)
            .consistency(ScanConsistency.NOT_BOUNDED);

        byte[] serialized = SerializationHelper.serializeToBytes(source);
        assertNotNull(serialized);

        N1qlParams deserialized = SerializationHelper.deserializeFromBytes(serialized, N1qlParams.class);
        assertEquals(source, deserialized);
    }

    @Test
    public void shouldInjectMaxParallelism() throws Exception {
        N1qlParams source = N1qlParams.build().maxParallelism(5);

        JsonObject expected = JsonObject.create().put("max_parallelism", "5");
        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectReadonly() throws Exception {
        N1qlParams source = N1qlParams.build().readonly(true);

        JsonObject expected = JsonObject.create().put("readonly", true);
        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectScapCap() throws Exception {
        N1qlParams source = N1qlParams.build().scanCap(5);

        JsonObject expected = JsonObject.create().put("scan_cap", "5");
        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectPipelineBatch() throws Exception {
        N1qlParams source = N1qlParams.build().pipelineBatch(99);

        JsonObject expected = JsonObject.create().put("pipeline_batch", "99");
        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectPipelineCap() throws Exception {
        N1qlParams source = N1qlParams.build().pipelineCap(-1);

        JsonObject expected = JsonObject.create().put("pipeline_cap", "-1");
        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectMutationTokenOnAtPlusWithDocument() throws Exception {
        JsonDocument doc = JsonDocument.create("doc", 0, JsonObject.empty(), 0,
            new MutationToken(1, 2345, 567, "travel-sample"));
        N1qlParams source = N1qlParams.build()
            .consistentWith(doc);

        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        JsonObject bucket = JsonObject.create().put("1",
            JsonArray.create().add(567L).add("2345"));
        JsonObject expected = JsonObject.create()
            .put("scan_consistency", "at_plus")
            .put("scan_vectors", JsonObject.create().put("travel-sample", bucket));
        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectMutationTokensOnAtPlusWithDocument() throws Exception {
        JsonDocument doc1 = JsonDocument.create("doc1", 0, JsonObject.empty(), 0,
            new MutationToken(1, 2345, 567, "bucket1"));

        JsonDocument doc2 = JsonDocument.create("doc2", 0, JsonObject.empty(), 0,
            new MutationToken(5, 2, 3, "bucket1"));
        JsonDocument doc3 = JsonDocument.create("doc3", 0, JsonObject.empty(), 0,
            new MutationToken(8, 1, 4, "bucket2"));

        N1qlParams source = N1qlParams.build()
            .consistentWith(doc1, doc2, doc3);

        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        JsonObject bucket1 = JsonObject.create()
            .put("1", JsonArray.from(567L, "2345"))
            .put("5", JsonArray.from(3L, "2"));

        JsonObject bucket2 = JsonObject.create()
            .put("8", JsonArray.from(4L, "1"));

        JsonObject expected = JsonObject.create()
            .put("scan_consistency", "at_plus")
            .put("scan_vectors", JsonObject.create().put("bucket1", bucket1).put("bucket2", bucket2));
        assertEquals(expected, actual);
    }

    @Test
    public void shouldOnlyUseHighestSeqnoTokenWithDocument() throws Exception {
        JsonDocument doc1 = JsonDocument.create("doc", 0, JsonObject.empty(), 0,
            new MutationToken(1, 2345, 567, "travel-sample"));
        JsonDocument doc2 = JsonDocument.create("doc", 0, JsonObject.empty(), 0,
            new MutationToken(1, 2345, 600, "travel-sample"));

        N1qlParams source = N1qlParams.build()
            .consistentWith(doc1, doc2);

        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        JsonObject bucket = JsonObject.create().put("1",
            JsonArray.create().add(600L).add("2345"));
        JsonObject expected = JsonObject.create()
            .put("scan_consistency", "at_plus")
            .put("scan_vectors", JsonObject.create().put("travel-sample", bucket));
        assertEquals(expected, actual);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void shouldFailIfConsistentWithAndConsistency() throws Exception {
        N1qlParams source = N1qlParams.build()
            .consistency(ScanConsistency.REQUEST_PLUS)
            .consistentWith(JsonDocument.create("doc2", 0, JsonObject.empty(), 0,
                new MutationToken(1, 2345, 567, "bucket")));

        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);
    }

    @Test
    public void shouldInjectCredentialsFromPairs() {
        N1qlParams source = N1qlParams.build()
                .withCredentials("foo", "a")
                .withCredentials("bar", "b");

        JsonObject expected = JsonObject.create().put("creds", JsonArray.from(
            JsonObject.create().put("user", "foo").put("pass", "a"),
            JsonObject.create().put("user", "bar").put("pass", "b")
        ));
        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldInjectCredentialsFromCredentialList() {
        List<Credential> credentialList = new ArrayList<Credential>(2);
        credentialList.add(new Credential("foo", "a"));
        credentialList.add(new Credential("bar", "b"));

        N1qlParams source = N1qlParams.build()
                .withCredentials(credentialList);

        JsonObject expected = JsonObject.create().put("creds", JsonArray.from(
                JsonObject.create().put("user", "foo").put("pass", "a"),
                JsonObject.create().put("user", "bar").put("pass", "b")
        ));
        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldReplaceSameLoginCredentials() {
        List<Credential> credentialList = new ArrayList<Credential>(2);
        credentialList.add(new Credential("foo", "a"));
        credentialList.add(new Credential("bar", "b"));

        N1qlParams source = N1qlParams.build()
                .withCredentials(credentialList)
                .withCredentials("bar", "c");

        JsonObject expected = JsonObject.create().put("creds", JsonArray.from(
                JsonObject.create().put("user", "foo").put("pass", "a"),
                JsonObject.create().put("user", "bar").put("pass", "c")
        ));
        JsonObject actual = JsonObject.empty();
        source.injectParams(actual);

        assertEquals(expected, actual);
    }
}
