package com.couchbase.client.java.query.dsl;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * Factory class for query functions.
 *
 * @author Michael Nitschinger
 */
public class Functions {

    public static Expression meta() {
        return x("META()");
    }

    public static Expression round(Expression expression) {
        return x("ROUND(" + expression.toString() + ")");
    }

    public static Expression round(Expression expression, int digits) {
        return x("ROUND(" + expression.toString() + ", " + digits + ")");
    }

    public static Expression length(Expression expression) {
        return x("LENGTH(" + expression.toString() + ")");
    }
}
