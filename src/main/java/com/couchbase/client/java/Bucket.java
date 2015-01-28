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

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.bucket.BucketManager;
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
import com.couchbase.client.java.error.ViewDoesNotExistException;
import com.couchbase.client.java.query.PreparedQuery;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryPlan;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.SpatialViewResult;
import com.couchbase.client.java.view.View;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import rx.Observable;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
     * @param type the {@link ReplicaMode} to select.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    List<JsonDocument> getFromReplica(String id, ReplicaMode type);

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
     * @param type the {@link ReplicaMode} to select.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    List<JsonDocument> getFromReplica(String id, ReplicaMode type, long timeout, TimeUnit timeUnit);

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
     * @param document the document to extract the ID from.
     * @param type the {@link ReplicaMode} to select.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type);

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
     * @param document the document to extract the ID from.
     * @param type the {@link ReplicaMode} to select.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type, long timeout, TimeUnit timeUnit);

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
     * @param type the {@link ReplicaMode} to select.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target);

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
     * @param type the {@link ReplicaMode} to select.
     * @param target the target document type to use.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a List containing zero to N {@link JsonDocument}s.
     */
    <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target, long timeout, TimeUnit timeUnit);

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
     * Insert or replace a {@link Document} with the default key/value timeout.
     *
     * If the given {@link Document} (identified by its unique ID) already exists, it will be overridden by the current
     * one. The returned {@link Document} contains original properties, but has the refreshed CAS value set.
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
     * Insert or replace a {@link Document} with a custom timeout.
     *
     * If the given {@link Document} (identified by its unique ID) already exists, it will be overridden by the current
     * one. The returned {@link Document} contains original properties, but has the refreshed CAS value set.
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
     * Insert or replace a {@link Document} and watch for durability constraints with the default key/value timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
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
     * Insert or replace a {@link Document} and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
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
     * Insert or replace a {@link Document} and watch for durability constraints with the default key/value timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
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
     * Insert or replace a {@link Document} and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
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
     * Insert or replace a {@link Document} and watch for durability constraints with the default key/value timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
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
     * Insert or replace a {@link Document} and watch for durability constraints with a custom timeout.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
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
     * Queries a Couchbase Server {@link View} with the default view timeout.
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
     * Queries a Couchbase Server Spatial {@link View} with the default view timeout.
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
     * Experimental: Queries a N1QL secondary index with the default query timeout. Said timeout includes the time it
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
     * @return a result containing all found rows and additional information.
     */
    QueryResult query(Statement statement);

    /**
     * Experimental: Queries a N1QL secondary index with a custom timeout. Said timeout includes the time it
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
    QueryResult query(Statement statement, long timeout, TimeUnit timeUnit);

    /**
     * Experimental: Queries a N1QL secondary index with the default query timeout. Said timeout includes the time it
     * takes to retrieve all of the rows and errors from server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param query the full {@link Query}, including statement and any other additional parameter.
     * @return a result containing all found rows and additional information.
     */
    QueryResult query(Query query);

    /**
     * Experimental: Queries a N1QL secondary index with a custom timeout. Said timeout includes the time it
     * takes to retrieve all of the rows and errors from server.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param query the full {@link Query}, including statement and any other additional parameter.
     * @param timeout the custom full timeout, including the time to retrieve all rows, errors, etc...
     * @param timeUnit the unit for the timeout.
     * @return a result containing all found rows and additional information.
     */
    QueryResult query(Query query, long timeout, TimeUnit timeUnit);


    /**
     * Experimental: Queries a N1QL secondary index and prepare an execution plan via the given
     * {@link String} statement, with the default timeout. Statement can contain placeholders.
     *
     * The resulting {@link QueryPlan} can be cached and (re)used later in a {@link PreparedQuery}.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement to prepare a plan for.
     * @return a {@link QueryPlan} that can be cached and reused later in {@link PreparedQuery}.
     */
    QueryPlan prepare(String statement);

    /**
     * Experimental: Queries a N1QL secondary index and prepare an execution plan via the given
     * {@link Statement}, with the default timeout. Statement can contain placeholders.
     *
     * The resulting {@link QueryPlan} can be cached and (re)used later in a {@link PreparedQuery}.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement to prepare a plan for.
     * @return a {@link QueryPlan} that can be cached and reused later in {@link PreparedQuery}.
     */
    QueryPlan prepare(Statement statement);

    /**
     * Experimental: Queries a N1QL secondary index and prepare an execution plan via the given
     * {@link String} statement, with a custom timeout. Statement can contain placeholders.
     *
     * The resulting {@link QueryPlan} can be cached and (re)used later in a {@link PreparedQuery}.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement to prepare a plan for.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link QueryPlan} that can be cached and reused later in {@link PreparedQuery}.
     */
    QueryPlan prepare(String statement, long timeout, TimeUnit timeUnit);

    /**
     * Experimental: Queries a N1QL secondary index and prepare an execution plan via the given
     * {@link Statement}, with a custom timeout. Statement can contain placeholders.
     *
     * The resulting {@link QueryPlan} can be cached and (re)used later in a {@link PreparedQuery}.
     *
     * This method throws under the following conditions:
     *
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
     * - The producer outpaces the SDK: {@link BackpressureException}
     * - The operation had to be cancelled while on the wire or the retry strategy cancelled it instead of
     *   retrying: {@link RequestCancelledException}
     *
     * @param statement the statement to prepare a plan for.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link QueryPlan} that can be cached and reused later in {@link PreparedQuery}.
     */
    QueryPlan prepare(Statement statement, long timeout, TimeUnit timeUnit);

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
     * - The CAS value was not correct: {@link CASMismatchException}
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
     * - The CAS value was not correct: {@link CASMismatchException}
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
     * - The CAS value was not correct: {@link CASMismatchException}
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
     * - The CAS value was not correct: {@link CASMismatchException}
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
     * - The operation takes longer than the specified timeout: {@link TimeoutException} wrapped in a {@link RuntimeException}
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
    <D extends Document<?>> Boolean touch(D document);

    /**
     * Renews the expiration time of a {@link Document} with a custom timeout.
     *
     * Compared to {@link #getAndTouch(Document)}, this method does not actually fetch the document from the server,
     * but it just resets its expiration time to the given value.
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
     * @param document the document to extract the ID and expiry from.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a copy of the document inserted.
     */
    <D extends Document<?>> Boolean touch(D document, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a default value of 0 with the default key/value
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
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta);

    /**
     * Increment or decrement a counter with the given value and a default value of 0 with a custom timeout.
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long timeout, TimeUnit timeUnit);

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
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial);

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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, long timeout, TimeUnit timeUnit);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with the
     * default key/value timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
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
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry);

    /**
     * Increment or decrement a counter with the given value and a initial value if it does not exist with a custom
     * timeout.
     *
     * This method allows to set an expiration time for the document as well. It is not allowed that the delta value
     * will bring the actual value below zero.
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
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a {@link Document} containing the resulting value.
     */
    JsonLongDocument counter(String id, long delta, long initial, int expiry, long timeout, TimeUnit timeUnit);

    /**
     * Append a {@link Document} to another one with the default key/value timeout.
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
     * @param document the document to be appended.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document);

    /**
     * Append a {@link Document} to another one with a custom timeout.
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
     * @param document the document to be appended.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D append(D document, long timeout, TimeUnit timeUnit);

    /**
     * Prepend a {@link Document} to another one with the default key/value timeout.
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
     * @param document the document to be prepended.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document);

    /**
     * Prepend a {@link Document} to another one with a custom timeout.
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
     * @param document the document to be prepended.
     * @param timeout the custom timeout.
     * @param timeUnit the unit for the timeout.
     * @return a document which mirrors the one supplied as an argument.
     */
    <D extends Document<?>> D prepend(D document, long timeout, TimeUnit timeUnit);

    /**
     * Provides access to the {@link BucketManager} for administrative access.
     *
     * The manager lets you perform operations such as flushing a bucket or creating and managing design documents.
     *
     * @return the bucket manager for administrative operations.
     */
    BucketManager bucketManager();

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

}
