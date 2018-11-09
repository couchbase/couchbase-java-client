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

import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.utils.Blocking;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.transcoder.TranscoderUtils;
import rx.functions.Func1;

/**
 * A utility class that allows to perform {@link N1qlQuery N1QL} and {@link SearchQuery FTS} queries
 * synchronously and receive a raw version of the service's JSON response.
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
public class RawQueryExecutor {

    private final CouchbaseEnvironment env;
    private final AsyncRawQueryExecutor async;

    public RawQueryExecutor(AsyncRawQueryExecutor async, CouchbaseEnvironment env) {
        this.env = env;
        this.async = async;
    }

    public RawQueryExecutor(String bucket, String password, ClusterFacade core, CouchbaseEnvironment env) {
        this(bucket, bucket, password, core, env);
    }

    public RawQueryExecutor(String bucket, String username, String password, ClusterFacade core, CouchbaseEnvironment env) {
        this(new AsyncRawQueryExecutor(bucket, username, password, core), env);
    }

    /**
     * Synchronously perform a {@link N1qlQuery} and return the raw N1QL response as a {@link JsonObject}.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link Bucket#query(N1qlQuery)} (like enforcing a server side timeout or managing prepared
     * statements).
     *
     * @param query the query to execute.
     * @return the N1QL response as a {@link JsonObject}.
     */
    public JsonObject n1qlToJsonObject(N1qlQuery query) {
        return Blocking.blockForSingle(async.n1qlToJsonObject(query), env.queryTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * Synchronously perform a {@link N1qlQuery} and return the raw N1QL response as a String.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link Bucket#query(N1qlQuery)} (like enforcing a server side timeout or managing prepared
     * statements).
     *
     * @param query the query to execute.
     * @return the N1QL response as a String.
     */
    public String n1qlToRawJson(N1qlQuery query) {
        return Blocking.blockForSingle(async.n1qlToRawJson(query), env.queryTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * Synchronously perform a {@link N1qlQuery} and apply a user function to deserialize the raw N1QL
     * response, which is represented as a "TranscoderUtils.ByteBufToArray".
     *
     * The array is derived from a {@link ByteBuf} that will be released, so it shouldn't be used
     * to back the returned instance. Its scope should be considered the scope of the call method.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link Bucket#query(N1qlQuery)} (like enforcing a server side timeout or managing prepared
     * statements).
     *
     * @param query the query to execute.
     * @param deserializer a deserializer function that transforms the byte representation of the response into a custom type T.
     * @param <T> the type of the response, once deserialized by the user-provided function.
     * @return the N1QL response as a T.
     */
    public <T> T n1qlToRawCustom(final N1qlQuery query, final Func1<TranscoderUtils.ByteBufToArray, T> deserializer) {
        return Blocking.blockForSingle(async.n1qlToRawCustom(query, deserializer), env.queryTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * Synchronously perform a {@link SearchQuery} and return the raw N1QL response as a {@link JsonObject}.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link Bucket#query(SearchQuery)} (like enforcing a server side timeout).
     *
     * @param query the query to execute.
     * @return the FTS response as a {@link JsonObject}.
     */
    public JsonObject ftsToJsonObject(SearchQuery query) {
        return Blocking.blockForSingle(async.ftsToJsonObject(query), env.searchTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * Synchronously perform a {@link SearchQuery} and return the raw N1QL response as a String.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link Bucket#query(SearchQuery)} (like enforcing a server side timeout).
     *
     * @param query the query to execute.
     * @return the FTS response as a String.
     */
    public String ftsToRawJson(SearchQuery query) {
        return Blocking.blockForSingle(async.ftsToRawJson(query), env.searchTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * Synchronously perform a {@link SearchQuery} and apply a user function to deserialize the raw JSON
     * FTS response, which is represented as a {@link String}.
     *
     * Note that the query is executed "as is", without any processing comparable to what is done in
     * {@link Bucket#query(SearchQuery)} (like enforcing a server side timeout).
     *
     * @param query the query to execute.
     * @param deserializer a deserializer function that transforms the String representation of the response into a custom type T.
     * @param <T> the type of the response, once deserialized by the user-provided function.
     * @return the FTS response as a T.
     */
    public <T> T ftsToRawCustom(final SearchQuery query, final Func1<String, T> deserializer) {
        return Blocking.blockForSingle(async.ftsToRawCustom(query, deserializer), env.searchTimeout(), TimeUnit.MILLISECONDS);
    }
}
