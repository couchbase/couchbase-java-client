/**
 * Copyright (C) 2009-2013 Couchbase, Inc.
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

package com.couchbase.client;

import com.couchbase.client.BucketTool.FunctionCallback;
import com.couchbase.client.clustermanager.BucketType;
import com.couchbase.client.internal.HttpCompletionListener;
import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.protocol.views.ComplexKey;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.DocsOperationImpl;
import com.couchbase.client.protocol.views.HttpOperation;
import com.couchbase.client.protocol.views.InvalidViewException;
import com.couchbase.client.protocol.views.NoDocsOperationImpl;
import com.couchbase.client.protocol.views.OnError;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.ReducedOperationImpl;
import com.couchbase.client.protocol.views.RowError;
import com.couchbase.client.protocol.views.SpatialViewDesign;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewDesign;
import com.couchbase.client.protocol.views.ViewOperation.ViewCallback;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.PersistTo;
import net.spy.memcached.TestConfig;
import net.spy.memcached.ops.OperationStatus;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the correct functionality of views.
 */
public class ViewTest {

  protected static TestingClient client = null;
  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools";
  private static final Map<String, Object> ITEMS;

  public static final String DESIGN_DOC_W_REDUCE = "doc_with_view";
  public static final String DESIGN_DOC_WO_REDUCE = "doc_without_view";
  public static final String DESIGN_DOC_OBSERVE = "doc_observe";
  public static final String DESIGN_DOC_BINARY = "doc_binary";
  public static final String VIEW_NAME_W_REDUCE = "view_with_reduce";
  public static final String VIEW_NAME_WO_REDUCE = "view_without_reduce";
  public static final String VIEW_NAME_FOR_DATED = "view_emitting_dated";
  public static final String VIEW_NAME_OBSERVE = "view_staletest";
  public static final String VIEW_NAME_BINARY = "view_binary";

  static {
    ITEMS = new HashMap<String, Object>();
    int d = 0;
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        for (int k = 0; k < 5; k++, d++) {
          String type = new String(new char[] { (char) ('f' + i) });
          String small = (new Integer(j)).toString();
          String large = (new Integer(k)).toString();
          String doc = generateDoc(type, small, large);
          ITEMS.put("key" + d, doc);
        }
      }
    }
  }

  protected static void initClient() throws Exception {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    client = new TestingClient(uris, "default", "");
  }

  @BeforeClass
  public static void before() throws Exception {
    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 0, true);

    BucketTool.FunctionCallback callback = new FunctionCallback() {
      @Override
      public void callback() throws Exception {
        initClient();
      }

      @Override
      public String success(long elapsedTime) {
        return "Client Initialization took " + elapsedTime + "ms";
      }
    };
    bucketTool.poll(callback);
    bucketTool.waitForWarmup(client);

    // Create some design documents
    String docUri = "/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_W_REDUCE;
    String view = "{\"language\":\"javascript\",\"views\":{\""
        + VIEW_NAME_W_REDUCE + "\":{\"map\":\"function (doc) { "
        + "if(doc.type != \\\"dated\\\") {emit(doc.type, 1)}}\","
        + "\"reduce\":\"_sum\" }}}";
    client.asyncHttpPut(docUri, view);

    // Create the view for oberserve integration test.
    docUri = "/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_OBSERVE;
    view = "{\"language\":\"javascript\",\"views\":{\""
        + VIEW_NAME_OBSERVE + "\":{\"map\":\"function (doc, meta) {"
        + " if(doc.type == \\\"observetest\\\") { emit(meta.id, null); "
        + "} }\"}}}";
    client.asyncHttpPut(docUri, view);

    // Creating the Design/View for the binary docs
    docUri = "/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_BINARY;
    view = "{\"language\":\"javascript\",\"views\":{\""
        + VIEW_NAME_BINARY + "\":{\"map\":\"function (doc, meta) "
        +"{ if(meta.id.match(/nonjson/)) { emit(meta.id, null); }}\"}}}";
    client.asyncHttpPut(docUri, view);

    docUri = "/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_WO_REDUCE;
    String view2 = "{\"language\":\"javascript\",\"views\":{\""
        + VIEW_NAME_FOR_DATED + "\":{\"map\":\"function (doc) {  "
        + "emit(doc.type, 1)}\"}}}";
    for (Entry<String, Object> item : ITEMS.entrySet()) {
      assert client.set(item.getKey(), 0, item.getValue()).get().booleanValue();
    }
    HttpFuture<String> asyncHttpPut = client.asyncHttpPut(docUri, view2);

    String response = asyncHttpPut.get();
    OperationStatus status = asyncHttpPut.getStatus();
    if (!status.isSuccess()) {
      assert false : "Could not load views: " + status.getMessage()
              + " with response " + response;
    }
    client.shutdown();
    System.out.println("Setup of design docs complete, "
            + "sleeping until they propogate.");
    Thread.sleep(5000);
  }

  @Before
  public void beforeTest() throws Exception {
    initClient();
  }

  /**
   * Shuts the client down and nulls the reference.
   *
   * @throws Exception
   */
  @After
  public void afterTest() throws Exception {
    client.shutdown();
    client = null;
  }

  @AfterClass
  public static void after() throws Exception {
    // Delete all design documents I created
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    TestingClient c = new TestingClient(uris, "default", "");
    c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_W_REDUCE).get();

    c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_WO_REDUCE).get();

    c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_OBSERVE).get();

    c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC_BINARY).get();
  }

  private static String generateDoc(String type, String small, String large) {
    return "{\"type\":\"" + type + "\"" + ",\"small range\":\"" + small + "\","
        + "\"large range\":\"" + large + "\"}";
  }

  private static String generateDatedDoc(int year, int month, int day) {
    return "{\"type\":\"dated\",\"year\":" + year + ",\"month\":" + month + ","
        + "\"day\":" + day + "}";
  }

  @Test
  public void testAssertions() {
    boolean caught = false;
    try {
      assert false;
    } catch (AssertionError e) {
      caught = true;
    }
    assertTrue("Assertions are not enabled!", caught);
  }

  /**
   * Tests the view query with docs i.e. includeDocs and no reduce.
   *
   * @pre Retrieve a view including docs from the client.
   *    Perform an async query on the view.
   * @post Assert row id and document id if successful.
   */
  @Test
  public void testQueryWithDocs() {
    Query query = new Query();
    query.setReduce(false);
    query.setIncludeDocs(true);
    query.setStale(Stale.FALSE);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    assert view != null : "Could not retrieve view";
    HttpFuture<ViewResponse> future = client.asyncQuery(view, query);
    ViewResponse response=null;
    try {
      response = future.get();
    } catch (ExecutionException ex) {
      Logger.getLogger(ViewTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (InterruptedException ex) {
      Logger.getLogger(ViewTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    assert future.getStatus().isSuccess() : future.getStatus();

    Iterator<ViewRow> itr = response.iterator();
    while (itr.hasNext()) {
      ViewRow row = itr.next();
      if (ITEMS.containsKey(row.getId())) {
        assert ITEMS.get(row.getId()).equals(row.getDocument());
      }
    }
    assert ITEMS.size() == response.size() : future.getStatus().getMessage();
  }

  /**
   * Tests the view query without includeDocs and reduce.
   *
   * @pre Retrieve a view from the client.
   *   Perform an async query on the view.
   * @post Assert status and the response size.
   * @throws Exception
   */
  @Test
  public void testViewNoDocs() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query);
    assert future.getStatus().isSuccess() : future.getStatus();
    ViewResponse response = future.get();

    Iterator<ViewRow> itr = response.iterator();
    while (itr.hasNext()) {
      ViewRow row = itr.next();
      if (!ITEMS.containsKey(row.getId())) {
        assert false : ("Got an item that I shouldn't have gotten.");
      }
    }
    assert response.size() == ITEMS.size() : future.getStatus();
  }

  @Test
  public void testViewQueryWithListener() throws Exception {
    final Query query = new Query();
    query.setReduce(false);

    HttpFuture<View> future =
      client.asyncGetView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);

    final CountDownLatch latch = new CountDownLatch(1);
    future.addListener(new HttpCompletionListener() {
      @Override
      public void onComplete(HttpFuture<?> f) throws Exception {
        View view = (View) f.get();
        HttpFuture<ViewResponse> queryFuture = client.asyncQuery(view, query);
        queryFuture.addListener(new HttpCompletionListener() {
          @Override
          public void onComplete(HttpFuture<?> f) throws Exception {
            ViewResponse resp = (ViewResponse) f.get();
            if (resp.size() == ITEMS.size()) {
              latch.countDown();
            }
          }
        });
      }
    });

    assertTrue(latch.await(3, TimeUnit.SECONDS));
  }

  @Test
  public void testViewFutureWithListener() throws Exception {
    final Query query = new Query();
    query.setReduce(false);
    query.setIncludeDocs(true);

    HttpFuture<View> future =
      client.asyncGetView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);

    final CountDownLatch latch = new CountDownLatch(1);
    future.addListener(new HttpCompletionListener() {
      @Override
      public void onComplete(HttpFuture<?> f) throws Exception {
        View view = (View) f.get();
        HttpFuture<ViewResponse> queryFuture = client.asyncQuery(view, query);
        queryFuture.addListener(new HttpCompletionListener() {
          @Override
          public void onComplete(HttpFuture<?> f) throws Exception {
            ViewResponse resp = (ViewResponse) f.get();
            if (resp.size() == ITEMS.size()) {
              latch.countDown();
            }
          }
        });
      }
    });

    assertTrue(latch.await(3, TimeUnit.SECONDS));
  }

  /**
   * Tests the view query with reduce functionality.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Iterate over the reduced result set and
   *   assert the key value and size of the returned results.
   * @throws Exception
   */
  @Test
  public void testReduce() throws Exception {
    Query query = new Query();
    query.setReduce(true);
    query.setStale(Stale.FALSE);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query);
    ViewResponse reduce = future.get();

    Iterator<ViewRow> itr = reduce.iterator();
    while (itr.hasNext()) {
      ViewRow row = itr.next();
      assertNull(row.getKey());
      assertEquals(ITEMS.size(), Integer.parseInt(row.getValue()));
    }
  }

  /**
   * Tests the view query with implicit reduce.
   *
   * @pre Retrieve a view from the client. Perform an async
   *    query on the view. When a view with reduce is selected,
   *    make sure that implicitly reduce is used to align with
   *    the UI behaviour.
   * @post  Iterate over the reduced result set and assert
   *    the key value and size of the returned results.
   */
  @Test
  public void testImplicitReduce() {
    Query query = new Query();
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    ViewResponse reduce = client.query(view, query);
    Iterator<ViewRow> iterator = reduce.iterator();
    while(iterator.hasNext()) {
      ViewRow row = iterator.next();
      assertNull(row.getKey());
      assertEquals(ITEMS.size(), Integer.parseInt(row.getValue()));
    }
  }

  /**
   * Tests the view query with query set descending.
   *
   * @pre Retrieve a view from the client.
   *   Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetDescending() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setDescending(true));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with last document id.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetEndKeyDocID() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setEndkeyDocID("an_id"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with grouping true.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetGroup() throws Exception {
    Query query = new Query();
    query.setReduce(true);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setGroup(true));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with grouping true and without reduce.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  InvalidViewException will be returned.
   * @throws Exception
   */
  @Test(expected = InvalidViewException.class)
  public void testQuerySetGroupNoReduce() throws Exception {
    Query query = new Query();
    query.setGroup(true);
    View view = client.getView(DESIGN_DOC_WO_REDUCE, VIEW_NAME_WO_REDUCE);
    client.asyncQuery(view, query).get();
  }

  /**
   * Tests the view query with group level as 1.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetGroupWithLevel() throws Exception {
    Query query = new Query();
    query.setReduce(true);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setGroupLevel(1));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with last result set included.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetInclusiveEnd() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setInclusiveEnd(true));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with a key id set. It will return
   *    only documents that match the specified key.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetKey() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setKey("a_key"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with limit as 10,
   *    to return only 10 documents.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   * @TODO Inspect the correctness of the limit.
   */
  @Test
  public void testQuerySetLimit() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setLimit(10));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with start and end key values.
   *    Returns records in the given key range.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetRange() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setRange("key0", "key2"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with start key. Return records
   *    with a value equal to or greater than the specified key.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetRangeStart() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setRangeStart("start"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with complex key as the starting key.
   *
   * @pre  Prepare a complex query with date as the criteria.
   *       Retrieve a view from the client.
   *       Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetRangeStartComplexKey() throws Exception {

    // create a mess of stuff to query
    for (int i = 2009; i<2013; i++) {
      for (int j = 1; j<13; j++) {
        for (int k = 1; k<32; k++) {
          client.add("date" + i + j + k, 600, generateDatedDoc(i, j, k));
        }
      }
    }

    // now query it
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setRangeStart(ComplexKey.of(2012, 9, 5)));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with last key. Stops returning
   *    records when the specified key is reached.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetRangeEnd() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setRangeEnd("end"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query with a skip number so as to skip
   *    that many records before starting to return the results.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetSkip() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setSkip(0));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query which allows the
   *    results from a stale view to be used.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetStale() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setStale(Stale.OK));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the view query by setting the start
   *    key doc id to return records starting with it.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetStartkeyDocID() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setStartkeyDocID("key0"));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Tests the query with OnError parameter set.
   *    Sets the response in the event of an error.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testQuerySetOnError() throws Exception {
    Query query = new Query();
    query.setReduce(false);
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setOnError(OnError.CONTINUE));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  @Test
  public void testViewLoadWithListener() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    client.asyncGetView(DESIGN_DOC_WO_REDUCE, VIEW_NAME_W_REDUCE).addListener(
      new HttpCompletionListener() {
      @Override
      public void onComplete(HttpFuture<?> httpFuture) throws Exception {
        if (httpFuture.getStatus().isSuccess()) {
          latch.countDown();
        }
      }
    });
    assertTrue(latch.await(1, TimeUnit.MINUTES));
  }

  /**
   * Tests the query with reduce as true but not set.
   *
   * @pre Retrieve a view from the client.
   *    Perform an async query on the view.
   * @post  InvalidViewException is caught and
   *    the query happens without reduce.
   * @throws Exception
   */
  @Test
  public void testReduceWhenNoneExists() throws Exception {
    Query query = new Query();
    query.setReduce(true);
    try {
      View view = client.getView(DESIGN_DOC_WO_REDUCE, VIEW_NAME_WO_REDUCE);
      client.asyncQuery(view, query);
    } catch (InvalidViewException e) {
      return; // Pass, no reduce exists.
    }
    assert false : ("No view exists and this query still happened");
  }

  /**
   * Tests the query with complex key of range end.
   *
   * @pre Retrieve a view from the client. Perform an async query on the view.
   * @post Assert the response status is not null.
   * @throws Exception
   */
  @Test
  public void testComplexKeyQuery() throws Exception {
    Query query = new Query();
    query.setReduce(false);

    ComplexKey rangeEnd = ComplexKey.of("end");
    View view = client.getView(DESIGN_DOC_W_REDUCE, VIEW_NAME_W_REDUCE);
    HttpFuture<ViewResponse> future =
        client.asyncQuery(view, query.setRangeEnd(rangeEnd));
    ViewResponse response = future.get();
    assert response != null : future.getStatus();
  }

  /**
   * Test view with docs with errors. It tries to
   * ensure that the client does not crash when receiving a
   * bad HTTP response.
   *
   * @pre Prepare a new view and instantiate a new Http
   * NoDocs operation on the same.Verify the Http Response
   * having the rows array as empty.
   * @post Validates the Http Operation as successful if
   * the view has got data and request reaches the server.
   * @throws Exception
   */
  @Test
  public void testViewDocsWithErrors() throws Exception {
    View view = new View("a", "b", "c", true, true);
    HttpOperation op = new DocsOperationImpl(null, view, new ViewCallback() {
      @Override
      public void receivedStatus(OperationStatus status) {
        assert status.isSuccess();
      }

      @Override
      public void complete() {
        // Do nothing
      }

      @Override
      public void gotData(ViewResponse response) {
        assert response.getErrors().size() == 2;
        Iterator<RowError> row = response.getErrors().iterator();
        assert row.next().getFrom().equals("127.0.0.1:5984");
        assert response.size() == 0;
      }
    });
    HttpResponse response =
        new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
    String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from"
        + "\":\"127.0.0.1:5984\",\"reason\":\"Design document `_design/test"
        + "foobar` missing in database `test_db_b`.\"},{\"from\":\"http://"
        + "localhost:5984/_view_merge/\",\"reason\":\"Design document `"
        + "_design/testfoobar` missing in database `test_db_c`.\"}]}";
    StringEntity entity = new StringEntity(entityString);
    response.setEntity(entity);
    op.handleResponse(response);
  }

  /**
   * Test view no docs with errors. It tries to
   * ensure that the client does not crash when receiving a
   * bad HTTP response.
   *
   * @pre Prepare a new view and instantiate a new Http
   * NoDocs operation on the same. Verify the Http Response
   * having the rows array as empty.
   * @post Validates the Http Operation as successful if
   * the view has got data and request reaches the server.
   * @throws Exception
   */
  @Test
  public void testViewNoDocsWithErrors() throws Exception {
    View view = new View("a", "b", "c", true, true);
    HttpOperation op = new NoDocsOperationImpl(null, view, new ViewCallback() {
      @Override
      public void receivedStatus(OperationStatus status) {
        assert status.isSuccess();
      }

      @Override
      public void complete() {
        // Do nothing
      }

      @Override
      public void gotData(ViewResponse response) {
        assert response.getErrors().size() == 2;
        Iterator<RowError> row = response.getErrors().iterator();
        assert row.next().getFrom().equals("127.0.0.1:5984");
        assert response.size() == 0;
      }
    });
    HttpResponse response =
        new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
    String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from"
        + "\":\"127.0.0.1:5984\",\"reason\":\"Design document `_design/test"
        + "foobar` missing in database `test_db_b`.\"},{\"from\":\"http://"
        + "localhost:5984/_view_merge/\",\"reason\":\"Design document `"
        + "_design/testfoobar` missing in database `test_db_c`.\"}]}";
    StringEntity entity = new StringEntity(entityString);
    response.setEntity(entity);
    op.handleResponse(response);
  }

  /**
   * Test view reduced with errors.
   *
   * @pre Prepare a new view and instantiate
   * a new Http Reduce operation on the same.
   * Verify the Http Response for the same.
   * @post Validates the Http Operation as
   * successful if the view has got data
   * and request reaches the server.
   * @throws Exception
   */
  @Test
  public void testViewReducedWithErrors() throws Exception {
    View view = new View("a", "b", "c", true, true);
    HttpOperation op = new ReducedOperationImpl(null, view, new ViewCallback() {
      @Override
      public void receivedStatus(OperationStatus status) {
        assert status.isSuccess();
      }

      @Override
      public void complete() {
        // Do nothing
      }

      @Override
      public void gotData(ViewResponse response) {
        assert response.getErrors().size() == 2;
        Iterator<RowError> row = response.getErrors().iterator();
        assert row.next().getFrom().equals("127.0.0.1:5984");
        assert response.size() == 0;
      }
    });
    HttpResponse response =
        new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
    String entityString = "{\"total_rows\":0,\"rows\":[],\"errors\": [{\"from"
        + "\":\"127.0.0.1:5984\",\"reason\":\"Design document `_design/test"
        + "foobar` missing in database `test_db_b`.\"},{\"from\":\"http://"
        + "localhost:5984/_view_merge/\",\"reason\":\"Design document `"
        + "_design/testfoobar` missing in database `test_db_c`.\"}]}";
    StringEntity entity = new StringEntity(entityString);
    response.setEntity(entity);
    op.handleResponse(response);
  }

  /**
   * Verifies the addition of data to the master with
   * the given integrity constraints in combination with
   * the stale=false query.
   *
   * @pre Set data to the client with observe in a loop. Prepare
   * an async view query with docs and iterate over the response.
   * @post Return the correct dataset as all got persisted.
   *
   * @throws InterruptedException the interrupted exception
   * @throws ExecutionException the execution exception
   */
  @Test
  public void testObserveWithStaleFalse()
    throws InterruptedException, ExecutionException {
    int docAmount = 500;
    for (int i = 1; i <= docAmount; i++) {
      String value = "{\"type\":\"observetest\",\"value\":"+i+"}";
      assertTrue(client.set("observetest"+i, 0, value, PersistTo.MASTER).get());
    }

    Query query = new Query().setStale(Stale.FALSE);
    View view = client.getView(DESIGN_DOC_OBSERVE, VIEW_NAME_OBSERVE);

    HttpFuture<ViewResponse> future = client.asyncQuery(view, query);

    ViewResponse response = future.get();
    assert response != null : future.getStatus();

    Iterator<ViewRow> iterator = response.iterator();
    List<ViewRow> returnedRows = new ArrayList<ViewRow>();
    while (iterator.hasNext()) {
      ViewRow row = iterator.next();
      returnedRows.add(row);
    }

    assertEquals(docAmount, returnedRows.size());
  }

  /**
   * This test case adds two non-JSON documents and
   * utilises a special view that returns them.
   *
   * @pre Create non-JSON documents and set them to the db.
   * Prepare a view query with docs and iterate over the response.
   * Find the non json documents in the result set and assert them.
   * @post This makes sure that the view handlers don't break when
   * non-JSON data is read from the view.
   */
  @Test
  public void testViewWithBinaryDocs() {
    // Create non-JSON documents
    Date now = new Date();
    client.set("nonjson1", 0, now);
    client.set("nonjson2", 0, 42);

    View view = client.getView(DESIGN_DOC_BINARY, VIEW_NAME_BINARY);
    Query query = new Query();
    query.setIncludeDocs(true);
    query.setReduce(false);
    query.setStale(Stale.FALSE);

    assert view != null : "Could not retrieve view";
    ViewResponse response = client.query(view, query);

    Iterator<ViewRow> itr = response.iterator();
    while (itr.hasNext()) {
      ViewRow row = itr.next();
      if(row.getKey().equals("nonjson1")) {
        assertEquals(now.toString(), row.getDocument().toString());
      }
      if(row.getKey().equals("nonjson2")) {
        assertEquals(42, row.getDocument());
      }
    }
  }

  /**
   * This tests the design document creation using
   * views and spatial views.
   *
   * @pre Create two array lists with views and spatial views.
   * Using these, prepare a design document object. Pass this
   * instance to call the method asyncCreateDesignDoc on the
   * client. Put the current thread to sleep for 2000ms and then
   * again query the client for the just created design document.
   * @post Asserts true if the size of views in the design document is 2.
   */
  @Test
  public void testDesignDocumentCreation() throws InterruptedException {
    List<ViewDesign> views = new ArrayList<ViewDesign>();
    List<SpatialViewDesign> spviews = new ArrayList<SpatialViewDesign>();

    ViewDesign view1 = new ViewDesign(
      "view1",
      "function(a, b) {}"
    );
    views.add(view1);

    ViewDesign view2 = new ViewDesign(
      "view2",
      "function(b, c) {}",
      "function(red) {}"
    );
    views.add(view2);

    SpatialViewDesign spview = new SpatialViewDesign(
      "spatialfoo",
      "function(map) {}"
    );
    spviews.add(spview);

    DesignDocument doc = new DesignDocument("mydesign", views, spviews);
    HttpFuture<Boolean> result;
    boolean success = true;
    try {
      result = client.asyncCreateDesignDoc(doc);
      assertTrue(result.get());
    } catch (Exception ex) {
      success = false;
    }
    assertTrue(success);

    Thread.sleep(2000);

    DesignDocument design = client.getDesignDocument("mydesign");
    assertEquals(2, design.getViews().size());
  }

  /**
   * This tests the design document creation using views.
   *
   * @pre Create an array list with views. Using this, prepare a
   * design document object. Pass this instance to call the method
   * asyncCreateDesignDoc on the client. Put the current thread to
   * sleep for 2000ms and then again query the client for the just
   * created design document.
   * @post Asserts true if the size of views in the design document is 1.
   */
  @Test
  public void testRawDesignDocumentCreation() throws InterruptedException {
    List<ViewDesign> views = new ArrayList<ViewDesign>();

    ViewDesign view = new ViewDesign(
      "viewname",
      "function(a, b) {}"
    );
    views.add(view);

    DesignDocument doc = new DesignDocument("rawdesign", views, null);
    HttpFuture<Boolean> result;
    boolean success = true;
    try {
      result = client.asyncCreateDesignDoc(doc.getName(), doc.toJson());
      assertTrue(result.get());
    } catch (Exception ex) {
      success = false;
    }
    assertTrue(success);

    Thread.sleep(2000);

    DesignDocument design = client.getDesignDocument("rawdesign");
    assertEquals(1, design.getViews().size());
  }

  /**
   * Test invalid design doc handling.
   *
   * @pre pass any string to retrieve the views from it.
   * @post Return the InvalidViewException.
   */
  @Test
  public void testInvalidDesignDocumentCreation() throws Exception {
    String content = "{certainly_not_a_view: true}";
    HttpFuture<Boolean> result = client.asyncCreateDesignDoc(
      "invalid_design", content);
    assertFalse(result.get());

    boolean success = false;
    try {
      client.getDesignDocument("invalid_design");
    } catch(InvalidViewException ex) {
      success = true;
    }
    assertTrue(success);
  }

  /**
   * This tests the design document deletion.
   *
   * @pre Create a design document object with the name
   * of the design document previously created. Asserts true
   * if the size of views in the design document is 2. Call the
   * method asyncDeleteDesignDoc on the client to delete this
   * existing design document. Put the current thread to sleep
   * for 2000ms and then again query the client for the just
   * deleted design document.
   * @post Asserts true for demonstrating the success
   * of the deletion operation.
   */
  @Test
  public void testDesignDocumentDeletion() throws InterruptedException {
    DesignDocument design = client.getDesignDocument("mydesign");
    assertEquals(2, design.getViews().size());

    boolean success = true;

    try {
      HttpFuture<Boolean> result = client.asyncDeleteDesignDoc("mydesign");
      assertTrue(result.get());
    } catch (Exception ex) {
      success = false;
    }
    assertTrue(success);

    Thread.sleep(2000);

    success = false;
    try {
      design = client.getDesignDocument("mydesign");
    } catch(InvalidViewException e) {
      success = true;
    }
    assertTrue(success);
  }

  /**
   * Test invalid view handling.
   *
   * @pre pass any string in the view name and the design
   * doc name to retrieve the database view from it.
   * @post Return the InvalidViewException.
   */
  @Test(expected=InvalidViewException.class)
  public void testInvalidViewHandling() {
    String designDoc = "invalid_design";
    String viewName = "invalid_view";
    View view = client.getView(designDoc, viewName);
    assertNull(view);
  }

  /**
   * Test invalid view on valid design doc.
   */
  @Test(expected=InvalidViewException.class)
  public void testInvalidViewOnValidDesignDoc() {
    View view = client.getView(DESIGN_DOC_W_REDUCE, "invalidViewName");
  }

  /**
   * This test tries to retrieve the design document
   * with an invalid name.
   *
   * @pre Use an invalid name for search a design document
   * in the server. Call getDesignDocument method.
   * @post The design document is not loaded and the test
   * passes if InvalidViewException is returned as expected.
   */
  @Test(expected=InvalidViewException.class)
  public void testInvalidDesignDocHandling() {
    String designDoc = "invalid_design";
    client.getDesignDocument(designDoc);
  }

}
