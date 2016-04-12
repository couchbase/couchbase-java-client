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