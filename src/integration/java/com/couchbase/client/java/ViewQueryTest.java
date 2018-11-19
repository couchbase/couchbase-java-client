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
package com.couchbase.client.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DesignDocumentDoesNotExistException;
import com.couchbase.client.java.error.ViewDoesNotExistException;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.AsyncViewRow;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.Stale;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

/**
 * Runs end-to-end {@link ViewQuery}s and verifies their output.
 *
 * @author Michael Nitschinger
 * @since 2.0.1
 */
public class ViewQueryTest {

    public static final int STORED_DOCS = 1000;

    private static CouchbaseTestContext ctx;

    /**
     * Populates th bucket with sample data and creates views for testing.
     */
    @BeforeClass
    public static void setupViews() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
            .adhoc(true)
            .bucketQuota(100)
            .bucketName("View")
            .build();

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
                    return ctx.bucket().async().insert(JsonDocument.create("user-" + id, content));
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

        try {
            DesignDocument stored = ctx.bucketManager().getDesignDocument("users");
            if (!stored.equals(designDoc)) {
                ctx.bucketManager().upsertDesignDocument(designDoc);
            }
        } catch (DesignDocumentDoesNotExistException ex) {
            ctx.bucketManager().upsertDesignDocument(designDoc);
        }
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void shouldQueryNonReducedView() {
        ViewResult result = ctx.bucket().query(ViewQuery.from("users", "by_name").stale(Stale.FALSE));
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
        ViewResult result = ctx.bucket().query(ViewQuery.from("users", "by_name").stale(Stale.FALSE));
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
        ViewResult result = ctx.bucket().query(ViewQuery.from("users", "by_age").stale(Stale.FALSE));
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(0, result.totalRows());

        List<ViewRow> rows = result.allRows();
        assertEquals(1, rows.size());
    }

    @Test
    public void shouldManuallyDisabledReduce() {
        ViewResult result = ctx.bucket().query(ViewQuery.from("users", "by_age").stale(Stale.FALSE).reduce(false), 3, TimeUnit.SECONDS);
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
        ViewResult result = ctx.bucket().query(ViewQuery.from("users", "by_name").key("foobar").stale(Stale.FALSE));
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(result.totalRows(), STORED_DOCS);

        assertEquals(0, result.allRows().size());
    }

    @Test
    public void shouldLoadDocumentsWithMapOnly() {
        ViewResult result = ctx.bucket().query(ViewQuery.from("users", "by_name").limit(10).stale(Stale.FALSE));
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
    public void shouldLoadDocumentsWithIncludeDocs() {
        ViewResult result = ctx.bucket().query(
            ViewQuery.from("users", "by_name").limit(10).stale(Stale.FALSE).includeDocs()
        );
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
    public void shouldIncludeDocsWithCustomTarget() {
        ViewResult result = ctx.bucket().query(
            ViewQuery.from("users", "by_name").limit(20).stale(Stale.FALSE).includeDocs(RawJsonDocument.class)
        );
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
            RawJsonDocument doc = row.document(RawJsonDocument.class);
            assertNotNull(doc.content());
            assertFalse(doc.content().isEmpty());
        }

        assertEquals(20, count);
    }

    @Test(expected = ClassCastException.class)
    public void shouldFailWhenWrongCustomTargetOnIncludeDocs() {
        ViewResult result = ctx.bucket().query(
            ViewQuery.from("users", "by_name").limit(20).stale(Stale.FALSE).includeDocs(RawJsonDocument.class)
        );
        assertNull(result.debug());
        assertNull(result.error());
        assertTrue(result.success());
        assertEquals(result.totalRows(), STORED_DOCS);

        Iterator<ViewRow> rows = result.rows();
        while(rows.hasNext()) {
            ViewRow row = rows.next();

            assertNotNull(row);
            row.document();
        }
    }

    @Test
    public void shouldComposeAsyncWithDocuments() {
        List<JsonDocument> documents = ctx.bucket()
            .async()
            .query(ViewQuery.from("users", "by_name").limit(50).stale(Stale.FALSE))
            .flatMap(new Func1<AsyncViewResult, Observable<AsyncViewRow>>() {
                @Override
                public Observable<AsyncViewRow> call(AsyncViewResult result) {
                    return result.error().flatMap(new Func1<JsonObject, Observable<? extends AsyncViewRow>>() {
                        @Override
                        public Observable<? extends AsyncViewRow> call(JsonObject e) {
                            return Observable.error(new CouchbaseException(e.toString()));
                        }
                    }).switchIfEmpty(result.rows());
                }
            })
            .flatMap(new Func1<AsyncViewRow, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(AsyncViewRow row) {
                    return row.document();
                }
            })
            .toList()
            .timeout(10, TimeUnit.SECONDS)
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
        ctx.bucket().query(ViewQuery.from("users", "foobar"));
    }

    @Test(expected = ViewDoesNotExistException.class)
    public void shouldFailWithInvalidDesignDocument() {
        ctx.bucket().query(ViewQuery.from("foo", "bar"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldFailToLoadDocumentWhenReduced() {
        ViewResult result = ctx.bucket().query(ViewQuery.from("users", "by_age").stale(Stale.FALSE));
        List<ViewRow> rows = result.allRows();

        assertEquals(1, rows.size());
        ViewRow row = rows.get(0);
        assertNull(row.id());
        assertNull(row.key());
        assertEquals(1000, row.value());

        row.document();
    }

    @Test
    public void shouldSucceedWithLargeKeysArray() throws IOException {
        InputStream ras = this.getClass().getResourceAsStream("/data/view/key_many.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(ras));
        String[] keys = reader.readLine().split(",");
        reader.close();
        JsonArray keysArray = JsonArray.from((Object[]) keys);

        ViewResult result = ctx.bucket().query(
                ViewQuery.from("users", "by_name")
                         .keys(keysArray)
        );

        assertTrue(result.success());
        assertNull(result.error());
    }

}
