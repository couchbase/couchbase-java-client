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
package com.couchbase.client.java.view;

import com.couchbase.client.java.SerializationHelper;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies the correct functionality of the {@link ViewQuery} DSL.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class ViewQueryTest {

    @Test
    public void shouldSetDefaults() {
        ViewQuery query = ViewQuery.from("design", "view");
        assertEquals("design", query.getDesign());
        assertEquals("view", query.getView());
        assertFalse(query.isDevelopment());
        assertTrue(query.toString().isEmpty());
    }

    @Test
    public void shouldReduce() {
        ViewQuery query = ViewQuery.from("design", "view").reduce();
        assertEquals("reduce=true", query.toString());

        query = ViewQuery.from("design", "view").reduce(true);
        assertEquals("reduce=true", query.toString());

        query = ViewQuery.from("design", "view").reduce(false);
        assertEquals("reduce=false", query.toString());
    }

    @Test
    public void shouldLimit() {
        ViewQuery query = ViewQuery.from("design", "view").limit(10);
        assertEquals("limit=10", query.toString());
    }

    @Test
    public void shouldSkip() {
        ViewQuery query = ViewQuery.from("design", "view").skip(3);
        assertEquals("skip=3", query.toString());
    }

    @Test
    public void shouldGroup() {
        ViewQuery query = ViewQuery.from("design", "view").group();
        assertEquals("group=true", query.toString());

        query = ViewQuery.from("design", "view").group(false);
        assertEquals("group=false", query.toString());
    }

    @Test
    public void shouldGroupLevel() {
        ViewQuery query = ViewQuery.from("design", "view").groupLevel(2);
        assertEquals("group_level=2", query.toString());
    }

    @Test
    public void shouldSetInclusiveEnd() {
        ViewQuery query = ViewQuery.from("design", "view").inclusiveEnd();
        assertEquals("inclusive_end=true", query.toString());

        query = ViewQuery.from("design", "view").inclusiveEnd(false);
        assertEquals("inclusive_end=false", query.toString());
    }

    @Test
    public void shouldSetStale() {
        ViewQuery query = ViewQuery.from("design", "view").stale(Stale.FALSE);
        assertEquals("stale=false", query.toString());

        query = ViewQuery.from("design", "view").stale(Stale.TRUE);
        assertEquals("stale=ok", query.toString());

        query = ViewQuery.from("design", "view").stale(Stale.UPDATE_AFTER);
        assertEquals("stale=update_after", query.toString());
    }

    @Test
    public void shouldSetOnError() {
        ViewQuery query = ViewQuery.from("design", "view").onError(OnError.CONTINUE);
        assertEquals("on_error=continue", query.toString());

        query = ViewQuery.from("design", "view").onError(OnError.STOP);
        assertEquals("on_error=stop", query.toString());

    }

    @Test
    public void shouldSetDebug() {
        ViewQuery query = ViewQuery.from("design", "view").debug();
        assertEquals("debug=true", query.toString());

        query = ViewQuery.from("design", "view").debug(false);
        assertEquals("debug=false", query.toString());
    }

    @Test
    public void shouldSetDescending() {
        ViewQuery query = ViewQuery.from("design", "view").descending();
        assertEquals("descending=true", query.toString());

        query = ViewQuery.from("design", "view").descending(false);
        assertEquals("descending=false", query.toString());
    }

    @Test
    public void shouldHandleKey() {
        ViewQuery query = ViewQuery.from("design", "view").key("key");
        assertEquals("key=%22key%22", query.toString());

        query = ViewQuery.from("design", "view").key(1);
        assertEquals("key=1", query.toString());

        query = ViewQuery.from("design", "view").key(true);
        assertEquals("key=true", query.toString());

        query = ViewQuery.from("design", "view").key(3.55);
        assertEquals("key=3.55", query.toString());

        query = ViewQuery.from("design", "view").key(JsonArray.from("foo", 3));
        assertEquals("key=%5B%22foo%22%2C3%5D", query.toString());

        query = ViewQuery.from("design", "view").key(JsonObject.empty().put("foo", true));
        assertEquals("key=%7B%22foo%22%3Atrue%7D", query.toString());
    }

    @Test
    public void shouldHandleKeys() {
        ViewQuery query = ViewQuery.from("design", "view").keys(JsonArray.from("foo", 3, true));
        assertEquals("keys=%5B%22foo%22%2C3%2Ctrue%5D", query.toString());
    }

    @Test
    public void shouldHandleStartKey() {
        ViewQuery query = ViewQuery.from("design", "view").startKey("key");
        assertEquals("startkey=%22key%22", query.toString());

        query = ViewQuery.from("design", "view").startKey(1);
        assertEquals("startkey=1", query.toString());

        query = ViewQuery.from("design", "view").startKey(true);
        assertEquals("startkey=true", query.toString());

        query = ViewQuery.from("design", "view").startKey(3.55);
        assertEquals("startkey=3.55", query.toString());

        query = ViewQuery.from("design", "view").startKey(JsonArray.from("foo", 3));
        assertEquals("startkey=%5B%22foo%22%2C3%5D", query.toString());

        query = ViewQuery.from("design", "view").startKey(JsonObject.empty().put("foo", true));
        assertEquals("startkey=%7B%22foo%22%3Atrue%7D", query.toString());
    }

    @Test
    public void shouldHandleEndKey() {
        ViewQuery query = ViewQuery.from("design", "view").endKey("key");
        assertEquals("endkey=%22key%22", query.toString());

        query = ViewQuery.from("design", "view").endKey(1);
        assertEquals("endkey=1", query.toString());

        query = ViewQuery.from("design", "view").endKey(true);
        assertEquals("endkey=true", query.toString());

        query = ViewQuery.from("design", "view").endKey(3.55);
        assertEquals("endkey=3.55", query.toString());

        query = ViewQuery.from("design", "view").endKey(JsonArray.from("foo", 3));
        assertEquals("endkey=%5B%22foo%22%2C3%5D", query.toString());

        query = ViewQuery.from("design", "view").endKey(JsonObject.empty().put("foo", true));
        assertEquals("endkey=%7B%22foo%22%3Atrue%7D", query.toString());
    }

    @Test
    public void shouldHandleStartKeyDocID() {
        ViewQuery query = ViewQuery.from("design", "view").startKeyDocId("mykey");
        assertEquals("startkey_docid=mykey", query.toString());
    }

    @Test
    public void shouldHandleEndKeyDocID() {
        ViewQuery query = ViewQuery.from("design", "view").endKeyDocId("mykey");
        assertEquals("endkey_docid=mykey", query.toString());
    }

    @Test
    public void shouldRespectDevelopmentParam() {
        ViewQuery query = ViewQuery.from("design", "view").development(true);
        assertTrue(query.isDevelopment());

        query = ViewQuery.from("design", "view").development(false);
        assertFalse(query.isDevelopment());
    }

    @Test
    public void shouldConcatMoreParams() {
        ViewQuery query = ViewQuery.from("design", "view")
            .descending()
            .debug()
            .development()
            .group()
            .reduce(false)
            .startKey(JsonArray.from("foo", true));
        assertEquals("reduce=false&group=true&debug=true&descending=true&startkey=%5B%22foo%22%2Ctrue%5D",
            query.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeLimit() {
        ViewQuery.from("design", "view").limit(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeSkip() {
        ViewQuery.from("design", "view").skip(-1);
    }

    @Test
    public void shouldToggleDevelopment() {
        ViewQuery query = ViewQuery.from("design", "view").development(true);
        assertTrue(query.isDevelopment());

        query = ViewQuery.from("design", "view").development(false);
        assertFalse(query.isDevelopment());
    }

    @Test
    public void shouldSupportSerialization() throws Exception {
        ViewQuery query = ViewQuery.from("design", "view")
            .descending()
            .debug()
            .development()
            .group()
            .reduce(false)
            .startKey(JsonArray.from("foo", true));

        byte[] serialized = SerializationHelper.serializeToBytes(query);
        assertNotNull(serialized);

        ViewQuery deserialized = SerializationHelper.deserializeFromBytes(serialized, ViewQuery.class);
        assertEquals(query, deserialized);
    }

}