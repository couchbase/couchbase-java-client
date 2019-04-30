/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderMissingPublicKeyException is thrown when the public key is
 * not set on the Crypto provider.
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderMissingPublicKeyException extends Exception {

    public CryptoProviderMissingPublicKeyException() {
        super();
    }

    public CryptoProviderMissingPublicKeyException(String message) {
        super(message);
    }

    public CryptoProviderMissingPublicKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoProviderMissingPublicKeyException(Throwable cause) {
        super(cause);
    }
}
