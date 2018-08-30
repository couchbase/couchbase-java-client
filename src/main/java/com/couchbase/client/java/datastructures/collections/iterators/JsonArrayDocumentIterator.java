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
package com.couchbase.client.java.datastructures.collections.iterators;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.subdoc.MultiMutationException;
import com.couchbase.client.java.subdoc.DocumentFragment;

/**
 * An {@link Iterator} that can iterate over a {@link JsonArrayDocument} identified by <code>id</code>
 * in the given <code>bucket</code>. Uses CAS to perform its {@link #remove()} operation.
 *
 * @author Simon Baslé
 * @author Subhashni Balakrishnan
 * @since 2.3.6
 */
@InterfaceStability.Committed
@InterfaceAudience.Private
public class JsonArrayDocumentIterator<E> implements Iterator<E> {

    private final Bucket bucket;
    private final String id;

    private long cas;
    private final Iterator<E> delegate;
    private int lastVisited = -1;
    private boolean doneRemove = false;

    public JsonArrayDocumentIterator(Bucket bucket, String id) {
        this.bucket = bucket;
        this.id = id;

        JsonArrayDocument current = bucket.get(id, JsonArrayDocument.class);
        this.cas = current.cas();
        this.delegate = (Iterator<E>) current.content().iterator();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public E next() {
        if (hasNext()) {
            lastVisited++;
            doneRemove = false;
        }
        return delegate.next();
    }

    @Override
    public void remove() {
        if (lastVisited < 0) {
            throw new IllegalStateException("Cannot remove before having started iterating");
        }
        //skip remove attempts past the first one after a next()
        if (doneRemove) {
            throw new IllegalStateException("Cannot remove twice in a row while iterating");
        }
        String path = "[" + lastVisited + "]";
        //use the cas to attempt to remove
        try {
            DocumentFragment<Mutation> itrRemoveResult = bucket.mutateIn(id)
                    .withCas(cas)
                    .remove(path)
                    .execute();
            //update the cas
            this.cas = itrRemoveResult.cas();
            //ok the remove succeeded in DB, let's reflect that in the iterator's backing collection and state
            delegate.remove();
            doneRemove = true;
            lastVisited--;
        } catch (CASMismatchException e) {
            throw new ConcurrentModificationException("Couldn't remove while iterating: " + e);
        } catch (MultiMutationException e) {
            if (e.firstFailureStatus() == ResponseStatus.SUBDOC_PATH_NOT_FOUND) {
                throw new IllegalStateException("Invalid remove index " + path);
            }
            throw e;
        }
    }
}