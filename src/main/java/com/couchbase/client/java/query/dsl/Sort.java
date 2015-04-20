package com.couchbase.client.java.query.dsl;

import static com.couchbase.client.java.query.dsl.Expression.x;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class Sort {

    private final Expression expression;
    private final Order ordering;

    private Sort(final Expression expression, final Order ordering) {
        this.expression = expression;
        this.ordering = ordering;
    }

    /**
     * Use default sort, don't specify an order in the resulting expression.
     */
    public static Sort def(final Expression expression) {
        return new Sort(expression, null);
    }

    /**
     * Use default sort, don't specify an order in the resulting expression.
     */
    public static Sort def(final String expression) {
        return def(x(expression));
    }

    public static Sort desc(final Expression expression) {
        return new Sort(expression, Order.DESC);
    }

    public static Sort desc(final String expression) {
        return desc(x(expression));
    }

    public static Sort asc(final Expression expression) {
        return new Sort(expression, Order.ASC);
    }

    public static Sort asc(final String expression) {
        return asc(x(expression));
    }

    @Override
    public String toString() {
        if (ordering != null) {
            return expression.toString() + " " + ordering;
        } else {
            return expression.toString();
        }
    }

    public static enum Order  {
        ASC,
        DESC
    }
}
