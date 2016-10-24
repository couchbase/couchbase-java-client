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
package com.couchbase.client.java.datastructures;

import java.util.Collections;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.subdoc.SubdocOperationResult;
import rx.functions.Func1;

/**
 * Helpers to map subdocument results for datastructures
 *
 * @author Subhashni Balakrishnan
 * @since 2.3.5
 */
public class ResultMappingUtils {

    /**
     * Creates anonymous function for mapping document fragment result to boolean
     *
     * @return anonymous function
     */
    public static Func1<DocumentFragment<Mutation>, Boolean> getMapResultFnForSubdocMutationToBoolean() {
        return new Func1<DocumentFragment<Mutation>, Boolean>() {
            @Override
            public Boolean call(DocumentFragment<Mutation> documentFragment) {
                ResponseStatus status = documentFragment.status(0);
                if (status == ResponseStatus.SUCCESS) {
                    return true;
                } else {
                    throw new CouchbaseException(status.toString());
                }
            }
        };
    }

    /**
     * Creates anonymous function for mapping full JsonDocument insert result to document fragment result
     *
     * @return anonymous function
     */
    public static Func1<JsonDocument, DocumentFragment<Mutation>> getMapFullDocResultToSubDocFn(final Mutation mutation) {
        return new Func1<JsonDocument, DocumentFragment<Mutation>>() {
            @Override
            public DocumentFragment<Mutation> call(JsonDocument document) {
                return new DocumentFragment<Mutation>(document.id(), document.cas(), document.mutationToken(),
                        Collections.singletonList(SubdocOperationResult.createResult(null, mutation, ResponseStatus.SUCCESS, null)));
            }
        };
    }

    /**
     * Creates anonymous function for mapping full JsonArrayDocument insert result to document fragment result
     *
     * @return anonymous function
     */
    public static Func1<JsonArrayDocument, DocumentFragment<Mutation>> getMapFullArrayDocResultToSubDocFn(final Mutation mutation) {
        return new Func1<JsonArrayDocument, DocumentFragment<Mutation>>() {
            @Override
            public DocumentFragment<Mutation> call(JsonArrayDocument document) {
                return new DocumentFragment<Mutation>(document.id(), document.cas(), document.mutationToken(),
                        Collections.singletonList(SubdocOperationResult.createResult(null, mutation, ResponseStatus.SUCCESS, null)));
            }
        };
    }

    /**
     * Useful for mapping exceptions of Multimutation or to be silent by mapping success to a valid subdocument result
     *
     * @return document fragment result
     */
    public static <E> DocumentFragment<Mutation> convertToSubDocumentResult(ResponseStatus status, Mutation mutation, E element) {
        return new DocumentFragment<Mutation>(null, 0, null,
                Collections.singletonList(SubdocOperationResult.createResult(null, mutation, status, element)));
    }
}
