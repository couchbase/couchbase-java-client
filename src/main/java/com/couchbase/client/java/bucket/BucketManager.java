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
package com.couchbase.client.java.bucket;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.error.IndexAlreadyExistsException;
import com.couchbase.client.java.error.IndexDoesNotExistException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.util.IndexInfo;
import com.couchbase.client.java.view.DesignDocument;

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
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
     *
     * @param name the name of the {@link DesignDocument}.
     * @return a {@link DesignDocument} if found.
     */
    DesignDocument getDesignDocument(String name);

    /**
     * Loads a published {@link DesignDocument} by its name with the a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return a {@link DesignDocument} if found.
     */
    DesignDocument getDesignDocument(String name, long timeout, TimeUnit timeUnit);

    /**
     * Loads a {@link DesignDocument} by its name from either development or production with the default management
     * timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if {@link DesignDocument} should be loaded from development or from production.
     * @return a {@link DesignDocument} if found.
     */
    DesignDocument getDesignDocument(String name, boolean development);

    /**
     * Loads a {@link DesignDocument}s by its name from either development or production with a custom timeout.
     *
     * This method throws:
     *
     * - java.util.concurrent.TimeoutException: If the timeout is exceeded.
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if {@link DesignDocument} should be loaded from development or from production.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return a {@link DesignDocument} if found.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
     *
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found
     *   in development.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found
     *   in development.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found
     *   in development.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found
     *   in development.
     *
     * @param name the name of the  {@link DesignDocument} to publish.
     * @param overwrite if an existing {@link DesignDocument} should be overridden.
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return the published {@link DesignDocument} on success.
     */
    DesignDocument publishDesignDocument(String name, boolean overwrite, long timeout, TimeUnit timeUnit);

    /**
     * List all N1QL GSI indexes that are registered for the current bucket, with the default management timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @return a List containing each relevant {@link IndexInfo} (can be empty if no index
     * is defined for this bucket).
     * @throws TranscodingException if the server response couldn't be parsed.
     */
    List<IndexInfo> listN1qlIndexes();

    /**
     * List all N1QL GSI indexes that are registered for the current bucket, with a custom timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     *
     * @return a List containing each relevant {@link IndexInfo} (can be empty if no index
     * is defined for this bucket).
     * @throws TranscodingException if the server response couldn't be parsed.
     */
    List<IndexInfo> listN1qlIndexes(long timeout, TimeUnit timeUnit);

    /**
     * Create a primary index for the current bucket, within the default management timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfExist if a primary index already exists, an exception will be thrown unless this is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @return true if the index was effectively created, (even in deferred mode) or false if the index existed and
     * ignoreIfExist is true.
     * @throws IndexAlreadyExistsException if the index already exists and ignoreIfExist is set to false.
     * @throws CouchbaseException if another error occurs during index creation.
     */
    boolean createN1qlPrimaryIndex(boolean ignoreIfExist, boolean defer);

    /**
     * Create a primary index for the current bucket, within a custom timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfExist if a primary index already exists, an exception will be thrown unless this is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @return true if the index was effectively created, (even in deferred mode) or false if the index existed and
     * ignoreIfExist is true.
     * @throws IndexAlreadyExistsException if the index already exists and ignoreIfExist is set to false.
     * @throws CouchbaseException if another error occurs during index creation.
     */
    boolean createN1qlPrimaryIndex(boolean ignoreIfExist, boolean defer, long timeout, TimeUnit timeUnit);

    /**
     * Create a custom-named primary index for the current bucket, within the default management timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfExist if a primary index already exists, an exception will be thrown unless this is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @param customName the custom name for the primary index.
     * @return true if the index was effectively created, (even in deferred mode) or false if the index existed and
     * ignoreIfExist is true.
     * @throws IndexAlreadyExistsException if the index already exists and ignoreIfExist is set to false.
     * @throws CouchbaseException if another error occurs during index creation.
     */
    boolean createN1qlPrimaryIndex(String customName, boolean ignoreIfExist, boolean defer);

    /**
     * Create a custom-named primary index for the current bucket, within a custom timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfExist if a primary index already exists, an exception will be thrown unless this is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @param timeout the custom timeout.
     * @param timeUnit the time unit for the custom timeout.
     * @param customName the custom name for the primary index.
     * @return true if the index was effectively created, (even in deferred mode) or false if the index existed and
     * ignoreIfExist is true.
     * @throws IndexAlreadyExistsException if the index already exists and ignoreIfExist is set to false.
     * @throws CouchbaseException if another error occurs during index creation.
     */
    boolean createN1qlPrimaryIndex(String customName, boolean ignoreIfExist, boolean defer, long timeout, TimeUnit timeUnit);

    /**
     * Create a secondary index for the current bucket, with the default management timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * This method allows to define fields of the index as a vararg, for convenience (actually accepting
     * {@link Expression} or {@link String}), but has the limitation that a WHERE clause cannot be set.
     *
     * @param indexName the name of the index.
     * @param ignoreIfExist if a secondary index already exists with that name, an exception will be thrown unless this
     *                      is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @param fields the JSON fields to index, in either {@link Expression} or {@link String} form.
     * @return true if the index was effectively created (even in deferred mode) or false if the index existed and
     * ignoreIfExist is true.
     * @throws IndexAlreadyExistsException if the index already exists and ignoreIfExist is set to false.
     * @throws CouchbaseException if another error occurs during index creation.
     * @see #createN1qlIndex(String, List, Expression, boolean, boolean)
     */
    boolean createN1qlIndex(String indexName, boolean ignoreIfExist, boolean defer, Object... fields); //for convenience

    /**
     * Create a secondary index for the current bucket, with the default management timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * This method allows to define fields of the index as a List, as well as an additional {@link Expression} for the
     * WHERE clause of the index (which can be null).
     *
     * @param indexName the name of the index.
     * @param fields the List of JSON fields to index, in either {@link Expression} or {@link String} form.
     * @param whereClause the {@link Expression} to use in the WHERE clause of the index.
     * @param ignoreIfExist if a secondary index already exists with that name, an exception will be thrown unless this
     *                      is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @return true if the index was effectively created (even in deferred mode) or false if the index existed and
     * ignoreIfExist is true.
     * @throws IndexAlreadyExistsException if the index already exists and ignoreIfExist is set to false.
     * @throws CouchbaseException if another error occurs during index creation.
     */
    boolean createN1qlIndex(String indexName, List<Object> fields, Expression whereClause, boolean ignoreIfExist,
            boolean defer); //for consistency with timeout api below

    /**
     * Create a secondary index for the current bucket, with a custom timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * This method allows to define fields of the index as a List, as well as an additional {@link Expression} for the
     * WHERE clause of the index (which can be null).
     *
     * @param indexName the name of the index.
     * @param fields the List of JSON fields to index, in either {@link Expression} or {@link String} form.
     * @param whereClause the {@link Expression} to use in the WHERE clause of the index.
     * @param ignoreIfExist if a secondary index already exists with that name, an exception will be thrown unless this
     *                      is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the custom timeout.
     * @return true if the index was effectively created (even in deferred mode) or false if the index existed and
     * ignoreIfExist is true.
     * @throws IndexAlreadyExistsException if the index already exists and ignoreIfExist is set to false.
     * @throws CouchbaseException if another error occurs during index creation.
     */
    boolean createN1qlIndex(String indexName, List<Object> fields, Expression whereClause, boolean ignoreIfExist,
            boolean defer, long timeout, TimeUnit timeUnit);

    /**
     * Drop the default primary index ({@link Index#PRIMARY_NAME}) associated with the current bucket, within the default management timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without any primary index won't cause an exception to be propagated.
     * @return true if the index was effectively dropped, false if it didn't exist and ignoreIfNotExist is set to true.
     * @throws IndexDoesNotExistException if the primary index doesn't exist and ignoreIfNotExist is set to false.
     * @throws CouchbaseException if another error occurs during index drop.
     */
    boolean dropN1qlPrimaryIndex(boolean ignoreIfNotExist);

    /**
     * Drop the default primary index ({@link Index#PRIMARY_NAME}) associated with the current bucket, within a custom timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without any primary index won't cause an exception to be propagated.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the custom timeout.
     * @return true if the index was effectively dropped, false if it didn't exist and ignoreIfNotExist is set to true.
     * @throws IndexDoesNotExistException if the primary index doesn't exist and ignoreIfNotExist is set to false.
     * @throws CouchbaseException if another error occurs during index drop.
     */
    boolean dropN1qlPrimaryIndex(boolean ignoreIfNotExist, long timeout, TimeUnit timeUnit);

    /**
     * Drop the given custom-named primary index associated with the current bucket, within the default management timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without any primary index won't cause an exception to be propagated.
     * @param customName the custom name for the primary index.
     * @return true if the index was effectively dropped, false if it didn't exist and ignoreIfNotExist is set to true.
     * @throws IndexDoesNotExistException if the primary index doesn't exist and ignoreIfNotExist is set to false.
     * @throws CouchbaseException if another error occurs during index drop.
     */
    boolean dropN1qlPrimaryIndex(String customName, boolean ignoreIfNotExist);

    /**
     * Drop the given custom-named primary index associated with the current bucket, within a custom timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without any primary index won't cause an exception to be propagated.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the custom timeout.
     * @param customName the custom name for the primary index.
     * @return true if the index was effectively dropped, false if it didn't exist and ignoreIfNotExist is set to true.
     * @throws IndexDoesNotExistException if the primary index doesn't exist and ignoreIfNotExist is set to false.
     * @throws CouchbaseException if another error occurs during index drop.
     */
    boolean dropN1qlPrimaryIndex(String customName, boolean ignoreIfNotExist, long timeout, TimeUnit timeUnit);

    /**
     * Drop the given secondary index associated with the current bucket, within the default management timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without the specified index won't cause an exception to be propagated.
     * @return true if the index was effectively dropped, false if it didn't exist and ignoreIfNotExist is set to true.
     * @throws IndexDoesNotExistException if the secondary index doesn't exist and ignoreIfNotExist is set to false.
     * @throws CouchbaseException if another error occurs during index drop.
     */
    boolean dropN1qlIndex(String name, boolean ignoreIfNotExist);

    /**
     * Drop the given secondary index associated with the current bucket, within a custom timeout.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without the specified index won't cause an exception to be propagated.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the custom timeout.
     * @return true if the index was effectively dropped, false if it didn't exist and ignoreIfNotExist is set to true.
     * @throws IndexDoesNotExistException if the secondary index doesn't exist and ignoreIfNotExist is set to false.
     * @throws CouchbaseException if another error occurs during index drop.
     */
    boolean dropN1qlIndex(String name, boolean ignoreIfNotExist, long timeout, TimeUnit timeUnit);

    /**
     * Instruct the query engine to trigger the build of indexes that have been deferred, within the default management
     * timeout. This only considers GSI indexes, as the index management API only deals with this type of indexes.
     *
     * This process itself is asynchronous, meaning that the call will immediately return despite indexes still being
     * in a "pending" or "building" state. This method will return a List of the names of indexes whose build has been
     * triggered.
     *
     * @return a {@link List} of index names, the names of the indexes that have been triggered.
     * @see #watchN1qlIndexes(List, long, TimeUnit) to poll for a list of indexes to become online.
     */
    List<String> buildN1qlDeferredIndexes();

    /**
     * Instruct the query engine to trigger the build of indexes that have been deferred, within a custom timeout.
     * This only considers GSI indexes, as the index management API only deals with this type of indexes.
     *
     * This process itself is asynchronous, meaning that the call will immediately return despite indexes still being
     * in a "pending" or "building" state. This method will return a List of the names of indexes whose build has been
     * triggered.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the custom timeout.
     * @return a {@link List} of index names, the names of the indexes that have been triggered.
     * @see #watchN1qlIndexes(List, long, TimeUnit) to poll for a list of indexes to become online.
     */
    List<String> buildN1qlDeferredIndexes(long timeout, TimeUnit timeUnit);

    /**
     * Watches all given indexes (possibly including the primary one), polling the query service until they become
     * "online" or the watchTimeout has expired. This only considers GSI indexes, as the index management API only
     * deals with this type of indexes.
     *
     * Note: You can activate DEBUG level logs on the "{@link DefaultAsyncBucketManager#INDEX_WATCH_LOG_NAME}" logger
     * to see various stages of the polling.
     *
     * You can also watch a primary index by using the {@link Index#PRIMARY_NAME} constant.
     *
     * @param watchList the names of the SECONDARY indexes to watch (can be empty).
     * @param watchTimeout the maximum duration for which to poll for the index to become online.
     * @param watchTimeUnit the time unit for the watchTimeout.
     * @return a {@link List} of the {@link IndexInfo} for the indexes that went online during the watch period. Can be
     * empty if all indexes where online, no index to watch or no index became online within the watchTimeout timeframe.
     */
    List<IndexInfo> watchN1qlIndexes(List<String> watchList, long watchTimeout, TimeUnit watchTimeUnit);
}
