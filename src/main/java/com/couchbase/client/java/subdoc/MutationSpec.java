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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;

/**
 * Internally represents a single mutation in a batch of subdocument mutations.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Private
public class MutationSpec {
    private final Mutation type;
    private final String path;
    private final Object fragment;
    private final boolean createParents;

    public MutationSpec(Mutation type, String path, Object fragment, boolean createParents) {
        //TODO check fragment class?
        this.type = type;
        this.path = path;
        this.fragment = fragment;
        this.createParents = createParents;
    }

    /**
     * @return the {@link Mutation type} of the mutation.
     */
    public Mutation type() {
        return type;
    }

    /**
     * @return the path targeted by the mutation.
     */
    public String path() {
        return path;
    }

    /**
     * @return the fragment value to apply as a mutation.
     */
    public Object fragment() {
        return fragment;
    }

    /**
     * @return true should the mutation create missing intermediary elements in the path (if it supports it).
     */
    public boolean createParents() {
        return createParents;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{").append(type());
        if (createParents) {
            sb.append(", createParents");
        }
        sb.append(':').append(path()).append('}');
        return sb.toString();
    }
}