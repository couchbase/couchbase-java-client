/*
 * Copyright (c) 2017 Couchbase, Inc.
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

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the functionality of the {@link SimpleAnalyticsQuery}.
 */
public class SimpleAnalyticsQueryTest {

    @Test
    public void shouldInitWithEmptyParams() {
        SimpleAnalyticsQuery query = new SimpleAnalyticsQuery("statement", null);
        assertEquals(AnalyticsParams.build(), query.params());
        assertEquals("statement", query.statement());
    }

    @Test
    public void shouldUseCustomParams() {
        AnalyticsParams params = AnalyticsParams.build().serverSideTimeout(1, TimeUnit.SECONDS);
        SimpleAnalyticsQuery query = new SimpleAnalyticsQuery("statement", params);
        assertEquals(params, query.params());
        assertEquals("statement", query.statement());
    }

}