/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderEncryptFailedException is thrown when the field cannot
 * be encrypted.
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderEncryptFailedException extends Exception {

    public CryptoProviderEncryptFailedException() {
        super();
    }

    public CryptoProviderEncryptFailedException(String message) {
        super(message);
    }

    public CryptoProviderEncryptFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoProviderEncryptFailedException(Throwable cause) {
        super(cause);
    }
}
