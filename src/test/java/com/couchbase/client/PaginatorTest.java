/**
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

package com.couchbase.client;

import com.couchbase.client.clustermanager.BucketType;
import com.couchbase.client.protocol.views.Paginator;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.Stale;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.TestConfig;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies the correct functionality of the View Paginator.
 */
public class PaginatorTest {

  protected static TestingClient client = null;
  private static final ArrayList<City> CITY_DOCS;
  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools";
  public static final String DESIGN_DOC = "cities";
  public static final String VIEW_NAME_MAPRED = "all";
  public static final String VIEW_NAME_SPATIAL = "europe";

  static {
    CITY_DOCS = new ArrayList<City>();

    CITY_DOCS.add(new City("amsterdam", "netherlands", "europe"));
    CITY_DOCS.add(new City("rome", "italy", "europe"));
    CITY_DOCS.add(new City("paris", "france", "europe"));
    CITY_DOCS.add(new City("vienna", "austria", "europe"));
    CITY_DOCS.add(new City("new_york", "usa", "north_america"));
    CITY_DOCS.add(new City("san_francisco", "usa", "north_america"));
    CITY_DOCS.add(new City("shanghai", "china", "asia"));
    CITY_DOCS.add(new City("tokyo", "japan", "asia"));
    CITY_DOCS.add(new City("moscow", "russia", "asia"));
  }

  /**
   * Helper class to generate city JSON documents.
   */
  public static class City {
    private String type = "city";
    private String name;
    private String country;
    private String continent;
    public City(String n, String c, String co) {
      name = n;
      country = c;
      continent = co;
    }
    public String getKey() {
      return "city:" + name;
    }
    public String toJson() throws JSONException {
      JSONObject obj = new JSONObject();
      obj.put("type", type);
      obj.put("name", name);
      obj.put("country", country);
      obj.put("continent", continent);
      return obj.toString();
    }
  }

  /**
   * Initialize the client connection.
   *
   * @throws Exception
   */
  protected static void initClient() throws Exception {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    client = new TestingClient(uris, "default", "");
  }

  /**
   * Setup the bucket and views to test.
   *
   * @throws Exception
   */
  @BeforeClass
  public static void before() throws Exception {
    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 0, true);

    BucketTool.FunctionCallback callback = new BucketTool.FunctionCallback() {
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

    String docUri = "/default/_design/" + TestingClient.MODE_PREFIX
        + DESIGN_DOC;
    String view = "{\"language\":\"javascript\",\"views\":{\""
        + VIEW_NAME_MAPRED + "\":{\"map\":\"function (doc) { "
        + "if(doc.type == \\\"city\\\") {emit([doc.continent, doc.country, "
        + "doc.name], 1)}}\","
        + "\"reduce\":\"_sum\" }}}";
    client.asyncHttpPut(docUri, view);

    for(City city : CITY_DOCS) {
      client.set(city.getKey(), 0, city.toJson());
    }

    System.out.println("Setup of design docs complete, "
            + "sleeping until they propogate.");
    client.shutdown(10, TimeUnit.SECONDS);
  }

  /**
   * Initialize the client new before every test to provide a clean state.
   * @throws Exception
   */
  @Before
  public void beforeTest() throws Exception {
    initClient();
  }

  @Test
  public void testMapReduceWithExactPage() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_MAPRED);
    Query query = new Query();
    query.setReduce(false).setStale(Stale.FALSE);
    int docsPerPage = 3;

    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);
    int pageCount = 0;
    int totalCount = 0;
    while(paginatedQuery.hasNext()) {
      pageCount++;
      ViewResponse response = paginatedQuery.next();
      for(ViewRow row : response) {
        totalCount++;
        assertFalse(row.getKey().isEmpty());
        assertEquals("1", row.getValue());
      }
    }

    int expected = (int)Math.ceil((double)CITY_DOCS.size() / docsPerPage);
    assertEquals(expected, pageCount);
    assertEquals(CITY_DOCS.size(), totalCount);
  }

  @Test
  public void testMapReduceWithOffsetPage() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_MAPRED);
    Query query = new Query();
    query.setReduce(false).setStale(Stale.FALSE);
    int docsPerPage = 4;

    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);
    int pageCount = 0;
    int totalCount = 0;
    while(paginatedQuery.hasNext()) {
      pageCount++;
      ViewResponse response = paginatedQuery.next();
      for(ViewRow row : response) {
        totalCount++;
        assertFalse(row.getKey().isEmpty());
        assertEquals("1", row.getValue());
      }
    }

    int expected = (int)Math.ceil((double)CITY_DOCS.size() / docsPerPage);
    assertEquals(expected, pageCount);
    assertEquals(CITY_DOCS.size(), totalCount);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidDocsPerPage() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_MAPRED);
    Query query = new Query();
    query.setReduce(false).setStale(Stale.FALSE);
    int docsPerPage = 0;
    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);
  }

  @Test
  public void testMapReduceWithLimit() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_MAPRED);
    Query query = new Query();
    int limit = 5;
    query.setReduce(false).setStale(Stale.FALSE).setLimit(limit);
    int docsPerPage = 4;

    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);
    int pageCount = 0;
    int totalCount = 0;
    while(paginatedQuery.hasNext()) {
      pageCount++;
      ViewResponse response = paginatedQuery.next();
      for(ViewRow row : response) {
        totalCount++;
        assertFalse(row.getKey().isEmpty());
        assertEquals("1", row.getValue());
      }
    }

    int expected = (int)Math.ceil((double)limit / docsPerPage);
    assertEquals(expected, pageCount);
    assertEquals(limit, totalCount);
  }

}