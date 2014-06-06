package com.couchbase.client.java.query.dsl.path;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface SelectResultPath extends OrderByPath {

    SelectPath union();

    SelectPath unionAll();
}
