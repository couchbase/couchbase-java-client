package com.couchbase.client.java.query;

import org.junit.Test;

import static com.couchbase.client.java.query.Expression.x;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ExpressionTest {

    @Test
    public void testGreaterThan() {
        Expression exp = x("firstname").gt(x("5"));
        assertEquals("firstname > 5", exp.toString());
    }

    @Test
    public void testGreaterThanWithAnd() {
        Expression exp = x("firstname").gt(x("5")).and(x("lastname").lt(x("4")));
        assertEquals("firstname > 5 AND lastname < 4 ", exp.toString());
    }
}