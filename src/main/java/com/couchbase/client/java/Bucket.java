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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.internal.PingReport;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.analytics.AnalyticsDeferredResultHandle;
import com.couchbase.client.java.analytics.AnalyticsQuery;
import com.couchbase.client.java.analytics.AnalyticsQueryResult;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.datastructures.MutationOptionBuilder;
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
import com.couchbase.client.java.error.subdoc.PathNotFoundException;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.repository.Repository;
import com.couchbase.client.java.subdoc.LookupInBuilder;
import com.couchbase.client.java.subdoc.MutateInBuilder;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.SpatialViewResult;
import com.couchbase.client.java.view.View;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import rx.Observable;

/**
 * Defines operations that can be executed synchronously against a Couchbase Server bucket.
 *
 * Note that only a subset of the provided operations are available for "memcached" type buckets. Also, some other
 * operations are only available against specific versions of Couchbase Server.
 *
 * Default timeouts are always applied and can be configured through the {@link CouchbaseEnvironment}. Overloads
 * are also available to change them on a per-call basis.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public interface Bucket {

    /**
     * Provides access to the underlying asynchronous bucket interface.
     *
     * @return the asynchronous bucket.
     */
    AsyncBucket async();

    /**
     * Returns the underlying "core-io" library through its {@link ClusterFacade}.
     *
     * Handle with care, with great power comes great responsibility. All additional checks which are normally performed
     * by this library are skipped.
     *
     * @return the underlying {@link ClusterFacade} from the "core-io" package.
     */
    ClusterFacade core();

    /**
     * The {@link CouchbaseEnvironment} used.
     *
     * @return the CouchbaseEnvironment.
     */
    CouchbaseEnvironment environment();

    /**
     * The name of the {@link Bucket}.
     *
     * @return the name of the bucket.
     */
    String name();

    /**
     * Retrieves a {@link JsonDocument} by its unique ID with the default key/value timeout.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, null is returned.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the unique ID of the document.
     * @return the found {@link JsonDocument} or null if not found.
     */
    JsonDocument get(String id);

    /**
     * Retrieves a {@link JsonDocument} by its unique ID with a custom timeout.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, null is returned.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the unique ID of the document.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link JsonDocument} or null if not found.
     */
    JsonDocument get(String id, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves any type of {@link Document} with the default key/value timeout.
     *
     * The document ID is taken out of the {@link Document} provided, as well as the target type to return. Note that
     * not the same document is returned, but rather a new one of the same type with the freshly loaded properties.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID is taken and the type is inferred.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D get(D document);

    /**
     * Retrieves any type of {@link Document} with a custom timeout.
     *
     * The document ID is taken out of the {@link Document} provided, as well as the target type to return. Note that
     * not the same document is returned, but rather a new one of the same type with the freshly loaded properties.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID is taken and the type is inferred.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D get(D document, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves any type of {@link Document} by its ID with the default key/value timeout.
     *
     * The document ID is taken out of the {@link Document} provided, as well as the target type to return. Note that
     * not the same document is returned, but rather a new one of the same type with the freshly loaded properties.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param target the target document type to use.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D get(String id, Class<D> target);

    /**
     * Retrieves any type of {@link Document} by its ID with a custom timeout.
     *
     * The document ID is taken out of the {@link Document} provided, as well as the target type to return. Note that
     * not the same document is returned, but rather a new one of the same type with the freshly loaded properties.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D get(String id, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Check whether a document with the given ID does exist in the bucket.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
    boolean exists(String id);

    /**
     * Check whether a document with the given ID does exist in the bucket.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return true if it exists, false otherwise.
     */
    boolean exists(String id, long timeout, TimeUnit timeUnit);

    /**
     * Check whether a document with the given ID does exist in the bucket.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
    <D extends Document<?>> boolean exists(D document);

    /**
     * Check whether a document with the given ID does exist in the bucket.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document where the ID is extracted from.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return true if it exists, false otherwise.
     */
    <D extends Document<?>> boolean exists(D document, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with the
     * default timeout.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param id id the unique ID of the document.
     * @param type the {@link ReplicaMode} to select.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    List<JsonDocument> getFromReplica(String id, ReplicaMode type);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with the
     * default timeout.
     *
     * This method has the {@link ReplicaMode#ALL} preselected. If you are only interested in the first
     * (or just some) values, you can iterate and then break out of the iterator loop. Documents
     * are pushed into the iterator as they arrive, which distinguishes this method from the {@link List}
     * equivalents which wait until all responses arrive.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param id the unique ID of the document.
     * @return the Iterator containing Documents as they arrive.
     */
    Iterator<JsonDocument> getFromReplica(String id);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with a
     * custom timeout.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param id id the unique ID of the document.
     * @param type the {@link ReplicaMode} to select.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    List<JsonDocument> getFromReplica(String id, ReplicaMode type, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with a
     * custom timeout.
     *
     * This method has the {@link ReplicaMode#ALL} preselected. If you are only interested in the first
     * (or just some) values, you can iterate and then break out of the iterator loop. Documents
     * are pushed into the iterator as they arrive, which distinguishes this method from the {@link List}
     * equivalents which wait until all responses arrive.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param id the unique ID of the document.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the Iterator containing Documents as they arrive.
     */
    Iterator<JsonDocument> getFromReplica(String id, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with the
     * default timeout.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param document the document to extract the ID from.
     * @param type the {@link ReplicaMode} to select.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with the
     * default timeout.
     *
     * This method has the {@link ReplicaMode#ALL} preselected. If you are only interested in the first
     * (or just some) values, you can iterate and then break out of the iterator loop. Documents
     * are pushed into the iterator as they arrive, which distinguishes this method from the {@link List}
     * equivalents which wait until all responses arrive.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param document the document to extract the ID from.
     * @return the Iterator containing Documents as they arrive.
     */
    <D extends Document<?>> Iterator<D> getFromReplica(D document);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with a
     * custom timeout.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param document the document to extract the ID from.
     * @param type the {@link ReplicaMode} to select.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with a
     * custom timeout.
     *
     * This method has the {@link ReplicaMode#ALL} preselected. If you are only interested in the first
     * (or just some) values, you can iterate and then break out of the iterator loop. Documents
     * are pushed into the iterator as they arrive, which distinguishes this method from the {@link List}
     * equivalents which wait until all responses arrive.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param document the document to extract the ID from.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the Iterator containing Documents as they arrive.
     */
    <D extends Document<?>> Iterator<D> getFromReplica(D document, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with the
     * default timeout.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param id the id of the document.
     * @param type the {@link ReplicaMode} to select.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with the
     * default timeout.
     *
     * This method has the {@link ReplicaMode#ALL} preselected. If you are only interested in the first
     * (or just some) values, you can iterate and then break out of the iterator loop. Documents
     * are pushed into the iterator as they arrive, which distinguishes this method from the {@link List}
     * equivalents which wait until all responses arrive.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param id the unique ID of the document.
     * @param target the target document type to use.
     * @return the Iterator containing Documents as they arrive.
     */
    <D extends Document<?>> Iterator<D> getFromReplica(String id, Class<D> target);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with a
     * custom timeout.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param id the id of the document.
     * @param type the {@link ReplicaMode} to select.
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves one or more, possibly stale, representations of a {@link JsonDocument} by its unique ID with a
     * custom timeout.
     *
     * This method has the {@link ReplicaMode#ALL} preselected. If you are only interested in the first
     * (or just some) values, you can iterate and then break out of the iterator loop. Documents
     * are pushed into the iterator as they arrive, which distinguishes this method from the {@link List}
     * equivalents which wait until all responses arrive.
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
     * succeed, most errors are swallowed (but logged) and the Observable will return all successful responses.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException}
     *   wrapped in a {@link RuntimeException}
     *
     * @param id the unique ID of the document.
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the Iterator containing Documents as they arrive.
     */
    <D extends Document<?>> Iterator<D> getFromReplica(String id, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve and lock a {@link JsonDocument} by its unique ID with the default key/value timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - A transient error occurred, most probably the key was already locked: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @return the found {@link JsonDocument} or null.
     */
    JsonDocument getAndLock(String id, int lockTime);

    /**
     * Retrieve and lock a {@link JsonDocument} by its unique ID with a custom timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - A transient error occurred, most probably the key was already locked: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link JsonDocument} or null.
     */
    JsonDocument getAndLock(String id, int lockTime, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve and lock a {@link Document} by its unique ID with the default key/value timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - A transient error occurred, most probably the key was already locked: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID is taken and the type is inferred.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D getAndLock(D document, int lockTime);

    /**
     * Retrieve and lock a {@link Document} by its unique ID with a custom timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - A transient error occurred, most probably the key was already locked: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID is taken and the type is inferred.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D getAndLock(D document, int lockTime, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve and lock a {@link Document} by its unique ID with the default key/value timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - A transient error occurred, most probably the key was already locked: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @param target the target document type to use.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target);

    /**
     * Retrieve and lock a {@link Document} by its unique ID with the a custom timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - A transient error occurred, most probably the key was already locked: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve and touch a {@link JsonDocument} by its unique ID with the default key/value timeout.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(String)}, but in addition it touches the document, which will reset
     * its configured expiration time to the value provided.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param expiry the new expiration time for the document.
     * @return the found {@link JsonDocument} or null.
     */
    JsonDocument getAndTouch(String id, int expiry);

    /**
     * Retrieve and touch a {@link JsonDocument} by its unique ID with the a custom timeout.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(String)}, but in addition it touches the document, which will reset
     * its configured expiration time to the value provided.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id id the unique ID of the document.
     * @param expiry the new expiration time for the document.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link JsonDocument} or null.
     */
    JsonDocument getAndTouch(String id, int expiry, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve and touch a {@link Document} by its unique ID with the default key/value timeout.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(Document)}, but in addition it touches the document, which will reset
     * its configured expiration time set on the given document itself.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID and expiry is taken and the type is inferred.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D getAndTouch(D document);

    /**
     * Retrieve and touch a {@link Document} by its unique ID with a custom timeout.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(Document)}, but in addition it touches the document, which will reset
     * its configured expiration time set on the given document itself.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the source document from which the ID and expiry is taken and the type is inferred.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D getAndTouch(D document, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve and touch a {@link Document} by its unique ID with the default key/value timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target);

    /**
     * Retrieve and touch a {@link Document} by its unique ID with a custom timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the found {@link Document} or null.
     */
    <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Insert a {@link Document} if it does not exist already with the default key/value timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the {@link Document} to insert.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D insert(D document);

    /**
     * Insert a {@link Document} if it does not exist already with a custom timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the {@link Document} to insert.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D insert(D document, long timeout, TimeUnit timeUnit);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints with the default
     * key/value timeout.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints with the default
     * key/value timeout.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D insert(D document, PersistTo persistTo);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D insert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints with the default
     * key/value timeout.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D insert(D document, ReplicateTo replicateTo);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D insert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Insert or overwrite a {@link Document} with the default key/value timeout.
     *
     * If the given {@link Document} (identified by its unique ID) already exists, it will be overridden by the current
     * one. The returned {@link Document} contains original properties, but has the refreshed CAS value set.
     *
     * Please note that this method will not use the {@link Document#cas()} for optimistic concurrency checks. If
     * this behavior is needed, the {@link #replace(Document)} method needs to be used.
     *
     * This operation will return successfully if the {@link Document} has been acknowledged in the managed cache layer
     * on the master server node. If increased data durability is a concern,
     * {@link #upsert(Document, PersistTo, ReplicateTo)} should be used instead.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the {@link Document} to upsert.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document);

    /**
     * Insert or overwrite a {@link Document} with a custom timeout.
     *
     * If the given {@link Document} (identified by its unique ID) already exists, it will be overridden by the current
     * one. The returned {@link Document} contains original properties, but has the refreshed CAS value set.
     *
     * Please note that this method will not use the {@link Document#cas()} for optimistic concurrency checks. If
     * this behavior is needed, the {@link #replace(Document, long, TimeUnit)} method needs to be used.
     *
     * This operation will return successfully if the {@link Document} has been acknowledged in the managed cache layer
     * on the master server node. If increased data durability is a concern,
     * {@link #upsert(Document, PersistTo, ReplicateTo)} should be used instead.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The request content is too big: {@link RequestTooBigException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the {@link Document} to upsert.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document, long timeout, TimeUnit timeUnit);

    /**
     * Insert or overwrite a {@link Document} and watch for durability constraints with the default key/value timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Please note that this method will not use the {@link Document#cas()} for optimistic concurrency checks. If
     * this behavior is needed, the {@link #replace(Document, PersistTo, ReplicateTo)} method needs to be used.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Insert or overwrite a {@link Document} and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Please note that this method will not use the {@link Document#cas()} for optimistic concurrency checks. If
     * this behavior is needed, the {@link #replace(Document, PersistTo, ReplicateTo, long, TimeUnit)} method needs to be used.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Insert or overwrite a {@link Document} and watch for durability constraints with the default key/value timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Please note that this method will not use the {@link Document#cas()} for optimistic concurrency checks. If
     * this behavior is needed, the {@link #replace(Document, PersistTo)} method needs to be used.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document, PersistTo persistTo);

    /**
     * Insert or overwrite a {@link Document} and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Please note that this method will not use the {@link Document#cas()} for optimistic concurrency checks. If
     * this behavior is needed, the {@link #replace(Document, PersistTo, long, TimeUnit)} method needs to be used.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Insert or overwrite a {@link Document} and watch for durability constraints with the default key/value timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Please note that this method will not use the {@link Document#cas()} for optimistic concurrency checks. If
     * this behavior is needed, the {@link #replace(Document, ReplicateTo)} method needs to be used.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo);

    /**
     * Insert or overwrite a {@link Document} and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Please note that this method will not use the {@link Document#cas()} for optimistic concurrency checks. If
     * this behavior is needed, the {@link #replace(Document, ReplicateTo, long, TimeUnit)} method needs to be used.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Replace a {@link Document} if it does already exist with the default key/value timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D replace(D document);

    /**
     * Replace a {@link Document} if it does already exist with a custom timeout.
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D replace(D document, long timeout, TimeUnit timeUnit);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints with the default key/value
     * timeout.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints with the default key/value
     * timeout.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D replace(D document, PersistTo persistTo);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D replace(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints with the default key/value
     * timeout.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D replace(D document, ReplicateTo replicateTo);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the new {@link Document}.
     */
    <D extends Document<?>> D replace(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server with the default key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(D document);

    /**
     * Removes a {@link Document} from the Server with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(D document, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement with the default key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement with the default key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param persistTo the persistence constraint to watch.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(D document, PersistTo persistTo);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param persistTo the persistence constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement with the default key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param replicateTo the replication constraint to watch.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(D document, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - A CAS value was set on the {@link Document} and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to remove, with the ID extracted.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server identified by its ID with the default key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @return the document containing the ID.
     */
    JsonDocument remove(String id);

    /**
     * Removes a {@link Document} from the Server identified by its ID with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    JsonDocument remove(String id, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with the default
     * key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return the document containing the ID.
     */
    JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with the default
     * key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @return the document containing the ID.
     */
    JsonDocument remove(String id, PersistTo persistTo);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    JsonDocument remove(String id, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with the default
     * key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param replicateTo the replication constraint to watch.
     * @return the document containing the ID.
     */
    JsonDocument remove(String id, ReplicateTo replicateTo);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    JsonDocument remove(String id, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server identified by its ID with the default key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param target the target document type to use.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(String id, Class<D> target);

    /**
     * Removes a {@link Document} from the Server identified by its ID with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(String id, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with the default
     * key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param target the target document type to use.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target,
        long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with the default
     * key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param target the target document type to use.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param persistTo the persistence constraint to watch.
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with the default
     * key/value timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param replicateTo the replication constraint to watch.
     * @param target the target document type to use.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target);

    /**
     * Removes a {@link Document} from the Server by its ID and apply a durability requirement with a custom timeout.
     *
     * The {@link Document} returned just has the document ID and its CAS value set, since the value and all other
     * associated properties have been removed from the server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The document to remove does not exist: {@link DocumentDoesNotExistException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to remove.
     * @param replicateTo the replication constraint to watch.
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return the document containing the ID.
     */
    <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Queries a Couchbase Server {@link View} with the {@link CouchbaseEnvironment#viewTimeout() default view timeout}.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - If the design document or view is not found: {@link ViewDoesNotExistException}
     *
     * @param query the query to perform.
     * @return a result containing all the found rows and additional information.
     */
    ViewResult query(ViewQuery query);

    /**
     * Queries a Couchbase Server Spatial {@link View} with the {@link CouchbaseEnvironment#viewTimeout() default view timeout}.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - If the design document or view is not found: {@link ViewDoesNotExistException}
     *
     * @param query the query to perform.
     * @return a result containing all the found rows and additional information.
     */
    SpatialViewResult query(SpatialViewQuery query);

    /**
     * Queries a Couchbase Server {@link View} with a custom timeout.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - If the design document or view is not found: {@link ViewDoesNotExistException}
     *
     * @param query the query to perform.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a result containing all the found rows and additional information.
     */
    ViewResult query(ViewQuery query, long timeout, TimeUnit timeUnit);

    /**
     * Queries a Couchbase Server Spatial {@link View} with a custom timeout.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - If the design document or view is not found: {@link ViewDoesNotExistException}
     *
     * @param query the query to perform.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a result containing all the found rows and additional information.
     */
    SpatialViewResult query(SpatialViewQuery query, long timeout, TimeUnit timeUnit);

    /**
     * Queries a N1QL secondary index with the {@link CouchbaseEnvironment#queryTimeout() default query timeout}.
     * Said timeout includes the time it takes to retrieve all of the rows and errors from server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement in a DSL form (start with a static select() import)
     * @return a result containing all found rows and additional information.
     */
    N1qlQueryResult query(Statement statement);

    /**
     * Queries a N1QL secondary index with a custom timeout. Said timeout includes the time it
     * takes to retrieve all of the rows and errors from server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement in a DSL form (start with a static select() import)
     * @param timeout the custom full timeout, including the time to retrieve all rows, errors, etc...
     * @param timeUnit the unit for the timeout.
     * @return a result containing all found rows and additional information.
     */
    N1qlQueryResult query(Statement statement, long timeout, TimeUnit timeUnit);

    /**
     * Queries a N1QL secondary index with the {@link CouchbaseEnvironment#queryTimeout() default query timeout}.
     * Said timeout includes the time it takes to retrieve all of the rows and errors from server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param query the full {@link N1qlQuery}, including statement and any other additional parameter.
     * @return a result containing all found rows and additional information.
     */
    N1qlQueryResult query(N1qlQuery query);

    /**
     * Queries a N1QL secondary index with a custom timeout. Said timeout includes the time it
     * takes to retrieve all of the rows and errors from server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param query the full {@link N1qlQuery}, including statement and any other additional parameter.
     * @param timeout the custom full timeout, including the time to retrieve all rows, errors, etc...
     * @param timeUnit the unit for the timeout.
     * @return a result containing all found rows and additional information.
     */
    N1qlQueryResult query(N1qlQuery query, long timeout, TimeUnit timeUnit);

    /**
     * Queries a Full-Text Index
     *
     * This method throws under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param query the query builder.
     * @return a query result containing the matches and additional information.
     */
    @InterfaceStability.Committed
    SearchQueryResult query(SearchQuery query);

    /**
     * Queries a Full-Text Index
     *
     * This method throws under the following conditions:
     *
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param query the query builder.
     * @param timeout the custom full timeout, including the time to retrieve all rows, errors, etc...
     * @param timeUnit the unit for the timeout.
     * @return a query result containing the matches and additional information.
     */
    @InterfaceStability.Committed
    SearchQueryResult query(SearchQuery query, long timeout, TimeUnit timeUnit);

    /**
     * Queries Couchbase Analytics
     *
     * @param query the query builder.
     * @return a query result containing the rows and additional information.
     */
    @InterfaceStability.Committed
    AnalyticsQueryResult query(AnalyticsQuery query);

    /**
     * Queries Couchbase Analytics
     *
     * @param query the query builder.
     * @param timeout the custom full timeout, including the time to retrieve all rows, errors, etc...
     * @param timeUnit the unit for the timeout.
     * @return a query result containing the rows and additional information.
     */
    @InterfaceStability.Committed
    AnalyticsQueryResult query(AnalyticsQuery query, long timeout, TimeUnit timeUnit);

    /**
     * Unlocks a write-locked {@link Document} with the default key/value timeout.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document does not exist: {@link DocumentDoesNotExistException}
     * - A transient error occurred, most probably the CAS value was not correct: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to unlock.
     * @param cas the CAS value which is mandatory to unlock it.
     * @return a Boolean indicating if the unlock was successful or not.
     */
    Boolean unlock(String id, long cas);

    /**
     * Unlocks a write-locked {@link Document} with a custom timeout.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document does not exist: {@link DocumentDoesNotExistException}
     * - A transient error occurred, most probably the CAS value was not correct: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document to unlock.
     * @param cas the CAS value which is mandatory to unlock it.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a Boolean indicating if the unlock was successful or not.
     */
    Boolean unlock(String id, long cas, long timeout, TimeUnit timeUnit);

    /**
     * Unlocks a write-locked {@link Document} with the default key/value timeout.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document does not exist: {@link DocumentDoesNotExistException}
     * - A transient error occurred, most probably the CAS value was not correct: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document where ID and CAS are extracted from.
     * @return a Boolean indicating if the unlock was successful or not.
     */
    <D extends Document<?>> Boolean unlock(D document);

    /**
     * Unlocks a write-locked {@link Document} with a custom timeout.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The document does not exist: {@link DocumentDoesNotExistException}
     * - A transient error occurred, most probably the CAS value was not correct: {@link TemporaryLockFailureException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document where ID and CAS are extracted from.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a Boolean indicating if the unlock was successful or not.
     */
    <D extends Document<?>> Boolean unlock(D document, long timeout, TimeUnit timeUnit);

    /**
     * Renews the expiration time of a {@link Document} with the default key/value timeout.
     *
     * Compared to {@link #getAndTouch(Document)}, this method does not actually fetch the document from the server,
     * but it just resets its expiration time to the given value.
     *
     * This method throws under the following conditions:
     *
     * - The document doesn't exist: {@link DocumentDoesNotExistException}
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param expiry the new expiration time. 0 means no expiry.
     * @return a Boolean indicating if the touch had been successful or not.
     */
    Boolean touch(String id, int expiry);

    /**
     * Renews the expiration time of a {@link Document} with a custom timeout.
     *
     * Compared to {@link #getAndTouch(Document)}, this method does not actually fetch the document from the server,
     * but it just resets its expiration time to the given value.
     *
     * This method throws under the following conditions:
     *
     * - The document doesn't exist: {@link DocumentDoesNotExistException}
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param expiry the new expiration time. 0 means no expiry.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a Boolean indicating if the touch had been successful or not.
     */
    Boolean touch(String id, int expiry, long timeout, TimeUnit timeUnit);

    /**
     * Renews the expiration time of a {@link Document} with the default key/value timeout.
     *
     * Compared to {@link #getAndTouch(Document)}, this method does not actually fetch the document from the server,
     * but it just resets its expiration time to the given value.
     *
     * This method throws under the following conditions:
     *
     * - The document doesn't exist: {@link DocumentDoesNotExistException}
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to extract the ID and expiry from.
     * @return a Boolean indicating if the touch had been successful or not.
     */
    <D extends Document<?>> Boolean touch(D document);

    /**
     * Renews the expiration time of a {@link Document} with a custom timeout.
     *
     * Compared to {@link #getAndTouch(Document)}, this method does not actually fetch the document from the server,
     * but it just resets its expiration time to the given value.
     *
     * This method throws under the following conditions:
     *
     * - The document doesn't exist: {@link DocumentDoesNotExistException}
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document to extract the ID and expiry from.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a Boolean indicating if the touch had been successful or not.
     */
    <D extends Document<?>> Boolean touch(D document, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value or throw an exception if it does not
     * exist yet with the default kvTimeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta);

    /**
     * Increment or decrement a counter with the given value or throw an exception if it does not
     * exist yet with the default kvTimeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - If the document does not exist: {@link DocumentDoesNotExistException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param persistTo the persistence constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, PersistTo persistTo);

    /**
     * Increment or decrement a counter with the given value or throw an exception if it does not
     * exist yet with the default kvTimeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - If the document does not exist: {@link DocumentDoesNotExistException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param replicateTo the replication constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, ReplicateTo replicateTo);

    /**
     * Increment or decrement a counter with the given value or throw an exception if it does not
     * exist yet with the default kvTimeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - If the document does not exist: {@link DocumentDoesNotExistException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Increment or decrement a counter with the given value or throw an exception if it does not
     * exist yet with a custom timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value or throw an exception if it does not
     * exist yet with a custom timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - If the document does not exist: {@link DocumentDoesNotExistException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param persistTo the persistence constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value or throw an exception if it does not
     * exist yet with a custom timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - If the document does not exist: {@link DocumentDoesNotExistException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value or throw an exception if it does not
     * exist yet with a custom timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - If the document does not exist: {@link DocumentDoesNotExistException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the default
     * key/value timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the default
     * key/value timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param persistTo the persistence constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, PersistTo persistTo);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the default
     * key/value timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param replicateTo the replication constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, ReplicateTo replicateTo);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the default
     * key/value timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param persistTo the persistence constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * It is not allowed that the delta value will bring the actual value below zero.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the
     * default key/value timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * *Note*: Right now it is only possible to set the TTL of the counter document when it is created, not
     * when it is updated! If this behavior is needed, please refer to the subdocument API and use the JSON
     * based counters!
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param expiry the new expiration time for the document, only used on creation.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the
     * default key/value timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * *Note*: Right now it is only possible to set the TTL of the counter document when it is created, not
     * when it is updated! If this behavior is needed, please refer to the subdocument API and use the JSON
     * based counters!
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param expiry the new expiration time for the document, only used on creation.
     * @param persistTo the persistence constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry, PersistTo persistTo);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the
     * default key/value timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * *Note*: Right now it is only possible to set the TTL of the counter document when it is created, not
     * when it is updated! If this behavior is needed, please refer to the subdocument API and use the JSON
     * based counters!
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param expiry the new expiration time for the document, only used on creation.
     * @param replicateTo the replication constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry, ReplicateTo replicateTo);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the
     * default key/value timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * *Note*: Right now it is only possible to set the TTL of the counter document when it is created, not
     * when it is updated! If this behavior is needed, please refer to the subdocument API and use the JSON
     * based counters!
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param expiry the new expiration time for the document, only used on creation.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * *Note*: Right now it is only possible to set the TTL of the counter document when it is created, not
     * when it is updated! If this behavior is needed, please refer to the subdocument API and use the JSON
     * based counters!
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param expiry the new expiration time for the document, only used on creation.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * *Note*: Right now it is only possible to set the TTL of the counter document when it is created, not
     * when it is updated! If this behavior is needed, please refer to the subdocument API and use the JSON
     * based counters!
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param expiry the new expiration time for the document, only used on creation.
     * @param persistTo the persistence constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * *Note*: Right now it is only possible to set the TTL of the counter document when it is created, not
     * when it is updated! If this behavior is needed, please refer to the subdocument API and use the JSON
     * based counters!
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param expiry the new expiration time for the document, only used on creation.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
     *
     * *Note*: Right now it is only possible to set the TTL of the counter document when it is created, not
     * when it is updated! If this behavior is needed, please refer to the subdocument API and use the JSON
     * based counters!
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original increment/decrement has already happened, so the actual
     * increment/decrement and the watching for durability constraints are two separate tasks internally.**
     *
     * @param id the id of the document.
     * @param delta the increment or decrement amount.
     * @param initial the initial value.
     * @param expiry the new expiration time for the document, only used on creation.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Append a {@link Document}s content to an existing one with the default key/value timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document, identified by its id, from which the content is appended to the existing one.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document);

    /**
     * Append a {@link Document}s content to an existing one with the default key/value timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original append has already happened, so the actual
     * append and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is appended to the existing one.
     * @param persistTo the persistence constraint to watch.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document, PersistTo persistTo);

    /**
     * Append a {@link Document}s content to an existing one with the default key/value timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original append has already happened, so the actual
     * append and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is appended to the existing one.
     * @param replicateTo the replication constraint to watch.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document, ReplicateTo replicateTo);

    /**
     * Append a {@link Document}s content to an existing one with the default key/value timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original append has already happened, so the actual
     * append and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is appended to the existing one.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Append a {@link Document}s content to an existing one with a custom timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document, identified by its id, from which the content is appended to the existing one.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document, long timeout, TimeUnit timeUnit);

    /**
     * Append a {@link Document}s content to an existing one with a custom timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original append has already happened, so the actual
     * append and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is appended to the existing one.
     * @param persistTo the persistence constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Append a {@link Document}s content to an existing one with a custom timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original append has already happened, so the actual
     * append and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is appended to the existing one.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Append a {@link Document}s content to an existing one with a custom timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the appended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original append has already happened, so the actual
     * append and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is appended to the existing one.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Prepend a {@link Document}s content to an existing one with the default key/value timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document, identified by its id, from which the content is prepended to the existing one.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document);

    /**
     * Prepend a {@link Document}s content to an existing one with the default key/value timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original prepend has already happened, so the actual
     * prepend and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is prepended to the existing one.
     * @param persistTo the persistence constraint to watch.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document, PersistTo persistTo);

    /**
     * Prepend a {@link Document}s content to an existing one with the default key/value timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original prepend has already happened, so the actual
     * prepend and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is prepended to the existing one.
     * @param replicateTo the replication constraint to watch.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document, ReplicateTo replicateTo);

    /**
     * Prepend a {@link Document}s content to an existing one with the default key/value timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original prepend has already happened, so the actual
     * prepend and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is prepended to the existing one.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
     * Prepend a {@link Document}s content to an existing one with a custom timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param document the document, identified by its id, from which the content is prepended to the existing one.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document, long timeout, TimeUnit timeUnit);

    /**
     * Prepend a {@link Document}s content to an existing one with a custom timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original prepend has already happened, so the actual
     * prepend and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is prepended to the existing one.
     * @param persistTo the persistence constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Prepend a {@link Document}s content to an existing one with a custom timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original prepend has already happened, so the actual
     * prepend and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is prepended to the existing one.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Prepend a {@link Document}s content to an existing one with a custom timeout.
     *
     * The {@link Document} returned explicitly has the {@link Document#content()} set to null, because the server
     * does not return the prepended result, so at this point the client does not know how the {@link Document} now
     * looks like. A separate {@link Bucket#get(Document)} call needs to be issued in order to get the full
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
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     * - The request content is too big: {@link RequestTooBigException}
     * - If the document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * A {@link DurabilityException} typically happens if the given amount of replicas needed to fulfill the durability
     * constraint cannot be met because either the bucket does not have enough replicas configured or they are not
     * available in a failover event. As an example, if one replica is configured and {@link ReplicateTo#TWO} is used,
     * the observable is errored with a  {@link DurabilityException}. The same can happen if one replica is configured,
     * but one node has been failed over and not yet rebalanced (hence, on a subset of the partitions there is no
     * replica available). **It is important to understand that the original prepend has already happened, so the actual
     * prepend and the watching for durability constraints are two separate tasks internally.**
     *
     * @param document the document, identified by its id, from which the content is prepended to the existing one.
     * @param persistTo the persistence constraint to watch.
     * @param replicateTo the replication constraint to watch.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Prepare a sub-document lookup through a {@link LookupInBuilder builder API}. You can use the builder to
     * describe one or several lookup operations inside an existing {@link JsonDocument}, then execute the lookup
     * synchronously by calling the {@link LookupInBuilder#execute()} method. Only the paths that you looked up
     * inside the document will be transferred over the wire, limiting the network overhead for large documents.
     *
     * @param docId the id of the JSON document to lookup in.
     * @return a builder to describe the lookup(s) to perform.
     * @see LookupInBuilder#execute()
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    LookupInBuilder lookupIn(String docId);

    /**
     * Prepare a sub-document mutation through a {@link MutateInBuilder builder API}. You can use the builder to
     * describe one or several mutation operations inside an existing {@link JsonDocument}, then execute them
     * synchronously by calling the {@link MutateInBuilder#execute()} method. Only the values that you want
     * mutated inside the document will be transferred over the wire, limiting the network overhead for large documents.
     * A get followed by a replace of the whole document isn't needed anymore.
     *
     * Note that you can set the expiry, check the CAS and ask for durability constraints in the builder using methods
     * prefixed by "with": {@link MutateInBuilder#withExpiry(int) withExpiry},
     * {@link MutateInBuilder#withCas(long) withCas},
     * {@link MutateInBuilder#withDurability(PersistTo, ReplicateTo) withDurability}.
     *
     * @param docId the id of the JSON document to mutate in.
     * @return a builder to describe the mutation(s) to perform.
     * @see MutateInBuilder#execute()
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    MutateInBuilder mutateIn(String docId);

    /**
     * Add a key value pair into CouchbaseMap
     *
     * If the underlying document for the map does not exist, this operation will create a new document to back
     * the data structure.
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key to be stored
     * @param value value to be stored
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <V> boolean mapAdd(String docId, String key, V value);


    /**
     * Add a key value pair into CouchbaseMap
     *
     * If the underlying document for the map does not exist, this operation will create a new document to back
     * the data structure.
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key to be stored
     * @param value value to be stored
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <V> boolean mapAdd(String docId, String key, V value, long timeout, TimeUnit timeUnit);

    /**
     * Add a key value pair into CouchbaseMap with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * If the underlying document for the map does not exist, this operation will create a new document to back
     * the data structure.
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key to be stored
     * @param value value to be stored
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <V> boolean mapAdd(String docId, String key, V value, MutationOptionBuilder mutationOptionBuilder);


    /**
     * Add a key value pair into CouchbaseMap with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * If the underlying document for the map does not exist, this operation will create a new document to back
     * the data structure.
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key to be stored
     * @param value value to be stored
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <V> boolean mapAdd(String docId, String key, V value, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Get value of a key in the CouchbaseMap
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the key is not found in the map {@link PathNotFoundException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key in the map
     * @param valueType value type class
     * @return value if found
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <V> V mapGet(String docId, String key, Class<V> valueType);

    /**
     * Get value of a key in the CouchbaseMap
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key in the map
     * @param valueType value type class
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return value if found
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <V> V mapGet(String docId, String key, Class<V> valueType, long timeout, TimeUnit timeUnit);

    /**
     * Remove a key value pair from CouchbaseMap
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key to be removed
     * @return true if successful, even if the key doesn't exist
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    boolean mapRemove(String docId, String key);

    /**
     * Remove a key value pair from CouchbaseMap
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key to be removed
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful, even if the key doesn't exist
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    boolean mapRemove(String docId, String key, long timeout, TimeUnit timeUnit);

    /**
     * Remove a key value pair from CouchbaseMap with additional mutation options provided by {@link MutationOptionBuilder}.
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key to be removed
     * @return true if successful, even if the key doesn't exist
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    boolean mapRemove(String docId, String key, MutationOptionBuilder mutationOptionBuilder);

    /**
     * Remove a key value pair from CouchbaseMap with additional mutation options provided by {@link MutationOptionBuilder}.
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param key key to be removed
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful, even if the key doesn't exist
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    boolean mapRemove(String docId, String key, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Returns the number key value pairs in CouchbaseMap
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @return number of key value pairs
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    int mapSize(String docId);

    /**
     * Returns the number key value pairs in CouchbaseMap
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the map
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return number of key value pairs
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    int mapSize(String docId, long timeout, TimeUnit timeUnit);

    /**
     * Get element at an index in the CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the index is not found in the list {@link PathNotFoundException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index in list
     * @param elementType element type class
     * @return value if found
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E listGet(String docId, int index, Class<E> elementType);


    /**
     * Get element at an index in the CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index in list
     * @param elementType element type class
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return value if found
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E listGet(String docId, int index, Class<E> elementType, long timeout, TimeUnit timeUnit);

    /**
     * Push an element to tail of CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param element element to be pushed into the queue
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listAppend(String docId, E element);

    /**
     * Push an element to tail of CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param element element to be pushed into the queue
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listAppend(String docId, E element, long timeout, TimeUnit timeUnit);

    /**
     * Push an element to tail of CouchbaseList with additional mutation options provided by {@link MutationOptionBuilder}.
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param element element to be pushed into the queue
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listAppend(String docId, E element, MutationOptionBuilder mutationOptionBuilder);

    /**
     * Push an element to tail of CouchbaseList with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param element element to be pushed into the queue
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listAppend(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Remove an element from an index in CouchbaseList
     *
     * This method throws under the following conditions:
     * - {@link IndexOutOfBoundsException} if index is not found
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index of the element in list
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    boolean listRemove(String docId, int index);


    /**
     * Remove an element from an index in CouchbaseList
     *
     * This method throws under the following conditions:
     * - {@link IndexOutOfBoundsException} if index is not found
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index of the element in list
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    boolean listRemove(String docId, int index, long timeout, TimeUnit timeUnit);

    /**
     * Remove an element from an index in CouchbaseList with additional mutation options provided by
     * {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - {@link IndexOutOfBoundsException} if index is not found
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index of the element in list
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    boolean listRemove(String docId, int index, MutationOptionBuilder mutationOptionBuilder);


    /**
     * Remove an element from an index in CouchbaseList with additional mutation options provided by
     * {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - {@link IndexOutOfBoundsException} if index is not found
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index of the element in list
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    boolean listRemove(String docId, int index, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Shift list head to element in CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param element element to shift as head of list
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listPrepend(String docId, E element);

    /**
     * Shift list head to element in CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param element element to shift as head of list
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listPrepend(String docId, E element, long timeout, TimeUnit timeUnit);

    /**
     * Shift list head to element in CouchbaseList with additional mutation options provided by
     * {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param element element to shift as head of list
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listPrepend(String docId, E element, MutationOptionBuilder mutationOptionBuilder);

    /**
     * Shift list head to element in CouchbaseList with additional mutation options provided by
     * {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param element element to shift as head of list
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listPrepend(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Add an element at an index in CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index in the list
     * @param element element to be added
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listSet(String docId, int index, E element);

    /**
     * Add an element at an index in CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index in the list
     * @param element element to be added
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listSet(String docId, int index, E element, long timeout, TimeUnit timeUnit);

    /**
     * Add an element at an index in CouchbaseList with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index in the list
     * @param element element to be added
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listSet(String docId, int index, E element, MutationOptionBuilder mutationOptionBuilder);

    /**
     * Add an element at an index in CouchbaseList with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param index index in the list
     * @param element element to be added
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean listSet(String docId, int index, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Returns the number of elements in CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @return number of elements
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    int listSize(String docId);

    /**
     * Returns the number of elements in CouchbaseList
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the list
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return number of elements
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    int listSize(String docId, long timeout, TimeUnit timeUnit);

    /**
     * Add an element into CouchbaseSet
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to be pushed into the set
     * @return true if successful, false if the element exists in set
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean setAdd(String docId, E element);

    /**
     * Add an element into CouchbaseSet
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to be pushed into the set
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful, false if the element exists in set
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean setAdd(String docId, E element, long timeout, TimeUnit timeUnit);

    /**
     * Add an element into CouchbaseSet with additional mutation options provided by
     * {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to be pushed into the set
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @return true if successful, false if the element exists in set
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean setAdd(String docId, E element, MutationOptionBuilder mutationOptionBuilder);

    /**
     * Add an element into CouchbaseSet with additional mutation options provided by
     * {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to be pushed into the set
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful, false if the element exists in set
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean setAdd(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Check if an element exists in CouchbaseSet
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to check for existence
     * @return true if element exists, false if the element does not exist
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean setContains(String docId, E element);


    /**
     * Check if an element exists in CouchbaseSet
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to check for existence
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if element exists, false if the element does not exist
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean setContains(String docId, E element, long timeout, TimeUnit timeUnit);

    /**
     * Removes an element from CouchbaseSet
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to be removed
     * @return element removed from set (fails silently by returning the element is not found in set)
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E setRemove(String docId, E element);

    /**
     * Removes an element from CouchbaseSet
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to be removed
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return element removed from set (fails silently by returning the element is not found in set)
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E setRemove(String docId, E element, long timeout, TimeUnit timeUnit);

    /**
     * Removes an element from CouchbaseSet with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to be removed
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @return element removed from set (fails silently by returning the element is not found in set)
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E setRemove(String docId, E element, MutationOptionBuilder mutationOptionBuilder);


    /**
     * Removes an element from CouchbaseSet with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param element element to be removed
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return element removed from set (fails silently by returning the element is not found in set)
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E setRemove(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Returns the number of elements in CouchbaseSet
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @return number of elements in set
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    int setSize(String docId);


    /**
     * Returns the number of elements in CouchbaseSet
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the set
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return number of elements in set
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    int setSize(String docId, long timeout, TimeUnit timeUnit);

    /**
     * Add an element into CouchbaseQueue
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param element element to be pushed into the queue
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean queuePush(String docId, E element);

    /**
     * Add an element into CouchbaseQueue
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param element element to be pushed into the queue
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean queuePush(String docId, E element, long timeout, TimeUnit timeUnit);

    /**
     * Add an element into CouchbaseQueue with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param element element to be pushed into the queue
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean queuePush(String docId, E element, MutationOptionBuilder mutationOptionBuilder);

    /**
     * Add an element into CouchbaseQueue with additional mutation options provided by {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param element element to be pushed into the queue
     * @param mutationOptionBuilder mutation options {@link MutationOptionBuilder}
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return true if successful
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> boolean queuePush(String docId, E element, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Removes the first element from CouchbaseQueue
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param elementType element type class
     * @return element removed from queue
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E queuePop(String docId, Class<E> elementType);


    /**
     * Removes the first element from CouchbaseQueue
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param elementType element type class
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return element removed from queue
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E queuePop(String docId, Class<E> elementType, long timeout, TimeUnit timeUnit);

    /**
     * Removes the first element from CouchbaseQueue with additional mutation options provided by
     * {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param elementType element type class
     * @return element removed from queue
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E queuePop(String docId, Class<E> elementType, MutationOptionBuilder mutationOptionBuilder);

    /**
     * Removes the first element from CouchbaseQueue with additional mutation options provided by
     * {@link MutationOptionBuilder}
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     * {@link DurabilityException}.
     * - A CAS value was set and it did not match with the server: {@link CASMismatchException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param elementType element type class
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return element removed from queue
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    <E> E queuePop(String docId, Class<E> elementType, MutationOptionBuilder mutationOptionBuilder, long timeout, TimeUnit timeUnit);

    /**
     * Returns the number of elements in CouchbaseQueue
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @return number of elements
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    int queueSize(String docId);

    /**
     * Returns the number of elements in CouchbaseQueue
     *
     * This method throws under the following conditions:
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     * retrying: {@link RequestCancelledException}
     * - If the underlying couchbase document does not exist: {@link DocumentDoesNotExistException}
     * - The server is currently not able to process the request, retrying may help: {@link TemporaryFailureException}
     * - The server is out of memory: {@link CouchbaseOutOfMemoryException}
     * - Unexpected errors are caught and contained in a generic {@link CouchbaseException}.
     *
     * @param docId document id backing the queue
     * @param timeout the custom timeout
     * @param timeUnit the unit for the timeout
     * @return number of elements
     */
    @InterfaceStability.Committed
    @InterfaceAudience.Public
    int queueSize(String docId, long timeout, TimeUnit timeUnit);

    /**
     * Invalidates and clears the internal query cache.
     *
     * This method can be used to explicitly clear the internal N1QL query cache. This cache will
     * be filled with non-adhoc query statements (query plans) to speed up those subsequent executions.
     *
     * Triggering this method will wipe out the complete cache, which will not cause an interruption but
     * rather all queries need to be re-prepared internally. This method is likely to be deprecated in
     * the future once the server side query engine distributes its state throughout the cluster.
     *
     * This method will not throw under any conditions.
     *
     * @return the number of entries in the cache before it was cleared out.
     */
    int invalidateQueryCache();

    /**
     * Provides access to the {@link BucketManager} for administrative access.
     *
     * The manager lets you perform operations such as flushing a bucket or creating and managing design documents.
     *
     * @return the bucket manager for administrative operations.
     */
    BucketManager bucketManager();

    /**
     * The {@link Repository} provides access to full object document mapping (ODM) capabilities.
     *
     * It allows you to work with POJO entities only and use annotations to customize the behaviour and mapping
     * characteristics.
     *
     * @return the repository for ODM capabilities.
     */
    @InterfaceAudience.Public
    @InterfaceStability.Experimental
    Repository repository();

    /**
     * Closes this bucket with the default disconnect timeout.
     *
     * @return true if the bucket was successfully closed.
     */
    Boolean close();

    /**
     * Closes this bucket with a custom timeout.
     *
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return true if the bucket was successfully closed.
     */
    Boolean close(long timeout, TimeUnit timeUnit);

    /**
     * Returns true if this bucket is already closed, false if it is still open.
     *
     * @return true if closed, false otherwise.
     */
    boolean isClosed();

    /**
     * Performs a diagnostic active "ping" call with a custom report ID on all services.
     *
     * Since no timeout is provided, the management timeout from the environment will be used.
     *
     * @param reportId the report ID to use in the report.
     * @return a ping report once created.
     */
    PingReport ping(String reportId);

    /**
     * Performs a diagnostic active "ping" call with a custom report ID on all services.
     *
     * Note that since each service has different timeouts, you need to provide a timeout that suits
     * your needs (how long each individual service ping should take max before it times out).
     *
     * @param reportId the report ID to use in the report.
     * @param timeout the timeout for each individual service.
     * @param timeUnit the unit for the timeout.
     * @return a ping report once created.
     */
    PingReport ping(String reportId, long timeout, TimeUnit timeUnit);

    /**
     * Performs a diagnostic active "ping" call with a random report id on all services.
     *
     * Since no timeout is provided, the management timeout from the environment will be used.
     *
     * @return a ping report once created.
     */
    PingReport ping();

    /**
     * Performs a diagnostic active "ping" call on all services with a random report id.
     *
     * Note that since each service has different timeouts, you need to provide a timeout that suits
     * your needs (how long each individual service ping should take max before it times out).
     *
     * @param timeout the timeout for each individual service.
     * @param timeUnit the unit for the timeout.
     * @return a ping report once created.
     */
    PingReport ping(long timeout, TimeUnit timeUnit);

    /**
     * Performs a diagnostic active "ping" call with a random report id on all services.
     *
     * Since no timeout is provided, the management timeout from the environment will be used.
     *
     * @param services collection of services which should be included.
     * @return a ping report once created.
     */
    PingReport ping(Collection<ServiceType> services);

    /**
     * Performs a diagnostic active "ping" call on a list of services with a random report id.
     *
     * Note that since each service has different timeouts, you need to provide a timeout that suits
     * your needs (how long each individual service ping should take max before it times out).
     *
     * @param services collection of services which should be included.
     * @param timeout the timeout for each individual service.
     * @param timeUnit the unit for the timeout.
     * @return a ping report once created.
     */
    PingReport ping(Collection<ServiceType> services, long timeout, TimeUnit timeUnit);

    /**
     * Performs a diagnostic active "ping" call with a custom report id on all services.
     *
     * Since no timeout is provided, the management timeout from the environment will be used.
     *
     * @param services collection of services which should be included.
     * @return a ping report once created.
     */
    PingReport ping(String reportId, Collection<ServiceType> services);

    /**
     * Performs a diagnostic active "ping" call against all the services provided with a custom
     * report id.
     *
     * Note that since each service has different timeouts, you need to provide a timeout that suits
     * your needs (how long each individual service ping should take max before it times out).
     *
     * @param reportId the report ID to use in the report.
     * @param services collection of services which should be included.
     * @param timeout the timeout for each individual service.
     * @param timeUnit the unit for the timeout.
     * @return a ping report once created.
     */
    PingReport ping(String reportId, Collection<ServiceType> services, long timeout, TimeUnit timeUnit);

 /**
     * Exports the deferred result handle to a serialized form which can be used across SDKs
     * @param handle the deferred result handle
     * @return the serialized bytes
     */
    @InterfaceStability.Experimental
    byte[] exportAnalyticsDeferredResultHandle(AnalyticsDeferredResultHandle handle);

    /**
     * Imports from json to create a {@link AnalyticsDeferredResultHandle}.
     * @param b the bytes to be converted to handle
     * @return the deferred handle instance
     */
    @InterfaceStability.Experimental
    AnalyticsDeferredResultHandle importAnalyticsDeferredResultHandle(byte[] b);
}
