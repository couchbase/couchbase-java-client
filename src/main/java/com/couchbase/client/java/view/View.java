package com.couchbase.client.java.view;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public interface View {

    String name();

    String map();

    String reduce();

    boolean hasReduce();

}
