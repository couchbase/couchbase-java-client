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
package com.couchbase.client.java.datastructures.collections;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Queue;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.datastructures.collections.iterators.JsonArrayDocumentIterator;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.subdoc.DocumentFragment;

/**
 * A CouchbaseQueue is a {@link Queue} backed by a {@link Bucket Couchbase} document (more
 * specifically a {@link JsonArrayDocument JSON array}).
 *
 * Note that as such, a CouchbaseQueue is restricted to the types that a {@link JsonArray JSON array}
 * can contain. JSON objects and sub-arrays can be represented as {@link JsonObject} and {@link JsonArray}
 * respectively. Null values are not allowed as they have special meaning for the {@link #peek()} and {@link #remove()}
 * methods of a queue.
 *
 * @param <E> the type of values in the queue.
 *
 * @author Simon Baslé
 * @author Subhashni Balakrishnan
 * @since 2.3.6
 */

@InterfaceStability.Committed
@InterfaceAudience.Public
public class CouchbaseQueue<E> extends AbstractQueue<E> {

    private static final int MAX_OPTIMISTIC_LOCKING_ATTEMPTS = Integer.parseInt(System.getProperty("com.couchbase.datastructureCASRetryLimit", "10"));
    private final String id;
    private final Bucket bucket;

    /**
     * Create a new {@link Bucket Couchbase-backed} Queue, backed by the document identified by <code>id</code>
     * in <code>bucket</code>. Note that if the document already exists, its content will be used as initial
     * content for this collection. Otherwise it is created empty.
     *
     * @param id the id of the Couchbase document to back the queue.
     * @param bucket the {@link Bucket} through which to interact with the document.
     */
    public CouchbaseQueue(String id, Bucket bucket) {
        this.bucket = bucket;
        this.id = id;

        try {
            bucket.insert(JsonArrayDocument.create(id, JsonArray.empty()));
        } catch (DocumentAlreadyExistsException ex) {
            // Ignore concurrent creations, keep on moving.
        }
    }

    /**
     * Create a new {@link Bucket Couchbase-backed} Queue, backed by the document identified by <code>id</code>
     * in <code>bucket</code>. Note that if the document already exists, its content is reset to the values
     * provided.
     *
     * Note that if you don't provide any value as a vararg, the {@link #CouchbaseQueue(String, Bucket)}
     * constructor will be invoked instead, which will use pre-existing values as content. To create a new
     * Queue and force it to be empty, use {@link #CouchbaseQueue(String, Bucket, Collection)} with an empty
     * collection.
     *
     * @param id the id of the Couchbase document to back the queue.
     * @param bucket the {@link Bucket} through which to interact with the document.
     * @param content vararg of the elements to initially store in the Queue.
     */
    public CouchbaseQueue(String id, Bucket bucket, E... content) {
        this.bucket = bucket;
        this.id = id;

        JsonArray array = JsonArray.create();
        for (E e : content) {
            if (!JsonValue.checkType(e)) {
                throw new ClassCastException();
            } else if (e == null) {
                throw new NullPointerException();
            }
            array.add(e);
        }
        bucket.upsert(JsonArrayDocument.create(id, array));
    }

    /**
     * Create a new {@link Bucket Couchbase-backed} Queue, backed by the document identified by <code>id</code>
     * in <code>bucket</code>. Note that if the document already exists, its content is reset to the values
     * provided in the <code>content</code> Collection.
     *
     * @param id the id of the Couchbase document to back the queue.
     * @param bucket the {@link Bucket} through which to interact with the document.
     * @param content collection of the elements to initially store in the Queue, in iteration order.
     */
    public CouchbaseQueue(String id, Bucket bucket, Collection<? extends E> content) {
        this.bucket = bucket;
        this.id = id;

        JsonArray array = JsonArray.create();
        for (E e : content) {
            if (!JsonValue.checkType(e)) {
                throw new ClassCastException();
            } else if (e == null) {
                throw new NullPointerException();
            }
            array.add(e);
        }

        bucket.upsert(JsonArrayDocument.create(id, array));
    }

    @Override
    public Iterator<E> iterator() {
        return new JsonArrayDocumentIterator<E>(bucket, id);
    }

    @Override
    public int size() {
        //TODO in Spock, GET_COUNT should be available on subdoc
        JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
        return current.content().size();
    }

    @Override
    public void clear() {
        bucket.upsert(JsonArrayDocument.create(id, JsonArray.empty()));
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException("Unsupported null value");
        }
        if (!JsonValue.checkType(e)) {
            throw new IllegalArgumentException("Unsupported value type.");
        }

        bucket.mutateIn(id).arrayPrepend("", e, false).execute();
        return true;
    }

    @Override
    public E poll() {
        String idx = "[-1]"; //FIFO queue as offer uses ARRAY_PREPEND
        for(int i = 0; i < MAX_OPTIMISTIC_LOCKING_ATTEMPTS; i++) {
            try {
                DocumentFragment<Lookup> current = bucket.lookupIn(id).get(idx).execute();
                long returnCas = current.cas();
                Object result = current.content(idx);
                DocumentFragment<Mutation> updated = bucket.mutateIn(id).remove(idx).withCas(returnCas).execute();
                return (E) result;
            } catch (CASMismatchException ex) {
                //will have to retry get-and-remove
            } catch (MultiMutationException ex) {
                if (ex.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    return null; //the queue is empty
                }
                throw ex;
            }
        }
        throw new ConcurrentModificationException("Couldn't perform poll in less than " + MAX_OPTIMISTIC_LOCKING_ATTEMPTS + " iterations");
    }

    @Override
    public E peek() {
        try {
            DocumentFragment<Lookup> current = bucket.lookupIn(id).get("[0]").execute();
            Object result = current.content(0);
            return (E) result;
        } catch (MultiMutationException ex) {
            if (ex.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                return null; //the queue is empty
            }
            throw ex;
        }
    }
}