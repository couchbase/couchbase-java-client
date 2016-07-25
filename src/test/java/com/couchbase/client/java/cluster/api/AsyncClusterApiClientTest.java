package com.couchbase.client.java.cluster.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.deps.io.netty.handler.codec.http.HttpMethod;
import com.couchbase.client.java.cluster.api.AsyncClusterApiClient;
import com.couchbase.client.java.cluster.api.AsyncRestBuilder;
import com.couchbase.client.java.cluster.api.ClusterApiClient;
import com.couchbase.client.java.cluster.api.Form;
import com.couchbase.client.java.cluster.api.RestBuilder;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import io.codearte.catchexception.shade.mockito.Mockito;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsyncClusterApiClientTest {

    private static ClusterFacade core;
    private static DefaultCouchbaseEnvironment env;
    private static AsyncClusterApiClient apiClient;

    @BeforeClass
    public static void init() {
        core = Mockito.mock(ClusterFacade.class);
        env = DefaultCouchbaseEnvironment.create();
        apiClient = new AsyncClusterApiClient("username", "password", core);
    }

    @Test
    public void testGetWithParamsBuildsFullUrl() throws Exception {
        AsyncRestBuilder restCall = apiClient.get("some", "path/")
                .withParam("foo", "fooValue")
                .withParam("bar", "barValue");

        assertThat(restCall.method()).isEqualTo(HttpMethod.GET);
        assertThat(restCall.path()).isEqualTo("/some/path");
        assertThat(restCall.asRequest().path()).isEqualTo("/some/path");
        assertThat(restCall.asRequest().pathWithParameters())
                .isEqualTo("/some/path?foo=fooValue&bar=barValue");
    }

    @Test
    public void testContentTypeChangesHeaders() {
        AsyncRestBuilder restCall = apiClient.get("some")
                .withHeader("Content-Type", "foo");
        assertThat(restCall.headers()).containsEntry("Content-Type", "foo");

        restCall.contentType("bar");
        assertThat(restCall.headers()).containsEntry("Content-Type", "bar");
    }

    @Test
    public void testBodyAssumesJsonAndChangesHeadersAndContentType() {
        AsyncRestBuilder restCall = apiClient.get("some")
                .withHeader("Content-Type", "foo")
                .withHeader("Accept", "*/*");
        assertThat(restCall.headers()).containsEntry("Content-Type", "foo");

        restCall.contentType("bar");
        restCall.accept("baz");
        assertThat(restCall.headers())
                .containsEntry("Content-Type", "bar")
                .containsEntry("Accept", "baz");

        restCall.body("body");
        assertThat(restCall.headers())
                .containsEntry("Content-Type", "application/json")
                .containsEntry("Accept", "application/json");
    }

    @Test
    public void testBodyRawDoesntAssumeContentType() {
        AsyncRestBuilder restCall = apiClient.get("some");
        restCall.bodyRaw("body");
        assertThat(restCall.headers()).doesNotContainKey("Content-Type");
    }

    private void assertRestCall(AsyncRestBuilder baseBuilder,
            HttpMethod expectedMethod, String expectedPath) {
        baseBuilder
                .withParam("foo", "fooValue")
                .withParam("bar", "barValue")
                .withHeader("X-Header", "foo")
                .contentType("text/plain")
                .bodyRaw("baz");

        assertThat(baseBuilder.method()).isEqualTo(expectedMethod);
        assertThat(baseBuilder.path()).isEqualTo(expectedPath);
        assertThat(baseBuilder.body()).isEqualTo("baz");
        assertThat(baseBuilder.headers()).containsOnly(
                entry("X-Header", "foo"),
                entry("Content-Type", "text/plain")
        );
        assertThat(baseBuilder.params()).containsOnly(
                entry("foo", "fooValue"),
                entry("bar", "barValue")
        );

        assertThat(baseBuilder.asRequest().path()).isEqualTo(expectedPath);
        assertThat(baseBuilder.asRequest().pathWithParameters())
                .isEqualTo(expectedPath + "?foo=fooValue&bar=barValue");
    }

    @Test
    public void testGet() throws Exception {
        assertRestCall(apiClient.get("get", "path/"), HttpMethod.GET, "/get/path");
    }

    @Test
    public void testPost() throws Exception {
        assertRestCall(apiClient.post("post", "path/"), HttpMethod.POST, "/post/path");
    }

    @Test
    public void testPut() throws Exception {
        assertRestCall(apiClient.put("put", "path/"), HttpMethod.PUT, "/put/path");
    }

    @Test
    public void testDelete() throws Exception {
        assertRestCall(apiClient.delete("delete", "path/"), HttpMethod.DELETE, "/delete/path");
    }

    @Test
    public void testFormUrlEncoding() {
        AsyncRestBuilder restCall = apiClient.get("foo")
                .bodyForm(Form.create()
                        .add("hostname", "192.168.0.1")
                        .add("data", "value&toto"));

        assertThat(restCall.body()).isEqualTo("hostname=192.168.0.1&data=value%26toto");
        assertThat(restCall.headers()).containsEntry("Content-Type", "application/x-www-form-urlencoded");
    }

}