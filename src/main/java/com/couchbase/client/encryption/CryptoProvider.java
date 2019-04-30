/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption;

/**
 * CryptoProvider interface for cryptographic algorithm provider implementations.
 *
 * @author Subhashni Balakrishnan
 * @since 0.1.0
 */
public interface CryptoProvider {

    /**
     * Get the key store provider set for the crypto provider use.
     *
     * @return Key store provider set
     */
    KeyStoreProvider getKeyStoreProvider();

    /**
     * Set the key store provider for the crypto provider to get keys from.
     *
     * @param provider Key store provider
     */
    void setKeyStoreProvider(KeyStoreProvider provider);

    /**
     * Encrypts the given data. Will throw exceptions if the key store and
     * key name are not set.
     *
     * @param data Data to be encrypted
     * @return encrypted bytes
     * @throws Exception on failure
     */
    byte[] encrypt(byte[] data) throws Exception;

    /**
     * Get the initialization vector size that prepended to the encrypted bytes
     *
     * @return iv size
     */
    int getIVSize();

    /**
     * Decrypts the given data. Will throw exceptions
     * if the key store and key name are not set.
     *
     * @param encrypted Encrypted data
     * @return decrypted bytes
     * @throws Exception on failure
     */
    byte[] decrypt(byte[] encrypted) throws Exception;

    /**
     * Get the signature for the integrity check using the key given.
     *
     * @param message The message to check for correctness
     * @return signature
     * @throws Exception on failure
     */
    byte[] getSignature(byte[] message) throws Exception;

    /**
     * verify the signature for the integrity check.
     *
     * @param message The message to check for correctness
     * @param signature Signature used for message
     * @return True if success
     * @throws Exception on failure
     */
    boolean verifySignature(byte[] message, byte[] signature) throws Exception;

    /**
     * Get the crypto provider algorithm name
     *
     * @return provider algorithm name
     */
    String getProviderAlgorithmName();

    /**
     * Get the crypto provider algorithm name, not the alias used for registering
     *
     * @return provider algorithm name
     */
     @Deprecated
     String getProviderName();

    /**
     * Check if the algorithm name is a match
     *
     * @param name name to check
     * @return true if there is a match
     */
    boolean checkAlgorithmNameMatch(String name);

    /**
     * Set the alias name on the provider
     *
     * @param alias alias for the provider
     */
    void setAlias(String alias);
}