package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.query.dsl.Expression;

/**
 * Filters resulting rows based on the given expression.
 *
 * The where condition is evaluated for each resulting row, and only rows evaluating true are retained. All
 * method overloads which do not take an {@link Expression} will be converted to one internally.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public interface WherePath extends GroupByPath {

    /**
     * Filter resulting rows based on the given expression.
     *
     * @param expression the filter expression.
     * @return the next possible steps.
     */
    GroupByPath where(Expression expression);

    /**
     * Filter resulting rows based on the given expression.
     *
     * The given string will be converted into an expression internally.
     *
     * @param expression the filter expression.
     * @return the next possible steps.
     */
    GroupByPath where(String expression);

}
