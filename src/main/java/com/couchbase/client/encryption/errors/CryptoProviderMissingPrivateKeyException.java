/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderMissingPrivateKeyException is thrown when the private key is
 * not set on the Crypto provider.
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderMissingPrivateKeyException extends Exception {

    public CryptoProviderMissingPrivateKeyException() {
        super();
    }

    public CryptoProviderMissingPrivateKeyException(String message) {
        super(message);
    }

    public CryptoProviderMissingPrivateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoProviderMissingPrivateKeyException(Throwable cause) {
        super(cause);
    }
}
