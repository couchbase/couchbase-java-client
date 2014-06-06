package com.couchbase.client.java.query.dsl;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class Alias {

    private final String alias;
    private final Expression original;

    private Alias(String alias, Expression original) {
        this.alias = alias;
        this.original = original;
    }

    public static Alias alias(String alias, Expression original) {
        return new Alias(alias, original);
    }

    public static Alias alias(String alias, String original) {
        return new Alias(alias, x(original));
    }

    @Override
    public String toString() {
        return alias + " = " + original.toString();
    }
}
