/*
 * Copyright (C) 2016 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
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
@InterfaceStability.Experimental
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
