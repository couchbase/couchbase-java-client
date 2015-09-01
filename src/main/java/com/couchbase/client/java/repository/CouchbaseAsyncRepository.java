/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.repository;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicaMode;
import com.couchbase.client.java.ReplicateTo;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.repository.mapping.DefaultEntityConverter;
import com.couchbase.client.java.repository.mapping.EntityConverter;
import rx.Observable;
import rx.functions.Func1;

@InterfaceAudience.Public
@InterfaceStability.Experimental
public class CouchbaseAsyncRepository implements AsyncRepository {

    private final EntityConverter converter;
    private final AsyncBucket bucket;

    public CouchbaseAsyncRepository(AsyncBucket bucket) {
        this.bucket = bucket;
        converter = new DefaultEntityConverter();
    }

    @Override
    public <T> Observable<EntityDocument<T>> get(String id, final Class<T> entityClass) {
        return Observable
            .just(id)
            .flatMap(new Func1<String, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(String id) {
                    return bucket.get(id);
                }
            })
            .map(new DocumentToType<T>(entityClass));
    }

    @Override
    public <T> Observable<EntityDocument<T>> getFromReplica(String id, final ReplicaMode type, Class<T> entityClass) {
        return Observable
            .just(id)
            .flatMap(new Func1<String, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(String id) {
                    return bucket.getFromReplica(id, type);
                }
            })
            .map(new DocumentToType<T>(entityClass));
    }

    @Override
    public <T> Observable<EntityDocument<T>> getAndLock(String id, final int lockTime, Class<T> entityClass) {
        return Observable
            .just(id)
            .flatMap(new Func1<String, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(String id) {
                    return bucket.getAndLock(id, lockTime);
                }
            })
            .map(new DocumentToType<T>(entityClass));
    }

    @Override
    public <T> Observable<EntityDocument<T>> getAndTouch(String id, final int expiry, Class<T> entityClass) {
        return Observable
            .just(id)
            .flatMap(new Func1<String, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(String id) {
                    return bucket.getAndTouch(id, expiry);
                }
            })
            .map(new DocumentToType<T>(entityClass));
    }

    @Override
    public <T> Observable<EntityDocument<T>> upsert(final EntityDocument<T> document) {
        return upsert(document, PersistTo.NONE, ReplicateTo.NONE);
    }

    @Override
    public <T> Observable<EntityDocument<T>> upsert(EntityDocument<T> document, PersistTo persistTo) {
        return upsert(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <T> Observable<EntityDocument<T>> upsert(EntityDocument<T> document, ReplicateTo replicateTo) {
        return upsert(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <T> Observable<EntityDocument<T>> upsert(final EntityDocument<T> document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        return Observable
            .just(document)
            .flatMap(new Func1<EntityDocument<T>, Observable<? extends Document<?>>>() {
                @Override
                public Observable<? extends Document<?>> call(EntityDocument<T> source) {
                    Document<?> converted = converter.fromEntity(source);
                    return bucket.upsert(converted, persistTo, replicateTo);
                }
            })
            .map(new Func1<Document<?>, EntityDocument<T>>() {
                @Override
                public EntityDocument<T> call(Document<?> stored) {
                    return EntityDocument.create(document.id(), document.expiry(), document.content(), stored.cas());
                }
            });
    }

    @Override
    public <T> Observable<EntityDocument<T>> insert(final EntityDocument<T> document) {
        return insert(document, PersistTo.NONE, ReplicateTo.NONE);
    }

    @Override
    public <T> Observable<EntityDocument<T>> insert(EntityDocument<T> document, PersistTo persistTo) {
        return insert(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <T> Observable<EntityDocument<T>> insert(EntityDocument<T> document, ReplicateTo replicateTo) {
        return insert(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <T> Observable<EntityDocument<T>> insert(final EntityDocument<T> document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        return Observable
            .just(document)
            .flatMap(new Func1<EntityDocument<T>, Observable<? extends Document<?>>>() {
                @Override
                public Observable<? extends Document<?>> call(EntityDocument<T> source) {
                    Document<?> converted = converter.fromEntity(source);
                    return bucket.insert(converted, persistTo, replicateTo);
                }
            })
            .map(new Func1<Document<?>, EntityDocument<T>>() {
                @Override
                public EntityDocument<T> call(Document<?> stored) {
                    return EntityDocument.create(document.id(), document.expiry(), document.content(), stored.cas());
                }
            });
    }

    @Override
    public <T> Observable<EntityDocument<T>> replace(final EntityDocument<T> document) {
        return replace(document, PersistTo.NONE, ReplicateTo.NONE);
    }

    @Override
    public <T> Observable<EntityDocument<T>> replace(EntityDocument<T> document, PersistTo persistTo) {
        return replace(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <T> Observable<EntityDocument<T>> replace(EntityDocument<T> document, ReplicateTo replicateTo) {
        return replace(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <T> Observable<EntityDocument<T>> replace(final EntityDocument<T> document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        return Observable
            .just(document)
            .flatMap(new Func1<EntityDocument<T>, Observable<? extends Document<?>>>() {
                @Override
                public Observable<? extends Document<?>> call(EntityDocument<T> source) {
                    Document<?> converted = converter.fromEntity(source);
                    return bucket.replace(converted, persistTo, replicateTo);
                }
            })
            .map(new Func1<Document<?>, EntityDocument<T>>() {
                @Override
                public EntityDocument<T> call(Document<?> stored) {
                    return EntityDocument.create(document.id(), document.expiry(), document.content(), stored.cas());
                }
            });
    }

    @Override
    public Observable<Boolean> exists(String id) {
        return bucket.exists(id);
    }

    @Override
    public <T> Observable<Boolean> exists(EntityDocument<T> document) {
        return Observable
            .just(document)
            .map(new Func1<EntityDocument<T>, String>() {
                @Override
                public String call(EntityDocument<T> source) {
                    Document<?> converted = converter.fromEntity(source);
                    return converted.id();
                }
            })
            .flatMap(new Func1<String, Observable<Boolean>>() {
                @Override
                public Observable<Boolean> call(String id) {
                    return exists(id);
                }
            });
    }

    @Override
    public <T> Observable<EntityDocument<T>> remove(EntityDocument<T> document) {
        return remove(document, PersistTo.NONE, ReplicateTo.NONE);
    }

    @Override
    public <T> Observable<EntityDocument<T>> remove(EntityDocument<T> document, PersistTo persistTo) {
        return remove(document, persistTo, ReplicateTo.NONE);
    }

    @Override
    public <T> Observable<EntityDocument<T>> remove(EntityDocument<T> document, ReplicateTo replicateTo) {
        return remove(document, PersistTo.NONE, replicateTo);
    }

    @Override
    public <T> Observable<EntityDocument<T>> remove(final EntityDocument<T> document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        return Observable
            .just(document)
            .map(new Func1<EntityDocument<T>, String>() {
                @Override
                public String call(EntityDocument<T> source) {
                    Document<?> converted = converter.fromEntity(source);
                    return converted.id();
                }
            })
            .flatMap(new Func1<String, Observable<EntityDocument<T>>>() {
                @Override
                @SuppressWarnings("unchecked")
                public Observable<EntityDocument<T>> call(String id) {
                    return remove(id, persistTo, replicateTo, (Class<T>) document.content().getClass());
                }
            });
    }

    @Override
    public <T> Observable<EntityDocument<T>> remove(String id, Class<T> entityClass) {
        return remove(id, PersistTo.NONE, ReplicateTo.NONE, entityClass);
    }

    @Override
    public <T> Observable<EntityDocument<T>> remove(String id, PersistTo persistTo, Class<T> entityClass) {
        return remove(id, persistTo, ReplicateTo.NONE, entityClass);
    }

    @Override
    public <T> Observable<EntityDocument<T>> remove(String id, ReplicateTo replicateTo, Class<T> entityClass) {
        return remove(id, PersistTo.NONE, replicateTo, entityClass);
    }

    @Override
    public <T> Observable<EntityDocument<T>> remove(String id, final PersistTo persistTo, final ReplicateTo replicateTo,
        Class<T> entityClass) {
        return Observable
            .just(id)
            .flatMap(new Func1<String, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(String id) {
                    return bucket.remove(id, persistTo, replicateTo);
                }
            })
            .map(new DocumentToType<T>(entityClass));
    }

    class DocumentToType<T> implements Func1<JsonDocument, EntityDocument<T>> {

        private final Class<T> entityClass;

        public DocumentToType(Class<T> entityClass) {
            this.entityClass = entityClass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public EntityDocument<T> call(JsonDocument document) {
            return converter.toEntity(document, entityClass);
        }
    }
}
