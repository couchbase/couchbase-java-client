/*
 * Copyright (c) 2017 Couchbase, Inc.
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

/**
 * Sub-document options builder. Options supported are
 *  createParents
 * 	xattr
 *
 * @author Subhashni Balakrishnan
 * @since 2.4.2
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class SubdocOptionsBuilder {
    private boolean createPath;
    private boolean xattr;
    private boolean expandMacros;

    public SubdocOptionsBuilder() {
    }

    public static SubdocOptionsBuilder builder() {
        return new SubdocOptionsBuilder();
    }

    /**
     * Set createParents to true to create missing intermediary nodes, else false.
     *
     * @deprecated Please use {@link #createPath(boolean)} instead, this method will be removed
     *             in the next major version.
     */
    @Deprecated
    public SubdocOptionsBuilder createParents(boolean createParents) {
        return createPath(createParents);
    }

    /**
     * Set true/false if the intermediate paths should be created.
     *
     * @param createPath true if they should be created, false otherwise.
     * @return this builder for chaining purposes.
     */
    public SubdocOptionsBuilder createPath(boolean createPath) {
        this.createPath = createPath;
        return this;
    }

    /**
     * Get createParents value set on builder
     *
     * @deprecated Please use {@link #createPath()} instead, this method will be removed
     *             in the next major version.
     */
    @Deprecated
    public boolean createParents() {
        return createPath();
    }

    /**
     * Returns true if the intermediate paths should be created.
     *
     * @return true if they should be created.
     */
    public boolean createPath() {
        return createPath;
    }

    /**
     * Set xattr to true to accessing extended attributes, else false.
     */
    @InterfaceStability.Committed
    public SubdocOptionsBuilder xattr(boolean xattr) {
        this.xattr = xattr;
        return this;
    }

    /**
     * Get xattr value set on builder
     */
    public boolean xattr() {
        return this.xattr;
    }

    /**
     * Controls whether macros such as ${Mutation.CAS} will be expanded by the server for this field.  Default is false.
     */
    @InterfaceAudience.Private
    public SubdocOptionsBuilder expandMacros(boolean expandMacros) {
        this.expandMacros = expandMacros;
        return this;
    }

    /**
     * Get whether macros will be expanded for this field.
     */
    @InterfaceAudience.Private
    public boolean expandMacros() {
        return this.expandMacros;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(" \"createPath\": " + createPath);
        sb.append(", \"xattr\":" + xattr);
        sb.append(", \"expandMacros\":" + expandMacros);
        sb.append("}");
        return sb.toString();
    }
}