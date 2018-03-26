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

package com.couchbase.client.java.subdoc;

import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.kv.subdoc.multi.Lookup;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.error.subdoc.DocumentNotJsonException;
import com.couchbase.client.java.error.subdoc.SubDocumentException;
import com.couchbase.client.java.util.Blocking;

/**
 * A builder for subdocument lookups. In order to perform the final set of operations, use the
 * {@link #execute()} method. Operations are performed synchronously (see {@link AsyncLookupInBuilder} for an asynchronous
 * version).
 *
 * Instances of this builder should be obtained through {@link Bucket#lookupIn(String)} rather than directly
 * constructed.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Committed
@InterfaceAudience.Public
public class LookupInBuilder {

    private final AsyncLookupInBuilder async;
    private final long defaultTimeout;
    private final TimeUnit defaultTimeUnit;
    private boolean accessDeleted;

    /**
     * Instances of this builder should be obtained through {@link Bucket#lookupIn(String)} rather than directly
     * constructed.
    */
    @InterfaceAudience.Private
    public LookupInBuilder(AsyncLookupInBuilder async, long defaultTimeout, TimeUnit defaultTimeUnit) {
        this.async = async;
        this.defaultTimeout = defaultTimeout;
        this.defaultTimeUnit = defaultTimeUnit;
    }

    /**
     * Set accessDeleted to true, if the document has been deleted to access xattrs
     *
     * @param accessDeleted true to access deleted document xattrs
     */
    @InterfaceStability.Committed
    public LookupInBuilder accessDeleted(boolean accessDeleted) {
        async.accessDeleted(accessDeleted);
        return this;
    }

    /**
     * Perform several {@link Lookup lookup} operations inside a single existing {@link JsonDocument JSON document},
     * using the default key/value timeout.
     * The list of path to look for inside the JSON is constructed through builder methods {@link #get(String...)} and
     * {@link #exists(String...)}.
     *
     * The subdocument API has the benefit of only transmitting the fragment of the document you work with
     * on the wire, instead of the whole document.
     *
     * If multiple operations are specified, each spec will receive an answer, overall contained in a
     * {@link DocumentFragment}, meaning that if sub-document level error conditions happen (like the path is malformed
     * or doesn't exist), the whole operation still succeeds.
     *
     * If a single operation is specified, then any error other that a path not found will throw the corresponding
     * {@link SubDocumentException}. Otherwise a {@link DocumentFragment} is returned.
     *
     * Calling {@link DocumentFragment#content(String)} or one of its variants on a failed spec/path will throw the
     * corresponding {@link SubDocumentException}. For successful gets, it will return the value (or null in the case
     * of a path not found, and only in this case). For exists, it will return true (or false for a path not found).
     *
     * To check for any error without throwing an exception, use {@link DocumentFragment#status(String)}
     * (or its index-based variant).
     *
     * To check that a given path (or index) is valid for calling {@link DocumentFragment#content(String) content()} on
     * it without raising an Exception, use {@link DocumentFragment#exists(String)}.
     *
     * One special fatal error can also happen, when the value couldn't be decoded from JSON. In that case,
     * the ResponseStatus for the path is {@link ResponseStatus#FAILURE} and the content(path) will throw a
     * {@link TranscodingException}.
     *
     * This operation throws under the following notable error conditions:
     *
     *  - The enclosing document does not exist: {@link DocumentDoesNotExistException}
     *  - The enclosing document is not JSON: {@link DocumentNotJsonException}
     *  - No lookup was defined through the builder API: {@link IllegalArgumentException}
     *
     * Other document-level error conditions are similar to those encountered during a document-level {@link AsyncBucket#get(String)}.
     *
     * @return a single {@link DocumentFragment} representing the whole list of results (1 for each spec), unless a
     * document-level error happened (in which case an exception is thrown).
     */
    public DocumentFragment<Lookup> execute() {
        return execute(defaultTimeout, defaultTimeUnit);
    }

    /**
     * Perform several {@link Lookup lookup} operations inside a single existing {@link JsonDocument JSON document},
     * using a specific timeout.
     * The list of path to look for inside the JSON is constructed through builder methods {@link #get(String...)} and
     * {@link #exists(String...)}.
     *
     * The subdocument API has the benefit of only transmitting the fragment of the document you work with
     * on the wire, instead of the whole document.
     *
     * If multiple operations are specified, each spec will receive an answer, overall contained in a
     * {@link DocumentFragment}, meaning that if sub-document level error conditions happen (like the path is malformed
     * or doesn't exist), the whole operation still succeeds.
     *
     * If a single operation is specified, then any error other that a path not found will throw the corresponding
     * {@link SubDocumentException}. Otherwise a {@link DocumentFragment} is returned.
     *
     * Calling {@link DocumentFragment#content(String)} or one of its variants on a failed spec/path will throw the
     * corresponding {@link SubDocumentException}. For successful gets, it will return the value (or null in the case
     * of a path not found, and only in this case). For exists, it will return true (or false for a path not found).
     *
     * To check for any error without throwing an exception, use {@link DocumentFragment#status(String)}
     * (or its index-based variant).
     *
     * To check that a given path (or index) is valid for calling {@link DocumentFragment#content(String) content()} on
     * it without raising an Exception, use {@link DocumentFragment#exists(String)}.
     *
     * One special fatal error can also happen, when the value couldn't be decoded from JSON. In that case,
     * the ResponseStatus for the path is {@link ResponseStatus#FAILURE} and the content(path) will throw a
     * {@link TranscodingException}.
     *
     * This operation throws under the following notable error conditions:
     *
     *  - The enclosing document does not exist: {@link DocumentDoesNotExistException}
     *  - The enclosing document is not JSON: {@link DocumentNotJsonException}
     *  - No lookup was defined through the builder API: {@link IllegalArgumentException}
     *
     * Other document-level error conditions are similar to those encountered during a document-level {@link AsyncBucket#get(String)}.
     *
     * @param timeout the specific timeout to apply for the operation.
     * @param timeUnit the time unit for the timeout.
     * @return a single {@link DocumentFragment} representing the whole list of results (1 for each spec), unless a
     * document-level error happened (in which case an exception is thrown).
     */
    public DocumentFragment<Lookup> execute(long timeout, TimeUnit timeUnit) {
        return this.async.execute(timeout, timeUnit).toBlocking().single();
    }

    /**
     * Get a value inside the JSON document.
     *
     * @param paths the paths inside the document where to get the value from.
     * @return this builder for chaining.
     */
    public LookupInBuilder get(String... paths) {
        this.async.get(paths);
        return this;
    }

    /**
     * Get full JSON document
     *
     * @return this builder for chaining.
     */
    @InterfaceStability.Experimental
    public LookupInBuilder get() {
        this.async.get();
        return this;
    }

    /**
     * Get a value inside the JSON document.
     *
     * @param path the paths inside the document where to get the value from.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @return this builder for chaining.
     */
    public LookupInBuilder get(String path, SubdocOptionsBuilder optionsBuilder) {
        this.async.get(path, optionsBuilder);
        return this;
    }

    /**
     * Get a value inside the JSON document.
     *
     * @param paths the paths inside the document where to get the value from.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @return this builder for chaining.
     */
    public LookupInBuilder get(Iterable<String> paths, SubdocOptionsBuilder optionsBuilder) {
        this.async.get(paths, optionsBuilder);
        return this;
    }

    /**
     * Check if a value exists inside the document (if it does not, attempting to get the
     * {@link DocumentFragment#content(int)} will raise an error).
     * This doesn't transmit the value on the wire if it exists, saving the corresponding byte overhead.
     *
     * @param paths the paths inside the document to check for existence.
     * @return this builder for chaining.
     */
    public LookupInBuilder exists(String... paths) {
        this.async.exists(paths);
        return this;
    }

    /**
     * Check if a value exists inside the document (if it does not, attempting to get the
     * {@link DocumentFragment#content(int)} will raise an error).
     * This doesn't transmit the value on the wire if it exists, saving the corresponding byte overhead.
     *
     * @param path the path inside the document to check for existence.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @return this builder for chaining.
     */
    public LookupInBuilder exists(String path, SubdocOptionsBuilder optionsBuilder) {
        this.async.exists(path, optionsBuilder);
        return this;
    }


    /**
     * Check if a value exists inside the document (if it does not, attempting to get the
     * {@link DocumentFragment#content(int)} will raise an error).
     * This doesn't transmit the value on the wire if it exists, saving the corresponding byte overhead.
     *
     * @param paths the paths inside the document to check for existence.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @return this builder for chaining.
     */
    public LookupInBuilder exists(Iterable<String> paths, SubdocOptionsBuilder optionsBuilder) {
        this.async.exists(paths, optionsBuilder);
        return this;
    }

    /**
     * Get a count of values inside the JSON document.
     *
     * This method is only available with Couchbase Server 5.0 and later.
     *
     * @param paths the paths inside the document where to get the count from.
     * @return this builder for chaining.
     */
    public LookupInBuilder getCount(String... paths) {
        this.async.getCount(paths);
        return this;
    }

    /**
     * Get a count of values inside the JSON document.
     *
     * This method is only available with Couchbase Server 5.0 and later.
     *
     * @param path the paths inside the document where to get the count from.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @return this builder for chaining.
     */
    public LookupInBuilder getCount(String path, SubdocOptionsBuilder optionsBuilder) {
        this.async.getCount(path, optionsBuilder);
        return this;
    }

    /**
     * Get a count of values inside the JSON document.
     *
     * This method is only available with Couchbase Server 5.0 and later.
     *
     * @param paths the paths inside the document where to get the count from.
     * @param optionsBuilder {@link SubdocOptionsBuilder}
     * @return this builder for chaining.
     */
    public LookupInBuilder getCount(Iterable<String> paths, SubdocOptionsBuilder optionsBuilder) {
        this.async.getCount(paths, optionsBuilder);
        return this;
    }

    /**
     * Set to true, includes the raw byte value for each GET in the results, in addition to the deserialized content.
     */
    public LookupInBuilder includeRaw(boolean includeRaw) {
        async.includeRaw(includeRaw);
        return this;
    }

    /**
     * @return true if this builder is configured to include raw byte values for each GET result.
     */
    public boolean isIncludeRaw() {
        return async.isIncludeRaw();
    }

    @Override
    public String toString() {
        return async.toString();
    }
}
