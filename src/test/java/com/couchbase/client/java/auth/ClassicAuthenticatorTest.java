package com.couchbase.client.java.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

public class ClassicAuthenticatorTest {

    @Test
    public void shouldReturnEmptyListForUnsetBucketCred() {
        ClassicAuthenticator auth = new ClassicAuthenticator()
                .cluster("admin", "password"); //test that cluster creds don't leak

        assertThat(auth.getCredentials(CredentialContext.BUCKET_KV, "foo"))
                .isEmpty();
        assertThat(auth.getCredentials(CredentialContext.BUCKET_N1QL, "foo"))
                .isEmpty();
        assertThat(auth.getCredentials(CredentialContext.BUCKET_VIEW, "foo"))
                .isEmpty();
        assertThat(auth.getCredentials(CredentialContext.BUCKET_FTS, "foo"))
                .isEmpty();
    }

    @Test
    public void shouldReturnEmptyListForUnsetClusterCred() {
        ClassicAuthenticator auth = new ClassicAuthenticator()
                .bucket("foo", "bar"); //test that bucket creds don't leak

        assertThat(auth.getCredentials(CredentialContext.CLUSTER_MANAGEMENT, null))
                .isEmpty();
    }

    @Test
    public void shouldIgnoreSpecificForClusterManagement() {
        ClassicAuthenticator auth = new ClassicAuthenticator()
                .cluster("foo", "bar");

        List<Credential> withoutSpecific = auth.getCredentials(CredentialContext.CLUSTER_MANAGEMENT, null);
        List<Credential> withSpecific = auth.getCredentials(CredentialContext.CLUSTER_MANAGEMENT, "bar");

        assertThat(withSpecific)
                .hasSize(1)
                .hasSize(withoutSpecific.size())
                .containsOnlyElementsOf(withoutSpecific);
    }

    @Test
    public void shouldReturnSingletonListForSetBucketCred() {
        final Credential expected = new Credential("foo", "bar");
        ClassicAuthenticator auth = new ClassicAuthenticator()
                .bucket("foo", "bar")
                .cluster("admin", "password"); //test that cluster creds don't leak

        assertThat(auth.getCredentials(CredentialContext.BUCKET_KV, "foo"))
                .containsOnly(expected);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_N1QL, "foo"))
                .containsOnly(expected);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_VIEW, "foo"))
                .containsOnly(expected);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_FTS, "foo"))
                .containsOnly(expected);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_MANAGEMENT, "foo"))
                .containsOnly(expected);
    }

    @Test
    public void shouldReturnSingletonListForSetClusterCred() {
        ClassicAuthenticator auth = new ClassicAuthenticator()
                .cluster("admin", "password")
                .bucket("foo", "bar"); //test that bucket creds don't leak

        Credential expected = new Credential("admin", "password");
        assertThat(auth.getCredentials(CredentialContext.CLUSTER_MANAGEMENT, null))
                .containsOnly(expected);
    }

    @Test
    public void shouldReturnListForSetClusterLevelContexts() {
        Credential c1 = new Credential("foo", "bar");
        Credential c2 = new Credential("fooz", "baz");

        ClassicAuthenticator auth = new ClassicAuthenticator()
                .bucket("foo", "bar")
                .bucket("fooz", "baz")
                .cluster("admin", "password");

        assertThat(auth.getCredentials(CredentialContext.CLUSTER_N1QL, null))
                .containsOnly(c1, c2);
        assertThat(auth.getCredentials(CredentialContext.CLUSTER_FTS, null))
                .containsOnly(c1, c2);
    }

    @Test
    public void shouldReturnEmptyListForUnsetClusterLevelContexts() {
        ClassicAuthenticator auth = new ClassicAuthenticator();

        assertThat(auth.getCredentials(CredentialContext.CLUSTER_N1QL, null)).isEmpty();
        assertThat(auth.getCredentials(CredentialContext.CLUSTER_FTS, null)).isEmpty();
    }

    @Test
    public void shouldReplaceCredentials() {
        Credential c1 = new Credential("foo", "bar");
        Credential c2 = new Credential("fooz", "baz");
        Credential c1bis = new Credential("foo", "oof");

        ClassicAuthenticator auth = new ClassicAuthenticator()
                .bucket("foo", "bar")
                .bucket("fooz", "baz");

        assertThat(auth.getCredentials(CredentialContext.BUCKET_KV, "foo")).containsOnly(c1);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_VIEW, "foo")).containsOnly(c1);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_N1QL, "foo")).containsOnly(c1);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_FTS, "foo")).containsOnly(c1);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_MANAGEMENT, "foo")).containsOnly(c1);
        assertThat(auth.getCredentials(CredentialContext.CLUSTER_N1QL, null)).containsOnly(c1, c2);
        assertThat(auth.getCredentials(CredentialContext.CLUSTER_FTS, null)).containsOnly(c1, c2);

        auth.bucket("foo", "oof");

        assertThat(auth.getCredentials(CredentialContext.BUCKET_KV, "foo")).containsOnly(c1bis);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_VIEW, "foo")).containsOnly(c1bis);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_N1QL, "foo")).containsOnly(c1bis);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_FTS, "foo")).containsOnly(c1bis);
        assertThat(auth.getCredentials(CredentialContext.BUCKET_MANAGEMENT, "foo")).containsOnly(c1bis);
        assertThat(auth.getCredentials(CredentialContext.CLUSTER_N1QL, null)).containsOnly(c1bis, c2);
        assertThat(auth.getCredentials(CredentialContext.CLUSTER_FTS, null)).containsOnly(c1bis, c2);
    }

    @Test
    public void shouldBeEmpty() {
        assertThat(new ClassicAuthenticator().isEmpty()).isTrue();
    }

    @Test
    public void shouldNotBeEmptyWithClusterCredentials() {
        assertThat(new ClassicAuthenticator().cluster("foo", "bar").isEmpty()).isFalse();
    }

    @Test
    public void shouldNotBeEmptyWithOneBucketCredential() {
        assertThat(new ClassicAuthenticator().bucket("foo", "bar").isEmpty()).isFalse();
    }

}