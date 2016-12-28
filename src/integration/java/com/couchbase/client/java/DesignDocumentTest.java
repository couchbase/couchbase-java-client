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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.error.DesignDocumentAlreadyExistsException;
import com.couchbase.client.java.error.DesignDocumentDoesNotExistException;
import com.couchbase.client.java.error.DesignDocumentException;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.SpatialView;
import com.couchbase.client.java.view.View;
import org.junit.Before;
import org.junit.Test;

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
    public void shouldHaveLessViewsWhenUpsertingWithOnlyNewViews() {
        List<View> views = Collections.singletonList(DefaultView.create("v1", "function(d,m){}"));
        DesignDocument designDocument = DesignDocument.create("upsert3", views);
        manager.upsertDesignDocument(designDocument);

        DesignDocument found = manager.getDesignDocument("upsert3");
        assertNotNull(found);
        assertEquals("upsert3", found.name());
        assertEquals(1, found.views().size());
        assertEquals("v1", found.views().get(0).name());
        assertNull(found.views().get(0).reduce());

        views = Collections.singletonList(DefaultView.create("v2", "function(d,m){}", "_count"));
        designDocument = DesignDocument.create("upsert3", views);
        manager.upsertDesignDocument(designDocument);

        found = manager.getDesignDocument("upsert3");
        assertNotNull(found);
        assertEquals("upsert3", found.name());
        assertEquals(1, found.views().size());
        assertEquals("v2", found.views().get(0).name());
        assertEquals("_count", found.views().get(0).reduce());
    }

    @Test
    public void shouldUseLatestDefinitionWhenAddingViewNameTwice() {
        List<View> views = new ArrayList<View>();
        views.add(DefaultView.create("v1", "function(d,m){}"));
        DesignDocument designDocument = DesignDocument.create("upsert4", views);
        designDocument.views().add(DefaultView.create("v1", "function(d,m){emit(null,null);}", "_count"));
        manager.upsertDesignDocument(designDocument);

        DesignDocument found = manager.getDesignDocument("upsert4");
        assertNotNull(found);
        assertEquals("upsert4", found.name());
        assertEquals(1, found.views().size());
        assertEquals("v1", found.views().get(0).name());
        assertEquals("function(d,m){emit(null,null);}", found.views().get(0).map());
        assertEquals("_count", found.views().get(0).reduce());

        //make also sure that the getDesignDocument views list is mutable
        found.views().add(DefaultView.create("v1", "function(d,m){emit(d.type, null);}", null));
        manager.upsertDesignDocument(found);

        found = manager.getDesignDocument("upsert4");
        assertNotNull(found);
        assertEquals("upsert4", found.name());
        assertEquals(1, found.views().size());
        assertEquals("v1", found.views().get(0).name());
        assertEquals("function(d,m){emit(d.type, null);}", found.views().get(0).map());
        assertNull(found.views().get(0).reduce());
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

    @Test(expected = DesignDocumentDoesNotExistException.class)
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

        manager.getDesignDocument("remove1");
    }

    @Test
    public void shouldPublishDesignDocument() {
        List<View> views = Arrays.asList(
            DefaultView.create("v1", "function(d,m){}"),
            DefaultView.create("v2", "function(d,m){}", "_count")
        );

        DesignDocument designDocument = DesignDocument.create("pub1", views);
        manager.upsertDesignDocument(designDocument, true);

        manager.publishDesignDocument("pub1");
        DesignDocument found = manager.getDesignDocument("pub1");
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
        manager.publishDesignDocument("pub2");
        manager.publishDesignDocument("pub2");
    }

    @Test
    public void shouldInsertAndGetWithSpatial() {
        List<View> views = Arrays.asList(
            SpatialView.create("geo", "function (doc) {if(doc.type == \"city\") { emit([doc.lon, doc.lat], null);}}")
        );

        DesignDocument designDocument = DesignDocument.create("withSpatial", views);
        manager.upsertDesignDocument(designDocument);

        DesignDocument found = manager.getDesignDocument("withSpatial");
        assertEquals(1, found.views().size());
        View view = found.views().get(0);
        assertTrue(view instanceof SpatialView);
        assertEquals("geo", view.name());
        assertNotNull(view.map());
        assertNull(view.reduce());
        assertFalse(view.hasReduce());
    }

    @Test
    public void shouldCreateAndLoadDesignDocumentWithOptions() {
        List<View> views = Arrays.asList(DefaultView.create("vOpts", "function(d,m){}", "_count"));
        Map<DesignDocument.Option, Long> options = new HashMap<DesignDocument.Option, Long>();
        options.put(DesignDocument.Option.UPDATE_MIN_CHANGES, 100L);
        options.put(DesignDocument.Option.REPLICA_UPDATE_MIN_CHANGES, 5000L);

        DesignDocument designDocument = DesignDocument.create("upsertWithOpts", views, options);
        manager.upsertDesignDocument(designDocument);

        DesignDocument loaded = manager.getDesignDocument("upsertWithOpts");
        assertEquals((Long) 100L, loaded.options().get(DesignDocument.Option.UPDATE_MIN_CHANGES));
        assertEquals((Long) 5000L, loaded.options().get(DesignDocument.Option.REPLICA_UPDATE_MIN_CHANGES));
    }

}
