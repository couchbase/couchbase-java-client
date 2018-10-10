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
    private final boolean createPath;
    private final boolean xattr;
    private final boolean expandMacros;

    @Deprecated
    public MutationSpec(Mutation type, String path, Object fragment, boolean createPath) {
        //TODO check fragment class?
        this.type = type;
        this.path = path;
        this.fragment = fragment;
        this.createPath = createPath;
        this.xattr = false;
        this.expandMacros = false;
    }

    public MutationSpec(Mutation type, String path, Object fragment, SubdocOptionsBuilder builder) {
        this.type = type;
        this.path = path;
        this.fragment = fragment;
        this.createPath = builder.createPath();
        this.xattr = builder.xattr();
        this.expandMacros = builder.expandMacros();
    }

    public MutationSpec(Mutation type, String path, Object fragment) {
        this.type = type;
        this.path = path;
        this.fragment = fragment;
        this.createPath = false;
        this.xattr = false;
        this.expandMacros = false;
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
    public boolean createPath() {
        return this.createPath;
    }

    /**
     * @return true if accessing extended attributes
     */
    public boolean xattr() {
        return this.xattr;
    }

    /**
     * @return true if macros will be expanded for this field
     */
    public boolean expandMacros() {
        return this.expandMacros;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"type\":" + type);
        sb.append(", \"path\":" + path);
        sb.append(", \"createPath\":" + createPath);
        sb.append(", \"xattr\":" + xattr);
        sb.append(", \"expandMacros\":" + expandMacros);
        sb.append('}');
        return sb.toString();
    }
}