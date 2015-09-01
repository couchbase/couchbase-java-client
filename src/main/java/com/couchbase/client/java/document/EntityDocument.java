/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.document;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.kv.MutationToken;

/**
 * The entity document is used to carry enclosed entities for the repository implementation.
 *
 * @author Michael Nitschinger
 * @since 1.2.0
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public class EntityDocument<T> implements Document<T> {

    private String id;
    private long cas;
    private int expiry;
    private T content;
    private MutationToken mutationToken;

    private EntityDocument(String id, int expiry, T content, long cas, MutationToken mutationToken) {
        if (expiry < 0) {
            throw new IllegalArgumentException("The Document expiry must not be negative.");
        }

        this.id = id;
        this.expiry = expiry;
        this.content = content;
        this.cas = cas;
        this.mutationToken = mutationToken;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public T content() {
        return content;
    }

    @Override
    public long cas() {
        return cas;
    }

    @Override
    public int expiry() {
        return expiry;
    }

    @Override
    public MutationToken mutationToken() {
        return mutationToken;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + "{");
        sb.append("id='").append(id).append('\'');
        sb.append(", cas=").append(cas);
        sb.append(", expiry=").append(expiry);
        sb.append(", content=").append(content);
        sb.append(", mutationToken=").append(mutationToken);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityDocument<?> that = (EntityDocument<?>) o;

        if (cas != that.cas) return false;
        if (expiry != that.expiry) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        return !(mutationToken != null ? !mutationToken.equals(that.mutationToken) : that.mutationToken != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (int) (cas ^ (cas >>> 32));
        result = 31 * result + expiry;
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (mutationToken != null ? mutationToken.hashCode() : 0);
        return result;
    }

    public static <T> EntityDocument<T> create(T content) {
        return create(null, 0, content, 0);
    }

    public static <T> EntityDocument<T> create(String id, T content) {
        return create(id, 0, content, 0);
    }

    public static <T> EntityDocument<T> create(String id, int expiry, T content) {
        return create(id, expiry, content, 0);
    }

    public static <T> EntityDocument<T> create(String id, T content, long cas) {
        return create(id, 0, content, cas);
    }

    public static <T> EntityDocument<T> create(String id, int expiry, T content, long cas) {
        return new EntityDocument<T>(id, expiry, content, cas, null);
    }

    public static <T> EntityDocument<T> create(String id, int expiry, T content, long cas, MutationToken mutationToken) {
        return new EntityDocument<T>(id, expiry, content, cas, mutationToken);
    }

}
