/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderNotFoundException is thrown when the provider is
 * not registered on the Crypto manager.
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderNotFoundException extends Exception {

    public CryptoProviderNotFoundException() {
        super();
    }

    public CryptoProviderNotFoundException(String message) {
        super(message);
    }

    public CryptoProviderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoProviderNotFoundException(Throwable cause) {
        super(cause);
    }
}
