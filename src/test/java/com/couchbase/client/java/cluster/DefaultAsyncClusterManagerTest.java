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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DefaultAsyncClusterManagerTest {

    @Test
    public void testGetConfigureBucketPayload() {
        BucketSettings settings = DefaultBucketSettings.builder()
                .name("foo")
                .replicas(3)
                .withSetting("foo", "bar")
                .withSetting("baz", 123)
                .build();

        String expectedToString = "DefaultBucketSettings{name='foo', type=COUCHBASE, quota=100, port=0, password='', replicas=3, indexReplicas=false, compressionMode=null, ejectionMethod=null, enableFlush=false" +
                ", customSettings={foo=bar, baz=123}}";
        String expectedInsert = "name=foo&ramQuotaMB=100&authType=sasl&replicaNumber=3&bucketType=membase&flushEnabled=0" +
                "&foo=bar&baz=123";
        String expectedUpdate = "ramQuotaMB=100&authType=sasl&replicaNumber=3&bucketType=membase&flushEnabled=0" +
                "&foo=bar&baz=123";


        DefaultAsyncClusterManager clusterManager = new DefaultAsyncClusterManager("login", "password", null, null, null);
        String payloadInsert = clusterManager.getConfigureBucketPayload(settings, true);
        String payloadUpdate = clusterManager.getConfigureBucketPayload(settings, false);

        assertThat(settings.toString()).isEqualTo(expectedToString);
        assertThat(payloadInsert).isEqualTo(expectedInsert);
        assertThat(payloadUpdate).isEqualTo(expectedUpdate);
    }

    @Test
    public void testConfigureBucketPayloadPrioritizesNative() {
        BucketSettings settings = DefaultBucketSettings.builder()
                .name("foo")
                .replicas(3)
                //add redundant settings
                .withSetting("name", "bar")
                .withSetting("ramQuotaMB", "bar")
                .withSetting("authType", "bar")
                .withSetting("replicaNumber", "bar")
                .withSetting("bucketType", "bar")
                .withSetting("flushEnabled", "bar")
                .build();

        String expected = "name=foo&ramQuotaMB=100&authType=sasl&replicaNumber=3&bucketType=membase&flushEnabled=0";

        DefaultAsyncClusterManager clusterManager = new DefaultAsyncClusterManager("login", "password", null, null, null);
        String payload = clusterManager.getConfigureBucketPayload(settings, true);

        assertThat(payload)
                .doesNotContain("bar")
                .isEqualTo(expected);
    }

    @Test
    public void shouldSerializeCompressionMode() {
        BucketSettings settings = DefaultBucketSettings.builder()
                .compressionMode(CompressionMode.ACTIVE)
                .build();

        DefaultAsyncClusterManager clusterManager = new DefaultAsyncClusterManager("login", "password", null, null, null);
        String payload = clusterManager.getConfigureBucketPayload(settings, false);

        assertTrue(payload.contains("compressionMode=active"));
    }

    @Test
    public void shouldSerializeEjectionMethod() {
        BucketSettings settings = DefaultBucketSettings.builder()
                .ejectionMethod(EjectionMethod.FULL)
                .build();

        DefaultAsyncClusterManager clusterManager = new DefaultAsyncClusterManager("login", "password", null, null, null);
        String payload = clusterManager.getConfigureBucketPayload(settings, false);

        assertTrue(payload.contains("evictionPolicy=fullEviction"));
    }
}