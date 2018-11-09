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
import com.couchbase.client.core.message.kv.subdoc.multi.MutationCommandBuilder;
import com.couchbase.client.core.message.kv.subdoc.multi.SubMultiMutationDocOptionsBuilder;
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
import com.couchbase.client.core.tracing.ThresholdLogReporter;
import com.couchbase.client.core.tracing.ThresholdLogSpan;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.deps.io.netty.util.internal.StringUtil;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.subdoc.BadDeltaException;
import com.couchbase.client.java.error.subdoc.CannotInsertValueException;
import com.couchbase.client.java.error.subdoc.DocumentNotJsonException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.error.subdoc.PathExistsException;
import com.couchbase.client.java.error.subdoc.PathInvalidException;
import com.couchbase.client.java.error.subdoc.PathMismatchException;
import com.couchbase.client.java.error.subdoc.PathNotFoundException;
import com.couchbase.client.java.error.subdoc.SubDocumentException;
import com.couchbase.client.java.error.subdoc.XattrOrderingException;
import com.couchbase.client.java.transcoder.subdoc.FragmentTranscoder;
import io.opentracing.Scope;
import io.opentracing.Span;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.couchbase.client.core.logging.RedactableArgument.user;
import static com.couchbase.client.java.bucket.api.Utils.addRequestSpan;
import static com.couchbase.client.java.bucket.api.Utils.applyTimeout;
import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

/**
 * A builder for subdocument mutations. In order to perform the final set of operations, use the
 * {@link #execute()} method. Operations are performed asynchronously (see {@link MutateInBuilder} for a synchronous
 * version).
 *
 * Instances of this builder should be obtained through {@link AsyncBucket#mutateIn(String)} rather than directly
 * constructed.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Committed
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
    protected boolean upsertDocument;
    protected boolean insertDocument;

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
    public Observable<DocumentFragment<Mutation>> execute() {
        return execute(0, null);
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
     *  - CAS was provided but optimistic locking failed: {@link CASMismatchException}
     *
     * When receiving a {@link MultiMutationException}, one can inspect the exception to find the zero-based index and
     * error {@link ResponseStatus status code} of the first failing {@link Mutation}. Subsequent mutations may have
     * also failed had they been attempted, but a single spec failing causes the whole operation to be cancelled.
     *
     * Other top-level error conditions are similar to those encountered during a document-level {@link AsyncBucket#replace(Document)}.
     *
     * @param timeout the specific timeout to apply for the operation.
     * @param timeUnit the time unit for the timeout.
     * @return an {@link Observable} of a single {@link DocumentFragment} (if successful) containing updated cas metadata.
     * Note that some individual results could also bear a value, like counter operations.
     */
    public Observable<DocumentFragment<Mutation>> execute(long timeout, TimeUnit timeUnit) {
        if (mutationSpecs.isEmpty()) {
            throw new IllegalArgumentException("Execution of a subdoc mutation requires at least one operation");
        } else if (mutationSpecs.size() == 1) { //FIXME implement single path optim
            //single path optimization
            return doSingleMutate(mutationSpecs.get(0), timeout, timeUnit);
        } else {
            return doMultiMutate(timeout, timeUnit);
        }
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document}
     * and watch for durability requirements.
     *
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
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * requirement cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original execute has already happened, so the actual
     * execute and the watching for durability requirements are two separate tasks internally.**
     *
     * @param persistTo the persistence requirement to watch.
     * @param replicateTo the replication requirement to watch.
     * @return an {@link Observable} of a single {@link DocumentFragment} (if successful) containing updated cas metadata.
     * Note that some individual results could also bear a value, like counter operations.
     */
    public Observable<DocumentFragment<Mutation>> execute(final PersistTo persistTo, final ReplicateTo replicateTo) {
        return execute(persistTo, replicateTo, 0, null);
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document}
     * and watch for durability requirements.
     *
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
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * requirement cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original execute has already happened, so the actual
     * execute and the watching for durability requirements are two separate tasks internally.**
     *
     * @param persistTo the persistence requirement to watch.
     * @param replicateTo the replication requirement to watch.
     * @param timeout the specific timeout to apply for the operation.
     * @param timeUnit the time unit for the timeout.
     * @return an {@link Observable} of a single {@link DocumentFragment} (if successful) containing updated cas metadata.
     * Note that some individual results could also bear a value, like counter operations.
     */
    public Observable<DocumentFragment<Mutation>> execute(final PersistTo persistTo, final ReplicateTo replicateTo, final long timeout, final TimeUnit timeUnit) {
        Observable<DocumentFragment<Mutation>> mutationResult = execute(timeout, timeUnit);

        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return mutationResult;
        }

        return mutationResult.flatMap(new Func1<DocumentFragment<Mutation>, Observable<DocumentFragment<Mutation>>>() {
            @Override
            public Observable<DocumentFragment<Mutation>> call(final DocumentFragment<Mutation> fragment) {
                Observable<DocumentFragment<Mutation>> result = Observe.call(core, bucketName, docId, fragment.cas(), false, fragment.mutationToken(),
                    persistTo.value(), replicateTo.value(),
                    environment.observeIntervalDelay(), environment.retryStrategy())
                    .map(new Func1<Boolean, DocumentFragment<Mutation>>() {
                        @Override
                        public DocumentFragment<Mutation> call(Boolean aBoolean) {
                            return fragment;
                        }
                    }).onErrorResumeNext(new Func1<Throwable, Observable<DocumentFragment<Mutation>>>() {
                        @Override
                        public Observable<DocumentFragment<Mutation>> call(Throwable throwable) {
                            return Observable.error(new DurabilityException(
                                "Durability requirement failed: " + throwable.getMessage(),
                                throwable));
                        }
                    });
                if (timeout > 0) {
                    result = result.timeout(timeout, timeUnit, environment.scheduler());
                }
                return result;
            }
        });
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document}
     * and watch for durability requirements.
     *
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
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * requirement cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original execute has already happened, so the actual
     * execute and the watching for durability requirements are two separate tasks internally.**
     *
     * @param persistTo the persistence requirement to watch.
     * @return an {@link Observable} of a single {@link DocumentFragment} (if successful) containing updated cas metadata.
     * Note that some individual results could also bear a value, like counter operations.
     */
    public Observable<DocumentFragment<Mutation>> execute(PersistTo persistTo) {
        return execute(persistTo, ReplicateTo.NONE, 0, null);
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document}
     * and watch for durability requirements.
     *
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
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * requirement cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original execute has already happened, so the actual
     * execute and the watching for durability requirements are two separate tasks internally.**
     *
     * @param persistTo the persistence requirement to watch.
     * @param timeout the specific timeout to apply for the operation.
     * @param timeUnit the time unit for the timeout.
     * @return an {@link Observable} of a single {@link DocumentFragment} (if successful) containing updated cas metadata.
     * Note that some individual results could also bear a value, like counter operations.
     */
    public Observable<DocumentFragment<Mutation>> execute(PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return execute(persistTo, ReplicateTo.NONE, timeout, timeUnit);
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document}
     * and watch for durability requirements.
     *
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
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * requirement cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original execute has already happened, so the actual
     * execute and the watching for durability requirements are two separate tasks internally.**
     *
     * @param replicateTo the replication requirement to watch.
     * @return an {@link Observable} of a single {@link DocumentFragment} (if successful) containing updated cas metadata.
     * Note that some individual results could also bear a value, like counter operations.
     */
    public Observable<DocumentFragment<Mutation>> execute(ReplicateTo replicateTo) {
        return execute(PersistTo.NONE, replicateTo, 0, null);
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document}
     * and watch for durability requirements.
     *
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
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * requirement cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original execute has already happened, so the actual
     * execute and the watching for durability requirements are two separate tasks internally.**
     *
     * @param replicateTo the replication requirement to watch.
     * @param timeout the specific timeout to apply for the operation.
     * @param timeUnit the time unit for the timeout.
     * @return an {@link Observable} of a single {@link DocumentFragment} (if successful) containing updated cas metadata.
     * Note that some individual results could also bear a value, like counter operations.
     */
    public Observable<DocumentFragment<Mutation>> execute(ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return execute(PersistTo.NONE, replicateTo, timeout, timeUnit);
    }


    //==== DOCUMENT level modifiers ====
    /**
     * Change the expiry of the enclosing document as part of the mutation.
     *
     * Note that if the document had an expiration time set, it needs to be set again
     * at this operation otherwise it will be reset to 0 (this is similar to a regular
     * mutation where a new expiration must be set as well).
     *
     * @param expiry the new expiry to apply (not setting it will disable expiration
     *               for the full document)
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

    /**
     * Set createDocument to true, if the document has to be created
     *
     * @param createDocument true to create document.
     * @deprecated Use {@link #upsertDocument(boolean)} instead.
     */
    @Deprecated
    public AsyncMutateInBuilder createDocument(boolean createDocument) {
        return upsertDocument(createDocument);
    }

    /**
     * Set upsertDocument to true, if the document has to be created
     *
     * @param upsertDocument true to create document.
     */
    public AsyncMutateInBuilder upsertDocument(boolean upsertDocument) {
        if (this.insertDocument && upsertDocument) {
            throw new IllegalArgumentException("Cannot set both upsertDocument and insertDocument to true");
        }
        this.upsertDocument = upsertDocument;
        return this;
    }

    /**
     * Set insertDocument to true, if the document has to be created only if it does not exist
     *
     * @param insertDocument true to insert document.
     */
    @InterfaceStability.Committed
    public AsyncMutateInBuilder insertDocument(boolean insertDocument) {
        if (this.upsertDocument && insertDocument) {
            throw new IllegalArgumentException("Cannot set both upsertDocument and insertDocument to true");
        }
        this.insertDocument = insertDocument;
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
        this.mutationSpecs.add(new MutationSpec(Mutation.REPLACE, path, fragment));
        return this;
    }

    /**
     * Replace an existing value by the given fragment.
     *
     * @param path the path where the value to replace is.
     * @param fragment the new value.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder replace(String path, T fragment, SubdocOptionsBuilder optionsBuilder) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for replace");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.REPLACE, path, fragment, optionsBuilder));
        return this;
    }

    /**
     * Insert a fragment, provided the last element of the path doesn't exist.
     *
     * @param path the path where to insert a new dictionary value.
     * @param fragment the new dictionary value to insert.
     * @param createPath true to create missing intermediary nodes.
     * @deprecated Use {@link #insert(String, Object, SubdocOptionsBuilder)} instead.
     */
    @Deprecated
    public <T> AsyncMutateInBuilder insert(String path, T fragment, boolean createPath) {
        return insert(path, fragment, new SubdocOptionsBuilder().createPath(createPath));
    }

    /**
     * Insert a fragment, provided the last element of the path doesn't exist.
     *
     * @param path the path where to insert a new dictionary value.
     * @param fragment the new dictionary value to insert.
     */
    public <T> AsyncMutateInBuilder insert(String path, T fragment) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for insert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.DICT_ADD, path, fragment));
        return this;
    }

    /**
     * Insert a fragment, provided the last element of the path doesn't exist.
     *
     * @param path the path where to insert a new dictionary value.
     * @param fragment the new dictionary value to insert.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder insert(String path, T fragment, SubdocOptionsBuilder optionsBuilder) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for insert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.DICT_ADD, path, fragment, optionsBuilder));
        return this;
    }

    /**
     * Insert a fragment, replacing the old value if the path exists.
     *
     * @param path the path where to insert (or replace) a dictionary value.
     * @param fragment the new dictionary value to be applied.
     * @param createPath true to create missing intermediary nodes.
     * @deprecated Use {@link #upsert(String, Object, SubdocOptionsBuilder)} instead.
     */
    @Deprecated
    public <T> AsyncMutateInBuilder upsert(String path, T fragment, boolean createPath) {
        return upsert(path, fragment, new SubdocOptionsBuilder().createPath(createPath));
    }

    /**
     * Insert a fragment, replacing the old value if the path exists.
     *
     * @param path the path where to insert (or replace) a dictionary value.
     * @param fragment the new dictionary value to be applied.
     */
    public <T> AsyncMutateInBuilder upsert(String path, T fragment) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for upsert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.DICT_UPSERT, path, fragment));
        return this;
    }

    /**
     * Upsert a full JSON document.
     *
     * @param content full content of the JSON document
     */
    @InterfaceStability.Committed
    public AsyncMutateInBuilder upsert(JsonObject content) {
        this.mutationSpecs.add(new MutationSpec(Mutation.UPSERTDOC, "", content));
        return this;
    }

    /**
     * Insert a fragment, replacing the old value if the path exists.
     *
     * @param path the path where to insert (or replace) a dictionary value.
     * @param fragment the new dictionary value to be applied.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder upsert(String path, T fragment, SubdocOptionsBuilder optionsBuilder) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for upsert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.DICT_UPSERT, path, fragment, optionsBuilder));
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
        this.mutationSpecs.add(new MutationSpec(Mutation.DELETE, path, null));
        return this;
    }

    /**
     * Remove an entry in a JSON document (scalar, array element, dictionary entry,
     * whole array or dictionary, depending on the path).
     *
     * @param path the path to remove.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder remove(String path, SubdocOptionsBuilder optionsBuilder) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for remove");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.DELETE, path, null, optionsBuilder));
        return this;
    }

    /**
     * Increment/decrement a numerical fragment in a JSON document.
     * If the value (last element of the path) doesn't exist the counter is created and takes the value of the delta.
     *
     * @param path the path to the counter (must be containing a number).
     * @param delta the value to increment or decrement the counter by.
     * @param createPath true to create missing intermediary nodes.
     * @deprecated Use {@link #counter(String, long, SubdocOptionsBuilder)} instead.
     */
    @Deprecated
    public AsyncMutateInBuilder counter(String path, long delta, boolean createPath) {
        return counter(path, delta, new SubdocOptionsBuilder().createPath(createPath));
    }

    /**
     * Increment/decrement a numerical fragment in a JSON document.
     * If the value (last element of the path) doesn't exist the counter is created and takes the value of the delta.
     *
     * @param path the path to the counter (must be containing a number).
     * @param delta the value to increment or decrement the counter by.
     */
    public AsyncMutateInBuilder counter(String path, long delta) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for counter");
        }
        // shortcircuit if delta is zero
        if (delta == 0L) {
            throw new BadDeltaException("Delta must not be 0");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.COUNTER, path, delta));
        return this;
    }

    /**
     * Increment/decrement a numerical fragment in a JSON document.
     * If the value (last element of the path) doesn't exist the counter is created and takes the value of the delta.
     *
     * @param path the path to the counter (must be containing a number).
     * @param delta the value to increment or decrement the counter by.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public AsyncMutateInBuilder counter(String path, long delta, SubdocOptionsBuilder optionsBuilder) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for counter");
        }
        // shortcircuit if delta is zero
        if (delta == 0L) {
            throw new BadDeltaException("Delta must not be 0");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.COUNTER, path, delta, optionsBuilder));
        return this;
    }

    /**
     * Prepend to an existing array, pushing the value to the front/first position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the front of the array.
     * @param createPath true to create missing intermediary nodes.
     * @deprecated Use {@link #arrayPrepend(String, Object, SubdocOptionsBuilder)} instead.
     */
    @Deprecated
    public <T> AsyncMutateInBuilder arrayPrepend(String path, T value, boolean createPath) {
        return arrayPrepend(path, value, new SubdocOptionsBuilder().createPath(createPath));
    }

    /**
     * Prepend to an existing array, pushing the value to the front/first position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the front of the array.
     */
    public <T> AsyncMutateInBuilder arrayPrepend(String path, T value) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_FIRST, path, value));
        return this;
    }

    /**
     * Prepend to an existing array, pushing the value to the front/first position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the front of the array.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder arrayPrepend(String path, T value, SubdocOptionsBuilder optionsBuilder) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_FIRST, path, value, optionsBuilder));
        return this;
    }

    /**
     * Prepend multiple values at once in an existing array, pushing all values in the collection's iteration order to
     * the front/start of the array.
     *
     * First value becomes the first element of the array, second value the second, etc... All existing values
     * are shifted right in the array, by the number of inserted elements.
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayPrepend(String, Object, boolean)} (String, Object)} by grouping
     * mutations in a single packet.
     *
     * For example given an array [ A, B, C ], prepending the values X and Y yields [ X, Y, A, B, C ]
     * and not [ [ X, Y ], A, B, C ].
     *
     * @param path the path of the array.
     * @param values the collection of values to insert at the front of the array as individual elements.
     * @param createPath true to create missing intermediary nodes.
     * @param <T> the type of data in the collection (must be JSON serializable).
     * @deprecated Use {@link #arrayPrependAll(String, Collection, SubdocOptionsBuilder)} instead.
     */
    @Deprecated
    public <T> AsyncMutateInBuilder arrayPrependAll(String path, Collection<T> values, boolean createPath) {
        return arrayPrependAll(path, values, new SubdocOptionsBuilder().createPath(createPath));
    }

    /**
     * Prepend multiple values at once in an existing array, pushing all values in the collection's iteration order to
     * the front/start of the array.
     *
     * First value becomes the first element of the array, second value the second, etc... All existing values
     * are shifted right in the array, by the number of inserted elements.
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayPrepend(String, Object, boolean)} (String, Object)} by grouping
     * mutations in a single packet.
     *
     * For example given an array [ A, B, C ], prepending the values X and Y yields [ X, Y, A, B, C ]
     * and not [ [ X, Y ], A, B, C ].
     *
     * @param path the path of the array.
     * @param values the collection of values to insert at the front of the array as individual elements.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @param <T> the type of data in the collection (must be JSON serializable).
     */
    public <T> AsyncMutateInBuilder arrayPrependAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_FIRST, path, new MultiValue<T>(values), optionsBuilder));
        return this;
    }

    /**
     * Prepend multiple values at once in an existing array, pushing all values to the front/start of the array.
     * This is provided as a convenience alternative to {@link #arrayPrependAll(String, Collection, boolean)}. Note
     * that parent nodes are not created when using this method (ie. createPath = false).
     *
     * First value becomes the first element of the array, second value the second, etc... All existing values
     * are shifted right in the array, by the number of inserted elements.
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayPrepend(String, Object, boolean)} (String, Object)} by grouping
     * mutations in a single packet.
     *
     * For example given an array [ A, B, C ], prepending the values X and Y yields [ X, Y, A, B, C ]
     * and not [ [ X, Y ], A, B, C ].
     *
     * @param path the path of the array.
     * @param values the values to insert at the front of the array as individual elements.
     * @param <T> the type of data in the collection (must be JSON serializable).
     * @see #arrayPrependAll(String, Collection, boolean) if you need to create missing intermediary nodes.
     */
    public <T> AsyncMutateInBuilder arrayPrependAll(String path, T... values) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_FIRST, path, new MultiValue<T>(values)));
        return this;
    }

    /**
     * Append to an existing array, pushing the value to the back/last position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the back of the array.
     * @param createPath true to create missing intermediary nodes.
     * @deprecated Use {@link #arrayAppend(String, Object, SubdocOptionsBuilder)} instead.
     */
    @Deprecated
    public <T> AsyncMutateInBuilder arrayAppend(String path, T value, boolean createPath) {
        return arrayAppend(path, value, new SubdocOptionsBuilder().createPath(createPath));
    }

    /**
     * Append to an existing array, pushing the value to the back/last position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the back of the array.
     */
    public <T> AsyncMutateInBuilder arrayAppend(String path, T value) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_LAST, path, value));
        return this;
    }

    /**
     * Append to an existing array, pushing the value to the back/last position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the back of the array.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder arrayAppend(String path, T value, SubdocOptionsBuilder optionsBuilder) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_LAST, path, value, optionsBuilder));
        return this;
    }

    /**
     * Append multiple values at once in an existing array, pushing all values in the collection's iteration order to
     * the back/end of the array.
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayAppend(String, Object, boolean)} by grouping mutations in
     * a single packet.
     *
     * For example given an array [ A, B, C ], appending the values X and Y yields [ A, B, C, X, Y ]
     * and not [ A, B, C, [ X, Y ] ].
     *
     * @param path the path of the array.
     * @param values the collection of values to individually insert at the back of the array.
     * @param createPath true to create missing intermediary nodes.
     * @param <T> the type of data in the collection (must be JSON serializable).
     * @deprecated Use {@link #arrayAppendAll(String, Collection, SubdocOptionsBuilder)} instead.
     */
    @Deprecated
    public <T> AsyncMutateInBuilder arrayAppendAll(String path, Collection<T> values, boolean createPath) {
        return arrayAppendAll(path, values, new SubdocOptionsBuilder().createPath(createPath));
    }


    /**
     * Append multiple values at once in an existing array, pushing all values in the collection's iteration order to
     * the back/end of the array.
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayAppend(String, Object, boolean)} by grouping mutations in
     * a single packet.
     *
     * For example given an array [ A, B, C ], appending the values X and Y yields [ A, B, C, X, Y ]
     * and not [ A, B, C, [ X, Y ] ].
     *
     * @param path the path of the array.
     * @param values the collection of values to individually insert at the back of the array.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @param <T> the type of data in the collection (must be JSON serializable).
     */
    public <T> AsyncMutateInBuilder arrayAppendAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_LAST, path, new MultiValue<T>(values), optionsBuilder));
        return this;
    }

    /**
     * Append multiple values at once in an existing array, pushing all values to the back/end of the array.
     * This is provided as a convenience alternative to {@link #arrayAppendAll(String, Collection, boolean)}.
     * Note that parent nodes are not created when using this method (ie. createPath = false).
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayAppend(String, Object, boolean)} by grouping mutations in
     * a single packet.
     *
     * For example given an array [ A, B, C ], appending the values X and Y yields [ A, B, C, X, Y ]
     * and not [ A, B, C, [ X, Y ] ].
     *
     * @param path the path of the array.
     * @param values the values to individually insert at the back of the array.
     * @param <T> the type of data in the collection (must be JSON serializable).
     * @see #arrayAppendAll(String, Collection, boolean) if you need to create missing intermediary nodes.
     */
    public <T> AsyncMutateInBuilder arrayAppendAll(String path, T... values) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_PUSH_LAST, path, new MultiValue<T>(values)));
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
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_INSERT, path, value));
        return this;
    }

    /**
     * Insert into an existing array at a specific position
     * (denoted in the path, eg. "sub.array[2]").
     *
     * @param path the path (including array position) where to insert the value.
     * @param value the value to insert in the array.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder arrayInsert(String path, T value, SubdocOptionsBuilder optionsBuilder) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for arrayInsert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_INSERT, path, value, optionsBuilder));
        return this;
    }

    /**
     * Insert multiple values at once in an existing array at a specified position (denoted in the
     * path, eg. "sub.array[2]"), inserting all values in the collection's iteration order at the given
     * position and shifting existing values beyond the position by the number of elements in the collection.
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayInsert(String, Object)} by grouping mutations in a single packet.
     *
     * For example given an array [ A, B, C ], inserting the values X and Y at position 1 yields [ A, B, X, Y, C ]
     * and not [ A, B, [ X, Y ], C ].
     *
     * @param path the path of the array.
     * @param values the values to insert at the specified position of the array, each value becoming an entry at or
     *               after the insert position.
     * @param <T> the type of data in the collection (must be JSON serializable).
     */
    public <T> AsyncMutateInBuilder arrayInsertAll(String path, Collection<T> values) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for arrayInsert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_INSERT, path, new MultiValue<T>(values)));
        return this;
    }

    /**
     * Insert multiple values at once in an existing array at a specified position (denoted in the
     * path, eg. "sub.array[2]"), inserting all values in the collection's iteration order at the given
     * position and shifting existing values beyond the position by the number of elements in the collection.
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayInsert(String, Object)} by grouping mutations in a single packet.
     *
     * For example given an array [ A, B, C ], inserting the values X and Y at position 1 yields [ A, B, X, Y, C ]
     * and not [ A, B, [ X, Y ], C ].
     *
     * @param path the path of the array.
     * @param values the values to insert at the specified position of the array, each value becoming an entry at or
     *               after the insert position.
     * @param <T> the type of data in the collection (must be JSON serializable).
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder arrayInsertAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for arrayInsert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_INSERT, path, new MultiValue<T>(values), optionsBuilder));
        return this;
    }

    /**
     * Insert multiple values at once in an existing array at a specified position (denoted in the
     * path, eg. "sub.array[2]"), inserting all values at the given position and shifting existing values
     * beyond the position by the number of elements in the collection. This is provided as a convenience
     * alternative to {@link #arrayInsertAll(String, Collection)}. Note that parent nodes are not created
     * when using this method (ie. createPath = false).
     *
     * Each item in the collection is inserted as an individual element of the array, but a bit of overhead
     * is saved compared to individual {@link #arrayInsert(String, Object)} by grouping mutations in a single packet.
     *
     * For example given an array [ A, B, C ], inserting the values X and Y at position 1 yields [ A, B, X, Y, C ]
     * and not [ A, B, [ X, Y ], C ].
     *
     * @param path the path of the array.
     * @param values the values to insert at the specified position of the array, each value becoming an entry at or
     *               after the insert position.
     * @param <T> the type of data in the collection (must be JSON serializable).
     * @see #arrayPrependAll(String, Collection, boolean) if you need to create missing intermediary nodes.
     */
    public <T> AsyncMutateInBuilder arrayInsertAll(String path, T... values) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Path must not be empty for arrayInsert");
        }
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_INSERT, path, new MultiValue<T>(values), false));
        return this;
    }

    /**
     * Insert a value in an existing array only if the value
     * isn't already contained in the array (by way of string comparison).
     *
     * @param path the path to mutate in the JSON.
     * @param value the value to insert.
     * @param createPath true to create missing intermediary nodes.
     * @deprecated Use {@link #arrayAddUnique(String, Object, SubdocOptionsBuilder)} instead.
     */
    @Deprecated
    public <T> AsyncMutateInBuilder arrayAddUnique(String path, T value, boolean createPath) {
        return arrayAddUnique(path, value, new SubdocOptionsBuilder().createPath(createPath));
    }

    /**
     * Insert a value in an existing array only if the value
     * isn't already contained in the array (by way of string comparison).
     *
     * @param path the path to mutate in the JSON.
     * @param value the value to insert.
     */
    public <T> AsyncMutateInBuilder arrayAddUnique(String path, T value) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_ADD_UNIQUE, path, value));
        return this;
    }


    /**
     * Insert a value in an existing array only if the value
     * isn't already contained in the array (by way of string comparison).
     *
     * @param path the path to mutate in the JSON.
     * @param value the value to insert.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> AsyncMutateInBuilder arrayAddUnique(String path, T value, SubdocOptionsBuilder optionsBuilder) {
        this.mutationSpecs.add(new MutationSpec(Mutation.ARRAY_ADD_UNIQUE, path, value, optionsBuilder));
        return this;
    }


    //==============================
    //multi operation implementation
    protected Observable<DocumentFragment<Mutation>> doMultiMutate(final long timeout, final TimeUnit timeUnit) {
        if (mutationSpecs.isEmpty()) {
            throw new IllegalArgumentException("At least one Mutation Spec is necessary for mutateIn");
        }
        boolean seenNonXattr = false;
        for (MutationSpec spec : mutationSpecs) {
            if (spec.xattr() && seenNonXattr) {
                throw new XattrOrderingException("Xattr-based commands must always come first in the builder!");
            } else if (!spec.xattr()) {
                seenNonXattr = true;
            }
        }

        Observable<DocumentFragment<Mutation>> mutations = Observable.defer(new Func0<Observable<DocumentFragment<Mutation>>>() {
            @Override
            public Observable<DocumentFragment<Mutation>> call() {
                List<ByteBuf> bufList = new ArrayList<ByteBuf>(mutationSpecs.size());
                final List<MutationCommand> commands = new ArrayList<MutationCommand>(mutationSpecs.size());

                for (int i = 0; i < mutationSpecs.size(); i++) {
                    MutationSpec spec = mutationSpecs.get(i);
                    if (spec.type() == Mutation.DELETE) {
                        commands.add(new MutationCommandBuilder(Mutation.DELETE, spec.path())
                                .xattr(spec.xattr()).build());
                    } else {
                        try {
                            ByteBuf buf = subdocumentTranscoder.encodeWithMessage(spec.fragment(), "Couldn't encode MutationSpec #" +
                                    i + " (" + spec.type() + " on " + spec.path() + ") in " + docId);
                            bufList.add(buf);
                            commands.add(new MutationCommandBuilder(spec.type(), spec.path(), buf)
                                    .createIntermediaryPath(spec.createPath())
                                    .xattr(spec.xattr())
                                    .expandMacros(spec.expandMacros()).build());
                        } catch (TranscodingException e) {
                            releaseAll(bufList);
                            return Observable.error(e);
                        }
                    }
                }

                final SubMultiMutationRequest req = new SubMultiMutationRequest(
                    docId,
                    bucketName,
                    expiry,
                    cas,
                    SubMultiMutationDocOptionsBuilder.builder()
                        .upsertDocument(upsertDocument)
                        .insertDocument(insertDocument),
                    commands
                );
                addRequestSpan(environment, req, "subdoc_multi_mutate");
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<MultiMutationResponse>>() {
                        @Override
                        public Observable<MultiMutationResponse> call(Subscriber subscriber) {
                            req.subscriber(subscriber);
                            return core.send(req);
                        }
                    })
                    .flatMap(new Func1<MultiMutationResponse, Observable<DocumentFragment<Mutation>>>() {
                        @Override
                        public Observable<DocumentFragment<Mutation>> call(MultiMutationResponse response) {
                            if (response.content() != null && response.content().refCnt() > 0) {
                                response.content().release();
                            }

                            if (environment.operationTracingEnabled()) {
                                environment.tracer().scopeManager()
                                    .activate(req.span(), true)
                                    .close();
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
                                        LOGGER.error(
                                            "Couldn't decode multi-mutation {} for {}/{}",
                                            user(result),
                                            user(docId),
                                            user(result.path()),
                                            e
                                        );
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

                                    return Observable
                                        .error(new MultiMutationException(index, errorStatus, mutationSpecs, errorException));
                                default:
                                    return Observable.error(SubdocHelper.commonSubdocErrors(response.status(), docId, "MULTI-MUTATION"));
                            }
                        }
                    }), req, environment, timeout, timeUnit);
            }
        });

        return subdocObserveMutation(mutations, timeout, timeUnit);
    }

    //================================
    //Single operation implementations
    protected Observable<DocumentFragment<Mutation>> doSingleMutate(MutationSpec spec, long timeout, TimeUnit timeUnit) {
        Observable<DocumentFragment<Mutation>> mutation;
        switch (spec.type()) {
            case DICT_UPSERT:
                mutation = doSingleMutate(spec, DICT_UPSERT_FACTORY, DICT_UPSERT_EVALUATOR, timeout, timeUnit);
                break;
            case DICT_ADD:
                mutation = doSingleMutate(spec, DICT_ADD_FACTORY, DICT_ADD_EVALUATOR, timeout, timeUnit);
                break;
            case REPLACE:
                mutation = doSingleMutate(spec, REPLACE_FACTORY, REPLACE_EVALUATOR, timeout, timeUnit);
                break;
            case ARRAY_PUSH_FIRST:
            case ARRAY_PUSH_LAST:
                mutation = doSingleMutate(spec, ARRAY_EXTEND_FACTORY, ARRAY_EXTEND_EVALUATOR, timeout, timeUnit);
                break;
            case ARRAY_INSERT:
                mutation = doSingleMutate(spec, ARRAY_INSERT_FACTORY, ARRAY_INSERT_EVALUATOR, timeout, timeUnit);
                break;
            case ARRAY_ADD_UNIQUE:
                mutation = doSingleMutate(spec, ARRAY_ADDUNIQUE_FACTORY, ARRAY_ADDUNIQUE_EVALUATOR, timeout, timeUnit);
                break;
            case COUNTER:
                mutation = counterIn(spec, timeout, timeUnit);
                break;
            case DELETE:
                mutation = removeIn(spec, timeout, timeUnit);
                break;
            default:
                mutation = Observable.error(new UnsupportedOperationException());
                break;
        }
        return subdocObserveMutation(mutation, timeout, timeUnit);
    }


    private final Func2<MutationSpec, ByteBuf, SubDictUpsertRequest> DICT_UPSERT_FACTORY =
            new Func2<MutationSpec, ByteBuf, SubDictUpsertRequest>() {
                @Override
                public SubDictUpsertRequest call(MutationSpec spec, ByteBuf buf) {
                    SubDictUpsertRequest request = new SubDictUpsertRequest(docId, spec.path(), buf, bucketName, expiry, cas);
                    request.createIntermediaryPath(spec.createPath());
                    request.xattr(spec.xattr());
                    request.expandMacros(spec.expandMacros());
                    request.upsertDocument(upsertDocument);
                    request.insertDocument(insertDocument);
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
                    request.createIntermediaryPath(spec.createPath());
                    request.xattr(spec.xattr());
                    request.expandMacros(spec.expandMacros());
                    request.upsertDocument(upsertDocument);
                    request.insertDocument(insertDocument);
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
                    request.createIntermediaryPath(spec.createPath());
                    request.xattr(spec.xattr());
                    request.expandMacros(spec.expandMacros());
                    request.upsertDocument(upsertDocument);
                    request.insertDocument(insertDocument);
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
                    request.createIntermediaryPath(spec.createPath());
                    request.xattr(spec.xattr());
                    request.upsertDocument(upsertDocument);
                    request.insertDocument(insertDocument);
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
                    SubArrayRequest request = new SubArrayRequest(docId, spec.path(),
                            SubArrayRequest.ArrayOperation.INSERT, buf, bucketName, expiry, cas);
                    request.createIntermediaryPath(spec.createPath());
                    request.xattr(spec.xattr());
                    return request;
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
                    request.createIntermediaryPath(spec.createPath());
                    request.xattr(spec.xattr());
                    request.upsertDocument(upsertDocument);
                    request.insertDocument(insertDocument);
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
        final Func3<ResponseStatus, String, String, Object> responseStatusDocIdAndPathToValueEvaluator,
        final long timeout, final TimeUnit timeUnit) {
        return Observable.defer(new Func0<Observable<DocumentFragment<Mutation>>>() {
            @Override
            public Observable<DocumentFragment<Mutation>> call() {
                Span requestSpan = null;
                if (environment.operationTracingEnabled()) {
                    Scope scope = environment.tracer()
                        .buildSpan("subdoc_mutate")
                        .startActive(false);
                    requestSpan = scope.span();
                    scope.close();
                }


                Scope encodeScope = null;
                if (requestSpan != null) {
                    encodeScope = environment.tracer()
                        .buildSpan("request_encoding")
                        .asChildOf(requestSpan)
                        .startActive(true);
                }

                ByteBuf buf;
                try {
                    buf = subdocumentTranscoder.encodeWithMessage(spec.fragment(),
                        "Couldn't encode subdoc fragment " + docId + "/" + spec.path() +
                            " \"" + spec.fragment() + "\"");
                } catch (TranscodingException e) {
                    return Observable.error(e);
                }

                if (encodeScope != null) {
                    encodeScope.close();
                    if (encodeScope.span() instanceof ThresholdLogSpan) {
                        encodeScope.span().setBaggageItem(ThresholdLogReporter.KEY_ENCODE_MICROS,
                            Long.toString(((ThresholdLogSpan) encodeScope.span()).durationMicros())
                        );
                    }
                }

                final AbstractSubdocMutationRequest request = requestFactory.call(spec, buf);
                if (requestSpan != null) {
                    request.span(requestSpan, environment);
                }
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                }).map(new Func1<SimpleSubdocResponse, DocumentFragment<Mutation>>() {
                    @Override
                    public DocumentFragment<Mutation> call(SimpleSubdocResponse response) {
                        //empty response for mutations
                        if (response.content() != null && response.content().refCnt() > 0) {
                            response.content().release();
                        }

                        ResponseStatus responseStatus = response.status();
                        try {
                            Object value = responseStatusDocIdAndPathToValueEvaluator.call(responseStatus, docId, spec.path());
                            SubdocOperationResult<Mutation> singleResult = SubdocOperationResult
                                .createResult(spec.path(), spec.type(), response.status(), value);
                            return new DocumentFragment<Mutation>(docId, response.cas(), response.mutationToken(), Collections.singletonList(singleResult));
                        } catch (SubDocumentException e) {
                            if (SubdocHelper.isSubdocLevelError(responseStatus)) {
                                throw new MultiMutationException(0, responseStatus, Collections.singletonList(spec), e);
                            } else {
                                throw e;
                            }
                        }
                    }
                }), request, environment, timeout, timeUnit);
            }
        });
    }

    private Observable<DocumentFragment<Mutation>> removeIn(final MutationSpec spec, final long timeout, final TimeUnit timeUnit) {
        return Observable.defer(new Func0<Observable<DocumentFragment<Mutation>>>() {
            @Override
            public Observable<DocumentFragment<Mutation>> call() {
                final SubDeleteRequest request = new SubDeleteRequest(docId, spec.path(), bucketName, expiry, cas);
                request.xattr(spec.xattr());
                addRequestSpan(environment, request, "subdoc_remove");
                return applyTimeout(deferAndWatch(
                    new Func1<Subscriber, Observable<SimpleSubdocResponse>>() {
                        @Override
                        public Observable<SimpleSubdocResponse> call(Subscriber s) {
                            request.subscriber(s);
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

                            if (environment.operationTracingEnabled()) {
                                environment.tracer().scopeManager()
                                    .activate(response.request().span(), true)
                                    .close();
                            }

                            ResponseStatus responseStatus = response.status();
                            if (!responseStatus.isSuccess()) {
                                //propagate subdocument level error inside a MultiMutationException
                                CouchbaseException subdocError = SubdocHelper.commonSubdocErrors(response.status(), docId, spec.path());
                                if (SubdocHelper.isSubdocLevelError(responseStatus)) {
                                    throw new MultiMutationException(0, responseStatus, Collections.singletonList(spec), subdocError);
                                } else {
                                    throw subdocError;
                                }
                            }

                            SubdocOperationResult<Mutation> singleResult = SubdocOperationResult
                                .createResult(spec.path(), spec.type(), response.status(), null);
                            return new DocumentFragment<Mutation>(docId, response.cas(), response.mutationToken(), Collections.singletonList(singleResult));
                        }
                    }), request, environment, timeout, timeUnit);
            }
        });
    }

    private Observable<DocumentFragment<Mutation>> counterIn(final MutationSpec spec, final long timeout, final TimeUnit timeUnit) {
        //these are repeated guards, the builder shouldn't allow to produce a non-long or 0-valued delta
        //shortcircuit if fragment is of bad type
        if (spec.fragment() != null && !(spec.fragment() instanceof Number)) {
            return Observable.error(new IllegalArgumentException("Counter fragment must be a long/integer"));
        }
        Number fragment = (Number) spec.fragment();
        // shortcircuit if delta is zero
        if (fragment == null || fragment.longValue() == 0L) {
            return Observable.error(new BadDeltaException());
        }

        final long delta = fragment.longValue();

        return Observable.defer(new Func0<Observable<DocumentFragment<Mutation>>>() {
            @Override
            public Observable<DocumentFragment<Mutation>> call() {
                final SubCounterRequest request = new SubCounterRequest(docId, spec.path(), delta, bucketName, expiry, cas);
                request.createIntermediaryPath(spec.createPath());
                request.xattr(spec.xattr());
                request.upsertDocument(upsertDocument);
                request.insertDocument(insertDocument);
                addRequestSpan(environment, request, "subdoc_counter");
                return applyTimeout(deferAndWatch(new Func1<Subscriber, Observable<SimpleSubdocResponse>>() {
                    @Override
                    public Observable<SimpleSubdocResponse> call(Subscriber s) {
                        request.subscriber(s);
                        return core.send(request);
                    }
                })
                .map(new Func1<SimpleSubdocResponse, DocumentFragment<Mutation>>() {
                    @Override
                    public DocumentFragment<Mutation> call(SimpleSubdocResponse response) {
                        try {
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

                                //propagate errors that are subdocument level in a MultiMutationException
                                CouchbaseException subdocError = SubdocHelper.commonSubdocErrors(response.status(), docId, spec.path());
                                if (SubdocHelper.isSubdocLevelError(status)) {
                                    throw new MultiMutationException(0, status, Collections.singletonList(spec), subdocError);
                                } else {
                                    throw subdocError;
                                }
                            }
                        } finally {
                            if (environment.operationTracingEnabled()) {
                                environment.tracer().scopeManager()
                                    .activate(response.request().span(), true)
                                    .close();
                            }
                        }
                    }
                }), request, environment, timeout, timeUnit);
            }
        });
    }

    //=============================
    //utility methods for mutations

    private <T> Observable<DocumentFragment<T>> subdocObserveMutation(Observable<DocumentFragment<T>> mutation, final long timeout, final TimeUnit timeUnit) {
        if (persistTo == PersistTo.NONE && replicateTo == ReplicateTo.NONE) {
            return mutation;
        }

        return mutation.flatMap(new Func1<DocumentFragment<T>, Observable<DocumentFragment<T>>>() {
            @Override
            public Observable<DocumentFragment<T>> call(final DocumentFragment<T> frag) {
                Observable<DocumentFragment<T>> result = Observe
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
                if (timeout > 0) {
                    result = result.timeout(timeout, timeUnit, environment.scheduler());
                }
                return result;
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
