package com.couchbase.client.java.cluster.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.deps.io.netty.handler.codec.http.HttpMethod;
import com.couchbase.client.java.cluster.api.ClusterApiClient;
import com.couchbase.client.java.cluster.api.Form;
import com.couchbase.client.java.cluster.api.RestBuilder;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import io.codearte.catchexception.shade.mockito.Mockito;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClusterApiClientTest {

    private static ClusterFacade core;
    private static DefaultCouchbaseEnvironment env;
    private static ClusterApiClient apiClient;

    @BeforeClass
    public static void init() {
        core = Mockito.mock(ClusterFacade.class);
        env = DefaultCouchbaseEnvironment.create();
        apiClient = new ClusterApiClient("username", "password", core, 1, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testGetWithParamsBuildsFullUrl() throws Exception {
        RestBuilder restCall = apiClient.get("some", "path/")
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
        RestBuilder restCall = apiClient.get("some")
                .withHeader("Content-Type", "foo");
        assertThat(restCall.headers()).containsEntry("Content-Type", "foo");

        restCall.contentType("bar");
        assertThat(restCall.headers()).containsEntry("Content-Type", "bar");
    }

    @Test
    public void testBodyAssumesJsonAndChangesHeadersAndContentType() {
        RestBuilder restCall = apiClient.get("some")
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
        RestBuilder restCall = apiClient.get("some");
        restCall.bodyRaw("body");
        assertThat(restCall.headers()).doesNotContainKey("Content-Type");
    }

    private void assertRestCall(RestBuilder baseBuilder,
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
    public void testBuildPathEmptyOrNull() {
        try {
            ClusterApiClient.buildPath();
            fail("Expected IllegalArgumentException for empty paths");
        } catch (IllegalArgumentException e) {
            //success
        }

        try {
            ClusterApiClient.buildPath(null);
            fail("Expected IllegalArgumentException for null paths");
        } catch (IllegalArgumentException e) {
            //success
        }
    }

    @Test
    public void testBuildPathNullElements() {
        assertThat(ClusterApiClient.buildPath("foo", null, "bar"))
                .isEqualTo("/foo/bar");
    }

    @Test
    public void testBuildPathSingleElement() {
        assertThat(ClusterApiClient.buildPath("foo")).as("foo")
                .isEqualTo("/foo");
        assertThat(ClusterApiClient.buildPath("/foo")).as("/foo")
                .isEqualTo("/foo");
        assertThat(ClusterApiClient.buildPath("foo/")).as("foo/")
                .isEqualTo("/foo");
        assertThat(ClusterApiClient.buildPath("/foo/")).as("/foo/")
                .isEqualTo("/foo");
    }

    @Test
    public void testBuildPathNoDoubleSlashes() {
        assertThat(ClusterApiClient.buildPath("foo", "bar")).as("no leading")
                .isEqualTo("/foo/bar");

        assertThat(ClusterApiClient.buildPath("/foo", "/bar")).as("leading 1 and leading 2")
                .isEqualTo("/foo/bar");

        assertThat(ClusterApiClient.buildPath("foo", "/bar")).as("leading 2")
                .isEqualTo("/foo/bar");

        assertThat(ClusterApiClient.buildPath("foo/", "/bar")).as("trailing 1 and leading 2")
                .isEqualTo("/foo/bar");

        assertThat(ClusterApiClient.buildPath("/foo/", "/bar/")).as("all trailing and leading")
                .isEqualTo("/foo/bar");
    }

    @Test
    public void testFormUrlEncoding() {
        RestBuilder restCall = apiClient.get("foo")
                .bodyForm(Form.create()
                        .add("hostname", "192.168.0.1")
                        .add("data", "value&toto"));

        assertThat(restCall.body()).isEqualTo("hostname=192.168.0.1&data=value%26toto");
        assertThat(restCall.headers()).containsEntry("Content-Type", "application/x-www-form-urlencoded");

    }

}