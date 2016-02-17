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

package com.couchbase.client.java.subdoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.core.message.kv.subdoc.multi.MultiMutationResponse;
import com.couchbase.client.core.message.kv.subdoc.multi.MultiResult;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.core.message.kv.subdoc.multi.MutationCommand;
import com.couchbase.client.core.message.kv.subdoc.multi.SubMultiMutationRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.AbstractSubdocMutationRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SimpleSubdocResponse;
import com.couchbase.client.core.message.kv.subdoc.simple.SubArrayRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubCounterRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubDeleteRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubDictAddRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubDictUpsertRequest;
import com.couchbase.client.core.message.kv.subdoc.simple.SubReplaceRequest;
import com.couchbase.client.core.message.observe.Observe;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.deps.io.netty.util.internal.StringUtil;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.subdoc.CannotInsertValueException;
import com.couchbase.client.java.error.subdoc.DocumentNotJsonException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.error.subdoc.PathExistsException;
import com.couchbase.client.java.error.subdoc.PathInvalidException;
import com.couchbase.client.java.error.subdoc.PathMismatchException;
import com.couchbase.client.java.error.subdoc.PathNotFoundException;
import com.couchbase.client.java.error.subdoc.ZeroDeltaException;
import com.couchbase.client.java.transcoder.subdoc.FragmentTranscoder;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;


/**
 * A builder for subdocument mutations. In order to perform the final set of operations, use the
 * {@link #doMutate()} method. Operations are performed asynchronously (see {@link MutateInBuilder} for a synchronous
 * version).
 *
 * Instances of this builder should be obtained through {@link AsyncBucket#mutateIn(String)} rather than directly
 * constructed.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class AsyncMutateInBuilder {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(AsyncMutateInBuilder.class);

    private final ClusterFacade core;
    private final CouchbaseEnvironment environment;
    private final String bucketName;
    private final FragmentTranscoder subdocumentTranscoder;

    protected final String docId;
    protected final List<MutationSpec> mutationSpecs;

    protected int expiry;
    protected long cas;
    protected PersistTo persistTo;
    protected ReplicateTo replicateTo;

    /**
     * Instances of this builder should be obtained through {@link AsyncBucket#mutateIn(String)} rather than directly
     * constructed.
     */
    @InterfaceAudience.Private
    public AsyncMutateInBuilder(ClusterFacade core, String bucketName, CouchbaseEnvironment environment,
            FragmentTranscoder transcoder, String docId) {
        if (docId == null || docId.isEmpty()) {
            throw new IllegalArgumentException("The document ID must not be null or empty.");
        }
        if (docId.getBytes().length > 250) {
            throw new IllegalArgumentException("The document ID must not be larger than 250 bytes");
        }

        this.core = core;
        this.bucketName = bucketName;
        this.environment = environment;
        this.subdocumentTranscoder = transcoder;
        this.docId = docId;
        this.mutationSpecs = new ArrayList<MutationSpec>();

        //values below can be customized by the builder
        this.expiry = 0;
        this.cas = 0L;
        this.persistTo = PersistTo.NONE;
        this.replicateTo = ReplicateTo.NONE;
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document}.
     * The list of mutations and paths to mutate in the JSON is added through builder methods like
     * {@link #arrayInsert(String, Object)}.
     *
     * Multi-mutations are applied as a whole, atomically at the document level. That means that if one of the mutations
     * fails, none of the mutations are applied. Otherwise, all mutations can be considered successful and the whole
     * operation will receive a {@link DocumentFragment} with the updated cas (and optionally {@link MutationToken}).
     *
     * The subdocument API has the benefit of only transmitting the fragment of the document you want to mutate
     * on the wire, instead of the whole document.
     *
     * This Observable most notable error conditions are:
     *
     *  - The enclosing document does not exist: {@link DocumentDoesNotExistException}
     *  - The enclosing document is not JSON: {@link DocumentNotJsonException}
     *  - No mutation was defined through the builder API: {@link IllegalArgumentException}
     *  - A mutation spec couldn't be encoded and the whole operation was cancelled: {@link TranscodingException}
     *  - The multi-mutation failed: {@link MultiMutationException}
     *  - The durability constraint could not be fulfilled because of a temporary or persistent problem: {@link DurabilityException}
     *  - CAS was provided but optimistic locking failed: {@link CASMismatchException}
     *
     * When receiving a {@link MultiMutationException}, one can inspect the exception to find the zero-based index and
     * error {@link ResponseStatus status code} of the first failing {@link Mutation}. Subsequent mutations may have
     * also failed had they been attempted, but a single spec failing causes the whole operation to be cancelled.
     *
     * Other top-level error conditions are similar to those encountered during a document-level {@link AsyncBucket#replace(Document)}.
     *
     * @return an {@link Observable} of a single {@link DocumentFragment} (if successful) containing updated cas metadata.
     * Note that some individual results could also bear a value, like counter operations.
     */
    public Observable<DocumentFragment<Mutation>> doMutate() {
        if (mutationSpecs.isEmpty()) {
            throw new IllegalArgumentException("Execution of a subdoc mutation requires at least one operation");
        } else if (mutationSpecs.size() == 1) { //FIXME implement single path optim
            //single path optimization
            return doSingleMutate(mutationSpecs.get(0));
        } else {
            return doMultiMutate();
        }
    }

    //==== DOCUMENT level modifiers ====
    /**
     * Change the expiry of the enclosing document as part of the mutation.
     *
     * @param expiry the new expiry to apply (or 0 to avoid changing the expiry)
     * @return this builder for chaining.
     */
    public AsyncMutateInBuilder withExpiry(int expiry) {
        this.expiry = expiry;
        return this;
    }

    /**
     * Apply the whole mutation using optimistic locking, checking against the provided CAS value.
     *
     * @param cas the CAS to compare the enclosing document to.
     * @return this builder for chaining.
     */
    public AsyncMutateInBuilder withCas(long cas) {
        this.cas = cas;
        return this;
    }

    /**
     * Set a persistence durability constraint for the whole mutation.
     *
     * @param persistTo the persistence durability constraint to observe.
     * @return this builder for chaining.
     */
    public AsyncMutateInBuilder withDurability(PersistTo persistTo) {
        this.persistTo = persistTo;
        return this;
    }

    /**
     * Set a replication durability constraint for the whole mutation.
     *
     * @param replicateTo the replication durability constraint to observe.
     * @return this builder for chaining.
     */
    public AsyncMutateInBuilder withDurability(ReplicateTo replicateTo) {
        this.replicateTo = replicateTo;
        return this;
    }

    /**
     * Set both a persistence and a replication durability constraints for the whole mutation.
     *
     * @param persistTo the persistence durability constraint to observe.
     * @param replicateTo the replication durability constraint to observe.
     * @return this builder for chaining.
     */
    public AsyncMutateInBuilder withDurability(PersistTo persistTo, ReplicateTo replicateTo) {
        this.persistTo = persistTo;
        this.replicateTo = replicateTo;
        return this;
    }

    //==== SUBDOC operation specs ====
    /**
     * Replace an existing value by the given fragment.
     *
     * @param path the path where the value to replace is.
     * @param fragment the new value.
     */
    public <T> AsyncMutateInBuilder replace(String path, T fragment) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for replace");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.REPLACE, path, fragment, false));
        return this;
    }

    /**
     * Insert a fragment provided the last element of the path doesn't exists,
     *
     * @param path the path where to insert a new dictionary value.
     * @param fragment the new dictionary value to insert.
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> AsyncMutateInBuilder insert(String path, T fragment, boolean createParents) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for insert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.DICT_ADD, path, fragment, createParents));
        return this;
    }

    /**
     * Insert a fragment, replacing the old value if the path exists.
     *
     * @param path the path where to insert (or replace) a dictionary value.
     * @param fragment the new dictionary value to be applied.
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> AsyncMutateInBuilder upsert(String path, T fragment, boolean createParents) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for upsert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.DICT_UPSERT, path, fragment, createParents));
        return this;
    }

     /**
     * Remove an entry in a JSON document (scalar, array element, dictionary entry,
     * whole array or dictionary, depending on the path).
     *
     * @param path the path to remove.
     */
    public <T> AsyncMutateInBuilder remove(String path) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for remove");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.DELETE, path, null, false));
        return this;
    }

    /**
     * Increment/decrement a numerical fragment in a JSON document.
     * If the value (last element of the path) doesn't exist the counter is created and takes the value of the delta.
     *
     * @param path the path to the counter (must be containing a number).
     * @param delta the value to increment or decrement the counter by.
     * @param createParents true to create missing intermediary nodes.
     */
    public AsyncMutateInBuilder counter(String path, long delta, boolean createParents) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for counter");
        }
        // shortcircuit if delta is zero
        if (delta == 0L) {
            throw new ZeroDeltaException();
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.COUNTER, path, delta, createParents));
        return this;
    }

    /**
     * Push to the front of an existing array, prepending the value.
     *
     * @param path the path of the array.
     * @param value the value to insert at the front of the array.
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> AsyncMutateInBuilder pushFront(String path, T value, boolean createParents) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_FIRST, path, value, createParents));
        return this;
    }

    /**
     * Push to the back of an existing array, appending the value.
     *
     * @param path the path of the array.
     * @param value the value to insert at the back of the array.
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> AsyncMutateInBuilder pushBack(String path, T value, boolean createParents) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_LAST, path, value, createParents));
        return this;
    }

    /**
     * Insert into an existing array at a specific position
     * (denoted in the path, eg. "sub.array[2]").
     *
     * @param path the path (including array position) where to insert the value.
     * @param value the value to insert in the array.
     */
    public <T> AsyncMutateInBuilder arrayInsert(String path, T value) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for arrayInsert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_INSERT, path, value, false));
        return this;
    }

    /**
     * Insert a value in an existing array only if the value
     * isn't already contained in the array (by way of string comparison).
     *
     * @param path the path to mutate in the JSON.
     * @param value the value to insert.
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> AsyncMutateInBuilder addUnique(String path, T value, boolean createParents) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_ADD_UNIQUE, path, value, createParents));
        return this;
    }


    //==============================
    //multi operation implementation
    protected Observable<DocumentFragment<Mutation>> doMultiMutate() {
        if (mutationSpecs.isEmpty()) {
            throw new IllegalArgumentException("At least one Mutation Spec is necessary for mutateIn");
        }

        Observable<DocumentFragment<Mutation>> mutations = Observable.defer(new Func0<Observable<MutationCommand>>() {
            @Override
            public Observable<MutationCommand> call() {
                List<ByteBuf> bufList = new ArrayList<ByteBuf>(mutationSpecs.size());
                final List<MutationCommand> commands = new ArrayList<MutationCommand>(mutationSpecs.size());

                for (int i = 0; i < mutationSpecs.size(); i++) {
                    MutationSpec spec = mutationSpecs.get(i);
                    if (spec.type() == Mutation.DELETE) {
                        commands.add(new MutationCommand(Mutation.DELETE, spec.path()));
                    } else {
                        try {
                            ByteBuf buf = subdocumentTranscoder.encodeWithMessage(spec.fragment(), "Couldn't encode MutationSpec #" +
                                    i + " (" + spec.type() + " on " + spec.path() + ") in " + docId);
                            bufList.add(buf);
                            commands.add(new MutationCommand(spec.type(), spec.path(), buf, spec.createParents()));
                        } catch (TranscodingException e) {
                            releaseAll(bufList);
                            return Observable.error(e);
                        }
                    }
                }
                return Observable.from(commands);
            }
        }).toList()
        .flatMap(new Func1<List<MutationCommand>, Observable<MultiMutationResponse>>(){
            @Override
            public Observable<MultiMutationResponse> call(List<MutationCommand> mutationCommands) {
                return core.send(new SubMultiMutationRequest(docId, bucketName, expiry, cas, mutationCommands));
            }
        }).flatMap(new Func1<MultiMutationResponse, Observable<DocumentFragment<Mutation>>>() {
            @Override
            public Observable<DocumentFragment<Mutation>> call(MultiMutationResponse response) {
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                if (response.status().isSuccess()) {
                    int resultSize = response.responses().size();
                    List<SubdocOperationResult<Mutation>> results = new ArrayList<SubdocOperationResult<Mutation>>(resultSize);
                    for (MultiResult<Mutation> result : response.responses()) {
                        try {
                            Object content = null;
                            if (result.value().isReadable()) {
                                //generic, so will transform dictionaries into JsonObject and arrays into JsonArray
                                content = subdocumentTranscoder.decode(result.value(), Object.class);
                            }
                            results.add(SubdocOperationResult
                                    .createResult(result.path(), result.operation(), result.status(), content));
                        } catch (TranscodingException e) {
                            LOGGER.error("Couldn't decode multi-lookup " + result.operation() + " for " + docId + "/" + result.path(), e);
                            results.add(SubdocOperationResult.createFatal(result.path(), result.operation(), e));
                        } finally {
                            if (result.value() != null) {
                                result.value().release();
                            }
                        }
                    }
                    return Observable.just(
                            new DocumentFragment<Mutation>(docId, response.cas(), response.mutationToken(), results));
                }

                switch(response.status()) {
                    case SUBDOC_MULTI_PATH_FAILURE:
                        int index = response.firstErrorIndex();
                        ResponseStatus errorStatus = response.firstErrorStatus();
                        String errorPath = mutationSpecs.get(index).path();
                        CouchbaseException errorException = SubdocHelper.commonSubdocErrors(errorStatus, docId, errorPath);

                        return Observable.error(new MultiMutationException(index, errorStatus, mutationSpecs, errorException));
                    default:
                        return Observable.error(SubdocHelper.commonSubdocErrors(response.status(), docId, "MULTI-MUTATION"));
                }
            }
        });

        return subdocObserveMutation(mutations);
    }

    //================================
    //Single operation implementations
    protected Observable<DocumentFragment<Mutation>> doSingleMutate(MutationSpec spec) {
        Observable<DocumentFragment<Mutation>> mutation;
        switch (spec.type()) {
            case DICT_UPSERT:
                mutation = doSingleMutate(spec, DICT_UPSERT_FACTORY, DICT_UPSERT_EVALUATOR);
                break;
            case DICT_ADD:
                mutation = doSingleMutate(spec, DICT_ADD_FACTORY, DICT_ADD_EVALUATOR);
                break;
            case REPLACE:
                mutation = doSingleMutate(spec, REPLACE_FACTORY, REPLACE_EVALUATOR);
                break;
            case ARRAY_PUSH_FIRST:
            case ARRAY_PUSH_LAST:
                mutation = doSingleMutate(spec, ARRAY_EXTEND_FACTORY, ARRAY_EXTEND_EVALUATOR);
                break;
            case ARRAY_INSERT:
                mutation = doSingleMutate(spec, ARRAY_INSERT_FACTORY, ARRAY_INSERT_EVALUATOR);
                break;
            case ARRAY_ADD_UNIQUE:
                mutation = doSingleMutate(spec, ARRAY_ADDUNIQUE_FACTORY, ARRAY_ADDUNIQUE_EVALUATOR);
                break;
            case COUNTER:
                mutation = counterIn(spec);
                break;
            case DELETE:
                mutation = removeIn(spec);
                break;
            default:
                mutation = Observable.error(new UnsupportedOperationException());
                break;
        }
        return subdocObserveMutation(mutation);
    }


    private final Func2<MutationSpec, ByteBuf, SubDictUpsertRequest> DICT_UPSERT_FACTORY =
            new Func2<MutationSpec, ByteBuf, SubDictUpsertRequest>() {
                @Override
                public SubDictUpsertRequest call(MutationSpec spec, ByteBuf buf) {
                    SubDictUpsertRequest request = new SubDictUpsertRequest(docId, spec.path(), buf, bucketName, expiry, cas);
                    request.createIntermediaryPath(spec.createParents());
                    return request;
                }
            };
    private static final Func3<ResponseStatus, String, String, Object> DICT_UPSERT_EVALUATOR =
            new Func3<ResponseStatus, String, String, Object>() {
                @Override
                public Object call(ResponseStatus status, String docId, String path) {
                    switch(status) {
                        case SUCCESS:
                            return null;
                        case SUBDOC_PATH_INVALID:
                            throw new PathInvalidException("Path " + path + " ends in an array index in "
                                    + docId + ", expected dictionary");
                        case SUBDOC_PATH_MISMATCH:
                            throw new PathMismatchException("Path " + path + " ends in a scalar value in "
                                    + docId + ", expected dictionary");
                        default:
                            throw SubdocHelper.commonSubdocErrors(status, docId, path);
                    }
                }
            };

    private final Func2<MutationSpec, ByteBuf, SubDictAddRequest> DICT_ADD_FACTORY =
            new Func2<MutationSpec, ByteBuf, SubDictAddRequest>() {
                @Override
                public SubDictAddRequest call(MutationSpec spec, ByteBuf buf) {
                    SubDictAddRequest request = new SubDictAddRequest(docId, spec.path(), buf, bucketName, expiry, cas);
                    request.createIntermediaryPath(spec.createParents());
                    return request;
                }
            };
    private static final Func3<ResponseStatus, String, String, Object> DICT_ADD_EVALUATOR =
            new Func3<ResponseStatus, String, String, Object>() {
                @Override
                public Object call(ResponseStatus status, String docId, String path) {
                    switch(status) {
                        case SUCCESS:
                            return null;
                        case SUBDOC_PATH_INVALID:
                            throw new PathInvalidException("Path " + path + " ends in an array index in "
                                    + docId + ", expected dictionary");
                        case SUBDOC_PATH_MISMATCH:
                            throw new PathMismatchException("Path " + path + " ends in a scalar value in "
                                    + docId + ", expected dictionary");
                        case SUBDOC_PATH_EXISTS:
                            throw new PathExistsException(docId, path);
                        default:
                            throw SubdocHelper.commonSubdocErrors(status, docId, path);
                    }
                }
            };

    private final Func2<MutationSpec, ByteBuf, SubReplaceRequest> REPLACE_FACTORY =
            new Func2<MutationSpec, ByteBuf, SubReplaceRequest>() {
                @Override
                public SubReplaceRequest call(MutationSpec spec, ByteBuf buf) {
                    SubReplaceRequest request = new SubReplaceRequest(docId, spec.path(), buf, bucketName, expiry, cas);
                    request.createIntermediaryPath(spec.createParents());
                    return request;
                }
            };
    private static final Func3<ResponseStatus, String, String, Object> REPLACE_EVALUATOR =
            new Func3<ResponseStatus, String, String, Object>() {
                @Override
                public Object call(ResponseStatus status, String docId, String path) {
                    switch(status) {
                        case SUCCESS:
                            return null;
                        case SUBDOC_PATH_NOT_FOUND:
                            throw new PathNotFoundException("Path to be replaced " + path + " not found in " + docId);
                        case SUBDOC_PATH_MISMATCH:
                            throw new PathMismatchException("Path " + path + " ends in a scalar value in "
                                    + docId + ", expected dictionary");
                        default:
                            throw SubdocHelper.commonSubdocErrors(status, docId, path);
                    }
                }
            };

    private final Func2<MutationSpec, ByteBuf, SubArrayRequest> ARRAY_EXTEND_FACTORY =
            new Func2<MutationSpec, ByteBuf, SubArrayRequest>() {
                @Override
                public SubArrayRequest call(MutationSpec spec, ByteBuf buf) {
                    SubArrayRequest.ArrayOperation op;
                    switch (spec.type()) {
                        case ARRAY_PUSH_FIRST:
                            op = SubArrayRequest.ArrayOperation.PUSH_FIRST;
                            break;
                        case ARRAY_PUSH_LAST:
                        default:
                            op = SubArrayRequest.ArrayOperation.PUSH_LAST;
                            break;
                    }

                    SubArrayRequest request = new SubArrayRequest(docId, spec.path(), op,
                            buf, bucketName, expiry, cas);
                    request.createIntermediaryPath(spec.createParents());
                    return request;
                }
            };
    private static final Func3<ResponseStatus, String, String, Object> ARRAY_EXTEND_EVALUATOR =
            new Func3<ResponseStatus, String, String, Object>() {
                @Override
                public Object call(ResponseStatus status, String docId, String path) {
                    if (status.isSuccess()) {
                        return null;
                    } else {
                        throw SubdocHelper.commonSubdocErrors(status, docId, path);
                    }
                }
            };

    private final Func2<MutationSpec, ByteBuf, SubArrayRequest> ARRAY_INSERT_FACTORY =
            new Func2<MutationSpec, ByteBuf, SubArrayRequest>() {
                @Override
                public SubArrayRequest call(MutationSpec spec, ByteBuf buf) {
                    return new SubArrayRequest(docId, spec.path(),
                            SubArrayRequest.ArrayOperation.INSERT, buf, bucketName, expiry, cas);
                }
            };
    private static final Func3<ResponseStatus, String, String, Object> ARRAY_INSERT_EVALUATOR =
            new Func3<ResponseStatus, String, String, Object>() {
                @Override
                public Object call(ResponseStatus status, String docId, String path) {
                    switch (status) {
                        case SUCCESS:
                            return null;
                        case SUBDOC_PATH_MISMATCH:
                            throw new PathMismatchException("The last component of path " + path
                                    + " in " + docId + " was expected to be an array element");
                        default:
                            throw SubdocHelper.commonSubdocErrors(status, docId, path);
                    }
                }
            };

    private final Func2<MutationSpec, ByteBuf, SubArrayRequest> ARRAY_ADDUNIQUE_FACTORY =
            new Func2<MutationSpec, ByteBuf, SubArrayRequest>() {
                @Override
                public SubArrayRequest call(MutationSpec spec, ByteBuf buf) {
                    SubArrayRequest request = new SubArrayRequest(docId, spec.path(),
                            SubArrayRequest.ArrayOperation.ADD_UNIQUE, buf, bucketName, expiry, cas);
                    request.createIntermediaryPath(spec.createParents());
                    return request;
                }
            };
    private static final Func3<ResponseStatus, String, String, Object> ARRAY_ADDUNIQUE_EVALUATOR =
            new Func3<ResponseStatus, String, String, Object>() {
                @Override
                public Object call(ResponseStatus status, String docId, String path) {
                    switch (status) {
                        case SUCCESS:
                            return null;
                        case SUBDOC_PATH_EXISTS:
                            throw new PathExistsException("The unique value already exist in array " + path
                                    + " in document " + docId);
                        case SUBDOC_VALUE_CANTINSERT:
                            throw new CannotInsertValueException("The unique value provided is not a JSON primitive");
                        case SUBDOC_PATH_MISMATCH:
                            throw new PathMismatchException("The array at " + path
                                    + " contains non-primitive JSON elements in document " + docId);
                        default:
                            throw SubdocHelper.commonSubdocErrors(status, docId, path);
                    }
                }
            };

    private Observable<DocumentFragment<Mutation>> doSingleMutate(final MutationSpec spec,
            final Func2<MutationSpec, ByteBuf, ? extends AbstractSubdocMutationRequest> requestFactory,
            final Func3<ResponseStatus, String, String, Object> responseStatusDocIdAndPathToValueEvaluator) {
        return Observable.defer(new Func0<Observable<SimpleSubdocResponse>>() {
            @Override
            public Observable<SimpleSubdocResponse> call() {
                ByteBuf buf;
                try {
                    buf = subdocumentTranscoder.encodeWithMessage(spec.fragment(),
                            "Couldn't encode subdoc fragment " + docId + "/" + spec.path() +
                            " \"" + spec.fragment() + "\"");
                } catch (TranscodingException e) {
                    return Observable.error(e);
                }

                return core.send(requestFactory.call(spec, buf));
            }
        }).map(new Func1<SimpleSubdocResponse, DocumentFragment<Mutation>>() {
            @Override
            public DocumentFragment<Mutation> call(SimpleSubdocResponse response) {
                //empty response for mutations
                if (response.content() != null && response.content().refCnt() > 0) {
                    response.content().release();
                }

                Object value = responseStatusDocIdAndPathToValueEvaluator.call(response.status(), docId, spec.path());
                SubdocOperationResult<Mutation> singleResult = SubdocOperationResult
                        .createResult(spec.path(), spec.type(), response.status(), value);
                return new DocumentFragment<Mutation>(docId, response.cas(), response.mutationToken(), Collections.singletonList(singleResult));
            }
        });
    }

    private Observable<DocumentFragment<Mutation>> removeIn(final MutationSpec spec) {
        return Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        SubDeleteRequest request = new SubDeleteRequest(docId, spec.path(), bucketName, expiry, cas);
                        return core.send(request);
                    }
                })
                .map(new Func1<SimpleSubdocResponse, DocumentFragment<Mutation>>() {
                    @Override
                    public DocumentFragment<Mutation> call(SimpleSubdocResponse response) {
                        //empty response for mutations
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        if (!response.status().isSuccess()) {
                            throw SubdocHelper.commonSubdocErrors(response.status(), docId, spec.path());
                        }

                        SubdocOperationResult<Mutation> singleResult = SubdocOperationResult
                                .createResult(spec.path(), spec.type(), response.status(), null);
                        return new DocumentFragment<Mutation>(docId, response.cas(), response.mutationToken(), Collections.singletonList(singleResult));
                    }
                });
    }

    private Observable<DocumentFragment<Mutation>> counterIn(final MutationSpec spec) {
        //these are repeated guards, the builder shouldn't allow to produce a non-long or 0-valued delta
        //shortcircuit if fragment is of bad type
        if (spec.fragment() != null && !(spec.fragment() instanceof Number)) {
            return Observable.error(new IllegalArgumentException("Counter fragment must be a long/integer"));
        }
        Number fragment = (Number) spec.fragment();
        // shortcircuit if delta is zero
        if (fragment == null || fragment.longValue() == 0L) {
            return Observable.error(new ZeroDeltaException());
        }

        final long delta = fragment.longValue();

        return Observable.defer(
                new Func0<Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call() {
                        SubCounterRequest request = new SubCounterRequest(docId, spec.path(), delta, bucketName, expiry, cas);
                        request.createIntermediaryPath(spec.createParents());
                        return core.send(request);
                    }
                })
                .map(new Func1<SimpleSubdocResponse, DocumentFragment<Mutation>>() {
                    @Override
                    public DocumentFragment<Mutation> call(SimpleSubdocResponse response) {

                        ResponseStatus status = response.status();
                        Object value;
                        if (status.isSuccess()) {
                            try {
                                value = Long.parseLong(response.content().toString(CharsetUtil.UTF_8));
                                SubdocOperationResult<Mutation> singleResult = SubdocOperationResult
                                        .createResult(spec.path(), spec.type(), status, value);
                                return new DocumentFragment<Mutation>(docId, response.cas(), response.mutationToken(), Collections.singletonList(singleResult));
                            } catch (NumberFormatException e) {
                                throw new TranscodingException("Couldn't parse counter response into a long", e);
                            } finally {
                                if (response.content() != null) {
                                    response.content().release();
                                }
                            }
                        } else {
                            if (response.content() != null) {
                                response.content().release();
                            }
                            throw SubdocHelper.commonSubdocErrors(status, docId, spec.path());
                        }

                    }
                });
    }

    //=============================
    //utility methods for mutations

    private <T> Observable<DocumentFragment<T>> subdocObserveMutation(Observable<DocumentFragment<T>> mutation) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return mutation;
        }

        return mutation.flatMap(new Func1<DocumentFragment<T>, Observable<DocumentFragment<T>>>() {
            @Override
            public Observable<DocumentFragment<T>> call(final DocumentFragment<T> frag) {
                return Observe
                    .call(core, bucketName, frag.id(), frag.cas(), false, frag.mutationToken(), persistTo.value(), replicateTo.value(),
                        environment.observeIntervalDelay(), environment.retryStrategy())
                    .map(new Func1<Boolean, DocumentFragment<T>>() {
                        @Override
                        public DocumentFragment<T> call(Boolean aBoolean) {
                            return frag;
                        }
                    })
                    .onErrorResumeNext(new Func1<Throwable, Observable<DocumentFragment<T>>>() {
                        @Override
                        public Observable<DocumentFragment<T>> call(Throwable throwable) {
                            return Observable.error(new DurabilityException(
                                "Durability requirement failed: " + throwable.getMessage(),
                                throwable));
                        }
                    });
            }
        });
    }

    private static void releaseAll(List<ByteBuf> byteBufs) {
        for (ByteBuf byteBuf : byteBufs) {
            if (byteBuf != null && byteBuf.refCnt() > 0) {
                byteBuf.release();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("mutateIn(").append(docId);
        if (expiry != 0)
            sb.append(", expiry=").append(expiry);

        if (cas != 0L)
            sb.append(", cas=").append(cas);

        if (persistTo != PersistTo.NONE)
            sb.append(", persistTo=").append(persistTo);

        if (replicateTo != ReplicateTo.NONE)
            sb.append(", replicateTo=").append(replicateTo);

        sb.append(")[");
        int pos = sb.length();
        for (MutationSpec mutationSpec : mutationSpecs) {
            sb.append(", ").append(mutationSpec);
        }
        sb.delete(pos, pos+2);
        sb.append(']');
        return sb.toString();
    }
}
