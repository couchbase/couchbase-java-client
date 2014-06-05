package com.couchbase.client.java.query;

import org.junit.Test;

import static com.couchbase.client.java.query.Expression.x;
import static org.junit.Assert.assertEquals;

/**
 * Verifies the functionality of a {@link Query}.
 *
 * @author Michael Nitschinger
 * @since 1.0
 */
public class QueryTest {

    @Test
    public void testSelectWithStrings() {
        Query query = Query.select("firstname", "lastname");
        assertEquals("SELECT firstname, lastname", query.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithEmptySelect() {
        Query.select();
    }

    @Test
    public void testFromWithString() {
        Query query = Query.select("firstname").from("default");
        assertEquals("SELECT firstname FROM default", query.toString());
    }

    @Test
    public void testWhere() {
        Query query = Query.select("*").from("default").where(x("age").gt(x("25")));
        assertEquals("SELECT * FROM default WHERE age > 25", query.toString());
    }

}
