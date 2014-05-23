package com.couchbase.client.java.query;

/**
 * Created by michael on 21/05/14.
 */
public class Query {

    private String converted;

    private Query(String query) {
        this.converted = query;
    }

    public static Query raw(String query) {
        return new Query(query);
    }

    public String export() {
        return converted;
    }
}
