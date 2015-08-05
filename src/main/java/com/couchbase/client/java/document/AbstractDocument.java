/**
 * Copyright (C) 2014 Couchbase, Inc.
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

import com.couchbase.client.core.message.kv.MutationToken;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Common parent implementation of a {@link Document}.
 *
 * It is recommended that all {@link Document} implementations extend from this class so that parameter checks
 * are consistently applied. It also ensures that equals and hashcode are applied on the contents and therefore
 * comparisons work as expected.
 *
 * @author Michael Nitschinger
 * @since 2.0.0
 */
public abstract class AbstractDocument<T> implements Document<T> {

    private String id;
    private long cas;
    private int expiry;
    private T content;
    private MutationToken mutationToken;

    /**
     * Constructor needed for possible subclass serialization.
     */
    protected AbstractDocument() {
    }

    protected AbstractDocument(String id, int expiry, T content, long cas) {
        this(id, expiry, content, cas, null);
    }

    protected AbstractDocument(String id, int expiry, T content, long cas, MutationToken mutationToken) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("The Document ID must not be null or empty.");
        }
        if (id.getBytes().length > 250) {
            throw new IllegalArgumentException("The Document ID must not be larger than 250 bytes");
        }
        if (expiry < 0) {
            throw new IllegalArgumentException("The Document expiry must not be negative.");
        }

        this.id = id;
        this.cas = cas;
        this.expiry = expiry;
        this.content = content;
        this.mutationToken = mutationToken;
    }

    @Override
    public String id() {
        return id;
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
    public T content() {
        return content;
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

        AbstractDocument<?> that = (AbstractDocument<?>) o;

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

    /**
     * Helper method to write the current document state to the output stream for serialization purposes.
     *
     * @param stream the stream to write to.
     * @throws IOException
     */
    protected void writeToSerializedStream(ObjectOutputStream stream) throws IOException {
        stream.writeLong(cas);
        stream.writeInt(expiry);
        stream.writeUTF(id);
        stream.writeObject(content);
        stream.writeObject(mutationToken);
    }

    /**
     * Helper method to create the document from an object input stream, used for serialization purposes.
     *
     * @param stream the stream to read from.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    protected void readFromSerializedStream(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        cas = stream.readLong();
        expiry = stream.readInt();
        id = stream.readUTF();
        content = (T) stream.readObject();
        mutationToken = (MutationToken) stream.readObject();
    }
}
