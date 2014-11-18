/**
 * Copyright (C) 2014 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java.document.json;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Verifies the functionality provided by a {@link JsonArray}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class JsonArrayTest {

    @Test
    public void shouldEqualBasedOnItsProperties() {
        JsonArray arr1 = JsonArray.create().add("foo").add("bar");
        JsonArray arr2 = JsonArray.create().add("foo").add("bar");
        assertEquals(arr1, arr2);

        arr1 = JsonArray.create().add("foo").add("baz");
        arr2 = JsonArray.create().add("foo").add("bar");
        assertNotEquals(arr1, arr2);

        arr1 = JsonArray.create().add("foo").add("bar").add("baz");
        arr2 = JsonArray.create().add("foo").add("bar");
        assertNotEquals(arr1, arr2);
    }

    @Test
    public void shouldConvertNumbers() {
        JsonArray arr = JsonArray.create().add(1L);

        assertEquals(new Double(1.0d), arr.getDouble(0));
        assertEquals(new Long(1L), arr.getLong(0));
        assertEquals(new Integer(1), arr.getInt(0));
    }

    @Test
    public void shouldConvertOverflowNumbers() {
        int maxValue = Integer.MAX_VALUE; //int max value is 2147483647
        long largeValue = maxValue + 3L;
        double largerThanIntMaxValue = largeValue + 0.56d;

        JsonArray arr = JsonArray.create().add(largerThanIntMaxValue);
        assertEquals(new Double(largerThanIntMaxValue), arr.getDouble(0));
        assertEquals(new Long(largeValue), arr.getLong(0));
        assertEquals(new Integer(maxValue), arr.getInt(0));
    }

}
