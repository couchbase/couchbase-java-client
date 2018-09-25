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
package com.couchbase.client.java.cluster;

import com.couchbase.client.java.bucket.BucketType;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultBucketSettingsTest {

    @Test
    public void shouldApplyDefaults() {
        BucketSettings settings = DefaultBucketSettings.create("mybucket");

        assertEquals(100, settings.quota());
        assertEquals(BucketType.COUCHBASE, settings.type());
        assertEquals("mybucket", settings.name());
        assertEquals("", settings.password());
        assertEquals(0, settings.port());
        assertEquals(0, settings.replicas());
        assertFalse(settings.indexReplicas());
        assertFalse(settings.enableFlush());
    }

    @Test
    public void shouldAllowToSetBucketType() {
        for (BucketType type : BucketType.values()) {
            assertEquals(type, DefaultBucketSettings.builder().type(type).build().type());
        }
    }

    @Test
    public void shouldAllowToEnableFlush() {
        assertTrue(DefaultBucketSettings.builder().enableFlush(true).build().enableFlush());
    }

    @Test
    public void shouldAllowToOverrideCompressionMode() {
        for (CompressionMode mode : CompressionMode.values()) {
            assertEquals(mode, DefaultBucketSettings.builder().compressionMode(mode).build().compressionMode());
        }
    }

    @Test
    public void shouldAllowToOverrideEjectionMethod() {
        for (EjectionMethod method : EjectionMethod.values()) {
            assertEquals(method, DefaultBucketSettings.builder().ejectionMethod(method).build().ejectionMethod());
        }
    }

}