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
package com.couchbase.client.java.util.rawQuerying;

import java.io.IOException;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.query.RawQueryRequest;
import com.couchbase.client.core.message.query.RawQueryResponse;
import com.couchbase.client.core.message.search.SearchQueryRequest;
import com.couchbase.client.core.message.search.SearchQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.deps.io.netty.util.ReferenceCountUtil;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.FtsConsistencyTimeoutException;
import com.couchbase.client.java.error.FtsMalformedRequestException;
import com.couchbase.client.java.error.QueryExecutionException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.queries.AbstractFtsQuery;
import com.couchbase.client.java.transcoder.JacksonTransformers;
import com.couchbase.client.java.transcoder.TranscoderUtils;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.couchbase.client.java.util.OnSubscribeDeferAndWatch.deferAndWatch;

/**
 * A utility class that allows to perform {@link N1qlQuery N1QL} and {@link SearchQuery FTS} queries
 * asynchronously and receive a raw version of the service's JSON response.
 *
 * The responses can directly be exposed as {@link JsonObject} or {@link String}, but custom methods allow
 * to work from a byte array (for N1QL) or String (for FTS) and perform a custom deserialization.
 *
 * Note that this class is outside of the Bucket API as it is uncommitted,
 * and is not common to all Couchbase SDKs.
 *
 * @author Simon Baslé
 * @since 2.3
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class AsyncRawQueryExecutor {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(AsyncRawQueryExecutor.class);

    private final String bucket;
    private final String username;
    private final String password;
    private final ClusterFacade core;

    public AsyncRawQueryExecutor(String bucket, String password, ClusterFacade core) {
        this(bucket, bucket, password, core);
    }

    public AsyncRawQueryExecutor(String bucket, String username, String password, ClusterFacade core) {
        this.bucket = bucket;
        this.username = username;
        this.password = password;
        this.core = core;
    }

    /**
     * Asynchronously perform a {@link N1qlQuery} and return the raw N1QL response as a {@link JsonObject}.
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link AsyncBucket#query(N1qlQuery)} (like enforcing a server side timeout or managing prepared
     * statements).
     *
     * @param query the query to execute.
     * @return an {@link Observable} of the N1QL response as a {@link JsonObject}.
     */
    public Observable<JsonObject> n1qlToJsonObject(N1qlQuery query) {
        return n1qlToRawCustom(query, new Func1<TranscoderUtils.ByteBufToArray, JsonObject>() {
            @Override
            public JsonObject call(TranscoderUtils.ByteBufToArray converted) {
                try {
                    return JacksonTransformers.MAPPER.readValue(converted.byteArray, converted.offset, converted.length, JsonObject.class);
                } catch (IOException e) {
                    throw new TranscodingException("Unable to deserialize the N1QL raw response", e);
                }
            }
        });
    }

    /**
     * Asynchronously perform a {@link N1qlQuery} and return the raw N1QL response as a String.
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link AsyncBucket#query(N1qlQuery)} (like enforcing a server side timeout or managing prepared
     * statements).
     *
     * @param query the query to execute.
     * @return an {@link Observable} of the N1QL response as a String.
     */
    public Observable<String> n1qlToRawJson(N1qlQuery query) {
        return n1qlToRawCustom(query, new Func1<TranscoderUtils.ByteBufToArray, String>() {
            @Override
            public String call(TranscoderUtils.ByteBufToArray converted) {
                return new String(converted.byteArray, converted.offset, converted.length, CharsetUtil.UTF_8);
            }
        });
    }

    /**
     * Asynchronously perform a {@link N1qlQuery} and apply a user function to deserialize the raw N1QL
     * response, which is represented as a "TranscoderUtils.ByteBufToArray".
     *
     * The array is derived from a {@link ByteBuf} that will be released, so it shouldn't be used
     * to back the returned instance. Its scope should be considered the scope of the call method.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link AsyncBucket#query(N1qlQuery)} (like enforcing a server side timeout or managing prepared
     * statements).
     *
     * @param query the query to execute.
     * @param deserializer a deserializer function that transforms the byte representation of the response into a custom type T.
     * @param <T> the type of the response, once deserialized by the user-provided function.
     * @return an {@link Observable} of the N1QL response as a T.
     */
    public <T> Observable<T> n1qlToRawCustom(final N1qlQuery query, final Func1<TranscoderUtils.ByteBufToArray, T> deserializer) {
        return deferAndWatch(new Func1<Subscriber, Observable<RawQueryResponse>>() {
            @Override
            public Observable<RawQueryResponse> call(Subscriber s) {
                RawQueryRequest request = RawQueryRequest.jsonQuery(query.n1ql().toString(), bucket, username, password, query.params().clientContextId());
                request.subscriber(s);
                return core.<RawQueryResponse>send(request);
            }
        }).map(new Func1<RawQueryResponse, T>() {
            @Override
            public T call(RawQueryResponse response) {
                try {
                    if (response.httpStatusCode() == 200) {
                        return deserializer.call(TranscoderUtils.byteBufToByteArray(response.jsonResponse()));
                    }
                    LOGGER.debug("Unable to perform raw N1QL query (see exception), body was: " +
                            response.jsonResponse().toString( CharsetUtil.UTF_8));
                    throw new QueryExecutionException("Unable to perform raw N1QL query: " + response.httpStatusCode() +
                            " - " + response.httpStatusMsg(), JsonObject.empty());
                } finally {
                    ReferenceCountUtil.release(response.jsonResponse());
                }
            }
        });
    }

    /**
     * Asynchronously perform a {@link SearchQuery} and return the raw N1QL response as a {@link JsonObject}.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link AsyncBucket#query(SearchQuery)} (like enforcing a server side timeout).
     *
     * @param query the query to execute.
     * @return an {@link Observable} of the FTS response as a {@link JsonObject}.
     */
    public Observable<JsonObject> ftsToJsonObject(SearchQuery query) {
        return ftsToRawCustom(query, new Func1<String, JsonObject>() {
            @Override
            public JsonObject call(String stringResponse) {
                return JsonObject.fromJson(stringResponse);
            }
        });
    }

    /**
     * Asynchronously perform a {@link SearchQuery} and return the raw N1QL response as a String.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link AsyncBucket#query(SearchQuery)} (like enforcing a server side timeout).
     *
     * @param query the query to execute.
     * @return an {@link Observable} of the FTS response as a String.
     */
    public Observable<String> ftsToRawJson(SearchQuery query) {
        return ftsToRawCustom(query, new Func1<String, String>() {
            @Override
            public String call(String stringResponse) {
                return stringResponse;
            }
        });
    }

    /**
     * Asynchronously perform a {@link SearchQuery} and apply a user function to deserialize the raw JSON
     * FTS response, which is represented as a {@link String}.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link AsyncBucket#query(SearchQuery)} (like enforcing a server side timeout).
     *
     * @param query the query to execute.
     * @param deserializer a deserializer function that transforms the String representation of the response into a custom type T.
     * @param <T> the type of the response, once deserialized by the user-provided function.
     * @return an {@link Observable} of the FTS response as a T.
     */
    public <T> Observable<T> ftsToRawCustom(final SearchQuery query, final Func1<String, T> deserializer) {
        final String indexName = query.indexName();
        final AbstractFtsQuery queryPart = query.query();

        return deferAndWatch(new Func1<Subscriber, Observable<? extends SearchQueryResponse>>() {
            @Override
            public Observable<? extends SearchQueryResponse> call(Subscriber subscriber) {
                SearchQueryRequest request = new SearchQueryRequest(indexName, query.export().toString(), bucket, username, password);
                request.subscriber(subscriber);
                return core.send(request);
            }
        }).map(new Func1<SearchQueryResponse, T>() {
            @Override
            public T call(SearchQueryResponse response) {
                if (response.status().isSuccess()) {
                    return deserializer.call(response.payload());
                } else if (response.status() == ResponseStatus.INVALID_ARGUMENTS) {
                    throw new FtsMalformedRequestException(response.payload());
                } else if (response.status() == ResponseStatus.FAILURE) {
                    throw new FtsConsistencyTimeoutException();
                } else {
                    throw new CouchbaseException("Could not query search index, " + response.status() + ": " + response.payload());
                }
            }
        });
    }
}
