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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.subdoc.DocumentNotJsonException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.util.Blocking;

/**
 * A builder for subdocument mutations. In order to perform the final set of operations, use the
 * {@link #execute()} method. Operations are performed synchronously (see {@link AsyncMutateInBuilder} for an
 * asynchronous version).
 *
 * Instances of this builder should be obtained through {@link Bucket#mutateIn(String)} rather than directly
 * constructed.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class MutateInBuilder {

    private final long defaultTimeout;
    private final TimeUnit defaultTimeUnit;
    private final AsyncMutateInBuilder asyncBuilder;

    /**
     Instances of this builder should be obtained through {@link Bucket#mutateIn(String)} rather than directly
     * constructed.
     */
    @InterfaceAudience.Private
    public MutateInBuilder(AsyncMutateInBuilder asyncBuilder, long defaultTimeout, TimeUnit defaultTimeUnit) {
        this.asyncBuilder = asyncBuilder;
        this.defaultTimeout = defaultTimeout;
        this.defaultTimeUnit = defaultTimeUnit;
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document},
     * with the default key/value timeout.
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
     * This operation throws under the following notable error conditions:
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
     * Other top-level error conditions are similar to those encountered during a document-level {@link Bucket#replace(Document)}.
     *
     * @return a {@link DocumentFragment} (if successful) containing updated cas metadata. Note that some individual
     * results could also bear a value, like counter operations.
     */
    public DocumentFragment<Mutation> execute() {
        return execute(defaultTimeout, defaultTimeUnit);
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document},
     * with the default key/value timeout and durability requirements.
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
     * This operation throws under the following notable error conditions:
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
     * Other top-level error conditions are similar to those encountered during a document-level {@link Bucket#replace(Document)}.
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
     * @return a {@link DocumentFragment} (if successful) containing updated cas metadata. Note that some individual
     * results could also bear a value, like counter operations.
     */
    public DocumentFragment<Mutation> execute(PersistTo persistTo, ReplicateTo replicateTo) {
        return execute(persistTo, replicateTo, defaultTimeout, defaultTimeUnit);
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document},
     * with the default key/value timeout and durability requirements.
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
     * This operation throws under the following notable error conditions:
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
     * Other top-level error conditions are similar to those encountered during a document-level {@link Bucket#replace(Document)}.
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
     * @return a {@link DocumentFragment} (if successful) containing updated cas metadata. Note that some individual
     * results could also bear a value, like counter operations.
     */
    public DocumentFragment<Mutation> execute(PersistTo persistTo) {
        return execute(persistTo, defaultTimeout, defaultTimeUnit);
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document},
     * with the default key/value timeout and durability requirements.
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
     * This operation throws under the following notable error conditions:
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
     * Other top-level error conditions are similar to those encountered during a document-level {@link Bucket#replace(Document)}.
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
     * @return a {@link DocumentFragment} (if successful) containing updated cas metadata. Note that some individual
     * results could also bear a value, like counter operations.
     */
    public DocumentFragment<Mutation> execute(ReplicateTo replicateTo) {
        return execute(replicateTo, defaultTimeout, defaultTimeUnit);
    }


    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document},
     * with a specific timeout.
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
     * This operation throws under the following most notable error conditions:
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
     * Other top-level error conditions are similar to those encountered during a document-level {@link Bucket#replace(Document)}.
     *
     * @param timeout the specific timeout to apply for the operation.
     * @param timeUnit the time unit for the timeout.
     * @return a {@link DocumentFragment} (if successful) containing updated cas metadata. Note that some individual
     * results could also bear a value, like counter operations.
     */
    public DocumentFragment<Mutation> execute(long timeout, TimeUnit timeUnit) {
        return asyncBuilder.execute(timeout, timeUnit).toBlocking().single();
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document},
     * with a specific timeout and durability requirements.
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
     * This operation throws under the following notable error conditions:
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
     * Other top-level error conditions are similar to those encountered during a document-level {@link Bucket#replace(Document)}.
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
     * @return a {@link DocumentFragment} (if successful) containing updated cas metadata. Note that some individual
     * results could also bear a value, like counter operations.
     */
    public DocumentFragment<Mutation> execute(PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBuilder.execute(persistTo, replicateTo, timeout, timeUnit).toBlocking().single();
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document},
     * with a specific timeout and durability requirements.
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
     * This operation throws under the following notable error conditions:
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
     * Other top-level error conditions are similar to those encountered during a document-level {@link Bucket#replace(Document)}.
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
     * @return a {@link DocumentFragment} (if successful) containing updated cas metadata. Note that some individual
     * results could also bear a value, like counter operations.
     */
    public DocumentFragment<Mutation> execute(PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBuilder.execute(persistTo, timeout, timeUnit).toBlocking().single();
    }

    /**
     * Perform several {@link Mutation mutation} operations inside a single existing {@link JsonDocument JSON document},
     * with a specific timeout and durability requirements.
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
     * This operation throws under the following notable error conditions:
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
     * Other top-level error conditions are similar to those encountered during a document-level {@link Bucket#replace(Document)}.
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
     * @return a {@link DocumentFragment} (if successful) containing updated cas metadata. Note that some individual
     * results could also bear a value, like counter operations.
     */
    public DocumentFragment<Mutation> execute(ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBuilder.execute(replicateTo, timeout, timeUnit).toBlocking().single();
    }

    //==== DOCUMENT level modifiers ====
    /**
     * Change the expiry of the enclosing document as part of the mutation.
     *
     * @param expiry the new expiry to apply (or 0 to avoid changing the expiry)
     * @return this builder for chaining.
     */
    public MutateInBuilder withExpiry(int expiry) {
        asyncBuilder.withExpiry(expiry);
        return this;
    }

    /**
     * Apply the whole mutation using optimistic locking, checking against the provided CAS value.
     *
     * @param cas the CAS to compare the enclosing document to.
     * @return this builder for chaining.
     */
    public MutateInBuilder withCas(long cas) {
        asyncBuilder.withCas(cas);
        return this;
    }

    /**
     * Set a persistence durability constraint for the whole mutation.
     *
     * @param persistTo the persistence durability constraint to observe.
     * @return this builder for chaining.
     */
    public MutateInBuilder withDurability(PersistTo persistTo) {
        asyncBuilder.withDurability(persistTo);
        return this;
    }

    /**
     * Set a replication durability constraint for the whole mutation.
     *
     * @param replicateTo the replication durability constraint to observe.
     * @return this builder for chaining.
     */
    public MutateInBuilder withDurability(ReplicateTo replicateTo) {
        asyncBuilder.withDurability(replicateTo);
        return this;
    }

    /**
     * Set both a persistence and replication durability constraints for the whole mutation.
     *
     * @param persistTo the persistence durability constraint to observe.
     * @param replicateTo the replication durability constraint to observe.
     * @return this builder for chaining.
     */
    public MutateInBuilder withDurability(PersistTo persistTo, ReplicateTo replicateTo) {
        asyncBuilder.withDurability(persistTo, replicateTo);
        return this;
    }

    /**
     *  Set createDocument to true, if the document has to be created.
     *
     *  Please use {@link #upsertDocument(boolean)} instead.
     *
     * @param createDocument true to create document.
     * @return this builder for chaining.
     */
    @Deprecated
    public MutateInBuilder createDocument(boolean createDocument) {
        asyncBuilder.createDocument(createDocument);
        return this;
    }

    /**
     *  Set upsertDocument to true, if the document has to be created.
     *
     * @param upsertDocument true to create document.
     * @return this builder for chaining.
     */
    @InterfaceStability.Committed
    public MutateInBuilder upsertDocument(boolean upsertDocument) {
        asyncBuilder.upsertDocument(upsertDocument);
        return this;
    }

    /**
     * Set insertDocument to true, if the document has to be created only if it does not exist
     *
     * @param insertDocument true to insert document.
     * @return this builder for chaining.
     */
    @InterfaceStability.Committed
    public MutateInBuilder insertDocument(boolean insertDocument) {
        asyncBuilder.insertDocument(insertDocument);
        return this;
    }

    //==== SUBDOC operation specs ====
    /**
     * Replace an existing value by the given fragment.
     *
     * @param path the path where the value to replace is.
     * @param fragment the new value.
     */
    public <T> MutateInBuilder replace(String path, T fragment) {
        asyncBuilder.replace(path, fragment);
        return this;
    }

    /**
     * Insert a fragment provided the last element of the path doesn't exists.
     *
     * @param path the path where to insert a new dictionary value.
     * @param fragment the new dictionary value to insert.
     * @param createPath true to create missing intermediary nodes.
     */
    @Deprecated
    public <T> MutateInBuilder insert(String path, T fragment, boolean createPath) {
        asyncBuilder.insert(path, fragment, createPath);
        return this;
    }

    /**
     * Insert a fragment provided the last element of the path doesn't exists.
     *
     * @param path the path where to insert a new dictionary value.
     * @param fragment the new dictionary value to insert.
     */
    public <T> MutateInBuilder insert(String path, T fragment) {
        asyncBuilder.insert(path, fragment);
        return this;
    }

    /**
     * Insert a fragment provided the last element of the path doesn't exists.
     *
     * @param path the path where to insert a new dictionary value.
     * @param fragment the new dictionary value to insert.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> MutateInBuilder insert(String path, T fragment, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.insert(path, fragment, optionsBuilder);
        return this;
    }

    /**
     * Insert a fragment, replacing the old value if the path exists.
     *
     * @param path the path where to insert (or replace) a dictionary value.
     * @param fragment the new dictionary value to be applied.
     * @param createPath true to create missing intermediary nodes.
     */
    @Deprecated
    public <T> MutateInBuilder upsert(String path, T fragment, boolean createPath) {
        asyncBuilder.upsert(path, fragment, new SubdocOptionsBuilder().createPath(createPath));
        return this;
    }

    /**
     * Insert a fragment, replacing the old value if the path exists.
     *
     * @param path the path where to insert (or replace) a dictionary value.
     * @param fragment the new dictionary value to be applied.
     */
    public <T> MutateInBuilder upsert(String path, T fragment) {
        asyncBuilder.upsert(path, fragment);
        return this;
    }

    /**
     * Upsert a full JSON document that doesn't exist.
     *
     * @param content full content of the JSON document
     */
    @InterfaceStability.Committed
    public MutateInBuilder upsert(JsonObject content) {
        asyncBuilder.upsert(content);
        return this;
    }

    /**
     * Insert a fragment, replacing the old value if the path exists.
     *
     * @param path the path where to insert (or replace) a dictionary value.
     * @param fragment the new dictionary value to be applied.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> MutateInBuilder upsert(String path, T fragment, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.upsert(path, fragment, optionsBuilder);
        return this;
    }

    /**
     * Remove an entry in a JSON document (scalar, array element, dictionary entry,
     * whole array or dictionary, depending on the path).
     *
     * @param path the path to remove.
     */
    public <T> MutateInBuilder remove(String path) {
        asyncBuilder.remove(path);
        return this;
    }


    /**
     * Remove an entry in a JSON document (scalar, array element, dictionary entry,
     * whole array or dictionary, depending on the path).
     *
     * @param path the path to remove.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     */
    public <T> MutateInBuilder remove(String path, SubdocOptionsBuilder optionsBuilder) {
        if (optionsBuilder.createPath()) {
            throw new IllegalArgumentException("Options createPath are not supported for remove");
        }
        asyncBuilder.remove(path, optionsBuilder);
        return this;
    }


    /**
     * Increment/decrement a numerical fragment in a JSON document.
     * If the value (last element of the path) doesn't exist the counter is created and takes the value of the delta.
     *
     * @param path the path to the counter (must be containing a number).
     * @param delta the value to increment or decrement the counter by.
     * @param createPath true to create missing intermediary nodes.
     */
    @Deprecated
    public MutateInBuilder counter(String path, long delta, boolean createPath) {
        asyncBuilder.counter(path, delta, new SubdocOptionsBuilder().createPath(createPath));
        return this;
    }

    /**
     * Increment/decrement a numerical fragment in a JSON document.
     * If the value (last element of the path) doesn't exist the counter is created and takes the value of the delta.
     *
     * @param path the path to the counter (must be containing a number).
     * @param delta the value to increment or decrement the counter by.
     */
    public MutateInBuilder counter(String path, long delta) {
        asyncBuilder.counter(path, delta);
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
    public MutateInBuilder counter(String path, long delta, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.counter(path, delta, optionsBuilder);
        return this;
    }

    /**
     * Prepend to an existing array, pushing the value to the front/first position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the front of the array.
     * @param createPath true to create missing intermediary nodes.
     */
    @Deprecated
    public <T> MutateInBuilder arrayPrepend(String path, T value, boolean createPath) {
        asyncBuilder.arrayPrepend(path, value, new SubdocOptionsBuilder().createPath(createPath));
        return this;
    }


    /**
     * Prepend to an existing array, pushing the value to the front/first position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the front of the array.
     */
    public <T> MutateInBuilder arrayPrepend(String path, T value) {
        asyncBuilder.arrayPrepend(path, value);
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
    public <T> MutateInBuilder arrayPrepend(String path, T value, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.arrayPrepend(path, value, optionsBuilder);
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
     */
    @Deprecated
    public <T> MutateInBuilder arrayPrependAll(String path, Collection<T> values, boolean createPath) {
        asyncBuilder.arrayPrependAll(path, values, new SubdocOptionsBuilder().createPath(createPath));
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
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @param <T> the type of data in the collection (must be JSON serializable).
     */
    public <T> MutateInBuilder arrayPrependAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.arrayPrependAll(path, values, optionsBuilder);
        return this;
    }

    /**
     * Prepend multiple values at once in an existing array, pushing all values to the front/start of the array.
     * This is provided as a convenience alternative to {@link #arrayPrependAll(String, Collection, boolean)}.
     * Note that parent nodes are not created when using this method (ie. createPath = false).
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
    public <T> MutateInBuilder arrayPrependAll(String path, T... values) {
        asyncBuilder.arrayPrependAll(path, values);
        return this;
    }

    /**
     * Append to an existing array, pushing the value to the back/last position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the back of the array.
     * @param createPath true to create missing intermediary nodes.
     */
    @Deprecated
    public <T> MutateInBuilder arrayAppend(String path, T value, boolean createPath) {
        asyncBuilder.arrayAppend(path, value, new SubdocOptionsBuilder().createPath(createPath));
        return this;
    }

    /**
     * Append to an existing array, pushing the value to the back/last position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the back of the array.
     */
    public <T> MutateInBuilder arrayAppend(String path, T value) {
        asyncBuilder.arrayAppend(path, value);
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
    public <T> MutateInBuilder arrayAppend(String path, T value, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.arrayAppend(path, value, optionsBuilder);
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
     */
    @Deprecated
    public <T> MutateInBuilder arrayAppendAll(String path, Collection<T> values, boolean createPath) {
        asyncBuilder.arrayAppendAll(path, values,  new SubdocOptionsBuilder().createPath(createPath));
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
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @param <T> the type of data in the collection (must be JSON serializable).
     */
    public <T> MutateInBuilder arrayAppendAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.arrayAppendAll(path, values, optionsBuilder);
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
    public <T> MutateInBuilder arrayAppendAll(String path, T... values) {
        asyncBuilder.arrayAppendAll(path, values);
        return this;
    }

    /**
     * Insert into an existing array at a specific position
     * (denoted in the path, eg. "sub.array[2]").
     *
     * @param path the path (including array position) where to insert the value.
     * @param value the value to insert in the array.
     */
    public <T> MutateInBuilder arrayInsert(String path, T value) {
        asyncBuilder.arrayInsert(path, value);
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
    public <T> MutateInBuilder arrayInsert(String path, T value, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.arrayInsert(path, value, optionsBuilder);
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
    public <T> MutateInBuilder arrayInsertAll(String path, Collection<T> values) {
        asyncBuilder.arrayInsertAll(path, values);
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
    public <T> MutateInBuilder arrayInsertAll(String path, Collection<T> values, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.arrayInsertAll(path, values, optionsBuilder);
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
    public <T> MutateInBuilder arrayInsertAll(String path, T... values) {
        asyncBuilder.arrayInsertAll(path, values);
        return this;
    }

    /**
     * Insert a value in an existing array only if the value
     * isn't already contained in the array (by way of string comparison).
     *
     * @param path the path to mutate in the JSON.
     * @param value the value to insert.
     * @param createPath true to create missing intermediary nodes.
     */
    @Deprecated
    public <T> MutateInBuilder arrayAddUnique(String path, T value, boolean createPath) {
        asyncBuilder.arrayAddUnique(path, value, new SubdocOptionsBuilder().createPath(createPath));
        return this;
    }

    /**
     * Insert a value in an existing array only if the value
     * isn't already contained in the array (by way of string comparison).
     *
     * @param path the path to mutate in the JSON.
     * @param value the value to insert.
     */
    public <T> MutateInBuilder arrayAddUnique(String path, T value) {
        asyncBuilder.arrayAddUnique(path, value);
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
    public <T> MutateInBuilder arrayAddUnique(String path, T value, SubdocOptionsBuilder optionsBuilder) {
        asyncBuilder.arrayAddUnique(path, value, optionsBuilder);
        return this;
    }

    @Override
    public String toString() {
        return asyncBuilder.toString();
    }
}
