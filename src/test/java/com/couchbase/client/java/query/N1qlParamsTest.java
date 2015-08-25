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

import com.couchbase.client.java.SerializationHelper;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests on {@link N1qlParams}.
 *
 * @author Simon Baslé
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
}
