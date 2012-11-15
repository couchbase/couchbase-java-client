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
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.SpatialView;
import com.couchbase.client.protocol.views.SpatialViewRowNoDocs;
import com.couchbase.client.protocol.views.SpatialViewRowWithDocs;
import com.couchbase.client.protocol.views.Stale;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Verifies the correct functionality of spatial view queries.
 */
public class SpatialViewTest {
  protected static TestingClient client = null;
  private static final ArrayList<City> CITY_DOCS;
  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
      + ":8091/pools";
  public static final String DESIGN_DOC = "cities";
  public static final String VIEW_NAME_SPATIAL = "all_cities";

  static {
    CITY_DOCS = new ArrayList<City>();

    CITY_DOCS.add(new City("amsterdam", 52.22, 4.53));
    CITY_DOCS.add(new City("rome", 41.54, 12.27));
    CITY_DOCS.add(new City("paris", 48.48, 2.20));
    CITY_DOCS.add(new City("vienna", 48.14, 16.20));
  }

  /**
   * Helper class to generate city JSON documents.
   */
  public static class City {
    private String type = "city";
    private String name;
    private double lat;
    private double lng;
    public City(String n, double la, double ln) {
      name = n;
      lat = la;
      lng = ln;
    }
    public String getKey() {
      return "city:" + name;
    }
    public String toJson() throws JSONException {
      JSONObject obj = new JSONObject();
      obj.put("type", type);
      obj.put("name", name);
      obj.put("lat", lat);
      obj.put("lng", lng);
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
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 0);

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
    String view = "{\"language\":\"javascript\",\"spatial\":{\""
        + VIEW_NAME_SPATIAL + "\":\"function (doc, meta) { "
        + "if(doc.type == \\\"city\\\") {emit({type: \\\"Point\\\", "
        + "coordinates: [doc.lng, doc.lat]}, [meta.id, doc.name])}}\"}}";
    client.asyncHttpPut(docUri, view);

    for(City city : CITY_DOCS) {
      client.set(city.getKey(), 0, city.toJson());
    }

    System.out.println("Setup of design docs complete, "
            + "sleeping until they propogate.");
    client.shutdown(10, TimeUnit.SECONDS);
  }

  /**
   * Remove all design documents created and used.
   *
   * @throws Exception
   */
  @AfterClass
  public static void after() throws Exception {
    List<URI> uris = new LinkedList<URI>();
    uris.add(URI.create(SERVER_URI));
    TestingClient c = new TestingClient(uris, "default", "");
    c.asyncHttpDelete("/default/_design/" + TestingClient.MODE_PREFIX
      + DESIGN_DOC).get();
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
  public void testSpatialWithoutDocs() {
    SpatialView view = client.getSpatialView(DESIGN_DOC, VIEW_NAME_SPATIAL);
    String expected = "/default/_design/" + DESIGN_DOC + "/_spatial/"
      + VIEW_NAME_SPATIAL;
    assertEquals(expected, view.getURI());

    Query query = new Query();
    query.setStale(Stale.FALSE);
    ViewResponse response = client.query(view, query);
    for(ViewRow row : response) {
      assertTrue(row instanceof SpatialViewRowNoDocs);
      assertFalse(row.getBbox().isEmpty());
      assertFalse(row.getGeometry().isEmpty());
      assertFalse(row.getValue().isEmpty());
    }
    assertEquals(CITY_DOCS.size(), response.size());
  }

  @Test
  public void testSpatialBbox() {
    SpatialView view = client.getSpatialView(DESIGN_DOC, VIEW_NAME_SPATIAL);
    Query query = new Query();
    query.setStale(Stale.FALSE).setBbox(0, 0, 50, 50);

    ViewResponse response = client.query(view, query);
    for(ViewRow row : response) {
      assertTrue(row instanceof SpatialViewRowNoDocs);
      assertFalse(row.getBbox().isEmpty());
      assertFalse(row.getGeometry().isEmpty());
      assertFalse(row.getValue().isEmpty());
    }
    assertEquals(3, response.size());
  }

  @Test
  public void testSpatialWithDocs() {
    SpatialView view = client.getSpatialView(DESIGN_DOC, VIEW_NAME_SPATIAL);
    Query query = new Query();
    query.setStale(Stale.FALSE).setIncludeDocs(true);

    ViewResponse response = client.query(view, query);
    for(ViewRow row : response) {
      assertTrue(row instanceof SpatialViewRowWithDocs);
      assertFalse(row.getBbox().isEmpty());
      assertFalse(row.getGeometry().isEmpty());
      assertFalse(row.getValue().isEmpty());
      assertFalse(((String)row.getDocument()).isEmpty());
    }
    assertEquals(CITY_DOCS.size(), response.size());
  }
}
