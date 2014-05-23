package com.couchbase.client.java.query;

/**
 * Created by michael on 05/05/14.
 */
public class ViewResult {

    private final String id;
    private final String key;
    private final Object value;

    public ViewResult(String id, String key, Object value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public String id() {
        return id;
    }

    public String key() {
        return key;
    }

    public Object value() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ViewRow{");
        sb.append("id='").append(id).append('\'');
        sb.append(", key='").append(key).append('\'');
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
