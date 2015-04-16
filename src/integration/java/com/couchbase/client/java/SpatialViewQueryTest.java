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
package com.couchbase.client.java;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.util.features.Version;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.SpatialView;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.SpatialViewResult;
import com.couchbase.client.java.view.SpatialViewRow;
import com.couchbase.client.java.view.Stale;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of {@link SpatialViewQuery}.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class SpatialViewQueryTest extends ClusterDependentTest {

    private static final JsonArray EUROPE_START = JsonArray.from(-10.8, 36.59);
    private static final JsonArray EUROPE_END = JsonArray.from(31.6, 70.67);

    private static final JsonObject[] FIXTURES = new JsonObject[] {
        JsonObject.create().put("name", "Vienna").put("lon", 16.36962890625).put("lat", 48.21094727794909),
        JsonObject.create().put("name", "Berlin").put("lon", 13.3978271484375).put("lat", 52.51622086393074),
        JsonObject.create().put("name", "Paris").put("lon", 2.373046875).put("lat", 48.864714761802794),
        JsonObject.create().put("name", "New York").put("lon", -73.970947265625).put("lat", 40.75557964275591),
        JsonObject.create().put("name", "San Francisco").put("lon", -122.47009277343749).put("lat", 37.76202988573211)
    };

    /**
     * Populates th bucket with sample data and creates views for testing.
     */
    @BeforeClass
    public static void setupSpatialViews() {
        ignoreIfClusterUnder(new Version(3,0,2));

        Observable
            .from(FIXTURES)
            .flatMap(new Func1<JsonObject, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(JsonObject content) {
                    String id = "city::" + content.getString("name");
                    content.put("type", "city");
                    return bucket().async().upsert(JsonDocument.create(id, content));
                }
            })
            .last()
            .toBlocking()
            .single();

        DesignDocument designDoc = DesignDocument.create("cities", Arrays.asList(
            SpatialView.create("by_location", "function (doc) { if (doc.type == \"city\") { emit([doc.lon, doc.lat], "
                + "null); } }"),
            SpatialView.create("by_geojson", "function (doc) { if (doc.type == \"city\") { emit({ \"type\": \"Point\","
                + " \"coordinates\":[doc.lon, doc.lat] }, null); } }")
        ));

        DesignDocument stored = bucketManager().getDesignDocument("cities");
        if (stored == null || !stored.equals(designDoc)) {
            bucketManager().upsertDesignDocument(designDoc);
        }
    }

    @Test
    public void shouldQuerySpatial() {
        SpatialViewResult result = bucket().query(SpatialViewQuery.from("cities", "by_location").stale(Stale.FALSE));
        List<SpatialViewRow> allRows = result.allRows();
        assertEquals(FIXTURES.length, allRows.size());

        for (SpatialViewRow row : allRows) {
            assertNotNull(row.id());
            assertEquals(2, row.key().size());
            assertNull(row.geometry());
            assertNull(row.value());
        }
    }

    @Test
    public void shouldQuerySpatialFromGeoJSON() {
        SpatialViewResult result = bucket().query(SpatialViewQuery.from("cities", "by_geojson").stale(Stale.FALSE));
        List<SpatialViewRow> allRows = result.allRows();
        assertEquals(FIXTURES.length, allRows.size());

        for (SpatialViewRow row : allRows) {
            assertNotNull(row.id());
            assertEquals(2, row.key().size());
            assertNotNull(row.geometry());
            assertNull(row.value());
            assertEquals("Point", row.geometry().getString("type"));
        }
    }

    @Test
    public void shouldQueryWithLimit() {
        SpatialViewResult result = bucket().query(SpatialViewQuery.from("cities", "by_geojson")
            .stale(Stale.FALSE)
            .limit(3));
        List<SpatialViewRow> allRows = result.allRows();

        assertEquals(3, allRows.size());
        for (SpatialViewRow row : allRows) {
            assertNotNull(row.id());
            assertEquals(2, row.key().size());
            assertNotNull(row.geometry());
            assertNull(row.value());
            assertEquals("Point", row.geometry().getString("type"));
        }
    }

    @Test
    public void shouldQueryInRange() {
        SpatialViewResult result = bucket().query(SpatialViewQuery.from("cities", "by_location")
            .stale(Stale.FALSE)
            .range(EUROPE_START, EUROPE_END));
        List<SpatialViewRow> allRows = result.allRows();

        assertEquals(3, allRows.size());
        for (SpatialViewRow row : allRows) {
            assertTrue(row.id().matches("city::(Vienna|Berlin|Paris)"));
            assertNull(row.value());
            assertNull(row.geometry());
        }
    }

    @Test
    public void shouldQueryWithIncludeDocs() {
        SpatialViewResult result = bucket().query(
            SpatialViewQuery.from("cities", "by_location").stale(Stale.FALSE).includeDocs()
        );
        List<SpatialViewRow> allRows = result.allRows();
        assertEquals(FIXTURES.length, allRows.size());

        for (SpatialViewRow row : allRows) {
            assertNotNull(row.document().content());
        }
    }

}
