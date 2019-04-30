/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderKeySizeException is thrown when the supplied key's size
 * does not match the Crypto provider's expected key size
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderKeySizeException extends Exception {

    public CryptoProviderKeySizeException() {
        super();
    }

    public CryptoProviderKeySizeException(String message) {
        super(message);
    }

    public CryptoProviderKeySizeException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoProviderKeySizeException(Throwable cause) {
        super(cause);
    }
}
