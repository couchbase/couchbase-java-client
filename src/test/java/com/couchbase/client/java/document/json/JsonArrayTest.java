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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies the functionality provided by a {@link JsonArray}.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
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


    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldNotNullPointerOnGetNumber() {
        JsonArray obj = JsonArray.empty();
        obj.get(0);
    }

    @Test
    public void shouldConstructEmptyArrayFromEmptyList() {
        JsonArray arr = JsonArray.from(Collections.emptyList());
        assertNotNull(arr);
        assertTrue(arr.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void shouldNullPointerOnNullList() {
        JsonArray.from((List) null);
    }

    @Test
    public void shouldConstructArrayFromList() {
        String item1 = "item1";
        Double item2 = 2.0d;
        Long item3 = 3L;
        Boolean item4 = true;
        JsonArray item5 = JsonArray.empty();
        JsonObject item6 = JsonObject.empty();

        JsonArray arr = JsonArray.from(Arrays.asList(item1, item2,
                item3, item4, item5, item6));

        assertEquals(6, arr.size());
        assertEquals(item1, arr.get(0));
        assertEquals(item2, arr.get(1));
        assertEquals(item3, arr.get(2));
        assertEquals(item4, arr.get(3));
        assertEquals(item5, arr.get(4));
        assertEquals(item6, arr.get(5));
    }

    @Test
    public void shouldAcceptNullItemInList() {
        JsonArray arr = JsonArray.from(Arrays.asList("item1", null, "item2"));
        assertNotNull(arr);
        assertEquals(3, arr.size());
        assertNotNull(arr.get(0));
        assertNotNull(arr.get(2));
        assertNull(arr.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDetectIncorrectItemInList() {
        Object badItem = new java.lang.CloneNotSupportedException();
        JsonArray arr = JsonArray.from(Arrays.asList("item1", "item2", badItem));
    }

    @Test
    public void shouldRecursiveParseList() {
        List<?> subList = Collections.singletonList("test");
        List<Object> source = new ArrayList<Object>(2);
        source.add("item1");
        source.add(subList);

        JsonArray arr = JsonArray.from(source);
        assertNotNull(arr);
        assertEquals(2, arr.size());
        assertEquals("item1", arr.getString(0));
        assertEquals(JsonArray.class, arr.get(1).getClass());
        assertEquals("test", arr.getArray(1).get(0));
    }

    @Test
    public void shouldRecursiveParseMap() {
        Map<String, ?> subMap = Collections.singletonMap("test", 2.5d);
        List<Object> source = new ArrayList<Object>(2);
        source.add("item1");
        source.add(subMap);

        JsonArray arr = JsonArray.from(source);
        assertNotNull(arr);
        assertEquals(2, arr.size());
        assertEquals("item1", arr.getString(0));
        assertEquals(JsonObject.class, arr.get(1).getClass());
        assertEquals(new Double(2.5d), arr.getObject(1).get("test"));
    }

    @Test
    public void shouldClassCastOnBadSubMap() {
        Map<Integer, String> badMap1 = Collections.singletonMap(1, "test");
        Map<String, Object> badMap2 = Collections.singletonMap("key1", (Object) new CloneNotSupportedException());

        List<?> source = Collections.singletonList(badMap1);
        try {
            JsonArray.from(source);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
            if (e.getCause() != null) {
                fail("No cause expected for sub map that are not Map<String, ?>");
            }
        } catch (Exception e) {
            fail("ClassCastException expected, not " + e);
        }

        source = Collections.singletonList(badMap2);
        try {
            JsonArray.from(source);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                fail("ClassCastException with an IllegalArgumentException cause expected");
            }
        } catch (Exception e) {
            fail("ClassCastException expected");
        }
    }

    @Test
    public void shouldClassCastWithCauseOnBadSubList() {
        List<?> badSubList = Collections.singletonList(new CloneNotSupportedException());
        List<?> source = Collections.singletonList(badSubList);
        try {
            JsonArray.from(source);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                fail("ClassCastException with an IllegalArgumentException cause expected");
            }
        } catch (Exception e) {
            fail("ClassCastException expected");
        }
    }


    @Test
    public void shouldTreatJsonValueNullConstantAsNull() {
        JsonArray arr = JsonArray.create();
        arr.add(JsonValue.NULL);
        arr.add(JsonObject.from(
                Collections.singletonMap("subNull", JsonValue.NULL)));
        arr.add(JsonArray.from(
                Collections.singletonList(JsonValue.NULL)));

        assertEquals(3, arr.size());
        assertNull(arr.get(0));
        assertNotNull(arr.getObject(1));
        assertTrue(arr.getObject(1).containsKey("subNull"));
        assertNull(arr.getObject(1).get("subNull"));

        assertNotNull(arr.getArray(2));
        assertNull(arr.getArray(2).get(0));
    }

    @Test
    public void shouldAddMapAsAJsonObject() {
        Map<String, Object> map = new HashMap<String, Object>(2);
        map.put("item1", "value1");
        map.put("item2", true);
        JsonArray arr = JsonArray.create().add(map);

        assertEquals(1, arr.size());
        assertNotNull(arr.get(0));
        assertTrue(arr.get(0) instanceof JsonObject);
        assertTrue(arr.getObject(0).containsKey("item1"));
        assertTrue(arr.getObject(0).containsKey("item2"));
    }

    @Test
    public void shouldAddListAsAJsonArray() {
        List<Object> list = new ArrayList<Object>(2);
        list.add("value1");
        list.add(true);
        JsonArray arr = JsonArray.create().add(list);

        assertEquals(1, arr.size());
        assertNotNull(arr.get(0));
        assertTrue(arr.get(0) instanceof JsonArray);
        assertEquals(2, arr.getArray(0).size());
        assertEquals("value1", arr.getArray(0).get(0));
        assertEquals(Boolean.TRUE, arr.getArray(0).get(1));
    }
}
