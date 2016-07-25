package com.couchbase.client.java.cluster.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Test;

public class AbstractClusterApiClientTest {

    @Test
    public void testBuildPathEmptyOrNull() {
        try {
            AbstractClusterApiClient.buildPath();
            fail("Expected IllegalArgumentException for empty paths");
        } catch (IllegalArgumentException e) {
            //success
        }

        try {
            AbstractClusterApiClient.buildPath(null);
            fail("Expected IllegalArgumentException for null paths");
        } catch (IllegalArgumentException e) {
            //success
        }
    }

    @Test
    public void testBuildPathNullElements() {
        assertThat(AbstractClusterApiClient.buildPath("foo", null, "bar"))
                .isEqualTo("/foo/bar");
    }

    @Test
    public void testBuildPathSingleElement() {
        assertThat(AbstractClusterApiClient.buildPath("foo")).as("foo")
                .isEqualTo("/foo");
        assertThat(AbstractClusterApiClient.buildPath("/foo")).as("/foo")
                .isEqualTo("/foo");
        assertThat(AbstractClusterApiClient.buildPath("foo/")).as("foo/")
                .isEqualTo("/foo");
        assertThat(AbstractClusterApiClient.buildPath("/foo/")).as("/foo/")
                .isEqualTo("/foo");
    }

    @Test
    public void testBuildPathNoDoubleSlashes() {
        assertThat(AbstractClusterApiClient.buildPath("foo", "bar")).as("no leading")
                .isEqualTo("/foo/bar");

        assertThat(AbstractClusterApiClient.buildPath("/foo", "/bar")).as("leading 1 and leading 2")
                .isEqualTo("/foo/bar");

        assertThat(AbstractClusterApiClient.buildPath("foo", "/bar")).as("leading 2")
                .isEqualTo("/foo/bar");

        assertThat(AbstractClusterApiClient.buildPath("foo/", "/bar")).as("trailing 1 and leading 2")
                .isEqualTo("/foo/bar");

        assertThat(AbstractClusterApiClient.buildPath("/foo/", "/bar/")).as("all trailing and leading")
                .isEqualTo("/foo/bar");
    }
}