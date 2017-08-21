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

package com.couchbase.client.java.subdoc;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.RequestTooBigException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.subdoc.CannotInsertValueException;
import com.couchbase.client.java.error.subdoc.BadDeltaException;
import com.couchbase.client.java.error.subdoc.DocumentNotJsonException;
import com.couchbase.client.java.error.subdoc.DocumentTooDeepException;
import com.couchbase.client.java.error.subdoc.NumberTooBigException;
import com.couchbase.client.java.error.subdoc.PathExistsException;
import com.couchbase.client.java.error.subdoc.PathInvalidException;
import com.couchbase.client.java.error.subdoc.PathMismatchException;
import com.couchbase.client.java.error.subdoc.PathNotFoundException;
import com.couchbase.client.java.error.subdoc.PathTooDeepException;
import com.couchbase.client.java.error.subdoc.ValueTooDeepException;

/**
 * Helper class to the subdocument API.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class SubdocHelper {

    private SubdocHelper() {}

    /**
     * Convert status that can happen in a subdocument context to corresponding exceptions.
     * Other status just become a {@link CouchbaseException}.
     */
    public static CouchbaseException commonSubdocErrors(ResponseStatus status, String id, String path) {
        switch (status) {
            case COMMAND_UNAVAILABLE:
            case ACCESS_ERROR:
                return new CouchbaseException("Access error for subdocument operations (This can also happen "+
                        "if the server version doesn't support it. Couchbase server 4.5 or later is required for Subdocument operations " +
                        "and Couchbase Server 5.0 or later is required for extended attributes access)");
            case NOT_EXISTS:
                return new DocumentDoesNotExistException("Document not found for subdoc API: " + id);
            case TEMPORARY_FAILURE:
            case SERVER_BUSY:
            case LOCKED:
                return  new TemporaryFailureException();
            case OUT_OF_MEMORY:
                return new CouchbaseOutOfMemoryException();
        //a bit specific for subdoc mutations
            case EXISTS:
                return new CASMismatchException("CAS provided in subdoc mutation didn't match the CAS of stored document " + id);
            case TOO_BIG:
                return new RequestTooBigException();
        //subdoc errors
            case SUBDOC_PATH_NOT_FOUND:
                return new PathNotFoundException(id, path);
            case SUBDOC_PATH_EXISTS:
                return new PathExistsException(id, path);
            case SUBDOC_DOC_NOT_JSON:
                return new DocumentNotJsonException(id);
            case SUBDOC_DOC_TOO_DEEP:
                return new DocumentTooDeepException(id);
            case SUBDOC_DELTA_RANGE:
                return new BadDeltaException();
            case SUBDOC_NUM_RANGE:
                return new NumberTooBigException();
            case SUBDOC_VALUE_TOO_DEEP:
                return new ValueTooDeepException(id, path);
            case SUBDOC_PATH_TOO_BIG:
                return new PathTooDeepException(path);
            //these two are a bit generic and should usually be handled upstream with a more meaningful message
            case SUBDOC_PATH_INVALID:
                return new PathInvalidException(id, path);
            case SUBDOC_PATH_MISMATCH:
                return new PathMismatchException(id, path);
            case SUBDOC_VALUE_CANTINSERT: //this shouldn't happen outside of add-unique, since we use JSON serializer
                return new CannotInsertValueException("Provided subdocument fragment is not valid JSON");
            default:
                return new CouchbaseException(status.toString());
        }
    }

    /**
     * Check whether a {@link ResponseStatus} is subdocument-level or not. That is to say an error code which,
     * if received in the context of a multi-operation, would not prevent the successful execution of other
     * operations in that packet.
     *
     * For instance, {@link ResponseStatus#SUBDOC_PATH_NOT_FOUND} is a subdoc error code that would not prevent
     * the execution of other operations whereas {@link ResponseStatus#NOT_EXISTS} is a document access error code
     * and would inherently invalidate any other operation within the theoretical packet.
     *
     * @param responseStatus the status code to check.
     * @return true if the status code denotes a subdocument-level error, false otherwise.
     */
    public static boolean isSubdocLevelError(ResponseStatus responseStatus) {
        switch(responseStatus) {
            case SUBDOC_PATH_NOT_FOUND:
            case SUBDOC_PATH_EXISTS:
            case SUBDOC_DELTA_RANGE:
            case SUBDOC_NUM_RANGE:
            case SUBDOC_VALUE_TOO_DEEP:
            case SUBDOC_PATH_TOO_BIG:
            case SUBDOC_PATH_INVALID:
            case SUBDOC_PATH_MISMATCH:
            case SUBDOC_VALUE_CANTINSERT:
                return true;
            case SUBDOC_DOC_NOT_JSON:
            case SUBDOC_DOC_TOO_DEEP:
            case SUBDOC_INVALID_COMBO:
            case SUBDOC_MULTI_PATH_FAILURE:
                return false;
            default:
                return false;
        }
    }
}
