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

import static org.junit.Assert.*;

import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

public class N1qlMetricsTest {

    @Test
    public void emptyMetricsShouldHaveZeroEverywhere() {
        N1qlMetrics metrics = new N1qlMetrics(JsonObject.create());

        assertEquals(N1qlMetrics.NO_TIME, metrics.elapsedTime());
        assertEquals(N1qlMetrics.NO_TIME, metrics.executionTime());
        assertEquals(0, metrics.errorCount());
        assertEquals(0, metrics.mutationCount());
        assertEquals(0, metrics.resultCount());
        assertEquals(0, metrics.warningCount());
        assertEquals(0, metrics.sortCount());
        assertEquals(0L, metrics.resultSize());

        assertEquals(0, metrics.asJsonObject().size());
    }

    @Test
    public void wellFormedJsonShouldBeCorrectlyTransformed() {
        JsonObject wellFormed = JsonObject.create()
                  .put("elapsedTime", "123.45ms")
                  .put("executionTime", "200.00ms")
                  .put("resultCount", 1)
                  .put("resultSize", 2L)
                  .put("errorCount", 3)
                  .put("warningCount", 4)
                  .put("mutationCount", 5)
                  .put("sortCount", 6);
        N1qlMetrics metrics = new N1qlMetrics(wellFormed);

        assertEquals("123.45ms", metrics.elapsedTime());
        assertEquals("200.00ms", metrics.executionTime());
        assertEquals(1, metrics.resultCount());
        assertEquals(2L, metrics.resultSize());
        assertEquals(3, metrics.errorCount());
        assertEquals(4, metrics.warningCount());
        assertEquals(5, metrics.mutationCount());
        assertEquals(6, metrics.sortCount());
    }

    @Test
    public void ensureSourceJsonIsReturnedByAsJson() {
        JsonObject test = JsonObject.create().put("test", "test");
        N1qlMetrics metrics = new N1qlMetrics(test);

        assertEquals(test, metrics.asJsonObject());
        assertSame(test, metrics.asJsonObject());
    }

    @Test
    public void ensureSourceJsonDoesntBackDefinedMetrics() {
        JsonObject partial = JsonObject.create().put("errorCount", 32);
        N1qlMetrics metrics = new N1qlMetrics(partial);
        partial.put("errorCount", 3);

        assertFalse(metrics.errorCount() == partial.getInt("errorCount"));
        assertEquals(32, metrics.errorCount());
        assertSame(partial, metrics.asJsonObject());
    }
}