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

import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.DurabilityException;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import rx.Observable;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Defines operations that can be executed against a Couchbase Server bucket.
 *
 * Note that only a subset of the provided operations are available for "memcached" type buckets. Also, some other
 * operations are only available against specific versions of Couchbase Server. If the operation is not available,
 * the {@link Observable} will fail immediately with a {@link UnsupportedOperationException}.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public interface Bucket {

    AsyncBucket async();

    String name();

    /**
     * Retrieves a {@link JsonDocument} by its unique ID.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * @param id the unique ID of the document.
     * @return an {@link Observable} eventually containing the found {@link JsonDocument}.
     */
    JsonDocument get(String id);

    JsonDocument get(String id, long timeout, TimeUnit timeUnit);

    /**
     * Retrieves any type of {@link Document} by its unique ID.
     *
     * The document ID is taken out of the {@link Document} provided, as well as the target type to return. Note that
     * not the same document is returned, but rather a new one of the same type with the freshly loaded properties.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * @param document the source document from which the ID is taken and the type is inferred.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> D get(D document);

    <D extends Document<?>> D get(D document, long timeout, TimeUnit timeUnit);


    /**
     * Retrieves any type of {@link Document} by its unique ID.
     *
     * This method differs from {@link #get(String)} in that if a specific {@link Document} type is passed in, the
     * appropriate {@link Transcoder} will be selected (and not JSON conversion).
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * @param id the unique ID of the document.
     * @param target the target document type to use.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> D get(String id, Class<D> target);

    <D extends Document<?>> D get(String id, Class<D> target, long timeout, TimeUnit timeUnit);

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
     * @param id id the unique ID of the document.
     * @param type the {@link ReplicaMode} to select.
     * @return an {@link Observable} eventually containing zero to N {@link JsonDocument}s.
     */
    List<JsonDocument> getFromReplica(String id, ReplicaMode type);

    List<JsonDocument> getFromReplica(String id, ReplicaMode type, long timeout, TimeUnit timeUnit);

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
     * @param document the source document from which the ID is taken and the type is inferred.
     * @param type the {@link ReplicaMode} to select.
     * @return an {@link Observable} eventually containing zero to N {@link Document}s.
     */
    <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type);

    <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type, long timeout, TimeUnit timeUnit);

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
     * @param id id the unique ID of the document.
     * @param type the {@link ReplicaMode} to select.
     * @param target the target document type to use.
     * @return an {@link Observable} eventually containing zero to N {@link Document}s.
     */
    <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target);

    <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target, long timeout, TimeUnit timeUnit);
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
     * @param id id the unique ID of the document.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @return an {@link Observable} eventually containing the found {@link JsonDocument}.
     */
    JsonDocument getAndLock(String id, int lockTime);

    JsonDocument getAndLock(String id, int lockTime, long timeout, TimeUnit timeUnit);

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
     * @param document the source document from which the ID is taken and the type is inferred.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> D getAndLock(D document, int lockTime);

    <D extends Document<?>> D getAndLock(D document, int lockTime, long timeout, TimeUnit timeUnit);

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
     * @param id id the unique ID of the document.
     * @param lockTime the time to write lock the document (max. 30 seconds).
     * @param target the target document type to use.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target);

    <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve and touch a {@link JsonDocument} by its unique ID.
     *
     * If the document is found, a {@link JsonDocument} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(String)}, but in addition it touches the document, which will reset
     * its configured expiration time to the value provided.
     *
     * @param id id the unique ID of the document.
     * @param expiry the new expiration time for the document.
     * @return an {@link Observable} eventually containing the found {@link JsonDocument}.
     */
    JsonDocument getAndTouch(String id, int expiry);

    JsonDocument getAndTouch(String id, int expiry, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve and touch a {@link Document} by its unique ID.
     *
     * If the document is found, a {@link Document} is returned. If the document is not found, the
     * {@link Observable} completes without an item emitted.
     *
     * This method works similar to {@link #get(Document)}, but in addition it touches the document, which will reset
     * its configured expiration time set on the given document itself.
     *
     * @param document the source document from which the ID and expiry is taken and the type is inferred.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> D getAndTouch(D document);

    <D extends Document<?>> D getAndTouch(D document, long timeout, TimeUnit timeUnit);

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
     * @param id id the unique ID of the document.
     * @param expiry the new expiration time for the document.
     * @param target the target document type to use.
     * @return an {@link Observable} eventually containing the found {@link Document}.
     */
    <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target);

    <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target, long timeout, TimeUnit timeUnit);

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
     * @param document the {@link Document} to insert.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> D insert(D document);

    <D extends Document<?>> D insert(D document, long timeout, TimeUnit timeUnit);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The original insert failed because the document is already stored: {@link DocumentAlreadyExistsException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo);

    <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The original insert failed because the document is already stored: {@link DocumentAlreadyExistsException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D insert(D document, PersistTo persistTo);

    <D extends Document<?>> D insert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Insert a {@link Document} if it does not exist already and watch for durability constraints.
     *
     * This method works exactly like {@link #insert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The original insert failed because the document is already stored: {@link DocumentAlreadyExistsException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D insert(D document, ReplicateTo replicateTo);
    <D extends Document<?>> D insert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

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
     * @param document the {@link Document} to upsert.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> D upsert(D document);
    <D extends Document<?>> D upsert(D document, long timeout, TimeUnit timeUnit);

    /**
     * Insert or replace a {@link Document} and watch for durability constraints.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo);
    <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Insert or replace a {@link Document} and watch for durability constraints.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D upsert(D document, PersistTo persistTo);
    <D extends Document<?>> D upsert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);


    /**
     * Insert or replace a {@link Document} and watch for durability constraints.
     *
     * This method works exactly like {@link #upsert(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo);
    <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

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
     * @param document the {@link Document} to replace.
     * @return an {@link Observable} eventually containing a new {@link Document}.
     */
    <D extends Document<?>> D replace(D document);
    <D extends Document<?>> D replace(D document, long timeout, TimeUnit timeUnit);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The original insert failed because the document is not found: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo);
    <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The original insert failed because the document is not found: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D replace(D document, PersistTo persistTo);
    <D extends Document<?>> D replace(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);

    /**
     * Replace a {@link Document} if it does exist and watch for durability constraints.
     *
     * This method works exactly like {@link #replace(Document)}, but afterwards watches the server states if the given
     * durability constraints are met. If this is the case, a new document is returned which contains the original
     * properties, but has the refreshed CAS value set.
     *
     * Under the following conditions errors can occur:
     *
     * - The original insert failed because the document is not found: {@link DocumentDoesNotExistException}
     * - The durability constraint could not be fulfilled because of a temporary or persistent problem:
     *   {@link DurabilityException}.
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
    <D extends Document<?>> D replace(D document, ReplicateTo replicateTo);
    <D extends Document<?>> D replace(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    <D extends Document<?>> D remove(D document);
    <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo);
    <D extends Document<?>> D remove(D document, PersistTo persistTo);
    <D extends Document<?>> D remove(D document, ReplicateTo replicateTo);
    <D extends Document<?>> D remove(D document, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> D remove(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> D remove(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    JsonDocument remove(String id);
    JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo);
    JsonDocument remove(String id, PersistTo persistTo);
    JsonDocument remove(String id, ReplicateTo replicateTo);
    JsonDocument remove(String id, long timeout, TimeUnit timeUnit);
    JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);
    JsonDocument remove(String id, PersistTo persistTo, long timeout, TimeUnit timeUnit);
    JsonDocument remove(String id, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit);

    <D extends Document<?>> D remove(String id, Class<D> target);
    <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target);
    <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target);
    <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target);
    <D extends Document<?>> D remove(String id, Class<D> target, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit);

    ViewResult query(ViewQuery query);
    QueryResult query(Query query);
    QueryResult query(String query);
    ViewResult query(ViewQuery query, long timeout, TimeUnit timeUnit);
    QueryResult query(Query query, long timeout, TimeUnit timeUnit);
    QueryResult query(String query, long timeout, TimeUnit timeUnit);

    Boolean unlock(String id, long cas);
    <D extends Document<?>> Boolean unlock(D document);
    Boolean unlock(String id, long cas, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> Boolean unlock(D document, long timeout, TimeUnit timeUnit);

    Boolean touch(String id, int expiry);
    <D extends Document<?>> Boolean touch(D document);
    Boolean touch(String id, int expiry, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> Boolean touch(D document, long timeout, TimeUnit timeUnit);

    JsonLongDocument counter(String id, long delta);
    JsonLongDocument counter(String id, long delta, long initial);
    JsonLongDocument counter(String id, long delta, long initial, int expiry);
    JsonLongDocument counter(String id, long delta, long timeout, TimeUnit timeUnit);
    JsonLongDocument counter(String id, long delta, long initial, long timeout, TimeUnit timeUnit);
    JsonLongDocument counter(String id, long delta, long initial, int expiry, long timeout, TimeUnit timeUnit);

    BucketManager bucketManager();

    <D extends Document<?>> D append(D document);
    <D extends Document<?>> D prepend(D document);

    <D extends Document<?>> D append(D document, long timeout, TimeUnit timeUnit);
    <D extends Document<?>> D prepend(D document, long timeout, TimeUnit timeUnit);

    /**
     * Closes the {@link Bucket}.
     *
     * @return an {@link Observable} eventually containing a new {@link Boolean} after close.
     */
    Boolean close();
    Boolean close(long timeout, TimeUnit timeUnit);

}
