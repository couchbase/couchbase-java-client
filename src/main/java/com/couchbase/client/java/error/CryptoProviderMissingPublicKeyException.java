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
 * CryptoProviderMissingPublicKeyException is thrown when the public key is
 * not set on the Crypto provider.
 *
 * @author Subhashni Balakrishnan
 * @since 2.6.0
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
