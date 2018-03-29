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
package com.couchbase.client.java.document.json;

import java.util.HashMap;
import java.util.Map;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.encryption.EncryptionProvider;

/**
 * Encryption configuration for the JSON value
 *
 * @author Subhashni Balakrishnan
 * @since 1.6.0
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class ValueEncryptionConfig {
    private EncryptionProvider provider;
    private Map<String, String> keys = new HashMap<String, String>();

    /**
     * Create encryption configuration instance with the provider.
     * Unless the keys are specifically set, the provider defaults will be used.
     *
     * @param provider Crypto provider
     */
    @InterfaceAudience.Public
    public ValueEncryptionConfig(EncryptionProvider provider) {
        this.provider = provider;
    }

    /**
     * Add the key name for symmetric (or key pair name for asymmetric) crypto
     *
     * @param keyName Key name
     */
    @InterfaceAudience.Public
    public void addKey(String keyName) {
        this.keys.put("encryption", keyName);
    }

    /**
     * Add a specific HMAC key name for AES data integrity check.
     *
     * @param keyName Key name
     */
    @InterfaceAudience.Public
    public void addHMACKey(String keyName) {
        this.keys.put("hmac", keyName);
    }

    /**
     * Get the provider name set on the config
     *
     * @return encryption provider name
     */
    @InterfaceAudience.Public
    public EncryptionProvider getProvider() {
        return this.provider;
    }

    /**
     * Get the encryption key name
     */
    @InterfaceAudience.Public
    public String getEncryptionKey() {
        return keys.get("encryption");
    }

    /**
     * Get the hmac key name
     */
    @InterfaceAudience.Public
    public String getHMACKey() {
        return keys.get("hmac");
    }
}