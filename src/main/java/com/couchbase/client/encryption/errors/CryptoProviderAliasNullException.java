/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderAliasNullException is thrown when the supplied argument to the
 * crypto manager fetch the crypto provider by name is null.
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderAliasNullException extends Exception {

    public CryptoProviderAliasNullException() { super(); }

    public CryptoProviderAliasNullException(String message) { super(message); }

    public CryptoProviderAliasNullException(String message, Throwable cause) { super(message, cause); }

    public CryptoProviderAliasNullException(Throwable cause) { super(cause); }
}