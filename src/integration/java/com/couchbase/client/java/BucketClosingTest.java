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
package com.couchbase.client.java;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.Before;
import org.junit.Test;

import com.couchbase.client.core.BucketClosedException;
import com.couchbase.client.java.util.ClusterDependentTest;
/**
 * Tests around closing a {@link Bucket} and preventing operations on it afterward
 *
 * @author Simon Baslé
 * @since 2.0.1
 */
public class BucketClosingTest extends ClusterDependentTest {

  @Before
  public void setup() {
    assumeFalse(CouchbaseTestContext.isMockEnabled());
  }

  @Test(expected = BucketClosedException.class)
  public void shouldPreventSyncCloseBucketThenSyncGet() {
    Bucket bucket;
    if (ctx.rbacEnabled()) {
      bucket = cluster().openBucket(bucketName());
    } else {
      bucket = cluster().openBucket(bucketName(), password());
    }
    bucket.close();

    bucket.get("someid");
    fail(); //if we managed to get here no exception at all where raised, fail
  }

  @Test(expected = BucketClosedException.class)
  public void shouldPreventSyncCloseBucketThenAsyncGet() {
    Bucket bucket;
    if (ctx.rbacEnabled()) {
      bucket = cluster().openBucket(bucketName());
    } else {
      bucket = cluster().openBucket(bucketName(), password());
    }
    bucket.close();

    bucket.async().get("someid").toBlocking().first();
    fail(); //if we managed to get here no exception at all where raised, fail
  }

  @Test(expected = BucketClosedException.class)
  public void shouldPreventAsyncClosedBucketThenGet() throws InterruptedException {
    Bucket bucket;
    if (ctx.rbacEnabled()) {
      bucket = cluster().openBucket(bucketName());
    } else {
      bucket = cluster().openBucket(bucketName(), password());
    }
    //ensures that even
    bucket.async().close().toBlocking().single();

    bucket.get("someid");
    fail(); //if we managed to get here no exception at all where raised, fail
  }

}
