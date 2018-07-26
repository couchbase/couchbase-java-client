/*
 * Copyright (c) 2018 Couchbase, Inc.
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
package com.couchbase.client.java.analytics;

import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Verifies the functionality of the analytics params.
 */
public class AnalyticsParamsTest {

    @Test
    public void shouldConvertTimeout() {
        AnalyticsParams params = AnalyticsParams.build().serverSideTimeout(1, TimeUnit.SECONDS);
        JsonObject result = JsonObject.create();
        params.injectParams(result);
        assertEquals("1s", result.getString("timeout"));
    }

    @Test
    public void shouldConfigurePrettyFalseByDefault() {
        AnalyticsParams params = AnalyticsParams.build();
        JsonObject result = JsonObject.create();
        params.injectParams(result);
        assertFalse(result.getBoolean("pretty"));
    }

    @Test
    public void shouldAllowToConfigurePretty() {
        AnalyticsParams params = AnalyticsParams.build().pretty(true);
        JsonObject result = JsonObject.create();
        params.injectParams(result);
        assertTrue(result.getBoolean("pretty"));
    }

}