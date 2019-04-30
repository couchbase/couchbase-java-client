/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption;

import com.couchbase.client.encryption.errors.CryptoProviderAliasNullException;
import com.couchbase.client.encryption.errors.CryptoProviderMissingPublicKeyException;
import com.couchbase.client.encryption.errors.CryptoProviderNotFoundException;
import com.couchbase.client.encryption.errors.CryptoProviderSigningFailedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Encryption configuration manager set on the couchbase environment for encryption/decryption
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoManager {

    private Map<String, CryptoProvider> cryptoProviderMap;

    /**
     * Creates an instance of Encryption configuration
     */
    public CryptoManager() {
        this.cryptoProviderMap = new HashMap<String, CryptoProvider>();
    }

    /**
     * Add an encryption algorithm provider
     *
     * @param name an alias name for the encryption provider
     * @param provider Encryption provider implementation
     * @throws Exception if the alias name is null or empty
     */
    public void registerProvider(String name, CryptoProvider provider) throws Exception {
        if (name == null || name.isEmpty()) {
            throw new CryptoProviderAliasNullException("Cryptographic providers require a non-null, empty alias be configured.");
        }
        this.cryptoProviderMap.put(name, provider);
        provider.setAlias(name);
    }

    /**
     * Get an encryption algorithm provider
     *
     * @param name an alias name for the encryption provider
     * @return encryption crypto provider instance
     * @throws Exception if the alias is null or empty or not configured
     */
    public CryptoProvider getProvider(String name) throws Exception {
        if (name == null || name.isEmpty()) {
            throw new CryptoProviderAliasNullException("Cryptographic providers require a non-null, empty alias be configured.");
        }
        if (!this.cryptoProviderMap.containsKey(name) || this.cryptoProviderMap.get(name) == null) {
            throw new CryptoProviderNotFoundException("The cryptographic provider could not be found for the alias: " + name);
        }
        return this.cryptoProviderMap.get(name);
  }

    /**
     * Private interface to workaround eager loading of exception classes in JVM
     * throws the required public key missing exception
     *
     * @param alias the alias name for the provider
     * @throws Exception always
     */
    public void throwMissingPublicKeyEx(String alias) throws Exception {
        throw new CryptoProviderMissingPublicKeyException("Cryptographic providers require a non-null, empty public and key identifier (kid) be configured for the alias: " + alias);
    }

    /**
     * Private interface to workaround eager loading of exception classes in JVM
     * throws the required Signing failed exception
     *
     * @param alias the alias name for the provider
     * @throws Exception always
     */
    public void throwSigningFailedEx(String alias) throws Exception {
        throw new CryptoProviderSigningFailedException("The authentication failed while checking the signature of the message payload for the alias: " + alias);
    }
}
