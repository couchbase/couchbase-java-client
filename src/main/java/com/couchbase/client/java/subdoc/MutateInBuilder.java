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
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.subdoc.DocumentNotJsonException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.util.Blocking;

/**
 * A builder for subdocument mutations. In order to perform the final set of operations, use the
 * {@link #doMutate()} method. Operations are performed synchronously (see {@link AsyncMutateInBuilder} for an
 * asynchronous version).
 *
 * Instances of this builder should be obtained through {@link Bucket#mutateIn(String)} rather than directly
 * constructed.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
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
     *  - The durability constraint could not be fulfilled because of a temporary or persistent problem: {@link DurabilityException}
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
    public DocumentFragment<Mutation> doMutate() {
        return doMutate(defaultTimeout, defaultTimeUnit);
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
     *  - The durability constraint could not be fulfilled because of a temporary or persistent problem: {@link DurabilityException}
     *  - CAS was provided but optimistic locking failed: {@link CASMismatchException}
     //TODO list all subdoc level errors here
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
    public DocumentFragment<Mutation> doMutate(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBuilder.doMutate(), timeout, timeUnit);
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
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> MutateInBuilder insert(String path, T fragment, boolean createParents) {
        asyncBuilder.insert(path, fragment, createParents);
        return this;
    }

    /**
     * Insert a fragment, replacing the old value if the path exists.
     *
     * @param path the path where to insert (or replace) a dictionary value.
     * @param fragment the new dictionary value to be applied.
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> MutateInBuilder upsert(String path, T fragment, boolean createParents) {
        asyncBuilder.upsert(path, fragment, createParents);
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
     * Increment/decrement a numerical fragment in a JSON document.
     * If the value (last element of the path) doesn't exist the counter is created and takes the value of the delta.
     *
     * @param path the path to the counter (must be containing a number).
     * @param delta the value to increment or decrement the counter by.
     * @param createParents true to create missing intermediary nodes.
     */
    public MutateInBuilder counter(String path, long delta, boolean createParents) {
        asyncBuilder.counter(path, delta, createParents);
        return this;
    }

    /**
     * Prepend to an existing array, pushing the value to the front/first position in
     * the array.
     *
     * @param path the path of the array.
     * @param value the value to insert at the front of the array.
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> MutateInBuilder arrayPrepend(String path, T value, boolean createParents) {
        asyncBuilder.arrayPrepend(path, value, createParents);
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
     * @param createParents true to create missing intermediary nodes.
     * @param <T> the type of data in the collection (must be JSON serializable).
     */
    public <T> MutateInBuilder arrayPrependAll(String path, Collection<T> values, boolean createParents) {
        asyncBuilder.arrayPrependAll(path, values, createParents);
        return this;
    }

    /**
     * Prepend multiple values at once in an existing array, pushing all values to the front/start of the array.
     * This is provided as a convenience alternative to {@link #arrayPrependAll(String, Collection, boolean)}.
     * Note that parent nodes are not created when using this method (ie. createParents = false).
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
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> MutateInBuilder arrayAppend(String path, T value, boolean createParents) {
        asyncBuilder.arrayAppend(path, value, createParents);
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
     * @param createParents true to create missing intermediary nodes.
     * @param <T> the type of data in the collection (must be JSON serializable).
     */
    public <T> MutateInBuilder arrayAppendAll(String path, Collection<T> values, boolean createParents) {
        asyncBuilder.arrayAppendAll(path, values, createParents);
        return this;
    }

    /**
     * Append multiple values at once in an existing array, pushing all values to the back/end of the array.
     * This is provided as a convenience alternative to {@link #arrayAppendAll(String, Collection, boolean)}.
     * Note that parent nodes are not created when using this method (ie. createParents = false).
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
     * path, eg. "sub.array[2]"), inserting all values at the given position and shifting existing values
     * beyond the position by the number of elements in the collection. This is provided as a convenience
     * alternative to {@link #arrayInsertAll(String, Collection)}. Note that parent nodes are not created
     * when using this method (ie. createParents = false).
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
     * @param createParents true to create missing intermediary nodes.
     */
    public <T> MutateInBuilder arrayAddUnique(String path, T value, boolean createParents) {
        asyncBuilder.arrayAddUnique(path, value, createParents);
        return this;
    }

    @Override
    public String toString() {
        return asyncBuilder.toString();
    }
}
