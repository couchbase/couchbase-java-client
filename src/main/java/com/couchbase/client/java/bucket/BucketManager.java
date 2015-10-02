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
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.view.DesignDocument;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides management capabilities for a {@link Bucket}.
 *
 * Operations provided on the {@link BucketManager} can be used to perform administrative tasks which require
 * bucket-level credentials like managing {@link DesignDocument}s or flushing a {@link Bucket}. Access to the
 * underlying {@link AsyncBucketManager} is provided through the {@link #async()} method.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface BucketManager {

    /**
     * Returns the underlying {@link AsyncBucketManager} for asynchronous execution.
     *
     * @return the underlying bucket manager.
     */
    AsyncBucketManager async();

    /**
     * Returns information about the connected bucket with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be decoded.
     *
     * @return bucket information wrapped in a {@link BucketInfo}.
     */
    BucketInfo info();

    /**
     * Returns information about the connected bucket with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return bucket information wrapped in a {@link BucketInfo}.
     */
    BucketInfo info(long timeout, TimeUnit timeUnit);

    /**
     * Flushes the bucket (removes all data) with the default management timeout.
     *
     * Note that flushing takes some time on the server to be performed properly, so do not set a too low timeout.
     * Also, flush needs to be enabled on the bucket, otherwise an exception will be raised.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.FlushDisabledException: If flush is disabled.
     * - com.couchbase.client.core.CouchbaseException: If the server response could not be parsed.
     *
     * @return true if the bucket was flushed, an exception thrown if otherwise.
     */
    Boolean flush();

    /**
     * Flushes the bucket (removes all data) with a custom timeout.
     *
     * Note that flushing takes some time on the server to be performed properly, so do not set a too low timeout.
     * Also, flush needs to be enabled on the bucket, otherwise an exception will be raised.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.FlushDisabledException: If flush is disabled.
     * - com.couchbase.client.core.CouchbaseException: If the server response could not be parsed.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return true if the bucket was flushed, an exception thrown if otherwise.
     */
    Boolean flush(long timeout, TimeUnit timeUnit);

    /**
     * Loads all published {@link DesignDocument}s with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @return a potentially empty list containing published {@link DesignDocument}s.
     */
    List<DesignDocument> getDesignDocuments();

    /**
     * Loads all published {@link DesignDocument}s with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return a potentially empty list containing published {@link DesignDocument}s.
     */
    List<DesignDocument> getDesignDocuments(long timeout, TimeUnit timeUnit);

    /**
     * Loads all {@link DesignDocument}s from either development or production with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param development if {@link DesignDocument}s should be loaded from development or from production.
     * @return a potentially empty list containing published {@link DesignDocument}s.
     */
    List<DesignDocument> getDesignDocuments(boolean development);

    /**
     * Loads all {@link DesignDocument}s from either development or production with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param development if {@link DesignDocument}s should be loaded from development or from production.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return a potentially empty list containing published {@link DesignDocument}s.
     */
    List<DesignDocument> getDesignDocuments(boolean development, long timeout, TimeUnit timeUnit);

    /**
     * Loads a published {@link DesignDocument} by its name with the default management timeout.
     *
     * If the {@link DesignDocument} is not found, null is returned.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the {@link DesignDocument}.
     * @return null if the document not found or a {@link DesignDocument}.
     */
    DesignDocument getDesignDocument(String name);

    /**
     * Loads a published {@link DesignDocument} by its name with the a custom timeout.
     *
     * If the {@link DesignDocument} is not found, null is returned.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return null if the document not found or a {@link DesignDocument}.
     */
    DesignDocument getDesignDocument(String name, long timeout, TimeUnit timeUnit);

    /**
     * Loads a {@link DesignDocument} by its name from either development or production with the default management
     * timeout.
     *
     * If the {@link DesignDocument} is not found, null is returned.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if {@link DesignDocument} should be loaded from development or from production.
     * @return null if the document not found or a {@link DesignDocument}.
     */
    DesignDocument getDesignDocument(String name, boolean development);

    /**
     * Loads a {@link DesignDocument}s by its name from either development or production with a custom timeout.
     *
     * If the {@link DesignDocument} is not found, null is returned.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if {@link DesignDocument} should be loaded from development or from production.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return null if the document not found or a {@link DesignDocument}.
     */
    DesignDocument getDesignDocument(String name, boolean development, long timeout, TimeUnit timeUnit);

    /**
     * Inserts a {@link DesignDocument} into production if it does not exist with the default management timeout.
     *
     * Note that inserting a {@link DesignDocument} is not an atomic operation, but instead internally performs a
     * {@link #getDesignDocument(String)} operation first. While expected to be very uncommon, a race condition may
     * happen if two users at the same time perform this operation with the same {@link DesignDocument}.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} exists.
     *
     * @param designDocument the {@link DesignDocument} to insert.
     * @return the inserted {@link DesignDocument} on success.
     */
    DesignDocument insertDesignDocument(DesignDocument designDocument);

    /**
     * Inserts a {@link DesignDocument} into production if it does not exist with a custom timeout.
     *
     * Note that inserting a {@link DesignDocument} is not an atomic operation, but instead internally performs a
     * {@link #getDesignDocument(String)} operation first. While expected to be very uncommon, a race condition may
     * happen if two users at the same time perform this operation with the same {@link DesignDocument}.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} exists.
     *
     * @param designDocument the {@link DesignDocument} to insert.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the inserted {@link DesignDocument} on success.
     */
    DesignDocument insertDesignDocument(DesignDocument designDocument, long timeout, TimeUnit timeUnit);

    /**
     * Inserts a {@link DesignDocument} into development or production if it does not exist with the default
     * management timeout.
     *
     * Note that inserting a {@link DesignDocument} is not an atomic operation, but instead internally performs a
     * {@link #getDesignDocument(String)} operation first. While expected to be very uncommon, a race condition may
     * happen if two users at the same time perform this operation with the same {@link DesignDocument}.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} exists.
     *
     * @param designDocument the {@link DesignDocument} to insert.
     * @param development if it should be inserted into development or production (published).
     * @return the inserted {@link DesignDocument} on success.
     */
    DesignDocument insertDesignDocument(DesignDocument designDocument, boolean development);

    /**
     * Inserts a {@link DesignDocument} into development or production if it does not exist with a custom timeout.
     *
     * Note that inserting a {@link DesignDocument} is not an atomic operation, but instead internally performs a
     * {@link #getDesignDocument(String)} operation first. While expected to be very uncommon, a race condition may
     * happen if two users at the same time perform this operation with the same {@link DesignDocument}.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} exists.
     *
     * @param designDocument the {@link DesignDocument} to insert.
     * @param development if it should be inserted into development or production (published).
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the inserted {@link DesignDocument} on success.
     */
    DesignDocument insertDesignDocument(DesignDocument designDocument, boolean development, long timeout,
        TimeUnit timeUnit);

    /**
     * Upserts (inserts or replaces) a {@link DesignDocument} into production with the default management timeout.
     *
     * If you want to add or update view definitions to an existing design document, you need to make sure you have
     * all the views (including old ones) in the DesignDocument. Use {@link #getDesignDocument(String)} to get the
     * old list and add your new view to it before calling this method.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param designDocument the {@link DesignDocument} to upsert.
     * @return the upserted {@link DesignDocument} on success.
     */
    DesignDocument upsertDesignDocument(DesignDocument designDocument);

    /**
     * Upserts (inserts or replaces) a {@link DesignDocument} into production with a custom timeout.
     *
     * If you want to add or update view definitions to an existing design document, you need to make sure you have
     * all the views (including old ones) in the DesignDocument. Use {@link #getDesignDocument(String)} to get the
     * old list and add your new view to it before calling this method.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param designDocument the {@link DesignDocument} to upsert.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the upserted {@link DesignDocument} on success.
     */
    DesignDocument upsertDesignDocument(DesignDocument designDocument, long timeout, TimeUnit timeUnit);

    /**
     * Upserts (inserts or replaces) a {@link DesignDocument} into production or development with the default management
     * timeout.
     *
     * If you want to add or update view definitions to an existing design document, you need to make sure you have
     * all the views (including old ones) in the DesignDocument. Use {@link #getDesignDocument(String)} to get the
     * old list and add your new view to it before calling this method.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param designDocument the {@link DesignDocument} to upsert.
     * @param development if the {@link DesignDocument} should be upserted into development or production.
     * @return the upserted {@link DesignDocument} on success.
     */
    DesignDocument upsertDesignDocument(DesignDocument designDocument, boolean development);

    /**
     * Upserts (inserts or replaces) a {@link DesignDocument} into production or development with a custom timeout.
     *
     * If you want to add or update view definitions to an existing design document, you need to make sure you have
     * all the views (including old ones) in the DesignDocument. Use {@link #getDesignDocument(String)} to get the
     * old list and add your new view to it before calling this method.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param designDocument the {@link DesignDocument} to upsert.
     * @param development if the {@link DesignDocument} should be upserted into development or production.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the upserted {@link DesignDocument} on success.
     */
    DesignDocument upsertDesignDocument(DesignDocument designDocument, boolean development, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link DesignDocument} from production by its name with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *
     * @param name the name of the {@link DesignDocument}.
     * @return true if succeeded, false otherwise.
     */
    Boolean removeDesignDocument(String name);

    /**
     * Removes a {@link DesignDocument} from production by its name with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return true if succeeded, false otherwise.
     */
    Boolean removeDesignDocument(String name, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link DesignDocument} from production or development by its name with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if the {@link DesignDocument} should be removed from development or production.
     * @return true if succeeded, false otherwise.
     */
    Boolean removeDesignDocument(String name, boolean development);

    /**
     * Removes a {@link DesignDocument} from production or development by its name with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if the {@link DesignDocument} should be removed from development or production.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return true if succeeded, false otherwise.
     */
    Boolean removeDesignDocument(String name, boolean development, long timeout, TimeUnit timeUnit);

    /**
     * Publishes a {@link DesignDocument} from development into production with the default management timeout.
     *
     * Note that this method does not override a already existing {@link DesignDocument}
     * (see {@link #publishDesignDocument(String, boolean)}) as an alternative.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} already
     *   exists.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the  {@link DesignDocument} to publish.
     * @return the published {@link DesignDocument} on success.
     */
    DesignDocument publishDesignDocument(String name);

    /**
     * Publishes a {@link DesignDocument} from development into production with a custom timeout.
     *
     * Note that this method does not override a already existing {@link DesignDocument}
     * (see {@link #publishDesignDocument(String, boolean)}) as an alternative.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} already
     *   exists.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the  {@link DesignDocument} to publish.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the published {@link DesignDocument} on success.
     */
    DesignDocument publishDesignDocument(String name, long timeout, TimeUnit timeUnit);

    /**
     * Publishes a {@link DesignDocument} from development into production with the default management timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} already
     *   exists and override is set to false.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the  {@link DesignDocument} to publish.
     * @param overwrite if an existing {@link DesignDocument} should be overridden.
     * @return the published {@link DesignDocument} on success.
     */
    DesignDocument publishDesignDocument(String name, boolean overwrite);

    /**
     * Publishes a {@link DesignDocument} from development into production with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.DesignDocumentAlreadyExistsException: If the {@link DesignDocument} already
     *   exists and override is set to false.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     *
     * @param name the name of the  {@link DesignDocument} to publish.
     * @param overwrite if an existing {@link DesignDocument} should be overridden.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the published {@link DesignDocument} on success.
     */
    DesignDocument publishDesignDocument(String name, boolean overwrite, long timeout, TimeUnit timeUnit);
}
