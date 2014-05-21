package com.couchbase.client.java.query;

/**
 * Created by michael on 21/05/14.
 */
public class N1qlQuery {

    private String converted;

    private N1qlQuery(String query) {
        this.converted = query;
    }

    public static N1qlQuery create(String query) {
        return new N1qlQuery(query);
    }

    public String export() {
        return converted;
    }
}
