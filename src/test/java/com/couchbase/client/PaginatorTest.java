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
import net.spy.memcached.PersistTo;
import net.spy.memcached.TestConfig;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
  public static final String VIEW_NAME_POPULATION_STRING =
    "by_population_string";
  public static final String VIEW_NAME_POPULATION_INT =
    "by_population_int";

  static {
    CITY_DOCS = new ArrayList<City>();

    CITY_DOCS.add(new City("amsterdam", "netherlands", "europe", 820654));
    CITY_DOCS.add(new City("rome", "italy", "europe", 2777979));
    CITY_DOCS.add(new City("paris", "france", "europe", 2234105));
    CITY_DOCS.add(new City("vienna", "austria", "europe", 1731236));
    CITY_DOCS.add(new City("new_york", "usa", "north_america", 8244910));
    CITY_DOCS.add(new City("san_francisco", "usa", "north_america", 812826));
    CITY_DOCS.add(new City("shanghai", "china", "asia", 23019148));
    CITY_DOCS.add(new City("tokyo", "japan", "asia", 13222760));
    CITY_DOCS.add(new City("moscow", "russia", "asia", 11979529));
  }

  /**
   * Helper class to generate city JSON documents.
   */
  public static class City {
    private final String type = "city";
    private final String name;
    private final String country;
    private final String continent;
    private final long population;

    /**
     * Instantiates a new city.
     *
     * @param n the name
     * @param c the country
     * @param co the continent
     */
    public City(String n, String c, String co, long pop) {
      name = n;
      country = c;
      continent = co;
      population = pop;
    }

    /**
     * Gets the key.
     * @return the key
     */
    public String getKey() {
      return "city:" + name;
    }

    /**
     * Converts json object to string.
     * @return the string
     * @throws JSONException the jSON exception
     */
    public String toJson() throws JSONException {
      JSONObject obj = new JSONObject();
      obj.put("type", type);
      obj.put("name", name);
      obj.put("country", country);
      obj.put("continent", continent);
      obj.put("population", population);
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
      + VIEW_NAME_MAPRED + "\":{\"map\":\""
      + "function (doc) { if(doc.type == \\\"city\\\") {emit([doc.continent, "
      + "doc.country, doc.name], 1)}}\",\"reduce\":\"_sum\"},"
      + "\"" + VIEW_NAME_POPULATION_STRING + "\":{\"map\":\"function(doc, meta)"
      + "{\\n  if(doc.type == \\\"city\\\" && doc.population) {\\n    "
      + "emit(doc.population.toString(), null);\\n  }\\n}\"},"
      + "\"" + VIEW_NAME_POPULATION_INT + "\":{\"map\":\"function(doc, meta)"
      + "{\\n  if(doc.type == \\\"city\\\" && doc.population) "
      + "{\\n    emit(doc.population, null);\\n  }\\n}\"}}}";

    client.asyncHttpPut(docUri, view).get();

    for(City city : CITY_DOCS) {
      client.set(city.getKey(), 0, city.toJson(), PersistTo.MASTER)
        .get(10, TimeUnit.SECONDS);
    }

    client.shutdown(10, TimeUnit.SECONDS);
    System.out.println("Setup of design docs complete, "
            + "sleeping until they propogate.");
    Thread.sleep(5000);
  }

  /**
   * Initialize the client new before every
   * test to provide a clean state.
   *
   * @throws Exception
   */
  @Before
  public void beforeTest() throws Exception {
    initClient();
  }

  /**
   * Test map reduce view functionality.
   *
   * @pre  Query the view to fetch all the records.
   * No filter is added on the view and a paginated
   * query is prepared with only 3 documents per page.
   * Iteration is performed on the result set.
   * @post  Assert if the view rows are empty or if
   * the expected row count doesn't match 1, or if the
   * number of pages don't match the calculated count.
   */
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

  /**
   * Test map reduce view functionality.
   *
   * @pre Query the view to fetch all the records.
   * No filter is added on the view and a paginated
   * query is prepared with only 3 documents per page.
   * Iteration is performed on the result set.
   * @post  Assert if the view rows are empty or if
   * the expected row count doesn't match 1, or if the
   * number of pages don't match the calculated count.
   */
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

  /**
   * Test invalid documents per page.
   *
   * @pre Query the view to fetch all the records.
   * No filter is added on the view and a paginated
   * query is prepared with 0 documents per page.
   * @post Test passes illegal argument exception
   * is returned.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidDocsPerPage() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_MAPRED);
    Query query = new Query();
    query.setReduce(false).setStale(Stale.FALSE);
    int docsPerPage = 0;
    client.paginatedQuery(view, query, docsPerPage);
  }

  /**
   * Test views using the map reduce functionality
   * by setting the limit.
   *
   * @pre Query the view to fetch all the records.
   * Limit of 5 is added on the view and a paginated
   * query is prepared with 4 documents per page.
   * Iterate over the result set.
   * @post Assert if the view rows are empty or if
   * the expected row count doesn't match 1,or if the
   * number of pages don't match the calculated count.
   */
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

  @Test
  public void testWithExactReduce() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_MAPRED);
    Query query = new Query();
    query.setGroupLevel(1);
    int docsPerPage = 1;

    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);

    int pageCount = 0;
    int totalCount = 0;
    while(paginatedQuery.hasNext()) {
      pageCount++;
      ViewResponse response = paginatedQuery.next();
      for(ViewRow row : response) {
        totalCount++;
        assertTrue("Reduce value is 0 or less",
          Integer.parseInt(row.getValue()) > 0);
      }
    }

    assertEquals(3, pageCount);
    assertEquals(3, totalCount);
  }

  @Test
  public void testWithOffsetReduce() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_MAPRED);
    Query query = new Query();
    query.setGroupLevel(1);
    int docsPerPage = 2;

    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);

    int pageCount = 0;
    int totalCount = 0;
    while(paginatedQuery.hasNext()) {
      pageCount++;
      ViewResponse response = paginatedQuery.next();
      for(ViewRow row : response) {
        totalCount++;
        assertTrue("Reduce value is 0 or less",
          Integer.parseInt(row.getValue()) > 0);
      }
    }

    assertEquals(2, pageCount);
    assertEquals(3, totalCount);
  }

  @Test
  public void testWithReduceAndLimit() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_MAPRED);
    Query query = new Query();
    query.setGroupLevel(1);
    query.setLimit(2);
    int docsPerPage = 1;

    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);

    int pageCount = 0;
    int totalCount = 0;
    while(paginatedQuery.hasNext()) {
      pageCount++;
      ViewResponse response = paginatedQuery.next();
      for(ViewRow row : response) {
        totalCount++;
        assertTrue("Reduce value is 0 or less",
          Integer.parseInt(row.getValue()) > 0);
      }
    }

    assertEquals(2, pageCount);
    assertEquals(2, totalCount);
  }

  @Test
  public void testStringifiedNumber() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_POPULATION_STRING);
    Query query = new Query();
    int docsPerPage = 2;

    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);
    paginatedQuery.forceKeyType(String.class);
    int pageCount = 0;
    int totalCount = 0;
    while(paginatedQuery.hasNext()) {
      pageCount++;
      ViewResponse response = paginatedQuery.next();
      for(ViewRow row : response) {
        totalCount++;
      }
    }

    int expected = (int)Math.ceil((double)CITY_DOCS.size() / docsPerPage);
    assertEquals(expected, pageCount);
    assertEquals(CITY_DOCS.size(), totalCount);
  }

  @Test
  public void testNumber() {
    View view = client.getView(DESIGN_DOC, VIEW_NAME_POPULATION_INT);
    Query query = new Query();
    int docsPerPage = 2;

    Paginator paginatedQuery = client.paginatedQuery(view, query, docsPerPage);
    int pageCount = 0;
    int totalCount = 0;
    while(paginatedQuery.hasNext()) {
      pageCount++;
      ViewResponse response = paginatedQuery.next();
      for(ViewRow row : response) {
        totalCount++;
      }
    }

    int expected = (int)Math.ceil((double)CITY_DOCS.size() / docsPerPage);
    assertEquals(expected, pageCount);
    assertEquals(CITY_DOCS.size(), totalCount);
  }

}
