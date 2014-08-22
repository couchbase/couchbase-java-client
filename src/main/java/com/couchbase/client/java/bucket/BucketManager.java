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
package com.couchbase.client.java.bucket;

import com.couchbase.client.java.view.DesignDocument;
import rx.Observable;

/**
 * Represents management APIs for a bucket.
 *
 * Most common operations involve CRUD operations for design documents and flushing a bucket.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public interface BucketManager {

    /**
     * Returns information about the connected bucket.
     *
     * @return bucket information.
     */
    Observable<BucketInfo> info();

    /**
     * Flushes the bucket if enabled.
     *
     * @return true if the bucket was flushed, false otherwise.
     */
    Observable<Boolean> flush();

    /**
     * Loads all design documents from production.
     *
     * @return all design documents that are published to production.
     */
    Observable<DesignDocument> getDesignDocuments();

    /**
     * Loads all design documents from either production or development.
     *
     * @param development if development environment should be used instead.
     * @return all design documents from either production or development.
     */
    Observable<DesignDocument> getDesignDocuments(boolean development);

    /**
     * Load a single design document from production, identified by its name.
     *
     * @param name the name of the design document.
     * @return a design document if found, an empty observable if not found.
     */
    Observable<DesignDocument> getDesignDocument(String name);

    /**
     * Load a single design document from development or production, identified by its name.
     *
     * @param name the name of the design document.
     * @return a design document if found, an empty observable if not found.
     */
    Observable<DesignDocument> getDesignDocument(String name, boolean development);

    /**
     * Insert a design document into production.
     *
     * @param designDocument the design document to insert.
     * @return the inserted design document.
     */
    Observable<DesignDocument> insertDesignDocument(DesignDocument designDocument);

    /**
     * Insert a design document into production or development.
     *
     * @param designDocument the design document to insert.
     * @return the inserted design document.
     */
    Observable<DesignDocument> insertDesignDocument(DesignDocument designDocument, boolean development);

    /**
     * Upsert a design document into production.
     *
     * @param designDocument the design document to upsert.
     * @return the inserted design document.
     */
    Observable<DesignDocument> upsertDesignDocument(DesignDocument designDocument);

    /**
     * Upsert a design document into production or development.
     *
     * @param designDocument the design document to insert.
     * @return the inserted design document.
     */
    Observable<DesignDocument> upsertDesignDocument(DesignDocument designDocument, boolean development);

    /**
     * Remove a design document from production.
     *
     * @param name the name of the design document.
     * @return the inserted design document.
     */
    Observable<Boolean> removeDesignDocument(String name);

    /**
     * Remove a design document from production or development.
     *
     * @param name the name of the design document.
     * @return the inserted design document.
     */
    Observable<Boolean> removeDesignDocument(String name, boolean development);

    /**
     * Publish a design document from development to production, but do not overwrite if it
     * already exists.
     *
     * Throws a DesignDocumentAlreadyExistsException if it does already exist.
     *
     * @param name the name of the design document.
     * @return the published design document.
     */
    Observable<DesignDocument> publishDesignDocument(String name);

    /**
     * Publish a design document from development to production, and optionally override it.
     *
     * Throws a DesignDocumentAlreadyExistsException if it does already exist and overwriting is disabled.
     *
     * @param name the name of the design document.
     * @return the published design document.
     */
    Observable<DesignDocument> publishDesignDocument(String name, boolean overwrite);
}
