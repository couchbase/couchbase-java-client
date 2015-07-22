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

import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.LegacyDocument;
import com.couchbase.client.java.document.StringDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.CouchbaseOutOfMemoryException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.error.RequestTooBigException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.error.TemporaryLockFailureException;
import com.couchbase.client.java.error.ViewDoesNotExistException;
import com.couchbase.client.java.query.AsyncQueryResult;
import com.couchbase.client.java.query.PreparedPayload;
import com.couchbase.client.java.query.PreparedQuery;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.core.QueryExecutor;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.repository.Repository;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.view.AsyncSpatialViewResult;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.View;
import com.couchbase.client.java.view.ViewQuery;
import rx.Observable;

/**
 * Defines operations that can be executed asynchronously against a Couchbase Server bucket.
 *
 * Note that only a subset of the provided operations are available for "memcached" type buckets. Also, some other
 * operations are only available against specific versions of Couchbase Server.
 *
 * Always apply a {@link Observable#timeout(long, TimeUnit)} of some form to add a boundary between the SDK as an
 * integration point and the application. Networks are unreliable, servers can fail and the SDK contains bugs. With applying
 * a timeout and reacting to them accordingly, application level code is less likely to fail.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface AsyncBucket {

    /**
     * The name of the {@link Bucket}.
     *
     * @return the name of the bucket.
     */
    String name();

    /**
     * The {@link CouchbaseEnvironment} used.
     *
     * @return the CouchbaseEnvironment.
     */
    CouchbaseEnvironment environment();

    /**
     * Returns the underlying "core-io" library through its {@link ClusterFacade}.
     *
     * Handle with care, with great power comes great responsibility. All additional checks which are normally performed
     * by this library are skipped.
     *
     * @return the underlying {@link ClusterFacade} from the "core-io" package.
     */
    Observable<ClusterFacade> core();

    /**
     * Retrieves a {@link JsonDocument} by its unique ID.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     *  The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the unique ID of the document.
     * @return an {@link Observable} eventually containing the found {@link JsonDocument}.
     */
    Observable<JsonDocument> get(String id);

    /**
     * Retrieves any type of {@link Document} by its unique ID.
     *
     * The document ID is taken out of the {@link Document} provided, as well as the target type to return. Note that
     * not the same document is returned, but rather a new one of the same type with the freshly loaded properties.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     *  The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID is taken and the type is inferred.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> Observable<D> get(D document);

    /**
     * Retrieves any type of {@link Document} by its unique ID.
     *
     * This method differs from {@link #get(String)} in that if a specific {@link Document} type is passed in, the
     * appropriate {@link Transcoder} will be selected (and not JSON conversion).
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     *  The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the unique ID of the document.
     * @param target the target document type to use.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> Observable<D> get(String id, Class<D> target);

    /**
     * Check whether a document with the given ID does exist in the bucket.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @return true if it exists, false otherwise.
     */
    Observable<Boolean> exists(String id);

    /**
     * Check whether a document with the given ID does exist in the bucket.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document where the ID is extracted from.
     * @return true if it exists, false otherwise.
     */
    <D extends Document<?>> Observable<Boolean> exists(D document);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID.
     *
     * Depending on the {@link ReplicaMode} selected, there can be none to four {@link JsonDocument} be returned
     * from the {@link Observable}. If {@link ReplicaMode#FIRST}, {@link ReplicaMode#SECOND} or
     * {@link ReplicaMode#THIRD} are selected zero or one documents are returned, if {@link ReplicaMode#ALL} is used,
     * all configured replicas plus the master node may return a document.
     *
     * If the document has not been replicated yet or if the replica or master are not available (because a node has
     * been failed over), no response is expected from these nodes.
     *
     * **Since data is replicated asynchronously, all data returned from this method must be considered stale. If the
     * appropriate {@link ReplicateTo} constraints are set on write and the operation returns successfully, then the
     * data can be considered as non-stale.**
     *
     * Note that the returning {@link JsonDocument} responses can come in any order.
     *
     * Because this method is considered to be a "last resort" call against the database if a regular get didn't
     * succeed, all errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * @param id id the unique ID of the document.
     * @param type the {@link ReplicaMode} to select.
     * @return an {@link Observable} eventually containing zero to N {@link JsonDocument}s.
     */
    Observable<JsonDocument> getFromReplica(String id, ReplicaMode type);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link Document} by its unique ID.
     *
     * The document ID is taken out of the {@link Document} provided, as well as the target type to return. Note that
     * not the same document is returned, but rather a new one of the same type with the freshly loaded properties.
     *
     * Depending on the {@link ReplicaMode} selected, there can be none to four {@link Document} be returned
     * from the {@link Observable}. If {@link ReplicaMode#FIRST}, {@link ReplicaMode#SECOND} or
     * {@link ReplicaMode#THIRD} are selected zero or one documents are returned, if {@link ReplicaMode#ALL} is used,
     * all configured replicas plus the master node may return a document.
     *
     * If the document has not been replicated yet or if the replica or master are not available (because a node has
     * been failed over), no response is expected from these nodes.
     *
     * **Since data is replicated asynchronously, all data returned from this method must be considered stale. If the
     * appropriate {@link ReplicateTo} constraints are set on write and the operation returns successfully, then the
     * data can be considered as non-stale.**
     *
     * Note that the returning {@link Document} responses can come in any order.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * Because this method is considered to be a "last resort" call against the database if a regular get didn't
     * succeed, all errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * @param document the source document from which the ID is taken and the type is inferred.
     * @param type the {@link ReplicaMode} to select.
     * @return an {@link Observable} eventually containing zero to N {@link Document}s.
     */
    <D extends Document<?>> Observable<D> getFromReplica(D document, ReplicaMode type);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link Document} by its unique ID.
     *
     * This method differs from {@link #getFromReplica(String, ReplicaMode)} in that if a specific {@link Document}
     * type is passed in, the appropriate {@link Transcoder} will be selected (and not JSON conversion).
     *
     * Depending on the {@link ReplicaMode} selected, there can be none to four {@link Document} be returned
     * from the {@link Observable}. If {@link ReplicaMode#FIRST}, {@link ReplicaMode#SECOND} or
     * {@link ReplicaMode#THIRD} are selected zero or one documents are returned, if {@link ReplicaMode#ALL} is used,
     * all configured replicas plus the master node may return a document.
     *
     * If the document has not been replicated yet or if the replica or master are not available (because a node has
     * been failed over), no response is expected from these nodes.
     *
     * **Since data is replicated asynchronously, all data returned from this method must be considered stale. If the
     * appropriate {@link ReplicateTo} constraints are set on write and the operation returns successfully, then the
     * data can be considered as non-stale.**
     *
     * Note that the returning {@link Document} responses can come in any order.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * Because this method is considered to be a "last resort" call against the database if a regular get didn't
     * succeed, all errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * @param id id the unique ID of the document.
     * @param type the {@link ReplicaMode} to select.
     * @param target the target document type to use.
     * @return an {@link Observable} eventually containing zero to N {@link Document}s.
     */
    <D extends Document<?>> Observable<D> getFromReplica(String id, ReplicaMode type, Class<D> target);

    /**
     * Retrieve and lock a {@link JsonDocument} by its unique ID.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(String)}, but in addition it (write) locks the document for the given
     * lock time interval. Note that this lock time is hard capped to 30 seconds, even if provided with a higher
     * value and is not configurable. The document will unlock afterwards automatically.
     *
     * Detecting an already locked document is done by checking for {@link TemporaryLockFailureException}. Note that
     * this exception can also be raised in other conditions, always when the error is transient and retrying may help.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - In case of transient error, most probably because key is already locked: {@link TemporaryLockFailureException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @return an {@link Observable} eventually containing the found {@link JsonDocument}.
     */
    Observable<JsonDocument> getAndLock(String id, int lockTime);

    /**
     * Retrieve and lock a {@link Document} by its unique ID.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(Document)}, but in addition it (write) locks the document for the given
     * lock time interval. Note that this lock time is hard capped to 30 seconds, even if provided with a higher
     * value and is not configurable. The document will unlock afterwards automatically.
     *
     * Detecting an already locked document is done by checking for {@link TemporaryLockFailureException}. Note that
     * this exception can also be raised in other conditions, always when the error is transient and retrying may help.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - In case of transient error, most probably because key is already locked: {@link TemporaryLockFailureException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID is taken and the type is inferred.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> Observable<D> getAndLock(D document, int lockTime);

    /**
     * Retrieve and lock a {@link Document} by its unique ID.
     *
     * This method differs from {@link #getAndLock(String, int)} in that if a specific {@link Document} type is passed
     * in, the appropriate {@link Transcoder} will be selected (and not JSON conversion).
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(String)}, but in addition it (write) locks the document for the given
     * lock time interval. Note that this lock time is hard capped to 30 seconds, even if provided with a higher
     * value and is not configurable. The document will unlock afterwards automatically.
     *
     * Detecting an already locked document is done by checking for {@link TemporaryLockFailureException}. Note that
     * this exception can also be raised in other conditions, always when the error is transient and retrying may help.
     *
     * The returned {@link Observable} can error under the following conditions:
     * - In case of transient error, most probably because key is already locked: {@link TemporaryLockFailureException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @param target the target document type to use.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> Observable<D> getAndLock(String id, int lockTime, Class<D> target);

    /**
     * Retrieve and touch a {@link JsonDocument} by its unique ID.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(String)}, but in addition it touches the document, which will reset
     * its configured expiration time to the value provided.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param expiry the new expiration time for the document.
     * @return an {@link Observable} eventually containing the found {@link JsonDocument}.
     */
    Observable<JsonDocument> getAndTouch(String id, int expiry);

    /**
     * Retrieve and touch a {@link Document} by its unique ID.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(Document)}, but in addition it touches the document, which will reset
     * its configured expiration time set on the given document itself.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID and expiry is taken and the type is inferred.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> Observable<D> getAndTouch(D document);

    /**
     * Retrieve and touch a {@link Document} by its unique ID.
     *
     * This method differs from {@link #getAndTouch(String, int)} in that if a specific {@link Document} type is passed
     * in, the appropriate {@link Transcoder} will be selected (and not JSON conversion).
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(String, Class)}, but in addition it touches the document, which will
     * reset its configured expiration time to the value provided.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param expiry the new expiration time for the document.
     * @param target the target document type to use.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> Observable<D> getAndTouch(String id, int expiry, Class<D> target);

    /**
     * Insert a {@link Document} if it does not exist already.
     *
     * If the given {@link Document} (identified by its unique ID) already exists, the observable errors with a
     * {@link DocumentAlreadyExistsException}. If the operation should also override the existing {@link Document},
     * {@link #upsert(Document)} should be used instead. It will always either return a document or fail with an error.
     *
     * The returned {@link Document} contains original properties, but has the refreshed CAS value set.
     *
     * This operation will return successfully if the {@link Document} has been acknowledged in the managed cache layer
     * on the master server node. If increased data durability is a concern,
     * {@link #insert(Document, PersistTo, ReplicateTo)} should be used instead.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The original insert failed because the document is already stored: {@link DocumentAlreadyExistsException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the {@link Document} to insert.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> insert(D document);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The original insert failed because the document is already stored: {@link DocumentAlreadyExistsException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original insert has already happened, so the actual
     * insert and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to insert.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> insert(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The original insert failed because the document is already stored: {@link DocumentAlreadyExistsException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original insert has already happened, so the actual
     * insert and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to insert.
     * @param persistTo the persistence constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> insert(D document, PersistTo persistTo);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The original insert failed because the document is already stored: {@link DocumentAlreadyExistsException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original insert has already happened, so the actual
     * insert and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to insert.
     * @param replicateTo the replication constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> insert(D document, ReplicateTo replicateTo);

    /**
     * Insert or replace a {@link Document}.
     *
     * If the given {@link Document} (identified by its unique ID) already exists, it will be overridden by the current
     * one. The returned {@link Document} contains original properties, but has the refreshed CAS value set.
     *
     * This operation will return successfully if the {@link Document} has been acknowledged in the managed cache layer
     * on the master server node. If increased data durability is a concern,
     * {@link #upsert(Document, PersistTo, ReplicateTo)} should be used instead.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the {@link Document} to upsert.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> upsert(D document);

    /**
     * Insert or replace a {@link Document} and watch for durability constraints.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original upsert has already happened, so the actual
     * upsert and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to upsert.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> upsert(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Insert or replace a {@link Document} and watch for durability constraints.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original upsert has already happened, so the actual
     * upsert and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to upsert.
     * @param persistTo the persistence constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> upsert(D document, PersistTo persistTo);

    /**
     * Insert or replace a {@link Document} and watch for durability constraints.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original upsert has already happened, so the actual
     * upsert and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to upsert.
     * @param replicateTo the replication constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> upsert(D document, ReplicateTo replicateTo);

    /**
     * Replace a {@link Document} if it does already exist.
     *
     * If the given {@link Document} (identified by its unique ID) does not exist already, the method errors with a
     * {@link DocumentDoesNotExistException}. If the operation should also insert the {@link Document},
     * {@link #upsert(Document)} should be used instead. It will always either return a document or fail with an error.
     *
     * The returned {@link Document} contains original properties, but has the refreshed CAS value set.
     *
     * This operation will return successfully if the {@link Document} has been acknowledged in the managed cache layer
     * on the master server node. If increased data durability is a concern,
     * {@link #replace(Document, PersistTo, ReplicateTo)} should be used instead.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The original replace failed because the document does not exist: {@link DocumentDoesNotExistException}
     * - The request content is too big: {@link RequestTooBigException}
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the {@link Document} to replace.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> replace(D document);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The original replace failed because the document does not exist: {@link DocumentDoesNotExistException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original replace has already happened, so the actual
     * replace and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to replace.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> replace(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The original replace failed because the document does not exist: {@link DocumentDoesNotExistException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original replace has already happened, so the actual
     * replace and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to replace.
     * @param persistTo the persistence constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> replace(D document, PersistTo persistTo);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The original replace failed because the document does not exist: {@link DocumentDoesNotExistException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original replace has already happened, so the actual
     * replace and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the {@link Document} to replace.
     * @param replicateTo the replication constraint to watch.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> Observable<D> replace(D document, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @return the document containing the ID.
     */
    <D extends Document<?>> Observable<D> remove(D document);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return the document containing the ID.
     */
    <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param persistTo the persistence constraint to watch.
     * @return the document containing the ID.
     */
    <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param replicateTo the replication constraint to watch.
     * @return the document containing the ID.
     */
    <D extends Document<?>> Observable<D> remove(D document, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server identified by its ID.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @return the document containing the ID.
     */
    Observable<JsonDocument> remove(String id);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return the document containing the ID.
     */
    Observable<JsonDocument> remove(String id, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @return the document containing the ID.
     */
    Observable<JsonDocument> remove(String id, PersistTo persistTo);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param replicateTo the replication constraint to watch.
     * @return the document containing the ID.
     */
    Observable<JsonDocument> remove(String id, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server identified by its ID.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param target the target document type to use.
     * @return the document containing the ID.
     */
    <D extends Document<?>> Observable<D> remove(String id, Class<D> target);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param target the target document type to use.
     * @return the document containing the ID.
     */
    <D extends Document<?>> Observable<D> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param target the target document type to use.
     * @return the document containing the ID.
     */
    <D extends Document<?>> Observable<D> remove(String id, PersistTo persistTo, Class<D> target);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param replicateTo the replication constraint to watch.
     * @param target the target document type to use.
     * @return the document containing the ID.
     */
    <D extends Document<?>> Observable<D> remove(String id, ReplicateTo replicateTo, Class<D> target);

    /**
     * Queries a Couchbase Server {@link View}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - If the design document or view is not found: {@link ViewDoesNotExistException}
     *
     * @param query the query to perform.
     * @return a result containing all the found rows and additional information.
     */
    Observable<AsyncViewResult> query(ViewQuery query);

    /**
     * Queries a Couchbase Server Spatial {@link View}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - If the design document or view is not found: {@link ViewDoesNotExistException}
     *
     * @param query the spatial query to perform.
     * @return a result containing all the found rows and additional information.
     */
    Observable<AsyncSpatialViewResult> query(SpatialViewQuery query);

    /**
     * Experimental: Queries a N1QL secondary index with a simple {@link Statement}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement in a DSL form (start with a static select() import).
     * @return a result containing all found rows and additional information.
     */
    Observable<AsyncQueryResult> query(Statement statement);

    /**
     * Experimental: Queries a N1QL secondary index.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param query the full {@link Query}.
     * @return a result containing all found rows and additional information.
     */
    Observable<AsyncQueryResult> query(Query query);

    /**
     * Experimental: Queries a N1QL secondary index and prepare an execution plan via the given
     * {@link Statement}. Statement can contain placeholders.
     * The resulting {@link PreparedPayload} can be cached and (re)used later in a {@link PreparedQuery}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement to prepare a plan for.
     * @return a {@link PreparedPayload} that can be cached and reused later in {@link PreparedQuery}.
     */
    Observable<PreparedPayload> prepare(Statement statement);

    /**
     * Experimental: Queries a N1QL secondary index and prepare an execution plan via the given
     * statement in {@link String} form. Statement can contain placeholders.
     * The resulting {@link PreparedPayload} can be cached and (re)used later in a {@link PreparedQuery}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement to prepare a plan for.
     * @return a {@link PreparedPayload} that can be cached and reused later in {@link PreparedQuery}.
     */
    Observable<PreparedPayload> prepare(String statement);

    /**
     * Unlocks a write-locked {@link Document}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - A transient error happened, most likely the CAS value was not correct: {@link TemporaryLockFailureException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to unlock.
     * @param cas the CAS value which is mandatory to unlock it.
     * @return a Boolean indicating if the unlock was successful or not.
     */
    Observable<Boolean> unlock(String id, long cas);

    /**
     * Unlocks a write-locked {@link Document}.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The document doesn't exist: {@link DocumentDoesNotExistException}
     * - A transient error happened, most likely the CAS value was not correct: {@link TemporaryLockFailureException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document where ID and CAS are extracted from.
     * @return a Boolean indicating if the unlock was successful. (note that unsuccessful touch will rather
     * raise an exception)
     */
    <D extends Document<?>> Observable<Boolean> unlock(D document);

    /**
     * Renews the expiration time of a {@link Document}.
     *
     * Compared to {@link #getAndTouch(Document)}, this method does not actually fetch the document from the server,
     * but it just resets its expiration time to the given value.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The document doesn't exist: {@link DocumentDoesNotExistException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param expiry the new expiration time. 0 means no expiry.
     * @return a Boolean indicating if the touch had been successful. (note that unsuccessful touch will rather
     * raise an exception)
     */
    Observable<Boolean> touch(String id, int expiry);

    /**
     * Renews the expiration time of a {@link Document}.
     *
     * Compared to {@link #getAndTouch(Document)}, this method does not actually fetch the document from the server,
     * but it just resets its expiration time to the given value.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to extract the ID and expiry from.
     * @return a copy of the document inserted.
     */
    <D extends Document<?>> Observable<Boolean> touch(D document);

    /**
     * Increment or decrement a counter with the given value and a default value of 0.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @return a {@link Document} containing the resulting value.
     */
    Observable<JsonLongDocument> counter(String id, long delta);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @return a {@link Document} containing the resulting value.
     */
    Observable<JsonLongDocument> counter(String id, long delta, long initial);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @return a {@link Document} containing the resulting value.
     */
    Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry);

    /**
     * Append a {@link Document} to another one.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link AsyncBucket#get(Document)} call needs to be issued in order to get the full
     * current content.
     *
     * If the {@link Document} does not exist, it needs to be created upfront. Note that {@link JsonDocument}s in all
     * forms are not supported, it is advised that the following ones are used:
     *
     * - {@link LegacyDocument}
     * - {@link StringDocument}
     * - {@link BinaryDocument}
     *
     * Note that this method does not support expiration on the {@link Document}. If set, it will be ignored.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to be appended.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> Observable<D> append(D document);

    /**
     * Prepend a {@link Document} to another one.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link AsyncBucket#get(Document)} call needs to be issued in order to get the full
     * current content.
     *
     * If the {@link Document} does not exist, it needs to be created upfront. Note that {@link JsonDocument}s in all
     * forms are not supported, it is advised that the following ones are used:
     *
     * - {@link LegacyDocument}
     * - {@link StringDocument}
     * - {@link BinaryDocument}
     *
     * Note that this method does not support expiration on the {@link Document}. If set, it will be ignored.
     *
     * The returned {@link Observable} can error under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to be prepended.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> Observable<D> prepend(D document);

    /**
     * Provides access to the {@link AsyncBucketManager} for administrative access.
     *
     * The manager lets you perform operations such as flushing a bucket or creating and managing design documents.
     *
     * @return the bucket manager for administrative operations.
     */
    Observable<AsyncBucketManager> bucketManager();

    /**
     * The {@link Repository} provides access to full object document mapping (ODM) capabilities.
     *
     * It allows you to work with POJO entities only and use annotations to customize the behaviour and mapping
     * characteristics.
     *
     * @return the repository for ODM capabilities.
     */
    Observable<AsyncRepository> repository();

    /**
     * Closes the {@link AsyncBucket}.
     *
     * @return an {@link Observable} eventually containing a new {@link Boolean} after close.
     */
    Observable<Boolean> close();

}
