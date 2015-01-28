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
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.ViewDoesNotExistException;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.view.*;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Runs end-to-end {@link ViewQuery}s and verifies their output.
 *
 * @author Michael Nitschinger
 * @since 2.0.1
 */
public class ViewQueryTest extends ClusterDependentTest {

    public static final int STORED_DOCS = 1000;

    /**
     * Populates th bucket with sample data and creates views for testing.
     */
    @BeforeClass
    public static void setupViews() {
        Observable
            .range(1, STORED_DOCS)
            .flatMap(new Func1<Integer, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(Integer id) {
                    JsonObject content = JsonObject.create()
                        .put("type", "user")
                        .put("name", "Mr. Foo Bar " + id)
                        .put("age", id % 100)
                        .put("active", (id % 2) == 0);
                    return bucket().async().insert(JsonDocument.create("user-" + id, content));
                }
            })
            .last()
            .toBlocking()
            .single();


        DesignDocument designDoc = DesignDocument.create(
            "users",
            Arrays.asList(
                DefaultView.create("by_name", "function (doc, meta) { if (doc.type == \"user\") " +
                    "{ emit(doc.name, null); } }"),
                DefaultView.create("by_age", "function (doc, meta) { if (doc.type == \"user\") " +
                    "{ emit(doc.age, null); } }", "_count")
            )
        );

        DesignDocument stored = bucketManager().getDesignDocument("users");
        if (stored == null || !stored.equals(designDoc)) {
            bucketManager().upsertDesignDocument(designDoc);
        }
    }

    @Test
    public void shouldQueryNonReducedView() {
        ViewResult result = bucket().query(ViewQuery.from("users", "by_name").stale(Stale.FALSE));
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(result.totalRows(), STORED_DOCS);

        int foundRows = 0;
        Iterator<ViewRow> rows = result.rows();
        while(rows.hasNext()) {
            ViewRow row = rows.next();
            assertNull(row.value());
            assertNotNull(row.id());
            assertNotNull(row.key());
            foundRows++;
        }
        assertEquals(STORED_DOCS, foundRows);
    }

    @Test
    public void shouldQueryViewWithIterator() {
        ViewResult result = bucket().query(ViewQuery.from("users", "by_name").stale(Stale.FALSE));
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(result.totalRows(), STORED_DOCS);

        int foundRows = 0;
        for (ViewRow row : result) {
            assertNull(row.value());
            assertNotNull(row.id());
            assertNotNull(row.key());
            foundRows++;
        }
        assertEquals(STORED_DOCS, foundRows);
    }

    @Test
    public void shouldQueryReducedView() {
        ViewResult result = bucket().query(ViewQuery.from("users", "by_age").stale(Stale.FALSE));
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(0, result.totalRows());

        List<ViewRow> rows = result.allRows();
        System.out.println(rows);
    }

    @Test
    public void shouldManuallyDisabledReduce() {
        ViewResult result = bucket().query(ViewQuery.from("users", "by_age").stale(Stale.FALSE).reduce(false));
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(result.totalRows(), STORED_DOCS);

        int foundRows = 0;
        Iterator<ViewRow> rows = result.rows();
        while(rows.hasNext()) {
            ViewRow row = rows.next();
            assertNull(row.value());
            assertNotNull(row.id());
            assertNotNull(row.key());
            foundRows++;
        }
        assertEquals(STORED_DOCS, foundRows);
    }

    @Test
    public void shouldReturnNoRowsWithNonMatchingQuery() {
        ViewResult result = bucket().query(ViewQuery.from("users", "by_name").key("foobar").stale(Stale.FALSE));
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(result.totalRows(), STORED_DOCS);

        assertEquals(0, result.allRows().size());
    }

    @Test
    public void shouldLoadDocumentsWithMapOnly() {
        ViewResult result = bucket().query(ViewQuery.from("users", "by_name").limit(10).stale(Stale.FALSE));
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(result.totalRows(), STORED_DOCS);

        int count = 0;
        Iterator<ViewRow> rows = result.rows();
        while(rows.hasNext()) {
            count++;
            ViewRow row = rows.next();

            assertNotNull(row);
            JsonDocument doc = row.document();
            assertTrue(doc.id().startsWith("user-"));
            assertTrue(doc.cas() != 0);
            assertTrue(doc.expiry() == 0);

            assertTrue(doc.content().getString("name").startsWith("Mr. Foo Bar"));
            assertTrue(doc.content().getString("type").equals("user"));
        }

        assertEquals(10, count);
    }

    @Test
    public void shouldComposeAsyncWithDocuments() {
        List<JsonDocument> documents = bucket()
            .async()
            .query(ViewQuery.from("users", "by_name").limit(50).stale(Stale.FALSE))
            .flatMap(new Func1<AsyncViewResult, Observable<AsyncViewRow>>() {
                @Override
                public Observable<AsyncViewRow> call(AsyncViewResult result) {
                    return result.rows();
                }
            })
            .flatMap(new Func1<AsyncViewRow, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(AsyncViewRow row) {
                    return row.document();
                }
            })
            .toList()
            .toBlocking()
            .single();

        assertEquals(50, documents.size());
        for (JsonDocument doc : documents) {
            assertTrue(doc.id().startsWith("user-"));
            assertTrue(doc.cas() != 0);
            assertTrue(doc.expiry() == 0);

            assertTrue(doc.content().getString("name").startsWith("Mr. Foo Bar"));
            assertTrue(doc.content().getString("type").equals("user"));
        }
    }

    @Test(expected = ViewDoesNotExistException.class)
    public void shouldFailWithInvalidViewName() {
        bucket().query(ViewQuery.from("users", "foobar"));
    }

    @Test(expected = ViewDoesNotExistException.class)
    public void shouldFailWithInvalidDesignDocument() {
        bucket().query(ViewQuery.from("foo", "bar"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldFailToLoadDocumentWhenReduced() {
        ViewResult result = bucket().query(ViewQuery.from("users", "by_age").stale(Stale.FALSE));
        List<ViewRow> rows = result.allRows();

        assertEquals(1, rows.size());
        ViewRow row = rows.get(0);
        assertNull(row.id());
        assertNull(row.key());
        assertEquals(1000, row.value());

        row.document();
    }

}
