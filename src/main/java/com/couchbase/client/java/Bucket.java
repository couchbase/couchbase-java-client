package com.couchbase.client.java;

import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.LongDocument;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.ViewQuery;
import com.couchbase.client.java.query.ViewResult;
import rx.Observable;

/**
 * Represents a Couchbase Server bucket.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public interface Bucket {


    Observable<JsonDocument> get(String id);
    <D extends Document<?>> Observable<D> get(D document);
    <D extends Document<?>> Observable<D> get(String id, Class<D> target);

    Observable<JsonDocument> getFromReplica(String id, ReplicaMode type);
    <D extends Document<?>> Observable<D> getFromReplica(D document, ReplicaMode type);
    <D extends Document<?>> Observable<D> getFromReplica(String id, ReplicaMode type, Class<D> target);


    Observable<JsonDocument> getAndLock(String id, int lockTime);
    <D extends Document<?>> Observable<D> getAndLock(D document, int lockTime);
    <D extends Document<?>> Observable<D> getAndLock(String id, int lockTime, Class<D> target);

    Observable<JsonDocument> getAndTouch(String id, int expiry);
    <D extends Document<?>> Observable<D> getAndTouch(D document);
    <D extends Document<?>> Observable<D> getAndTouch(String id, int expiry, Class<D> target);

    /**
    * Insert a {@link Document}.
    *
    * @param document the document to insert.
    * @param <D> the type of the document, which is inferred from the instance.
    * @return the document again.
    */
    <D extends Document<?>> Observable<D> insert(D document);

    <D extends Document<?>> Observable<D> insert(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
    * Upsert a {@link Document}.
    *
    * @param document the document to upsert.
    * @param <D> the type of the document, which is inferred from the instance.
    * @return the document again.
    */
    <D extends Document<?>> Observable<D> upsert(D document);

    <D extends Document<?>> Observable<D> upsert(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
    * Replace a {@link Document}.
    *
    * @param document the document to replace.
    * @param <D> the type of the document, which is inferred from the instance.
    * @return the document again.
    */
    <D extends Document<?>> Observable<D> replace(D document);

    <D extends Document<?>> Observable<D> replace(D document, PersistTo persistTo, ReplicateTo replicateTo);


    /**
    * Remove the given {@link Document}.
    *
    * @param document
    * @param <D>
    * @return
    */
    <D extends Document<?>> Observable<D> remove(D document);

    <D extends Document<?>> Observable<D> remove(D document, PersistTo persistTo, ReplicateTo replicateTo);

    /**
    * Remove the document by the given document ID.
    *
    * @param id
    * @return
    */
    Observable<JsonDocument> remove(String id);

    Observable<JsonDocument> remove(String id, PersistTo persistTo, ReplicateTo replicateTo);


    /**
    * Remove the document by the given ID and cast it to a custom target document.
    *
    * @param id
    * @param target
    * @param <D>
    * @return
    */
    <D extends Document<?>> Observable<D> remove(String id, Class<D> target);

    <D extends Document<?>> Observable<D> remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target);

    /**
    * Queries a View defined by the {@link ViewQuery} and returns a {@link ViewResult}
    * for each emitted row in the view.
    *
    * @param query the query for the view.
    * @return a row for each result (from 0 to N).
    */
    Observable<ViewResult> query(ViewQuery query);

    /**
     * Runs a {@link Query} and returns a {@link QueryResult} for each emitted row in the result.
     *
     * @param query the query.
     * @return a row for each result (from 0 to N).
     */
    Observable<QueryResult> query(Query query);

    /**
     * Runs a raw N1QL query and returns a {@link QueryResult} for each emitted row in the result.
     *
     * @param query the query.
     * @return
     */
    Observable<QueryResult> query(String query);

    /**
     * Flushes the bucket.
     *
     * @return
     */
    Observable<Boolean> flush();

    Observable<Boolean> unlock(String id, long cas);
    <D extends Document<?>> Observable<Boolean> unlock(D document);

    Observable<Boolean> touch(String id, int expiry);
    <D extends Document<?>> Observable<Boolean> touch(D document);

    Observable<LongDocument> counter(String id, long delta, long initial, int expiry);

}
