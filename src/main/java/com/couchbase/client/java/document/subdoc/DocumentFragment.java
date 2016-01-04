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

package com.couchbase.client.java.document.subdoc;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;

/**
 * A fragment of a {@link JsonDocument JSON Document} that can be any JSON value (including String, {@link JsonObject},
 * {@link JsonArray}, etc...), as returned and used in the sub-document API.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DocumentFragment<T> {

    private String id;
    private String path;
    private T fragment;
    private long cas;
    private int expiry;
    private MutationToken mutationToken;

    private DocumentFragment(String id, String path, int expiry, T fragment, long cas, MutationToken mutationToken) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("The DocumentFragment ID must not be null or empty.");
        }
        if (id.getBytes().length > 250) {
            throw new IllegalArgumentException("The DocumentFragment ID must not be larger than 250 bytes");
        }
        if (expiry < 0) {
            throw new IllegalArgumentException("The DocumentFragment expiry must not be negative.");
        }
        this.id = id;
        this.path = path;
        this.fragment = fragment;
        this.cas = cas;
        this.expiry = expiry;
        this.mutationToken = mutationToken;
    }

    /**
     * Creates a new {@link DocumentFragment} (id, path and fragment) to represent a mutation result (with expiry,
     * cas and mutationToken).
     *
     * This is usually only used internally, for return value of subdocument mutations, as the mutationToken
     * is populated through the analysis of a server response.
     *
     * @param id the id of the document that was mutated.
     * @param path the path at which the mutation occurred.
     * @param expiry the new expiry that was requested along the mutation, or 0L if not changed (document could still
     *               have been previously set to expire).
     * @param fragment the new value after mutation (could be null in case of a delete).
     * @param cas the new CAS value after mutation.
     * @param mutationToken the {@link MutationToken} that marks this mutation if available, null otherwise.
     * @param <T> the type of the fragment content.
     * @return a DocumentFragment representing a mutation result.
     */
    @InterfaceAudience.Private
    public static <T> DocumentFragment<T> create(String id, String path, int expiry, T fragment, long cas,
            MutationToken mutationToken) {
        return new DocumentFragment<T>(id, path, expiry, fragment, cas, mutationToken);
    }

    /**
     * Creates a new {@link DocumentFragment} (id, path and fragment) to represent a mutation input (with expiry).
     *
     * @param id the id of the document to be mutated.
     * @param path the path at which the mutation should occur.
     * @param expiry the new expiry requested along the mutation, or 0L to ignore (document could still
     *               have been previously set to expire).
     * @param fragment the new value after mutation (could be null in case of a delete).
     * @param <T> the type of the fragment content.
     * @return a DocumentFragment representing a mutation input.
     */
    public static <T> DocumentFragment<T> create(String id, String path, int expiry, T fragment) {
        return new DocumentFragment<T>(id, path, expiry, fragment, 0L, null);
    }

    /**
     * Creates a new {@link DocumentFragment} (id, path and fragment) to represent a mutation input
     * using optimistic locking (CAS).
     *
     * @param id the id of the document to be mutated.
     * @param path the path at which the mutation should occur.
     * @param fragment the new value after mutation (could be null in case of a delete).
     * @param cas the expected CAS value to be matched with the document's CAS to validate the mutation.
     * @param <T> the type of the fragment content.
     * @return a DocumentFragment representing a mutation input.
     */
    public static <T> DocumentFragment<T> create(String id, String path, T fragment, long cas) {
        return new DocumentFragment<T>(id, path, 0, fragment, cas, null);
    }

    /**
     * Creates a new {@link DocumentFragment} (id, path and fragment) to represent a mutation input
     * using optimistic locking (CAS).
     *
     * @param id the id of the document to be mutated.
     * @param path the path at which the mutation should occur.
     * @param expiry the new expiry requested along the mutation, or 0L to ignore (document could still
     *               have been previously set to expire).
     * @param fragment the new value after mutation (could be null in case of a delete).
     * @param cas the expected CAS value to be matched with the document's CAS to validate the mutation.
     * @param <T> the type of the fragment content.
     * @return a DocumentFragment representing a mutation input.
     */
    public static <T> DocumentFragment<T> create(String id, String path, int expiry, T fragment, long cas) {
        return new DocumentFragment<T>(id, path, expiry, fragment, cas, null);
    }

    /**
     * Creates a new {@link DocumentFragment} (id, path and fragment) to represent a simple mutation input.
     * No optimistic locking is performed and the enclosing document's expiration is not modified.
     *
     * @param id the id of the document to be mutated.
     * @param path the path at which the mutation should occur.
     * @param fragment the new value after mutation (could be null in case of a delete).
     * @param <T> the type of the fragment content.
     * @return a DocumentFragment representing a mutation input.
     */
    public static <T> DocumentFragment<T> create(String id, String path, T fragment) {
        return new DocumentFragment<T>(id, path, 0, fragment, 0L, null);
    }

    /**
     * @return the {@link JsonDocument#id() id} of the enclosing JSON document in which this fragment belongs.
     */
    public String id() {
        return this.id;
    }

    /**
     * @return the path inside the enclosing JSON document at which this fragment is found, or is to be mutated.
     */
    public String path() {
         return this.path;
    }

    /**
     * @return the fragment, the value either found at the path or the target value of the mutation at the path.
     */
    public T fragment() {
       return this.fragment;
    }

    /**
     * The CAS (Create-and-Set) value can either be set by the user when the DocumentFragment is the input for
     * a mutation, or by the SDK when it is the return value of a subdoc operation.
     *
     * When the fragment is used as an input parameter to a mutation, the CAS will be compared to the one of
     * the {@link #id() target enclosing JSON document} and mutation only applied if both CAS match.
     *
     * When the fragment is the return value of the operation, the CAS is populated with the one from the enclosing
     * JSON document.
     *
     * @return the CAS value related to the enclosing JSON document.
     */
    public long cas() {
        return this.cas;
    }

    /**
     * @return the expiry to be applied to the enclosing JSON document along a mutation.
     */
    public int expiry() {
        return this.expiry;
    }

    /**
     * @return the updated {@link MutationToken} related to the enclosing JSON document after a mutation
     * (when the fragment is the return value of said mutation).
     */
    public MutationToken mutationToken() {
        return this.mutationToken;
    }

    @Override
    public String toString() {
        return "DocumentFragment{" +
                "id='" + id + '\'' +
                ", path='" + path + '\'' +
                ", fragment=" + fragment +
                ", cas=" + cas +
                ", expiry=" + expiry +
                ", mutationToken=" + mutationToken +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DocumentFragment<?> that = (DocumentFragment<?>) o;

        if (cas != that.cas) {
            return false;
        }
        if (expiry != that.expiry) {
            return false;
        }
        if (!id.equals(that.id)) {
            return false;
        }
        if (!path.equals(that.path)) {
            return false;
        }
        if (fragment != null ? !fragment.equals(that.fragment) : that.fragment != null) {
            return false;
        }
        return mutationToken != null ? mutationToken.equals(that.mutationToken) : that.mutationToken == null;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + (fragment != null ? fragment.hashCode() : 0);
        result = 31 * result + (int) (cas ^ (cas >>> 32));
        result = 31 * result + expiry;
        result = 31 * result + (mutationToken != null ? mutationToken.hashCode() : 0);
        return result;
    }
}