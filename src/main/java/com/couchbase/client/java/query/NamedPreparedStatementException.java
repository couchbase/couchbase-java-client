/*
 * Copyright (c) 2016 Couchbase, Inc.
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
package com.couchbase.client.java.query;

import com.couchbase.client.core.CouchbaseException;

/**
 * An exception marking the fact that a Named Prepared Statement in N1QL couldn't be executed and
 * that there was a fallback to re-preparing it.
 *
 * @author Simon Baslé
 * @since 2.2
 */
public class NamedPreparedStatementException extends CouchbaseException {

    public NamedPreparedStatementException(String message) {
        super(message);
    }

    public NamedPreparedStatementException(String message, Throwable cause) {
        super(message, cause);
    }
}
