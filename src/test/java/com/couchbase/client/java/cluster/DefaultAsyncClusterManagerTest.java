package com.couchbase.client.java.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DefaultAsyncClusterManagerTest {

    @Test
    public void testGetConfigureBucketPayload() throws Exception {
        BucketSettings settings = DefaultBucketSettings.builder()
                .name("foo")
                .replicas(3)
                .withSetting("foo", "bar")
                .withSetting("baz", 123)
                .build();

        String expectedToString = "DefaultClusterBucketSettings{name='foo', type=COUCHBASE, quota=0, port=0, password='', replicas=3, indexReplicas=false, enableFlush=false" +
                ", customSettings={foo=bar, baz=123}}";
        String expectedInsert = "name=foo&ramQuotaMB=0&authType=sasl&saslPassword=&replicaNumber=3&bucketType=membase&flushEnabled=0" +
                "&foo=bar&baz=123";
        String expectedUpdate = "ramQuotaMB=0&authType=sasl&saslPassword=&replicaNumber=3&bucketType=membase&flushEnabled=0" +
                "&foo=bar&baz=123";


        DefaultAsyncClusterManager clusterManager = new DefaultAsyncClusterManager("login", "password", null, null, null);
        String payloadInsert = clusterManager.getConfigureBucketPayload(settings, true);
        String payloadUpdate = clusterManager.getConfigureBucketPayload(settings, false);

        assertThat(settings.toString()).isEqualTo(expectedToString);
        assertThat(payloadInsert).isEqualTo(expectedInsert);
        assertThat(payloadUpdate).isEqualTo(expectedUpdate);
    }

    @Test
    public void testConfigureBucketPayloadPrioritizesNative() throws Exception {
        BucketSettings settings = DefaultBucketSettings.builder()
                .name("foo")
                .replicas(3)
                //add redundant settings
                .withSetting("name", "bar")
                .withSetting("ramQuotaMB", "bar")
                .withSetting("authType", "bar")
                .withSetting("saslPassword", "bar")
                .withSetting("replicaNumber", "bar")
                .withSetting("bucketType", "bar")
                .withSetting("flushEnabled", "bar")
                .build();

        String expected = "name=foo&ramQuotaMB=0&authType=sasl&saslPassword=&replicaNumber=3&bucketType=membase&flushEnabled=0";

        DefaultAsyncClusterManager clusterManager = new DefaultAsyncClusterManager("login", "password", null, null, null);
        String payload = clusterManager.getConfigureBucketPayload(settings, true);

        assertThat(payload)
                .doesNotContain("bar")
                .isEqualTo(expected);
    }
}