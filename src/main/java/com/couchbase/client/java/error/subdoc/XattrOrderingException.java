/*
 * Copyright (c) 2017 Couchbase, Inc.
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
 * This exception is raised when non-xattr based commands are set before xattr-based ones.
 *
 * The server is expecting xattr commands at the very beginning and if this is not the case it will fail
 * with an error.
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class XattrOrderingException extends SubDocumentException {

    public XattrOrderingException(String message) {
        super(message);
    }

    public XattrOrderingException(String message, Throwable cause) {
        super(message, cause);
    }

    public XattrOrderingException(Throwable cause) {
        super(cause);
    }

}
