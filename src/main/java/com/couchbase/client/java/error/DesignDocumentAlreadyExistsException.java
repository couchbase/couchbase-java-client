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
package com.couchbase.client.java.error;

/**
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DesignDocumentAlreadyExistsException extends DesignDocumentException {

    public DesignDocumentAlreadyExistsException() {
	super();
    }

    public DesignDocumentAlreadyExistsException(String message) {
        super(message);
    }

    public DesignDocumentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public DesignDocumentAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
