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

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.view.DesignDocument;
import rx.Observable;

/**
 * Provides management capabilities for a {@link AsyncBucket}.
 *
 * Operations provided on the {@link BucketManager} can be used to perform administrative tasks which require
 * bucket-level credentials like managing {@link DesignDocument}s or flushing a {@link Bucket}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface AsyncBucketManager {

    /**
     * Returns information about the connected bucket.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @return bucket information wrapped in a {@link BucketInfo}.
     */
    Observable<BucketInfo> info();

    /**
     * Flushes the bucket (removes all data).
     *
     * Note that flush needs to be enabled on the bucket, otherwise an exception will be raised.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.FlushDisabledException: If flush is disabled.
     * - com.couchbase.client.core.CouchbaseException: If the server response could not be parsed.
     *
     * @return true if the bucket was flushed, an failed {@link Observable} otherwise.
     */
    Observable<Boolean> flush();

    /**
     * Loads all published {@link DesignDocument}s.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @return zero to N invocations containing published {@link DesignDocument}s.
     */
    Observable<DesignDocument> getDesignDocuments();

    /**
     * Loads all {@link DesignDocument}s from development or production.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param development if the {@link DesignDocument}s should be loaded from development or production.
     * @return zero to N invocations containing published {@link DesignDocument}s.
     */
    Observable<DesignDocument> getDesignDocuments(boolean development);

    /**
     * Loads a published {@link DesignDocument} by its name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the {@link DesignDocument}.
     * @return and empty observable if the document not found or a {@link DesignDocument}.
     */
    Observable<DesignDocument> getDesignDocument(String name);

    /**
     * Loads a {@link DesignDocument} by its name from either development or production.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if it should be loaded from development or production.
     * @return and empty observable if the document not found or a {@link DesignDocument}.
     */
    Observable<DesignDocument> getDesignDocument(String name, boolean development);

    /**
     * Inserts a {@link DesignDocument} into production if it does not exist.
     *
     * Note that inserting a {@link DesignDocument} is not an atomic operation, but instead internally performs a
     * {@link #getDesignDocument(String)} operation first. While expected to be very uncommon, a race condition may
     * happen if two users at the same time perform this operation with the same {@link DesignDocument}.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} exists.
     *
     * @param designDocument the {@link DesignDocument} to insert.
     * @return the inserted {@link DesignDocument} on success.
     */
    Observable<DesignDocument> insertDesignDocument(DesignDocument designDocument);

    /**
     * Inserts a {@link DesignDocument} into development or production if it does not exist.
     *
     * Note that inserting a {@link DesignDocument} is not an atomic operation, but instead internally performs a
     * {@link #getDesignDocument(String)} operation first. While expected to be very uncommon, a race condition may
     * happen if two users at the same time perform this operation with the same {@link DesignDocument}.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} exists.
     *
     * @param designDocument the {@link DesignDocument} to insert.
     * @param development if it should be inserted into development or production (published).
     * @return the inserted {@link DesignDocument} on success.
     */
    Observable<DesignDocument> insertDesignDocument(DesignDocument designDocument, boolean development);

    /**
     * Upserts (inserts or replaces) a {@link DesignDocument} into production.
     *
     * If you want to add or update view definitions to an existing design document, you need to make sure you have
     * all the views (including old ones) in the DesignDocument. Use {@link #getDesignDocument(String)} to get the
     * old list and add your new view to it before calling this method.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param designDocument the {@link DesignDocument} to upsert.
     * @return the upserted {@link DesignDocument} on success.
     */
    Observable<DesignDocument> upsertDesignDocument(DesignDocument designDocument);

    /**
     * Upserts (inserts or replaces) a {@link DesignDocument} into production or development.
     *
     * If you want to add or update view definitions to an existing design document, you need to make sure you have
     * all the views (including old ones) in the DesignDocument. Use {@link #getDesignDocument(String)} to get the
     * old list and add your new view to it before calling this method.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param designDocument the {@link DesignDocument} to upsert.
     * @param development if the {@link DesignDocument} should be upserted into development or production.
     * @return the upserted {@link DesignDocument} on success.
     */
    Observable<DesignDocument> upsertDesignDocument(DesignDocument designDocument, boolean development);

    /**
     * Removes a {@link DesignDocument} from production by its name.
     *
     * @param name the name of the {@link DesignDocument}.
     * @return true if succeeded, false otherwise.
     */
    Observable<Boolean> removeDesignDocument(String name);

    /**
     * Removes a {@link DesignDocument} from production or development by its name.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if the {@link DesignDocument} should be removed from development or production.
     * @return true if succeeded, false otherwise.
     */
    Observable<Boolean> removeDesignDocument(String name, boolean development);

    /**
     * Publishes a {@link DesignDocument} from development into production.
     *
     * Note that this method does not override a already existing {@link DesignDocument}
     * (see {@link #publishDesignDocument(String, boolean)}) as an alternative.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} already
     *   exists.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the  {@link DesignDocument} to publish.
     * @return the published {@link DesignDocument} on success.
     */
    Observable<DesignDocument> publishDesignDocument(String name);

    /**
     * Publishes a {@link DesignDocument} from development into production.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} already
     *   exists and override is set to false.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the  {@link DesignDocument} to publish.
     * @param overwrite if an existing {@link DesignDocument} should be overridden.
     * @return the published {@link DesignDocument} on success.
     */
    Observable<DesignDocument> publishDesignDocument(String name, boolean overwrite);
}
