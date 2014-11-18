package com.couchbase.client.java;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.bucket.AsyncBucketManager;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.bucket.DefaultBucketManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.query.AsyncQueryResult;
import com.couchbase.client.java.query.DefaultQueryResult;
import com.couchbase.client.java.query.Query;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.transcoder.Transcoder;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.DefaultViewResult;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import rx.functions.Func1;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CouchbaseBucket implements Bucket {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncBucket asyncBucket;
    private final CouchbaseEnvironment environment;
    private final long kvTimeout;
    private final String name;
    private final String password;
    private final ClusterFacade core;

    public CouchbaseBucket(final CouchbaseEnvironment env, final ClusterFacade core, final String name, final String password,
        final List<Transcoder<? extends Document, ?>> customTranscoders) {
        asyncBucket = new CouchbaseAsyncBucket(core, name, password, customTranscoders);
        this.environment = env;
        this.kvTimeout = env.kvTimeout();
        this.name = name;
        this.password = password;
        this.core = core;
    }

    @Override
    public AsyncBucket async() {
        return asyncBucket;
    }

    @Override
    public String name() {
        return asyncBucket.name();
    }

    @Override
    public JsonDocument get(String id) {
        return get(id, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument get(String id, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .get(id)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D get(D document) {
        return get(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D get(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .get(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D get(String id, Class<D> target) {
        return get(id, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D get(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .get(id, target)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public List<JsonDocument> getFromReplica(String id, ReplicaMode type) {
        return getFromReplica(id, type, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public List<JsonDocument> getFromReplica(String id, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getFromReplica(id, type)
            .toList()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type) {
        return getFromReplica(document, type, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(D document, ReplicaMode type, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getFromReplica(document, type)
            .toList()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target) {
        return getFromReplica(id, type, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> List<D> getFromReplica(String id, ReplicaMode type, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getFromReplica(id, type, target)
            .toList()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public JsonDocument getAndLock(String id, int lockTime) {
        return getAndLock(id, lockTime, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument getAndLock(String id, int lockTime, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getAndLock(id, lockTime)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D getAndLock(D document, int lockTime) {
        return getAndLock(document, lockTime, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndLock(D document, int lockTime, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getAndLock(document, lockTime)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target) {
        return getAndLock(id, lockTime, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndLock(String id, int lockTime, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getAndLock(id, lockTime, target)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public JsonDocument getAndTouch(String id, int expiry) {
        return getAndTouch(id, expiry, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument getAndTouch(String id, int expiry, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getAndTouch(id, expiry)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(D document) {
        return getAndTouch(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getAndTouch(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target) {
        return getAndTouch(id, expiry, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D getAndTouch(String id, int expiry, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .getAndTouch(id, expiry, target)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D insert(D document) {
        return insert(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .insert(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return insert(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .insert(document, persistTo, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo) {
        return insert(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .insert(document, persistTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D insert(D document, ReplicateTo replicateTo) {
        return insert(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D insert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .insert(document, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D upsert(D document) {
        return upsert(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .upsert(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return upsert(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .upsert(document, persistTo, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo) {
        return upsert(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .upsert(document, persistTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo) {
        return upsert(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D upsert(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .upsert(document, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D replace(D document) {
        return replace(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .replace(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return replace(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .replace(document, persistTo, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo) {
        return replace(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .replace(document, persistTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D replace(D document, ReplicateTo replicateTo) {
        return replace(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D replace(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .replace(document, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D remove(D document) {
        return remove(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(document, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo) {
        return remove(document, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(D document, ReplicateTo replicateTo) {
        return remove(document, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(document, persistTo, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D remove(D document, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(document, persistTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D remove(D document, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(document, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public JsonDocument remove(String id) {
        return remove(id, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo) {
        return remove(id, persistTo, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo) {
        return remove(id, persistTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument remove(String id, ReplicateTo replicateTo) {
        return remove(id, replicateTo, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonDocument remove(String id, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(id)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(id, persistTo, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public JsonDocument remove(String id, PersistTo persistTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(id, persistTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public JsonDocument remove(String id, ReplicateTo replicateTo, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(id, replicateTo)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D remove(String id, Class<D> target) {
        return remove(id, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target) {
        return remove(id, persistTo, replicateTo, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target) {
        return remove(id, persistTo, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target) {
        return remove(id, replicateTo, target, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D remove(String id, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(id, target)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(id, persistTo, replicateTo, target)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D remove(String id, PersistTo persistTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(id, persistTo, target)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public <D extends Document<?>> D remove(String id, ReplicateTo replicateTo, Class<D> target, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .remove(id, replicateTo, target)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public ViewResult query(ViewQuery query) {
        return query(query, environment.viewTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public QueryResult query(Query query) {
        return query(query, environment.queryTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public QueryResult query(String query) {
        return query(query, environment.queryTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public ViewResult query(ViewQuery query, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .query(query)
            .map(new Func1<AsyncViewResult, ViewResult>() {
                @Override
                public ViewResult call(AsyncViewResult asyncViewResult) {
                    return new DefaultViewResult(environment, CouchbaseBucket.this,
                        asyncViewResult.rows(), asyncViewResult.totalRows(), asyncViewResult.success(),
                        asyncViewResult.error(), asyncViewResult.debug());
                }
            })
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public QueryResult query(Query query, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .query(query)
            .map(new Func1<AsyncQueryResult, QueryResult>() {
                @Override
                public QueryResult call(AsyncQueryResult asyncQueryResult) {
                    return new DefaultQueryResult(environment, asyncQueryResult.rows(),
                        asyncQueryResult.info(), asyncQueryResult.error(), asyncQueryResult.success());
                }
            })
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public QueryResult query(String query, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .query(query)
            .map(new Func1<AsyncQueryResult, QueryResult>() {
                @Override
                public QueryResult call(AsyncQueryResult asyncQueryResult) {
                    return new DefaultQueryResult(environment, asyncQueryResult.rows(),
                        asyncQueryResult.info(), asyncQueryResult.error(), asyncQueryResult.success());
                }
            })
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public Boolean unlock(String id, long cas) {
        return unlock(id, cas, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> Boolean unlock(D document) {
        return unlock(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean unlock(String id, long cas, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .unlock(id, cas)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> Boolean unlock(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .unlock(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public Boolean touch(String id, int expiry) {
        return touch(id, expiry, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> Boolean touch(D document) {
        return touch(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean touch(String id, int expiry, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .touch(id, expiry)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> Boolean touch(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .touch(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public JsonLongDocument counter(String id, long delta) {
        return counter(id, delta, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial) {
        return counter(id, delta, initial, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry) {
        return counter(id, delta, initial, expiry, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .counter(id, delta)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .counter(id, delta, initial)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public JsonLongDocument counter(String id, long delta, long initial, int expiry, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .counter(id, delta, initial, expiry)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public BucketManager bucketManager() {
        return asyncBucket
            .bucketManager()
            .map(new Func1<AsyncBucketManager, BucketManager>() {
                @Override
                public BucketManager call(AsyncBucketManager asyncBucketManager) {
                    return DefaultBucketManager.create(environment, name, password, core);
                }
            })
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D append(D document) {
        return append(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D prepend(D document) {
        return prepend(document, kvTimeout, TIMEOUT_UNIT);
    }

    @Override
    public <D extends Document<?>> D append(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .append(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public <D extends Document<?>> D prepend(D document, long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .prepend(document)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public Boolean close() {
        return close(environment.managementTimeout(), TIMEOUT_UNIT);
    }

    @Override
    public Boolean close(long timeout, TimeUnit timeUnit) {
        return asyncBucket
            .close()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public String toString() {
        return "Bucket[" + name() + "]";
    }
}
