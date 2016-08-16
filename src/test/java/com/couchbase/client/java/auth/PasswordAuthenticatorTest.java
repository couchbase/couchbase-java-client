package com.couchbase.client.java.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

public class PasswordAuthenticatorTest {

    @Test
    public void shouldReturnEmptyListForUnsetBucketCred() {
        PasswordAuthenticator auth = new PasswordAuthenticator()
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
        PasswordAuthenticator auth = new PasswordAuthenticator()
                .bucket("foo", "bar"); //test that bucket creds don't leak

        assertThat(auth.getCredentials(CredentialContext.CLUSTER_MANAGEMENT, null))
                .isEmpty();
    }

    @Test
    public void shouldIgnoreSpecificForClusterManagement() {
        PasswordAuthenticator auth = new PasswordAuthenticator()
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
        PasswordAuthenticator auth = new PasswordAuthenticator()
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
        PasswordAuthenticator auth = new PasswordAuthenticator()
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

        PasswordAuthenticator auth = new PasswordAuthenticator()
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
        PasswordAuthenticator auth = new PasswordAuthenticator();

        assertThat(auth.getCredentials(CredentialContext.CLUSTER_N1QL, null)).isEmpty();
        assertThat(auth.getCredentials(CredentialContext.CLUSTER_FTS, null)).isEmpty();
    }

    @Test
    public void shouldReplaceCredentials() {
        Credential c1 = new Credential("foo", "bar");
        Credential c2 = new Credential("fooz", "baz");
        Credential c1bis = new Credential("foo", "oof");

        PasswordAuthenticator auth = new PasswordAuthenticator()
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
        assertThat(new PasswordAuthenticator().isEmpty()).isTrue();
    }

    @Test
    public void shouldNotBeEmptyWithClusterCredentials() {
        assertThat(new PasswordAuthenticator().cluster("foo", "bar").isEmpty()).isFalse();
    }

    @Test
    public void shouldNotBeEmptyWithOneBucketCredential() {
        assertThat(new PasswordAuthenticator().bucket("foo", "bar").isEmpty()).isFalse();
    }

}