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

/**
 * Subdocument exception thrown when the delta in an arithmetic operation (eg counter) is invalid. In this
 * SDK, this is equivalent to say that the delta is zero.
 *
 * Note that the server also returns the corresponding error code when the delta value itself is too big,
 * or not a number, but since the SDK enforces deltas to be of type long, these cases shouldn't come up.
 *
 * @author Simon Baslé
 * @since 2.2
 * @see ValueTooDeepException if the delta is valid but applying it to the current value would cause an overflow.
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class BadDeltaException extends SubDocumentException {

    public BadDeltaException(String message) {
        super(message);
    }

    public BadDeltaException() {
        super("Delta must not be zero, or is otherwise invalid");
    }
}
