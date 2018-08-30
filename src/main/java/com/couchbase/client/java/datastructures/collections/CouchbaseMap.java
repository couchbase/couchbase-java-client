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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.document.json.JsonValue;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.error.subdoc.PathNotFoundException;
import com.couchbase.client.java.subdoc.DocumentFragment;

/**
 * A CouchbaseMap is a {@link Map} backed by a {@link Bucket Couchbase} document (more specifically a
 * {@link JsonDocument JSON object}).
 *
 * Null keys are NOT permitted, and keys are restricted to {@link String}.
 *
 * Values in a CouchbaseMap are restricted to the types that a {@link JsonObject JSON objects}
 * can contain. JSON sub-objects and sub-arrays can be represented as {@link JsonObject} and {@link JsonArray}
 * respectively.
 *
 * @param <V> the type of values in the map (restricted to {@link JsonObject}.
 *
 * @author Simon Baslé
 * @author Subhashni Balakrishnan
 * @since 2.3.6
 */

@InterfaceStability.Committed
@InterfaceAudience.Public
public class CouchbaseMap<V> extends AbstractMap<String, V> {

    private static final int MAX_OPTIMISTIC_LOCKING_ATTEMPTS = Integer.parseInt(System.getProperty("com.couchbase.datastructureCASRetryLimit", "10"));
    private final String id;
    private final Bucket bucket;

    /**
     * Create a new {@link CouchbaseMap}, backed by the document identified by <code>id</code>
     * in the given Couchbase <code>bucket</code>. Note that if the document already exists,
     * its content will be used as initial content for this collection. Otherwise it is created empty.
     *
     * @param id the id of the Couchbase document to back the map.
     * @param bucket the {@link Bucket} through which to interact with the document.
     */
    public CouchbaseMap(String id, Bucket bucket) {
        this.id = id;
        this.bucket = bucket;

        try {
            bucket.insert(JsonDocument.create(id, JsonObject.empty()));
        } catch (DocumentAlreadyExistsException ex) {
            // Ignore concurrent creations, keep on moving.
        }
    }

    /**
     * Create a new {@link CouchbaseMap}, backed by the document identified by <code>id</code>
     * in the given Couchbase <code>bucket</code>. Note that if the document already exists, its content is
     * reset to the values copied from the given <code>data</code> Map.
     *
     * A null or empty data map will re-initialize any pre-existing document to an empty content.
     *
     * @param id the id of the Couchbase document to back the list.
     * @param bucket the {@link Bucket} through which to interact with the document.
     * @param data Map of the elements to initially store in the CouchbaseMap.
     */
    public CouchbaseMap(String id, Bucket bucket, Map<String, ? extends V> data) {
        this.id = id;
        this.bucket = bucket;

        JsonObject content = JsonObject.create();
        if (data != null && !data.isEmpty()) {
            for (Entry<String, ? extends V> entry : data.entrySet()) {
                if (entry.getKey() == null) {
                    throw new NullPointerException("Attempted to create a map with a null key");
                }
                content.put(entry.getKey(), entry.getValue());
            }
        }
        JsonDocument initial = JsonDocument.create(id, content);
        bucket.upsert(initial);
    }

    @Override
    public V put(String key, V value) {
        if (key == null) {
            throw new NullPointerException("Unsupported null key");
        }
        if (!JsonValue.checkType(value)) {
            throw new IllegalArgumentException("Unsupported value type.");
        }

        for(int i = 0; i < MAX_OPTIMISTIC_LOCKING_ATTEMPTS; i++) {
            try {
                DocumentFragment<Lookup> current = bucket.lookupIn(id).get(key).execute();
                long returnCas = current.cas();
                Object result = null;
                if (current.exists(key)) {
                    result = current.content(key);
                }
                bucket.mutateIn(id).upsert(key, value, false).withCas(returnCas).execute();
                return (V) result;
            } catch (CASMismatchException ex) {
                //will need to retry get-and-set
            }
        }
        throw new ConcurrentModificationException("Couldn't perform put in less than " + MAX_OPTIMISTIC_LOCKING_ATTEMPTS + " iterations");
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            throw new NullPointerException("Unsupported null key");
        }
        try {
            return (V) bucket.lookupIn(id)
                    .get(String.valueOf(key))
                    .execute()
                    .content(0);
        } catch (PathNotFoundException e) {
            return null;
        }
    }



    @Override
    public V remove(Object key) {
        if (key == null) {
            throw new NullPointerException("Unsupported null key");
        }
        String idx = String.valueOf(key);
        for(int i = 0; i < MAX_OPTIMISTIC_LOCKING_ATTEMPTS; i++) {
            try {
                DocumentFragment<Lookup> current = bucket.lookupIn(id).get(idx).execute();
                long returnCas = current.cas();
                Object result = current.content(idx);
                DocumentFragment<Mutation> updated = bucket.mutateIn(id).remove(idx).withCas(returnCas).execute();
                return (V) result;
            } catch (CASMismatchException ex) {
                //will have to retry get-and-remove
            } catch (MultiMutationException ex) {
                if (ex.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                    return null;
                }
                throw ex;
            }
        }
        throw new ConcurrentModificationException("Couldn't perform remove in less than " + MAX_OPTIMISTIC_LOCKING_ATTEMPTS + " iterations");
    }

    @Override
    public void clear() {
        //optimized version over AbstractMap's (which uses the entry set)
        bucket.upsert(JsonDocument.create(id, JsonObject.empty()));
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return new CouchbaseEntrySet((Map<String, V>) bucket.get(id).content().toMap());
    }

    @Override
    public boolean containsKey(Object key) {
        return (Boolean) bucket
                .lookupIn(id).exists(String.valueOf(key))
                .execute()
                .content(0);
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(value); //TODO use ARRAY_CONTAINS subdoc operator when available
    }

    @Override
    public int size() {
        return super.size(); //TODO use COUNT subdoc operator when available
    }

    private class CouchbaseEntrySet implements Set<Map.Entry<String, V>> {

        private final Set<Map.Entry<String, V>> delegate;

        private CouchbaseEntrySet(Map<String, V> data) {
            this.delegate = data.entrySet();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<Entry<String, V>> iterator() {
            return new CouchbaseEntrySetIterator(delegate.iterator());
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(Entry<String, V> stringVEntry) {
            return delegate.add(stringVEntry);
        }

        @Override
        public boolean remove(Object o) {
            if (delegate.remove(o)) {
                if (o instanceof Map.Entry) {
                    Entry<String, V> entry = (Entry<String, V>) o;
                    CouchbaseMap.this.remove(entry.getKey());
                } else {
                    throw new IllegalStateException("Expected entrySet remove() to remove an entry");
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Entry<String, V>> c) {
            return delegate.addAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
            CouchbaseMap.this.clear();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        public int hashCode() {
            return delegate.hashCode();
        }

        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }
    }

    private class CouchbaseEntrySetIterator implements Iterator<Entry<String, V>> {

        private final Iterator<Entry<String, V>> delegateItr;
        private Entry<String, V> lastNext = null;

        public CouchbaseEntrySetIterator(Iterator<Entry<String, V>> iterator) {
            this.delegateItr = iterator;
        }

        @Override
        public boolean hasNext() {
            return delegateItr.hasNext();
        }

        @Override
        public Entry<String, V> next() {
            this.lastNext = delegateItr.next();
            return lastNext;
        }

        @Override
        public void remove() {
            if (lastNext == null)
                throw new IllegalStateException("next() hasn't been called before remove()");
            delegateItr.remove();
            CouchbaseMap.this.remove(lastNext.getKey());
        }
    }
}