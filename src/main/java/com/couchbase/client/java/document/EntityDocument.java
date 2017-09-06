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
@InterfaceStability.Uncommitted
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
