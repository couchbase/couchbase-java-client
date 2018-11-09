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
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.error.IndexAlreadyExistsException;
import com.couchbase.client.java.error.IndexDoesNotExistException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.query.Index;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.util.IndexInfo;
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
     *
     * @param name the name of the {@link DesignDocument}.
     * @return an observable that resolves into a  {@link DesignDocument}.
     */
    Observable<DesignDocument> getDesignDocument(String name);

    /**
     * Loads a {@link DesignDocument} by its name from either development or production.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.TranscodingException: If the server response could not be parsed.
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
     *
     * @param name the name of the {@link DesignDocument}.
     * @param development if it should be loaded from development or production.
     * @return an observable that resolves into a  {@link DesignDocument}.
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
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
     *
     *
     * @param name the name of the {@link DesignDocument}.
     * @return true if succeeded, false otherwise.
     */
    Observable<Boolean> removeDesignDocument(String name);

    /**
     * Removes a {@link DesignDocument} from production or development by its name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found
     *   in development.
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
     * - com.couchbase.client.java.error.DesignDocumentDoesNotExistException: If {@link DesignDocument} is not found
     *   in development.
     *
     * @param name the name of the  {@link DesignDocument} to publish.
     * @param overwrite if an existing {@link DesignDocument} should be overridden.
     * @return the published {@link DesignDocument} on success.
     */
    Observable<DesignDocument> publishDesignDocument(String name, boolean overwrite);

    /**
     * List all N1QL GSI indexes that are registered for the current bucket.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link TranscodingException} if the server response couldn't be parsed.
     *
     * @return an {@link Observable} that will get notified of each relevant {@link IndexInfo} (can be empty if no
     * relevant index is defined for this bucket).
     */
    Observable<IndexInfo> listN1qlIndexes();

    /**
     * Create a primary index for the current bucket.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link IndexAlreadyExistsException} if the index already exists and ignoreIfExist is set to false.
     *  - {@link CouchbaseException} if another error occurs during index creation.
     *
     * @param ignoreIfExist if a primary index already exists, an exception will be thrown unless this is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @return an {@link Observable} that will get notified with a single Boolean.TRUE if the index was effectively created
     * (even in deferred mode), Boolean.FALSE if the index existed and ignoreIfExist is true.
     */
    Observable<Boolean> createN1qlPrimaryIndex(boolean ignoreIfExist, boolean defer);

    /**
     * Create a custom-named primary index for the current bucket.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link IndexAlreadyExistsException} if the index already exists and ignoreIfExist is set to false.
     *  - {@link CouchbaseException} if another error occurs during index creation.
     *
     * @param customName the custom name for the primary index.
     * @param ignoreIfExist if a primary index already exists, an exception will be thrown unless this is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @return an {@link Observable} that will get notified with a single Boolean.TRUE if the index was effectively created
     * (even in deferred mode), Boolean.FALSE if the index existed and ignoreIfExist is true.
     */
    Observable<Boolean> createN1qlPrimaryIndex(String customName, boolean ignoreIfExist, boolean defer);

    /**
     * Create a secondary index for the current bucket.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * The convenience of providing fields as a vararg of either String or Expression comes with the limitation that
     * a WHERE clause cannot be provided. See {@link #createN1qlIndex(String, List, Expression, boolean, boolean)} for
     * that.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link IndexAlreadyExistsException} if the index already exists and ignoreIfExist is set to false.
     *  - {@link CouchbaseException} if another error occurs during index creation.
     *
     * @param indexName the name of the index.
     * @param ignoreIfExist if a secondary index already exists with that name, an exception will be thrown unless this
     *                      is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @param fields the JSON fields to index, in either {@link Expression} or {@link String} form.
     * @return an {@link Observable} that will get notified with a single Boolean.TRUE if the index was effectively created
     * (even in deferred mode), Boolean.FALSE if the index existed and ignoreIfExist is true.
     * @see #createN1qlIndex(String, List, Expression, boolean, boolean)
     */
    Observable<Boolean> createN1qlIndex(String indexName, boolean ignoreIfExist, boolean defer, Object... fields);

    /**
     * Create a secondary index for the current bucket, with a WHERE clause.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link IndexAlreadyExistsException} if the index already exists and ignoreIfExist is set to false.
     *  - {@link CouchbaseException} if another error occurs during index creation.
     *
     * @param indexName the name of the index.
     * @param fields the JSON fields to index, in the form of a {@link List} containing a mix of {@link Expression} or {@link String}.
     * @param whereClause the {@link Expression} to use in the WHERE clause of the index.
     * @param ignoreIfExist if a secondary index already exists with that name, an exception will be thrown unless this
     *                      is set to true.
     * @param defer true to defer building of the index until {@link #buildN1qlDeferredIndexes()} is called (or a direct call
     *              to the corresponding query service API).
     * @return an {@link Observable} that will get notified with a single Boolean.TRUE if the index was effectively created
     * (even in deferred mode), Boolean.FALSE if the index existed and ignoreIfExist is true.
     */
    Observable<Boolean> createN1qlIndex(final String indexName, List<Object> fields, Expression whereClause,
            final boolean ignoreIfExist, boolean defer);

    /**
     * Drop the default primary index ({@link Index#PRIMARY_NAME}) associated with the current bucket.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link IndexDoesNotExistException} if the primary index doesn't exist and ignoreIfNoExist is set to false.
     *  - {@link CouchbaseException} if another error occurs during index drop.
     *
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without any primary index won't cause an exception to be propagated.
     * @return an {@link Observable} that will get notified with a single Boolean.TRUE if the index was effectively dropped.
     */
    Observable<Boolean> dropN1qlPrimaryIndex(boolean ignoreIfNotExist);

    /**
     * Drop the given custom-named primary index associated with the current bucket.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link IndexDoesNotExistException} if the primary index doesn't exist and ignoreIfNoExist is set to false.
     *  - {@link CouchbaseException} if another error occurs during index drop.
     *
     * @param customName the custom name of the primary index.
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without any primary index won't cause an exception to be propagated.
     * @return an {@link Observable} that will get notified with a single Boolean.TRUE if the index was effectively dropped.
     */
    Observable<Boolean> dropN1qlPrimaryIndex(String customName, boolean ignoreIfNotExist);

    /**
     * Drop the given secondary index associated with the current bucket.
     *
     * The index management API only deals with GSI type of indexes, which allows it to uniquely identify indexes
     * by name.
     *
     * The {@link Observable} can error under the following conditions:
     *
     *  - {@link IndexDoesNotExistException} if the secondary index doesn't exist and ignoreIfNoExist is set to false.
     *  - {@link CouchbaseException} if another error occurs during index drop.
     *
     * @param ignoreIfNotExist if true, attempting to drop on a bucket without the specified index won't cause an exception to be propagated.
     * @return an {@link Observable} that will get notified with a single Boolean.TRUE if the index was effectively dropped.
     */
    Observable<Boolean> dropN1qlIndex(String name, boolean ignoreIfNotExist);

    /**
     * Instruct the query engine to trigger the build of indexes that have been deferred. This only considers GSI
     * indexes, as the index management API only deals with this type of indexes.
     *
     * This process itself is asynchronous, meaning that the call will immediately return despite indexes still being
     * in a "pending" or "building" state. This method will return a List of the names of indexes whose build has been
     * triggered, in a single emission.
     *
     * @return an {@link Observable} that will get notified with a single List of index names, the names of the indexes that
     * have been triggered.
     * @see #watchN1qlIndexes(List, long, TimeUnit) to poll for a list of indexes to become online.
     */
    Observable<List<String>> buildN1qlDeferredIndexes();

    /**
     * Watches all given indexes (possibly including the primary one), polling the query service until they become
     * "online" or the watchTimeout has expired.. This only considers GSI indexes, as the index management API only
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
     * @return a stream of the {@link IndexInfo} for the indexes that went online during the watch period. Can be empty
     * if all indexes where online, no index to watch or no index became online within the watchTimeout timeframe.
     */
    Observable<IndexInfo> watchN1qlIndexes(List<String> watchList, long watchTimeout, TimeUnit watchTimeUnit);
}
