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
package com.couchbase.client.java;

import static org.junit.Assert.fail;

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

  @Test(expected = BucketClosedException.class)
  public void shouldPreventSyncCloseBucketThenSyncGet() {
    Bucket bucket = cluster().openBucket(bucketName(), password());
    bucket.close();

    bucket.get("someid");
    fail(); //if we managed to get here no exception at all where raised, fail
  }

  @Test(expected = BucketClosedException.class)
  public void shouldPreventSyncCloseBucketThenAsyncGet() {
    Bucket bucket = cluster().openBucket(bucketName(), password());
    bucket.close();

    bucket.async().get("someid").toBlocking().first();
    fail(); //if we managed to get here no exception at all where raised, fail
  }

  @Test(expected = BucketClosedException.class)
  public void shouldPreventAsyncClosedBucketThenGet() throws InterruptedException {
    Bucket bucket = cluster().openBucket(bucketName(), password());
    //ensures that even
    bucket.async().close().toBlocking().single();

    bucket.get("someid");
    fail(); //if we managed to get here no exception at all where raised, fail
  }

}
