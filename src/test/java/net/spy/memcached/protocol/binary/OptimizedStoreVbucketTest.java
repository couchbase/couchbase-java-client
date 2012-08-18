/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

package net.spy.memcached.protocol.binary;

import com.couchbase.client.CouchbaseClientBaseCase;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.FailInjectingCouchbaseConnectionFactory;
import com.couchbase.client.TestingCouchbaseClient;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import net.spy.memcached.BuildInfo;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.TestConfig;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationState;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreType;

/**
 * A test for optimized sets.
 *
 * This should generate an optimized set of sets, with one set destined for the
 * wrong vbucket in the middle of the group.  This wrong
 */
public class OptimizedStoreVbucketTest extends CouchbaseClientBaseCase {
  private TestingCouchbaseClient tclient;
  @Override
  protected void initClient() throws Exception {
    initClient(new FailInjectingCouchbaseConnectionFactory(Arrays.asList(
      URI.create("http://" + TestConfig.IPV4_ADDR + ":8091/pools")), "default",
      ""));
  }

  @Override
  protected void initClient(ConnectionFactory cf) throws Exception {
    tclient = new TestingCouchbaseClient((CouchbaseConnectionFactory) cf);
    client = tclient;
  }

  public void failedtotestOptimizedStore() {
    OptimizedSetImpl toTry = new OptimizedSetImpl(new StoreOperationImpl(
      StoreType.set, "key", 0, 10,
      "value".getBytes(), 0, null)); // flags, expiration, data, cas

    toTry.addOperation(new StoreOperationImpl(
      StoreType.set, "key2", 0, 10,
      "value".getBytes(), 0, null));

    toTry.initialize();
    assert toTry.getState() == OperationState.WRITE_QUEUED : "Write was not "
      + "queued as expected.";

    tclient.enqueueTestOperation("key", toTry);
  }

  public void testOptimizedStore() throws InterruptedException,
    ExecutionException {

    System.err.println("Using spymemcached build " + BuildInfo.GIT_HASH);

    ArrayList<OperationFuture<Boolean>> completed =
      new ArrayList<OperationFuture<Boolean>>();

    OperationFuture<Boolean> lastBogus, lastReal;

    System.err.println("Setting regular and bogus keys.  Bogus should retry.");
    for (int i=1; i<19; i++) {
      String kv = "iter" + i;
      client.set(kv, 0, kv);
      if (i%10 == 0) {
        OperationFuture<Boolean> justSet = client.set("bogus" + i, 0, kv);
        completed.add(justSet);
      }
    }

    // verify they're all successful
    System.err.println("Verifying all have been set successfully.");
    for (OperationFuture doneOpf : completed) {
      int times = 0;
      OperationStatus status;
      long startTime = System.currentTimeMillis();
      try {
        doneOpf.get();
      } catch (RuntimeException ex) {
        fail("Timed out while verifying set OperationFuture: "
          + doneOpf.toString() + " with exception " + ex.toString());
      }
      System.err.println("Time to get() from the future:"
        + (startTime - System.currentTimeMillis()));
      status = doneOpf.getStatus();
      do {

        if (times == 30) {
          fail("Tried " + times + " times and did not see a successful set"
            + " with key: \"" + doneOpf.getKey() + "\".  Operation status"
            + " message: " + status.getMessage());
        }
        if (times > 0) {
          double backoffMillis = Math.pow(2, times);
          backoffMillis = Math.min(1000, backoffMillis); // 1 sec max
          Thread.sleep((int) backoffMillis);
          System.err.println("Backing off to get a successful set, tries so"
            + " far: " + times);
        }
        times++;
      } while (!status.isSuccess());

      Object result = client.get(doneOpf.getKey());
      assertNotNull("Key was not set " + doneOpf.getKey(), result);

    }

  }

}
