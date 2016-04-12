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

package com.couchbase.client.java.error.subdoc;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.error.TranscodingException;

/**
 * Subdocument exception thrown when the provided value cannot be inserted at the given path.
 *
 * It is actually thrown when the delta in an counter operation is valid, but applying that delta would
 * result in an out-of-range number (over {@link Long#MAX_VALUE} or under {@link Long#MIN_VALUE}).
 *
 * Note that the other case in the protocol where this can happen is when the value is invalid JSON,
 * but since the SDK serializes data to JSON beforehand, this cannot happen (a {@link TranscodingException}
 * would be thrown instead in this case).
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class CannotInsertValueException extends SubDocumentException {

    public CannotInsertValueException(String reason) {
        super(reason);
    }
}
