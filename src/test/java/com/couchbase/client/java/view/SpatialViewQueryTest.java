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
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Verifies the functionality of a {@link SpatialViewQuery}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class SpatialViewQueryTest {

    @Test
    public void shouldSetDefaults() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view");
        assertEquals("design", query.getDesign());
        assertEquals("view", query.getView());
        assertFalse(query.isDevelopment());
        assertTrue(query.toString().isEmpty());
    }

    @Test
    public void shouldSetStartRange() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").startRange(JsonArray.from(5.87, 47.27, 1000));
        assertEquals("start_range=[5.87,47.27,1000]", query.toString());
    }

    @Test
    public void shouldSetEndRange() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").endRange(JsonArray.from(15.04, 55.06, null));
        assertEquals("end_range=[15.04,55.06,null]", query.toString());
    }

    @Test
    public void shouldSetRange() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view")
            .range(JsonArray.from(null, null, 1000), JsonArray.from(null, null, 2000));
        assertEquals("start_range=[null,null,1000]&end_range=[null,null,2000]", query.toString());
    }

    @Test
    public void shouldLimit() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").limit(10);
        assertEquals("limit=10", query.toString());
    }

    @Test
    public void shouldSkip() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").skip(3);
        assertEquals("skip=3", query.toString());
    }

    @Test
    public void shouldSetStale() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").stale(Stale.FALSE);
        assertEquals("stale=false", query.toString());

        query = SpatialViewQuery.from("design", "view").stale(Stale.TRUE);
        assertEquals("stale=ok", query.toString());

        query = SpatialViewQuery.from("design", "view").stale(Stale.UPDATE_AFTER);
        assertEquals("stale=update_after", query.toString());
    }

    @Test
    public void shouldSetOnError() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").onError(OnError.CONTINUE);
        assertEquals("on_error=continue", query.toString());

        query = SpatialViewQuery.from("design", "view").onError(OnError.STOP);
        assertEquals("on_error=stop", query.toString());

    }

    @Test
    public void shouldSetDebug() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").debug();
        assertEquals("debug=true", query.toString());

        query = SpatialViewQuery.from("design", "view").debug(false);
        assertEquals("debug=false", query.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeLimit() {
        SpatialViewQuery.from("design", "view").limit(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeSkip() {
        SpatialViewQuery.from("design", "view").skip(-1);
    }

    @Test
    public void shouldToggleDevelopment() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").development(true);
        assertTrue(query.isDevelopment());

        query = SpatialViewQuery.from("design", "view").development(false);
        assertFalse(query.isDevelopment());
    }

    @Test
    public void shouldSupportSerialization() throws Exception {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view")
            .debug()
            .development();

        byte[] serialized = SerializationHelper.serializeToBytes(query);
        assertNotNull(serialized);

        SpatialViewQuery deserialized = SerializationHelper.deserializeFromBytes(serialized, SpatialViewQuery.class);
        assertEquals(query, deserialized);
    }

    @Test
    public void shouldIncludeDocs() {
        SpatialViewQuery query = SpatialViewQuery.from("design", "view").includeDocs();
        assertTrue(query.isIncludeDocs());
        assertEquals(JsonDocument.class, query.includeDocsTarget());

        query = SpatialViewQuery.from("design", "view").includeDocs(JsonDocument.class);
        assertTrue(query.isIncludeDocs());
        assertEquals(JsonDocument.class, query.includeDocsTarget());

        query = SpatialViewQuery.from("design", "view");
        assertFalse(query.isIncludeDocs());
        assertNull(query.includeDocsTarget());

        query = SpatialViewQuery.from("design", "view").includeDocs(false, RawJsonDocument.class);
        assertFalse(query.isIncludeDocs());
        assertEquals(RawJsonDocument.class, query.includeDocsTarget());
    }
}