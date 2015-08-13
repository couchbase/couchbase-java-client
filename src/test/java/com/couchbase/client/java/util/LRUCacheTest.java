/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of the LRU cache.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public class LRUCacheTest {

    @Test
    public void shouldInsertData() {
        LRUCache<String, Integer> cache = new LRUCache<String, Integer>(10);

        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put("item1", 1);

        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());

        cache.put("item2", 2);

        assertFalse(cache.isEmpty());
        assertEquals(2, cache.size());
    }

    @Test
    public void shouldGetData() {
        LRUCache<String, Integer> cache = new LRUCache<String, Integer>(10);

        assertNull(cache.get("item1"));
        assertNull(cache.get("item2"));

        cache.put("item1", 1);
        cache.put("item2", 2);

        assertEquals((Integer) 1, cache.get("item1"));
        assertEquals((Integer) 2, cache.get("item2"));

        cache.remove("item1");
        cache.remove("item2");

        assertNull(cache.get("item1"));
        assertNull(cache.get("item2"));
    }

    @Test
    public void shouldEvictEldest() {
        LRUCache<String, Integer> cache = new LRUCache<String, Integer>(5);

        cache.put("item1", 1);
        cache.put("item2", 2);
        cache.put("item3", 3);
        cache.put("item4", 4);
        cache.put("item5", 5);

        assertNotNull(cache.get("item1"));
        assertNotNull(cache.get("item2"));
        assertNotNull(cache.get("item3"));
        assertNotNull(cache.get("item4"));
        assertNotNull(cache.get("item5"));

        cache.get("item3");
        cache.get("item1");

        cache.put("item6", 6);

        assertNotNull(cache.get("item1"));
        assertNotNull(cache.get("item3"));
        assertNotNull(cache.get("item4"));
        assertNotNull(cache.get("item5"));
        assertNotNull(cache.get("item6"));

        assertNull(cache.get("item2"));
    }

}