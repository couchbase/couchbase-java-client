/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.document.json;

import com.couchbase.client.java.SerializationHelper;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

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

    @Test
    public void shouldExportEscapedJsonValue() {
        JsonArray arr = JsonArray.create().add("\"\b\r\n\f\t\\/");
        String escaped = "\\\"\\b\\r\\n\\f\\t\\\\/";
        assertEquals("[\"" + escaped + "\"]", arr.toString());
    }

    @Test
    public void shouldExportEscapedControlCharInValue() {
        JsonArray arr = JsonArray.create().add("\u001F");
        String escaped = "\\u001F";
        assertEquals("[\"" + escaped + "\"]", arr.toString());
    }

    @Test
    public void shouldConvertSubJsonValuesToCollections() {
        JsonObject sub1 = JsonObject.create().put("sub1.1", "test");
        JsonArray sub2 = JsonArray.create().add("sub2.1");
        JsonArray arr = JsonArray.create()
                                   .add(sub1)
                                   .add(sub2);

        List<Object> asList = arr.toList();
        Object mSub1 = asList.get(0);
        Object mSub2 = asList.get(1);

        assertNotNull(mSub1);
        assertTrue(mSub1 instanceof Map);
        assertEquals("test", ((Map) mSub1).get("sub1.1"));

        assertNotNull(mSub2);
        assertTrue(mSub2 instanceof List);
        assertEquals("sub2.1", ((List) mSub2).get(0));
    }

    @Test
    public void shouldThrowIfAddingToSelf() {
        JsonArray arr = JsonArray.create();
        try {
            arr.add(arr);
            fail();
        } catch (IllegalArgumentException e) {
            //success
        }
        try {
            arr.add((Object) arr);
            fail();
        } catch (IllegalArgumentException e) {
            //success
        }
    }

    @Test
    public void shouldConvertFromStringJson() {
        String someJson = "[ 123, \"value\", true, [ 123 ], { \"sub\": \"obj\" }]";
        JsonArray expected = JsonArray.create()
                                        .add(123)
                                        .add("value")
                                        .add(true)
                                        .add(JsonArray.from(123))
                                        .add(JsonObject.create().put("sub", "obj"));

        JsonArray converted = JsonArray.fromJson(someJson);

        assertEquals(expected, converted);
    }

    @Test
    public void shouldConvertNullStringToNull() {
        assertNull(JsonArray.fromJson("null"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToConvertBadJsonString() {
        String badJson = "This is not \"JSON\"!";
        JsonArray.fromJson(badJson);
    }

    @Test
    public void shouldFailToConvertNonArrayJson() {
        String bad1 = "true";
        String bad2 = "123";
        String bad3 = "\"string\"";
        String bad4 = "{\"some\": \"value\"}";

        try { JsonArray.fromJson(bad1); fail(); } catch (IllegalArgumentException e) { }
        try { JsonArray.fromJson(bad2); fail(); } catch (IllegalArgumentException e) { }
        try { JsonArray.fromJson(bad3); fail(); } catch (IllegalArgumentException e) { }
        try { JsonArray.fromJson(bad4); fail(); } catch (IllegalArgumentException e) { }
    }

    @Test
    public void shouldSupportSerialization() throws Exception {
        JsonArray original = JsonArray.from("a", "b", JsonObject.create().put("foo", "bar"));

        byte[] serialized = SerializationHelper.serializeToBytes(original);
        assertNotNull(serialized);

        JsonArray deserialized = SerializationHelper.deserializeFromBytes(serialized, JsonArray.class);
        assertEquals(original, deserialized);
    }

    @Test
    public void shouldSupportBigInteger() throws Exception {
        BigInteger bigint = new BigInteger("12345678901234567890");
        JsonArray original = JsonArray.from(bigint);


        String encoded = original.toString();
        assertEquals("[12345678901234567890]", encoded);

        JsonArray decoded = JsonArray.fromJson(encoded);
        assertEquals(bigint, decoded.getBigInteger(0));
        assertTrue(decoded.getNumber(0) instanceof BigInteger);

    }

    @Test
    public void shouldSupportBigDecimalConverted() throws Exception {
        assumeFalse(CouchbaseTestContext.isCi());

        BigDecimal bigdec = new BigDecimal("1234.5678901234567890432423432324");
        JsonArray original = JsonArray.from(bigdec);


        String encoded = original.toString();
        assertEquals("[1234.5678901234567890432423432324]", encoded);

        JsonArray decoded = JsonArray.fromJson(encoded);
        // This happens because of double rounding, set com.couchbase.json.decimalForFloat true for better accuracy
        // but more overhead
        assertEquals(
            new BigDecimal("1234.5678901234568911604583263397216796875"), decoded.getBigDecimal(0)
        );
        assertEquals(1234.567890123457, decoded.getDouble(0), 0);
        assertTrue(decoded.getNumber(0) instanceof Double);
    }
}
