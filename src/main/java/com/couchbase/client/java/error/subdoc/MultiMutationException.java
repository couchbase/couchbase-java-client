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

import java.util.List;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.java.subdoc.MutateInBuilder;
import com.couchbase.client.java.subdoc.MutationSpec;

/**
 * Exception denoting that at least one error occurred when applying
 * multiple mutations using the sub-document API (a {@link MutateInBuilder#execute()} with at least two mutations).
 * None of the mutations were applied.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class MultiMutationException extends SubDocumentException {

    private final int index;
    private final ResponseStatus status;
    private final List<MutationSpec> originalSpec;

    public MultiMutationException(int index, ResponseStatus errorStatus, List<MutationSpec> originalSpec,
            CouchbaseException errorException) {
        super("Multiple mutation could not be applied. First problematic failure at " + index
                + " with status " + errorStatus, errorException);
        this.index = index;
        this.status = errorStatus;
        this.originalSpec = originalSpec;
    }

    /**
     * @return the zero-based index of the first mutation spec that caused the multi mutation to fail.
     */
    public int firstFailureIndex() {
        return index;
    }

    /**
     * @return the error status for the first mutation spec that caused the multi mutation to fail.
     */
    public ResponseStatus firstFailureStatus() {
        return status;
    }

    /**
     * @return the list of {@link MutationSpec} that was originally passed to the multi-mutation operation.
     */
    public List<MutationSpec> originalSpec() {
        return originalSpec;
    }

    /**
     * @return the mutation spec in {@link #originalSpec()} at index {@link #firstFailureIndex()}.
     */
    public MutationSpec firstFailureSpec() {
        return originalSpec.get(index);
    }
}
