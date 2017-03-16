/*
 * Copyright (c) 2016 Couchbase, Inc.
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
package com.couchbase.client.java.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * An {@link Authenticator} based on login/password credentials.
 *
 * @author Simon Baslé
 * @since 2.3
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class ClassicAuthenticator implements Authenticator {

    private Credential clusterManagementCredential = null;
    private Map<String, Credential> bucketCredentials = new HashMap<String, Credential>();

    @Override
    public List<Credential> getCredentials(CredentialContext context, String specific) {
        switch (context) {
            case BUCKET_KV:
            case BUCKET_N1QL:
            case BUCKET_FTS:
            case BUCKET_ANALYTICS:
            case BUCKET_VIEW:
            case BUCKET_MANAGEMENT:
                return bucketCredentialOrEmpty(specific);
            case CLUSTER_MANAGEMENT:
                return clusterManagementCredential == null
                        ? Collections.<Credential>emptyList()
                        : Collections.singletonList(clusterManagementCredential);
            case CLUSTER_FTS:
            case CLUSTER_N1QL:
            case CLUSTER_ANALYTICS:
                return new ArrayList(bucketCredentials.values());
            default:
                throw new IllegalArgumentException("Unsupported credential context " + context + " for this Authenticator type");
        }
    }

    private List<Credential> bucketCredentialOrEmpty(String specific) {
        final Credential cred = bucketCredentials.get(specific);
        if (cred == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(cred);
        }
    }

    @Override
    public boolean isEmpty() {
        return clusterManagementCredential == null && bucketCredentials.isEmpty();
    }

    /**
     * Sets the {@link CredentialContext#BUCKET_KV} / {@link CredentialContext#BUCKET_N1QL} credential for the
     * the *bucketName* specific.
     *
     * @param bucketName the name of the bucket for which to set a password.
     * @param password the password for the bucket.
     * @return this {@link ClassicAuthenticator} for chaining.
     */
    public ClassicAuthenticator bucket(String bucketName, String password) {
        this.bucketCredentials.put(bucketName, new Credential(bucketName, password));
        return this;
    }

    /**
     * Sets the {@link CredentialContext#CLUSTER_MANAGEMENT} credential. Specific is ignored in this context.
     *
     * @param adminName the administrative login to use.
     * @param adminPassword the administrative password to use.
     * @return this {@link ClassicAuthenticator} for chaining.
     */
    public ClassicAuthenticator cluster(String adminName, String adminPassword) {
        this.clusterManagementCredential = new Credential(adminName, adminPassword);
        return this;
    }
}
