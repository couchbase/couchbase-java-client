/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderDecryptFailedException is thrown when the encrypted
 * field fetched from the server cannot be decrypted.
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderDecryptFailedException extends Exception {

    public CryptoProviderDecryptFailedException() { super(); }

    public CryptoProviderDecryptFailedException(String message) { super(message); }

    public CryptoProviderDecryptFailedException(String message, Throwable cause) { super(message, cause); }

    public CryptoProviderDecryptFailedException(Throwable cause) { super(cause); }
}
