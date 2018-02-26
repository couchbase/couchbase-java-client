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
package com.couchbase.client.java.bucket;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.util.IndexInfo;
import com.couchbase.client.java.util.Blocking;
import com.couchbase.client.java.view.DesignDocument;

public class DefaultBucketManager implements BucketManager {

    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private final AsyncBucketManager asyncBucketManager;
    private final long timeout;

    DefaultBucketManager(final CouchbaseEnvironment environment, final String bucket, final String username, final String password,
        final ClusterFacade core) {
        asyncBucketManager = DefaultAsyncBucketManager.create(bucket, username, password, core, environment);
        this.timeout = environment.managementTimeout();
    }

    public static DefaultBucketManager create(final CouchbaseEnvironment environment, final String bucket,
                                              final String password, final ClusterFacade core) {
        return new DefaultBucketManager(environment, bucket, bucket, password, core);
    }

    public static DefaultBucketManager create(final CouchbaseEnvironment environment, final String bucket, final String username,
        final String password, final ClusterFacade core) {
        return new DefaultBucketManager(environment, bucket, username, password, core);
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
        return Blocking.blockForSingle(asyncBucketManager.info().single(), timeout, timeUnit);
    }

    @Override
    public Boolean flush(final long timeout, final TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.flush().single(), timeout, timeUnit);
    }

    @Override
    public List<DesignDocument> getDesignDocuments(final long timeout, final TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.getDesignDocuments().toList(), timeout, timeUnit);
    }

    @Override
    public List<DesignDocument> getDesignDocuments(final boolean development, final long timeout,
        final TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.getDesignDocuments(development).toList(), timeout, timeUnit);
    }

    @Override
    public DesignDocument getDesignDocument(final String name, final long timeout, final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.getDesignDocument(name).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public DesignDocument getDesignDocument(final String name, final boolean development, final long timeout,
        final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.getDesignDocument(name, development).singleOrDefault(null), timeout, timeUnit
        );
    }

    @Override
    public DesignDocument insertDesignDocument(final DesignDocument designDocument, final long timeout,
        final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.insertDesignDocument(designDocument).single(), timeout, timeUnit
        );
    }

    @Override
    public DesignDocument insertDesignDocument(final DesignDocument designDocument, final boolean development,
        final long timeout, final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.insertDesignDocument(designDocument, development).single(), timeout, timeUnit
        );
    }

    @Override
    public DesignDocument upsertDesignDocument(final DesignDocument designDocument, final long timeout,
        final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.upsertDesignDocument(designDocument).single(), timeout, timeUnit
        );
    }

    @Override
    public DesignDocument upsertDesignDocument(final DesignDocument designDocument, final boolean development,
        final long timeout, final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.upsertDesignDocument(designDocument, development).single(), timeout, timeUnit
        );
    }

    @Override
    public Boolean removeDesignDocument(final String name, final long timeout, final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.removeDesignDocument(name).single(), timeout, timeUnit
        );
    }

    @Override
    public Boolean removeDesignDocument(final String name, final boolean development, final long timeout,
        final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.removeDesignDocument(name, development).single(), timeout, timeUnit
        );
    }

    @Override
    public DesignDocument publishDesignDocument(final String name, final long timeout, final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.publishDesignDocument(name).single(), timeout, timeUnit
        );
    }

    @Override
    public DesignDocument publishDesignDocument(final String name, final boolean overwrite, final long timeout,
        final TimeUnit timeUnit) {
        return Blocking.blockForSingle(
            asyncBucketManager.publishDesignDocument(name, overwrite).single(), timeout, timeUnit
        );
    }

    @Override
    public List<IndexInfo> listN1qlIndexes() {
        return listN1qlIndexes(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<IndexInfo> listN1qlIndexes(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.listN1qlIndexes().toList(), timeout, timeUnit);
    }

    @Override
    public boolean createN1qlPrimaryIndex(boolean ignoreIfExist, boolean defer) {
        return createN1qlPrimaryIndex(ignoreIfExist, defer, timeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean createN1qlPrimaryIndex(boolean ignoreIfExist, boolean defer, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.createN1qlPrimaryIndex(ignoreIfExist, defer), timeout, timeUnit);
    }

    @Override
    public boolean createN1qlPrimaryIndex(String customName, boolean ignoreIfExist, boolean defer) {
        return createN1qlPrimaryIndex(customName, ignoreIfExist, defer, timeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean createN1qlPrimaryIndex(String customName, boolean ignoreIfExist, boolean defer, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.createN1qlPrimaryIndex(customName, ignoreIfExist, defer), timeout, timeUnit);
    }

    private boolean createN1qlIndex(String indexName, boolean ignoreIfExist, boolean defer, long timeout, TimeUnit timeUnit, Object... fields) {
        return Blocking.blockForSingle(asyncBucketManager.createN1qlIndex(indexName, ignoreIfExist, defer, fields),
                timeout, timeUnit);
    }

    @Override
    public boolean createN1qlIndex(String indexName, boolean ignoreIfExist, boolean defer, Object... fields) {
        return createN1qlIndex(indexName, ignoreIfExist, defer, timeout, TIMEOUT_UNIT, fields);
    }

    @Override
    public boolean createN1qlIndex(String indexName, List<Object> fields, Expression whereClause, boolean ignoreIfExist, boolean defer) {
        return createN1qlIndex(indexName, fields, whereClause, ignoreIfExist, defer, timeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean createN1qlIndex(String indexName, List<Object> fields, Expression whereClause, boolean ignoreIfExist, boolean defer,
            long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.createN1qlIndex(indexName, fields, whereClause, ignoreIfExist, defer),
                timeout, timeUnit);
    }

    @Override
    public boolean dropN1qlPrimaryIndex(boolean ignoreIfNotExist) {
        return dropN1qlPrimaryIndex(ignoreIfNotExist, timeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean dropN1qlPrimaryIndex(boolean ignoreIfNotExist, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.dropN1qlPrimaryIndex(ignoreIfNotExist), timeout, timeUnit);
    }

    @Override
    public boolean dropN1qlPrimaryIndex(String customName, boolean ignoreIfNotExist) {
        return dropN1qlPrimaryIndex(customName, ignoreIfNotExist, timeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean dropN1qlPrimaryIndex(String customName, boolean ignoreIfNotExist, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.dropN1qlPrimaryIndex(customName, ignoreIfNotExist), timeout, timeUnit);
    }

    @Override
    public boolean dropN1qlIndex(String name, boolean ignoreIfNotExist) {
        return dropN1qlIndex(name, ignoreIfNotExist, timeout, TIMEOUT_UNIT);
    }

    @Override
    public boolean dropN1qlIndex(String name, boolean ignoreIfNotExist, long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.dropN1qlIndex(name, ignoreIfNotExist), timeout, timeUnit);
    }

    @Override
    public List<String> buildN1qlDeferredIndexes() {
        return buildN1qlDeferredIndexes(timeout, TIMEOUT_UNIT);
    }

    @Override
    public List<String> buildN1qlDeferredIndexes(long timeout, TimeUnit timeUnit) {
        return Blocking.blockForSingle(asyncBucketManager.buildN1qlDeferredIndexes(), timeout, timeUnit);
    }

    @Override
    public List<IndexInfo> watchN1qlIndexes(List<String> watchList, long watchTimeout, TimeUnit watchTimeUnit) {
        return asyncBucketManager.watchN1qlIndexes(watchList, watchTimeout, watchTimeUnit)
                .toList()
                .toBlocking()
                .single();
    }
}
