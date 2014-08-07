package com.couchbase.client.java;

import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.view.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        manager = bucket().bucketManager().toBlocking().single();
    }

    @Test
    public void shouldInsertDesignDocument() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}", "_count"));
        DesignDocument designDocument = DesignDocument.create("insert1", views);
        manager.insertDesignDocument(designDocument).toBlocking().single();

        DesignDocument found = manager.getDesignDocument("insert1").toBlocking().singleOrDefault(null);
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
        manager.insertDesignDocument(designDocument).toBlocking().single();
    }

    @Test(expected = DesignDocumentAlreadyExistsException.class)
    public void shouldNotDoubleInsertDesignDocument() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}", "_count"));
        DesignDocument designDocument = DesignDocument.create("insert2", views);
        manager.insertDesignDocument(designDocument).toBlocking().single();

        DesignDocument found = manager.getDesignDocument("insert2").toBlocking().singleOrDefault(null);
        assertNotNull(found);
        assertEquals("insert2", found.name());
        assertEquals(1, found.views().size());
        assertEquals("function(d,m){}", found.views().get(0).map());
        assertEquals("_count", found.views().get(0).reduce());

        manager.insertDesignDocument(designDocument).toBlocking().single();
    }

    @Test
    public void shouldUpsertDesignDocument() {
        List<View> views = Arrays.asList(DefaultView.create("v1", "function(d,m){}"));
        DesignDocument designDocument = DesignDocument.create("upsert1", views);
        manager.upsertDesignDocument(designDocument).toBlocking().single();

        DesignDocument found = manager.getDesignDocument("upsert1").toBlocking().single();
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
        manager.upsertDesignDocument(designDocument).toBlocking().single();

        DesignDocument found = manager.getDesignDocument("upsert2").toBlocking().single();
        assertNotNull(found);
        assertEquals("upsert2", found.name());
        assertEquals(1, found.views().size());
        assertEquals("function(d,m){}", found.views().get(0).map());
        assertNull(found.views().get(0).reduce());

        views = Arrays.asList(
            DefaultView.create("v1", "function(d,m){}"),
            DefaultView.create("v2", "function(d,m){}", "_count")
        );
        designDocument = DesignDocument.create("upsert2", views);
        manager.upsertDesignDocument(designDocument).toBlocking().single();

        found = manager.getDesignDocument("upsert2").toBlocking().single();
        assertNotNull(found);
        assertEquals("upsert2", found.name());
        assertEquals(2, found.views().size());
        assertEquals("function(d,m){}", found.views().get(0).map());
        assertNull(found.views().get(0).reduce());
        assertEquals("function(d,m){}", found.views().get(1).map());
        assertEquals("_count", found.views().get(1).reduce());
    }

    @Test
    public void shouldGetDesignDocuments() {

    }

    @Test
    public void shouldRemoveDesignDocument() {

    }

    @Test
    public void shouldPublishDesignDocument() {

    }

    @Test
    public void shouldNotOverrideOnPublish() {

    }

}
