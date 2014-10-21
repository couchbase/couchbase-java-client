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

import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.error.DesignDocumentAlreadyExistsException;
import com.couchbase.client.java.error.DesignDocumentException;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.View;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Verifies the functionality of the Design Document management facilities.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class DesignDocumentTest extends ClusterDependentTest {

    private BucketManager manager;

    @Before
    public void setup() {
        manager = bucket().bucketManager();

        List<DesignDocument> designDocuments = manager.getDesignDocuments();
        for (DesignDocument ddoc : designDocuments) {
            manager.removeDesignDocument(ddoc.name());
        }
    }

    @Test
    public void shouldInsertDesignDocument() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}", "_count"));
        DesignDocument designDocument = DesignDocument.create("insert1", views);
        manager.insertDesignDocument(designDocument);

        DesignDocument found = manager.getDesignDocument("insert1");
        assertNotNull(found);
        assertEquals("insert1", found.name());
        assertEquals(1, found.views().size());
        assertEquals("function(d,m){}", found.views().get(0).map());
        assertEquals("_count", found.views().get(0).reduce());
    }

    @Test(expected = DesignDocumentException.class)
    public void shouldFailOnInvalidMapFunction() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "notValid"));
        DesignDocument designDocument = DesignDocument.create("invalidInsert", views);
        manager.insertDesignDocument(designDocument);
    }

    @Test(expected = DesignDocumentAlreadyExistsException.class)
    public void shouldNotDoubleInsertDesignDocument() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}", "_count"));
        DesignDocument designDocument = DesignDocument.create("insert2", views);
        manager.insertDesignDocument(designDocument);

        DesignDocument found = manager.getDesignDocument("insert2");
        assertNotNull(found);
        assertEquals("insert2", found.name());
        assertEquals(1, found.views().size());
        assertEquals("function(d,m){}", found.views().get(0).map());
        assertEquals("_count", found.views().get(0).reduce());

        manager.insertDesignDocument(designDocument);
    }

    @Test
    public void shouldUpsertDesignDocument() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}"));
        DesignDocument designDocument = DesignDocument.create("upsert1", views);
        manager.upsertDesignDocument(designDocument);

        DesignDocument found = manager.getDesignDocument("upsert1");
        assertNotNull(found);
        assertEquals("upsert1", found.name());
        assertEquals(1, found.views().size());
        assertEquals("function(d,m){}", found.views().get(0).map());
        assertNull(found.views().get(0).reduce());
    }

    @Test
    public void shouldDoubleUpsertDesignDocument() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}"));
        DesignDocument designDocument = DesignDocument.create("upsert2", views);
        manager.upsertDesignDocument(designDocument);

        DesignDocument found = manager.getDesignDocument("upsert2");
        assertNotNull(found);
        assertEquals("upsert2", found.name());
        assertEquals(1, found.views().size());
        assertEquals("function(d,m){}", found.views().get(0).map());
        assertNull(found.views().get(0).reduce());

        views = Arrays.asList(
            DefaultView.create("v1", "function(d,m){}", "_count"),
            DefaultView.create("v2", "function(d,m){}", "_count")
        );
        designDocument = DesignDocument.create("upsert2", views);
        manager.upsertDesignDocument(designDocument);

        found = manager.getDesignDocument("upsert2");
        assertNotNull(found);
        assertEquals("upsert2", found.name());
        assertEquals(2, found.views().size());
        assertEquals("function(d,m){}", found.views().get(0).map());
        assertEquals("_count", found.views().get(0).reduce());
        assertEquals("function(d,m){}", found.views().get(1).map());
        assertEquals("_count", found.views().get(1).reduce());
    }

    @Test
    public void shouldGetDesignDocuments() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}"));
        DesignDocument designDocument1 = DesignDocument.create("doc1", views);
        DesignDocument designDocument2 = DesignDocument.create("doc3", views);
        DesignDocument designDocument3 = DesignDocument.create("doc2", views);

        manager.upsertDesignDocument(designDocument1);
        manager.upsertDesignDocument(designDocument2);
        manager.upsertDesignDocument(designDocument3);

        List<DesignDocument> docs = manager.getDesignDocuments();
        assertTrue(docs.size() >= 3);
        int found = 0;
        for (DesignDocument doc : docs) {
            if (doc.name().equals("doc1") || doc.name().equals("doc2") || doc.name().equals("doc3")) {
                found++;
            }
        }
        assertEquals(3, found);
    }

    @Test
    public void shouldRemoveDesignDocument() {
        List<View> views = Arrays.asList(
            DefaultView.create("v1", "function(d,m){}"),
            DefaultView.create("v2", "function(d,m){}", "_count")
        );

        DesignDocument designDocument = DesignDocument.create("remove1", views);
        manager.upsertDesignDocument(designDocument);

        DesignDocument found = manager.getDesignDocument("remove1");
        assertNotNull(found);
        assertEquals("remove1", found.name());
        assertEquals(2, found.views().size());

        assertTrue(manager.removeDesignDocument("remove1"));

        found = manager.getDesignDocument("remove1");
        assertNull(found);
    }

    @Test
    public void shouldPublishDesignDocument() {
        List<View> views = Arrays.asList(
            DefaultView.create("v1", "function(d,m){}"),
            DefaultView.create("v2", "function(d,m){}", "_count")
        );

        DesignDocument designDocument = DesignDocument.create("pub1", views);
        manager.upsertDesignDocument(designDocument, true);

        DesignDocument found = manager.getDesignDocument("pub1");
        assertNull(found);

        manager.publishDesignDocument("pub1");

        found = manager.getDesignDocument("pub1");
        assertNotNull(found);
        assertEquals("pub1", found.name());
        assertEquals(2, found.views().size());
    }

    @Test(expected = DesignDocumentAlreadyExistsException.class)
    public void shouldNotOverrideOnPublish() {
        List<View> views = Arrays.asList(
            DefaultView.create("v1", "function(d,m){}"),
            DefaultView.create("v2", "function(d,m){}", "_count")
        );

        DesignDocument designDocument = DesignDocument.create("pub2", views);
        manager.upsertDesignDocument(designDocument, true);

        DesignDocument found = manager.getDesignDocument("pub2");
        assertNull(found);

        manager.publishDesignDocument("pub2");
        manager.publishDesignDocument("pub2");
    }

}
