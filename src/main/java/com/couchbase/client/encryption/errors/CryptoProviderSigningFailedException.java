/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Use of this software is subject to the Couchbase Inc. Enterprise Subscription License Agreement
 * which may be found at https://www.couchbase.com/ESLA-11132015.
 */

package com.couchbase.client.encryption.errors;

/**
 * CryptoProviderSigningFailedException is thrown when the crypto provider
 * is not able to sign/verify
 *
 * @author Subhashni Balakrishnan
 * @since 1.0.0
 */
public class CryptoProviderSigningFailedException extends Exception {

	public CryptoProviderSigningFailedException() { super(); }

	public CryptoProviderSigningFailedException(String message) { super(message); }

	public CryptoProviderSigningFailedException(String message, Throwable cause) { super(message, cause); }

	public CryptoProviderSigningFailedException(Throwable cause) {
		super(cause);
	}
}
