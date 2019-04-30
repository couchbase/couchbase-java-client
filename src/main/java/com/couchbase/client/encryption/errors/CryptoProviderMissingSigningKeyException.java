/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderMissingSigningKeyException is thrown when the signing key is
 * not set on the Crypto provider.
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderMissingSigningKeyException extends Exception {

    public CryptoProviderMissingSigningKeyException() {
        super();
    }

    public CryptoProviderMissingSigningKeyException(String message) {
        super(message);
    }

    public CryptoProviderMissingSigningKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoProviderMissingSigningKeyException(Throwable cause) {
        super(cause);
    }
}
