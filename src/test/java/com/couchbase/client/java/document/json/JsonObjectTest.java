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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies the functionality provided by a {@link JsonObject}.
 *
 * @author Michael Nitschinger
 * @author Simon Baslé
 * @since 2.0
 */
public class JsonObjectTest {

    @Test
    public void shouldExportEmptyObject() {
        String result = JsonObject.empty().toString();
        assertEquals("{}", result);
    }

    @Test
    public void shouldExportStrings() {
        String result = JsonObject.empty().put("key", "value").toString();
        assertEquals("{\"key\":\"value\"}", result);
    }

    @Test
    public void shouldExportNestedObjects() {
        JsonObject obj = JsonObject.empty()
            .put("nested", JsonObject.empty().put("a", true));
        assertEquals("{\"nested\":{\"a\":true}}", obj.toString());
    }

    @Test
    public void shouldExportNestedArrays() {
        JsonObject obj = JsonObject.empty()
            .put("nested", JsonArray.empty().add(true).add(4).add("foo"));
        assertEquals("{\"nested\":[true,4,\"foo\"]}", obj.toString());
    }

    @Test
    public void shouldReturnNullWhenNotFound() {
        JsonObject obj = JsonObject.empty();
        assertNull(obj.getInt("notfound"));
    }

    @Test
    public void shouldEqualBasedOnItsProperties() {
        JsonObject obj1 = JsonObject.create().put("foo", "bar");
        JsonObject obj2 = JsonObject.create().put("foo", "bar");
        assertEquals(obj1, obj2);

        obj1 = JsonObject.create().put("foo", "baz");
        obj2 = JsonObject.create().put("foo", "bar");
        assertNotEquals(obj1, obj2);

        obj1 = JsonObject.create().put("foo", "bar").put("bar", "baz");
        obj2 = JsonObject.create().put("foo", "bar");
        assertNotEquals(obj1, obj2);
    }

    @Test
    public void shouldConvertNumbers() {
        JsonObject obj = JsonObject.create().put("number", 1L);

        assertEquals(new Double(1.0d), obj.getDouble("number"));
        assertEquals(new Long(1L), obj.getLong("number"));
        assertEquals(new Integer(1), obj.getInt("number"));
    }

    @Test
    public void shouldConvertOverflowNumbers() {
        int maxValue = Integer.MAX_VALUE; //int max value is 2147483647
        long largeValue = maxValue + 3L;
        double largerThanIntMaxValue = largeValue + 0.56d;

        JsonObject obj = JsonObject.create().put("number", largerThanIntMaxValue);
        assertEquals(new Double(largerThanIntMaxValue), obj.getDouble("number"));
        assertEquals(new Long(largeValue), obj.getLong("number"));
        assertEquals(new Integer(maxValue), obj.getInt("number"));
    }

    @Test
    public void shouldNotNullPointerOnGetNumber() {
        JsonObject obj = JsonObject.empty();

        assertNull(obj.getDouble("number"));
        assertNull(obj.getLong("number"));
        assertNull(obj.getInt("number"));
    }

    @Test
    public void shouldConstructEmptyObjectFromEmptyMap() {
        JsonObject obj = JsonObject.from(Collections.<String, Object>emptyMap());
        assertNotNull(obj);
        assertTrue(obj.isEmpty());
    }


    @Test(expected = NullPointerException.class)
    public void shouldNullPointerOnNullMap() {
        JsonObject.from(null);
    }

    @Test
    public void shouldConstructJsonObjectFromMap() {
        String item1 = "item1";
        Double item2 = 2.2d;
        Long item3 = 3L;
        Boolean item4 = true;
        JsonArray item5 = JsonArray.empty();
        JsonObject item6 = JsonObject.empty();
        Map<String, Object> source = new HashMap<String, Object>(6);
        source.put("key1", item1);
        source.put("key2", item2);
        source.put("key3", item3);
        source.put("key4", item4);
        source.put("key5", item5);
        source.put("key6", item6);

        JsonObject obj = JsonObject.from(source);
        assertNotNull(obj);
        assertEquals(6, obj.size());
        assertEquals(item1, obj.get("key1"));
        assertEquals(item2, obj.get("key2"));
        assertEquals(item3, obj.get("key3"));
        assertEquals(item4, obj.get("key4"));
        assertEquals(item5, obj.get("key5"));
        assertEquals(item6, obj.get("key6"));
    }

    @Test(expected = NullPointerException.class)
    public void shouldDetectNullKeyInMap() {
        Map<String, Double> badMap = new HashMap<String, Double>(2);
        badMap.put("key1", 1.1d);
        badMap.put(null, 2.2d);
        JsonObject.from(badMap);
    }

    @Test
    public void shouldAcceptNullValueInMap() {
        Map<String, Long> badMap = new HashMap<String, Long>(2);
        badMap.put("key1", 1L);
        badMap.put("key2", null);

        JsonObject obj = JsonObject.from(badMap);
        assertNotNull(obj);
        assertEquals(2, obj.size());
        assertNotNull(obj.get("key1"));
        assertNull(obj.get("key2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDetectIncorrectItemInMap() {
        Object badItem = new CloneNotSupportedException();
        Map<String, Object> badMap = new HashMap<String, Object>(1);
        badMap.put("key1", badItem);
        JsonObject.from(badMap);
    }

    @Test
    public void shouldRecursivelyParseMaps() {
        Map<String, Double> subMap = new HashMap<String, Double>(2);
        subMap.put("value1", 1.2d);
        subMap.put("value2", 3.4d);
        Map<String, Object> recurseMap = new HashMap<String, Object>(2);
        recurseMap.put("key1", "test");
        recurseMap.put("key2", subMap);

        JsonObject obj = JsonObject.from(recurseMap);
        assertNotNull(obj);
        assertEquals(2, obj.size());
        assertEquals("test", obj.getString("key1"));

        assertNotNull(obj.get("key2"));
        assertEquals(JsonObject.class, obj.get("key2").getClass());
        assertEquals(2, obj.getObject("key2").size());
    }

    @Test
    public void shouldRecursivelyParseLists() {
        List<Double> subList = new ArrayList<Double>(2);
        subList.add(1.2d);
        subList.add(3.4d);
        Map<String, Object> recurseMap = new HashMap<String, Object>(2);
        recurseMap.put("key1", "test");
        recurseMap.put("key2", subList);

        JsonObject obj = JsonObject.from(recurseMap);
        assertNotNull(obj);
        assertEquals(2, obj.size());
        assertEquals("test", obj.getString("key1"));

        assertNotNull(obj.get("key2"));
        assertEquals(JsonArray.class, obj.get("key2").getClass());
        assertEquals(2, obj.getArray("key2").size());
    }

    @Test
    public void shouldClassCastOnBadSubMap() {
        Map<Integer, String> badMap1 = Collections.singletonMap(1, "test");
        Map<String, Object> badMap2 = Collections.singletonMap("key1", (Object) new CloneNotSupportedException());

        Map<String, Object> sourceMap = new HashMap<String, Object>();
        sourceMap.put("subMap", badMap1);
        try {
            JsonObject.from(sourceMap);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
            if (e.getCause() != null) {
                fail("No cause expected for sub map that are not Map<String, ?>");
            }
        } catch (Exception e) {
            fail("ClassCastException expected, not " + e);
        }

        sourceMap.clear();
        sourceMap.put("subMap", badMap2);
        try {
            JsonObject.from(sourceMap);
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
        Map<String, ?> source = Collections.singletonMap("test", badSubList);
        try {
            JsonObject.from(source);
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
        JsonObject obj = JsonObject.create();
        obj.put("directNull", JsonValue.NULL);
        obj.put("subMapWithNull", JsonObject.from(
                Collections.singletonMap("subNull", JsonValue.NULL)));
        obj.put("subArrayWithNull", JsonArray.from(
                Collections.singletonList(JsonValue.NULL)));

        assertTrue(obj.containsKey("directNull"));
        assertNull(obj.get("directNull"));

        assertNotNull(obj.getObject("subMapWithNull"));
        assertTrue(obj.getObject("subMapWithNull").containsKey("subNull"));
        assertNull(obj.getObject("subMapWithNull").get("subNull"));

        assertNotNull(obj.getArray("subArrayWithNull"));
        assertNull(obj.getArray("subArrayWithNull").get(0));
    }

    @Test
    public void shouldPutMapAsAJsonObject() {
        Map<String, Object> map = new HashMap<String, Object>(2);
        map.put("item1", "value1");
        map.put("item2", true);
        JsonObject obj = JsonObject.create().put("sub", map);

        assertTrue(obj.containsKey("sub"));
        assertNotNull(obj.get("sub"));
        assertTrue(obj.get("sub") instanceof JsonObject);
        assertTrue(obj.getObject("sub").containsKey("item1"));
        assertTrue(obj.getObject("sub").containsKey("item2"));
    }

    @Test
    public void shouldPutListAsAJsonArray() {
        List<Object> list = new ArrayList<Object>(2);
        list.add("value1");
        list.add(true);
        JsonObject obj = JsonObject.create().put("sub", list);

        assertTrue(obj.containsKey("sub"));
        assertNotNull(obj.get("sub"));
        assertTrue(obj.get("sub") instanceof JsonArray);
        assertEquals(2, obj.getArray("sub").size());
        assertEquals("value1", obj.getArray("sub").get(0));
        assertEquals(Boolean.TRUE, obj.getArray("sub").get(1));
    }
}