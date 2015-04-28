package com.couchbase.client.java.document;

public class EntityDocument<T> implements Document<T> {

    private String id;
    private long cas;
    private int expiry;
    private T content;

    private EntityDocument(String id, int expiry, T content, long cas) {
        if (expiry < 0) {
            throw new IllegalArgumentException("The Document expiry must not be negative.");
        }

        this.id = id;
        this.expiry = expiry;
        this.content = content;
        this.cas = cas;
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
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + "{");
        sb.append("id='").append(id).append('\'');
        sb.append(", cas=").append(cas);
        sb.append(", expiry=").append(expiry);
        sb.append(", content=").append(content);
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
        return !(content != null ? !content.equals(that.content) : that.content != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (int) (cas ^ (cas >>> 32));
        result = 31 * result + expiry;
        result = 31 * result + (content != null ? content.hashCode() : 0);
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
        return new EntityDocument<T>(id, expiry, content, cas);
    }

}
