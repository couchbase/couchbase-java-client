/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.error;

/**
 * CryptoProviderSigningFailedException is thrown when the crypto provider
 * is not able to sign
 *
 * @author Subhashni Balakrishnan
 * @since 2.6.0
 */
public class CryptoProviderSigningFailedException extends Exception {

    public CryptoProviderSigningFailedException() { super(); }

    public CryptoProviderSigningFailedException(String message) { super(message); }

    public CryptoProviderSigningFailedException(String message, Throwable cause) { super(message, cause); }

    public CryptoProviderSigningFailedException(Throwable cause) {
        super(cause);
    }
}
