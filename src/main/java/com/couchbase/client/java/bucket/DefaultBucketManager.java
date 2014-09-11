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
package com.couchbase.client.java.bucket;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.view.DesignDocument;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultBucketManager implements BucketManager {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncBucketManager asyncBucketManager;
    private final long timeout;

    DefaultBucketManager(final CouchbaseEnvironment environment, final String bucket, final String password,
        final ClusterFacade core) {
        asyncBucketManager = DefaultAsyncBucketManager.create(bucket, password, core);
        this.timeout = environment.managementTimeout();
    }

    public static DefaultBucketManager create(final CouchbaseEnvironment environment, final String bucket,
        final String password, final ClusterFacade core) {
        return new DefaultBucketManager(environment, bucket, password, core);
    }

    @Override
    public AsyncBucketManager async() {
        return asyncBucketManager;
    }

    @Override
    public BucketInfo info() {
        return info(timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean flush() {
        return flush(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<DesignDocument> getDesignDocuments() {
        return getDesignDocuments(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<DesignDocument> getDesignDocuments(final boolean development) {
        return getDesignDocuments(development, timeout, TIMEOUT_UNIT);
    }

    @Override
    public DesignDocument getDesignDocument(final String name) {
        return getDesignDocument(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public DesignDocument getDesignDocument(final String name, final boolean development) {
        return getDesignDocument(name, development, timeout, TIMEOUT_UNIT);
    }

    @Override
    public DesignDocument insertDesignDocument(final DesignDocument designDocument) {
        return insertDesignDocument(designDocument, timeout, TIMEOUT_UNIT);
    }

    @Override
    public DesignDocument insertDesignDocument(final DesignDocument designDocument, final boolean development) {
        return insertDesignDocument(designDocument, development, timeout, TIMEOUT_UNIT);
    }

    @Override
    public DesignDocument upsertDesignDocument(final DesignDocument designDocument) {
        return upsertDesignDocument(designDocument, timeout, TIMEOUT_UNIT);
    }

    @Override
    public DesignDocument upsertDesignDocument(final DesignDocument designDocument, final boolean development) {
        return upsertDesignDocument(designDocument, development, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean removeDesignDocument(final String name) {
        return removeDesignDocument(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public Boolean removeDesignDocument(final String name, final boolean development) {
        return removeDesignDocument(name, development, timeout, TIMEOUT_UNIT);
    }

    @Override
    public DesignDocument publishDesignDocument(final String name) {
        return publishDesignDocument(name, timeout, TIMEOUT_UNIT);
    }

    @Override
    public DesignDocument publishDesignDocument(final String name, final boolean overwrite) {
        return publishDesignDocument(name, overwrite, timeout, TIMEOUT_UNIT);
    }

    @Override
    public BucketInfo info(final long timeout, final TimeUnit timeUnit) {
        return asyncBucketManager
            .info()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public Boolean flush(final long timeout, final TimeUnit timeUnit) {
        return asyncBucketManager
            .flush()
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public List<DesignDocument> getDesignDocuments(final long timeout, final TimeUnit timeUnit) {
        return asyncBucketManager
            .getDesignDocuments()
            .timeout(timeout, timeUnit)
            .toList()
            .toBlocking()
            .single();
    }

    @Override
    public List<DesignDocument> getDesignDocuments(final boolean development, final long timeout,
        final TimeUnit timeUnit) {
        return asyncBucketManager
            .getDesignDocuments(development)
            .timeout(timeout, timeUnit)
            .toList()
            .toBlocking()
            .single();
    }

    @Override
    public DesignDocument getDesignDocument(final String name, final long timeout, final TimeUnit timeUnit) {
        return asyncBucketManager
            .getDesignDocument(name)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public DesignDocument getDesignDocument(final String name, final boolean development, final long timeout,
        final TimeUnit timeUnit) {
        return asyncBucketManager
            .getDesignDocument(name, development)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .singleOrDefault(null);
    }

    @Override
    public DesignDocument insertDesignDocument(final DesignDocument designDocument, final long timeout,
        final TimeUnit timeUnit) {
        return asyncBucketManager
            .insertDesignDocument(designDocument)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public DesignDocument insertDesignDocument(final DesignDocument designDocument, final boolean development,
        final long timeout, final TimeUnit timeUnit) {
        return asyncBucketManager
            .insertDesignDocument(designDocument, development)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public DesignDocument upsertDesignDocument(final DesignDocument designDocument, final long timeout,
        final TimeUnit timeUnit) {
        return asyncBucketManager
            .upsertDesignDocument(designDocument)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public DesignDocument upsertDesignDocument(final DesignDocument designDocument, final boolean development,
        final long timeout, final TimeUnit timeUnit) {
        return asyncBucketManager
            .upsertDesignDocument(designDocument, development)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public Boolean removeDesignDocument(final String name, final long timeout, final TimeUnit timeUnit) {
        return asyncBucketManager
            .removeDesignDocument(name)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public Boolean removeDesignDocument(final String name, final boolean development, final long timeout,
        final TimeUnit timeUnit) {
        return asyncBucketManager
            .removeDesignDocument(name, development)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public DesignDocument publishDesignDocument(final String name, final long timeout, final TimeUnit timeUnit) {
        return asyncBucketManager
            .publishDesignDocument(name)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

    @Override
    public DesignDocument publishDesignDocument(final String name, final boolean overwrite, final long timeout,
        final TimeUnit timeUnit) {
        return asyncBucketManager
            .publishDesignDocument(name, overwrite)
            .timeout(timeout, timeUnit)
            .toBlocking()
            .single();
    }

}
