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
    private final boolean attributeAccess;

    @Deprecated
    public MutationSpec(Mutation type, String path, Object fragment, boolean createParents) {
        //TODO check fragment class?
        this.type = type;
        this.path = path;
        this.fragment = fragment;
        this.createParents = createParents;
        this.attributeAccess = false;
    }

    public MutationSpec(Mutation type, String path, Object fragment, SubdocOptionsBuilder builder) {
        this.type = type;
        this.path = path;
        this.fragment = fragment;
        this.createParents = builder.createParents();
        this.attributeAccess = builder.attributeAccess();
    }

    public MutationSpec(Mutation type, String path, Object fragment) {
        this.type = type;
        this.path = path;
        this.fragment = fragment;
        this.createParents = false;
        this.attributeAccess = false;
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
        return this.createParents;
    }

    /**
     * @return true if accessing extended attributes
     */
    public boolean attributeAccess() {
        return this.attributeAccess;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{").append(type());
        if (createParents) {
            sb.append(", createParents");
        }
        if (attributeAccess) {
            sb.append(", attributeAccess");
        }
        sb.append(':').append(path()).append('}');
        return sb.toString();
    }
}