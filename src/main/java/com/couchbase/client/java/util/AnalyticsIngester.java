/*
 * Copyright (c) 2018 Couchbase, Inc.
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

package com.couchbase.client.java.util;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.analytics.AnalyticsQuery;
import com.couchbase.client.java.analytics.AsyncAnalyticsQueryResult;
import com.couchbase.client.java.analytics.AsyncAnalyticsQueryRow;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.util.retry.RetryBuilder;
import rx.Completable;
import rx.Observable;
import rx.functions.Func1;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class allows to take results from an analytics
 * result and turn it back into KV operations that will be inserted.
 */
@InterfaceAudience.Public
@InterfaceStability.Experimental
public enum AnalyticsIngester {
    ;

    /**
     * The default ID generator being used, which just generates a UUID.
     */
    private static final Func1<JsonObject, String> DEFAULT_ID_GENERATOR = new Func1<JsonObject, String>() {
        @Override
        public String call(JsonObject jsonObject) {
            return UUID.randomUUID().toString();
        }
    };

    /**
     * Takes an {@link AnalyticsQuery} and ingests all rows back into the KV layer as documents with
     * default settings applied.
     *
     * @param bucket the bucket where to write back into.
     * @param query the analytics query to execute.
     * @return a {@link Completable} which suggests once complete or failed.
     */
    public static Completable ingest(final Bucket bucket, final AnalyticsQuery query) {
        return ingest(bucket, query, null);
    }

    /**
     * Takes an {@link AnalyticsQuery} and ingests all rows back into the KV layer as documents.
     *
     * @param bucket the bucket where to write back into.
     * @param query the analytics query to execute.
     * @param options the ingest options to change default behavior.
     * @return a {@link Completable} which suggests once complete or failed.
     */
    public static Completable ingest(final Bucket bucket, final AnalyticsQuery query, final IngestOptions options) {
        final IngestOptions opts = options == null ? IngestOptions.ingestOptions() : options;

        if (opts.ingestMethod == IngestMethod.REPLACE && opts.idGenerator.equals(DEFAULT_ID_GENERATOR)) {
            throw new IllegalArgumentException("IngestMethod.REPLACE does not work with the default ID generator " +
                    "which only creates new UUIDs and will make every replace operation fail. Please create " +
                    "your own ID Generator!");
        }

        final long kvTimeout = opts.kvTimeout > 0
                ? opts.kvTimeout
                : bucket.environment().kvTimeout();
        final long anTimeout = opts.analyticsTimeout > 0
                ? opts.analyticsTimeout
                : bucket.environment().analyticsTimeout();

        return bucket
            .async()
            .query(query)
            .timeout(anTimeout, TimeUnit.MILLISECONDS)
            .flatMap(new Func1<AsyncAnalyticsQueryResult, Observable<JsonDocument>>() {
                @Override
                public Observable<JsonDocument> call(AsyncAnalyticsQueryResult result) {
                    Observable<RowWithError> errors = result.errors().map(new Func1<JsonObject, RowWithError>() {
                        @Override
                        public RowWithError call(JsonObject error) {
                            return new RowWithError(null, error);
                        }
                    });

                    Observable<RowWithError> rows = result.rows().map(new Func1<AsyncAnalyticsQueryRow, RowWithError>() {
                        @Override
                        public RowWithError call(AsyncAnalyticsQueryRow row) {
                            return new RowWithError(row, null);
                        }
                    });

                    return Observable
                        .merge(rows, errors)
                        .map(new Func1<RowWithError, RowWithError>() {
                            @Override
                            public RowWithError call(RowWithError rwe) {
                                if (rwe.error != null) {
                                    throw new CouchbaseException(rwe.error.toString());
                                }
                                return rwe;
                            }
                        })
                        .flatMap(new Func1<RowWithError, Observable<JsonDocument>>() {
                            @Override
                            public Observable<JsonDocument> call(RowWithError rwe) {
                                JsonObject data = opts.dataConverter.call(rwe.row.value());
                                String id = opts.idGenerator.call(data);
                                JsonDocument doc = JsonDocument.create(id, data);

                                Observable<JsonDocument> result;
                                switch (opts.ingestMethod) {
                                    case INSERT:
                                        result = bucket.async().insert(doc);
                                        break;
                                    case UPSERT:
                                        result = bucket.async().upsert(doc);
                                        break;
                                    case REPLACE:
                                        result = bucket.async().replace(doc);
                                        break;
                                    default:
                                        return Observable.error(
                                            new UnsupportedOperationException("Unsupported ingest method")
                                        );
                                }
                                result = result.timeout(kvTimeout, TimeUnit.MILLISECONDS);
                                if (opts.retryBuilder != null) {
                                    result = result.retryWhen(opts.retryBuilder.build());
                                }
                                if (opts.ignoreIngestError) {
                                    result = result.onErrorResumeNext(Observable.<JsonDocument>empty());
                                }
                                return result;
                            }
                        });
                }
            })
            .last()
            .toCompletable();
    }

    public static class IngestOptions {

        private IngestOptions() {}

        long analyticsTimeout = 0;
        long kvTimeout = 0;
        IngestMethod ingestMethod = IngestMethod.UPSERT;
        boolean ignoreIngestError = false;
        Func1<JsonObject, JsonObject> dataConverter = new Func1<JsonObject, JsonObject>() {
            @Override
            public JsonObject call(JsonObject in) {
                return in;
            }
        };
        Func1<JsonObject, String> idGenerator = DEFAULT_ID_GENERATOR;
        RetryBuilder retryBuilder = RetryBuilder
            .anyOf(BackpressureException.class, TemporaryFailureException.class)
            .max(10)
            .delay(Delay.exponential(TimeUnit.MILLISECONDS, 500, 2));

        /**
         * Create ingest options to modify default behavior.
         */
        public static IngestOptions ingestOptions() {
            return new IngestOptions();
        }

        /**
         * Customizes the timeout used for the analytics query.
         *
         * @param timeout the timeout for the analytics op.
         * @param timeUnit the timeunit for the timeout.
         * @return these {@link IngestOptions} for chaining purposes.
         */
        public IngestOptions analyticsTimeout(final long timeout, final TimeUnit timeUnit) {
            this.analyticsTimeout = timeUnit.toMillis(timeout);
            return this;
        }

        /**
         * Customizes the timeout used for each kv mutation operation.
         *
         * @param timeout the timeout for the kv op.
         * @param timeUnit the timeunit for the timeout.
         * @return these {@link IngestOptions} for chaining purposes.
         */
        public IngestOptions kvTimeout(final long timeout, final TimeUnit timeUnit) {
            this.kvTimeout = timeUnit.toMillis(timeout);
            return this;
        }

        /**
         * Allows to customize the ingest method used for each kv operation.
         *
         * @param ingestMethod the ingest method to use.
         * @return these {@link IngestOptions} for chaining purposes.
         */
        public IngestOptions ingestMethod(final IngestMethod ingestMethod) {
            this.ingestMethod = ingestMethod;
            return this;
        }

        /**
         * Allows to ignore individual kv mutation failures and keep going.
         *
         * @param ignoreIngestError true if should be ignored.
         * @return these {@link IngestOptions} for chaining purposes.
         */
        public IngestOptions ignoreIngestError(final boolean ignoreIngestError) {
            this.ignoreIngestError = ignoreIngestError;
            return this;
        }

        /**
         * Allows to customize the retry strategy in use for each individual
         * kv operation.
         *
         * @param retryBuilder the retry builder to use.
         * @return these {@link IngestOptions} for chaining purposes.
         */
        public IngestOptions retryBuilder(final RetryBuilder retryBuilder) {
            this.retryBuilder = retryBuilder;
            return this;
        }

        /**
         * Allows to specify a custom ID generator instead of the default UUID one.
         *
         * @param idGenerator the id generator to use.
         * @return these {@link IngestOptions} for chaining purposes.
         */
        public IngestOptions idGenerator(final Func1<JsonObject, String> idGenerator) {
            this.idGenerator = idGenerator;
            return this;
        }

        /**
         * Allows to specify a custom converter which modifies each document from the query
         * before it is stored back in the kv service.
         *
         * @param dataConverter the converter to use.
         * @return these {@link IngestOptions} for chaining purposes.
         */
        public IngestOptions dataConverter(final Func1<JsonObject, JsonObject> dataConverter) {
            this.dataConverter = dataConverter;
            return this;
        }

    }

    /**
     * Describes how the data should be ingested back into the kv service.
     */
    public enum IngestMethod {
        /**
         * Uses the {@link Bucket#insert(Document)} method.
         */
        INSERT,
        /**
         * Uses the {@link Bucket#upsert(Document)} method.
         */
        UPSERT,
        /**
         * Uses the {@link Bucket#replace(Document)} method.
         */
        REPLACE
    }

    private static class RowWithError {
        private final AsyncAnalyticsQueryRow row;
        private final JsonObject error;

        RowWithError(final AsyncAnalyticsQueryRow row, final JsonObject error) {
            this.row = row;
            this.error = error;
        }
    }

}
