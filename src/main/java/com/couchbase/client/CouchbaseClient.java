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

import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.internal.ViewFuture;
import com.couchbase.client.protocol.views.DocsOperationImpl;
import com.couchbase.client.protocol.views.HttpOperation;
import com.couchbase.client.protocol.views.NoDocsOperationImpl;
import com.couchbase.client.protocol.views.Paginator;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.ReducedOperationImpl;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewFetcherOperation;
import com.couchbase.client.protocol.views.ViewFetcherOperationImpl;
import com.couchbase.client.protocol.views.ViewOperation.ViewCallback;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.couchbase.client.protocol.views.ViewsFetcherOperation;
import com.couchbase.client.protocol.views.ViewsFetcherOperationImpl;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.config.Bucket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.CASValue;
import net.spy.memcached.CachedData;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.compat.CloseUtil;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.GetlOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.transcoders.Transcoder;

import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;

/**
 * A client for Couchbase Server.
 */
public class CouchbaseClient extends MemcachedClient
  implements CouchbaseClientIF, Reconfigurable {

  private static final String MODE_PRODUCTION = "production";
  private static final String MODE_DEVELOPMENT = "development";
  private static final String DEV_PREFIX = "dev_";
  private static final String PROD_PREFIX = "";
  public static final String MODE_PREFIX;
  private static final String MODE_ERROR;

  private ViewConnection vconn;
  protected volatile boolean reconfiguring = false;

  /**
   * Properties priority from highest to lowest:
   *
   * 1. Property defined in user code.
   * 2. Property defined on command line.
   * 3. Property defined in cbclient.properties.
   */
  static {
    Properties properties = new Properties(System.getProperties());
    String viewmode = properties.getProperty("viewmode", null);

    if (viewmode == null) {
      FileInputStream fs = null;
      try {
        URL url =  ClassLoader.getSystemResource("cbclient.properties");
        if (url != null) {
          fs = new FileInputStream(new File(url.getFile()));
          properties.load(fs);
        }
        viewmode = properties.getProperty("viewmode");
      } catch (IOException e) {
        // Properties file doesn't exist. Error logged later.
      } finally {
        if (fs != null) {
          CloseUtil.close(fs);
        }
      }
    }

    if (viewmode == null) {
      MODE_ERROR = "viewmode property isn't defined. Setting viewmode to"
        + " production mode";
      MODE_PREFIX = PROD_PREFIX;
    } else if (viewmode.equals(MODE_PRODUCTION)) {
      MODE_ERROR = "viewmode set to production mode";
      MODE_PREFIX = PROD_PREFIX;
    } else if (viewmode.equals(MODE_DEVELOPMENT)) {
      MODE_ERROR = "viewmode set to development mode";
      MODE_PREFIX = DEV_PREFIX;
    } else {
      MODE_ERROR = "unknown value \"" + viewmode + "\" for property viewmode"
          + " Setting to production mode";
      MODE_PREFIX = PROD_PREFIX;
    }
  }

  /**
   * Get a CouchbaseClient based on the REST response from a Couchbase server.
   *
   * This constructor is merely a convenience for situations where the bucket
   * name is the same as the user name. This is commonly the case.
   *
   * To connect to the "default" special bucket for a given cluster, use an
   * empty string as the password.
   *
   * If a password has not been assigned to the bucket, it is typically an empty
   * string.
   *
   * @param baseList the URI list of one or more servers from the cluster
   * @param bucketName the bucket name in the cluster you wish to use
   * @param pwd the password for the bucket
   * @throws IOException if connections could not be made
   * @throws ConfigurationException if the configuration provided by the server
   *           has issues or is not compatible
   */
  public CouchbaseClient(List<URI> baseList, String bucketName, String pwd)
    throws IOException {
    this(new CouchbaseConnectionFactory(baseList, bucketName, pwd));
  }

  /**
   * Get a CouchbaseClient based on the REST response from a Couchbase server
   * where the username is different than the bucket name.
   *
   * To connect to the "default" special bucket for a given cluster, use an
   * empty string as the password.
   *
   * If a password has not been assigned to the bucket, it is typically an empty
   * string.
   *
   * @param baseList the URI list of one or more servers from the cluster
   * @param bucketName the bucket name in the cluster you wish to use
   * @param usr the username for the bucket; this nearly always be the same as
   *          the bucket name
   * @param pwd the password for the bucket
   * @throws IOException if connections could not be made
   * @throws ConfigurationException if the configuration provided by
   *          the server has issues or is not compatible
   */
  public CouchbaseClient(final List<URI> baseList, final String bucketName,
      final String usr, final String pwd) throws IOException {
    this(new CouchbaseConnectionFactory(baseList, bucketName, pwd));
  }

  /**
   * Get a CouchbaseClient based on the REST response from a Couchbase server
   * where the username is different than the bucket name.
   *
   * Note that when specifying a ConnectionFactory you must specify a
   * BinaryConnectionFactory. Also the ConnectionFactory's protocol and locator
   * values are always overwritten. The protocol will always be binary and the
   * locator will be chosen based on the bucket type you are connecting to.
   *
   * To connect to the "default" special bucket for a given cluster, use an
   * empty string as the password.
   *
   * If a password has not been assigned to the bucket, it is typically an empty
   * string.
   *
   * The subscribe variable is determines whether or not we will subscribe to
   * the configuration changes feed. This constructor should be used when
   * calling super from subclasses of CouchbaseClient since the subclass might
   * want to start the changes feed later.
   *
   * @param cf the ConnectionFactory to use to create connections
   * @param subscribe whether or not to subscribe to config changes
   * @throws IOException if connections could not be made
   * @throws ConfigurationException if the configuration provided by the server
   *           has issues or is not compatible
   */
  public CouchbaseClient(CouchbaseConnectionFactory cf)
    throws IOException {
    super(cf, AddrUtil.getAddresses(cf.getVBucketConfig().getServers()));
    List<InetSocketAddress> addrs =
      AddrUtil.getAddressesFromURL(cf.getVBucketConfig().getCouchServers());

    getLogger().info(MODE_ERROR);
    vconn = cf.createViewConnection(addrs);
    cf.getConfigurationProvider().subscribe(cf.getBucketName(), this);
  }

  /**
   * This function is called when there is a topology change in the cluster.
   * This function is intended for internal use only.
   */
  public void reconfigure(Bucket bucket) {
    reconfiguring = true;
    try {
      vconn.reconfigure(bucket);
      ((CouchbaseConnection)mconn).reconfigure(bucket);
    } catch (IllegalArgumentException ex) {
      getLogger().warn("Failed to reconfigure client, staying with "
          + "previous configuration.", ex);
    } finally {
      reconfiguring = false;
    }
  }



  /**
   * Gets access to a view contained in a design document from the cluster.
   *
   * The purpose of a view is take the structured data stored within the
   * Couchbase Server database as JSON documents, extract the fields and
   * information, and to produce an index of the selected information.
   *
   * The result is a view on the stored data. The view that is created
   * during this process allows you to iterate, select and query the
   * information in your database from the raw data objects that have
   * been stored.
   *
   * @param designDocumentName the name of the design document.
   * @param viewName the name of the view to get.
   * @return a View object from the cluster.
   * @throws InterruptedException if the operation is interrupted while in
   *           flight
   * @throws ExecutionException if an error occurs during execution
   */
  public HttpFuture<View> asyncGetView(String designDocumentName,
      final String viewName) {
    designDocumentName = MODE_PREFIX + designDocumentName;
    String bucket = ((CouchbaseConnectionFactory)connFactory).getBucketName();
    String uri = "/" + bucket + "/_design/" + designDocumentName;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<View> crv = new HttpFuture<View>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new ViewFetcherOperationImpl(request, bucket, designDocumentName,
            viewName, new ViewFetcherOperation.ViewFetcherCallback() {
              private View view = null;

              @Override
              public void receivedStatus(OperationStatus status) {
                crv.set(view, status);
              }

              @Override
              public void complete() {
                couchLatch.countDown();
              }

              @Override
              public void gotData(View v) {
                view = v;
              }
            });
    crv.setOperation(op);
    addOp(op);
    assert crv != null : "Problem retrieving view";
    return crv;
  }

  /**
   * Gets a future with a list of views for a given design document from the
   * cluster.
   *
   * The purpose of a view is take the structured data stored within the
   * Couchbase Server database as JSON documents, extract the fields and
   * information, and to produce an index of the selected information.
   *
   * The result is a view on the stored data. The view that is created
   * during this process allows you to iterate, select and query the
   * information in your database from the raw data objects that have
   * been stored.
   *
   * @param designDocumentName the name of the design document.
   * @return a future containing a List of View objects from the cluster.
   */
  public HttpFuture<List<View>> asyncGetViews(String designDocumentName) {
    designDocumentName = MODE_PREFIX + designDocumentName;
    String bucket = ((CouchbaseConnectionFactory)connFactory).getBucketName();
    String uri = "/" + bucket + "/_design/" + designDocumentName;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<List<View>> crv =
        new HttpFuture<List<View>>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op = new ViewsFetcherOperationImpl(request, bucket,
        designDocumentName, new ViewsFetcherOperation.ViewsFetcherCallback() {
          private List<View> views = null;

          @Override
          public void receivedStatus(OperationStatus status) {
            crv.set(views, status);
          }

          @Override
          public void complete() {
            couchLatch.countDown();
          }

          @Override
          public void gotData(List<View> v) {
            views = v;
          }
        });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  /**
   * Gets access to a view contained in a design document from the cluster.
   *
   * The purpose of a view is take the structured data stored within the
   * Couchbase Server database as JSON documents, extract the fields and
   * information, and to produce an index of the selected information.
   *
   * The result is a view on the stored data. The view that is created
   * during this process allows you to iterate, select and query the
   * information in your database from the raw data objects that have
   * been stored.
   *
   * @param designDocumentName the name of the design document.
   * @param viewName the name of the view to get.
   * @return a View object from the cluster.
   */
  public View getView(final String designDocumentName, final String viewName) {
    try {
      return asyncGetView(designDocumentName, viewName).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted getting views", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed getting views", e);
    }
  }

  /**
   * Gets a list of views for a given design document from the cluster.
   *
   * @param designDocumentName the name of the design document.
   * @return a list of View objects from the cluster.
   */
  public List<View> getViews(final String designDocumentName) {
    try {
      return asyncGetViews(designDocumentName).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted getting views", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed getting views", e);
    }
  }

  public HttpFuture<ViewResponse> asyncQuery(View view, Query query) {
    if (query.willReduce()) {
      return asyncQueryAndReduce(view, query);
    } else if (query.willIncludeDocs()) {
      return asyncQueryAndIncludeDocs(view, query);
    } else {
      return asyncQueryAndExcludeDocs(view, query);
    }
  }

  /**
   * Asynchronously queries a Couchbase view and returns the result.
   * The result can be accessed row-wise via an iterator. This
   * type of query will return the view result along with all of the documents
   * for each row in the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  private HttpFuture<ViewResponse> asyncQueryAndIncludeDocs(View view,
      Query query) {
    assert view != null : "Who passed me a null view";
    assert query != null : "who passed me a null query";
    String viewUri = view.getURI();
    String queryToRun = query.toString();
    assert viewUri != null : "view URI seems to be null";
    assert queryToRun != null  : "query seems to be null";
    String uri = viewUri + queryToRun;
    getLogger().info("lookin for:" + uri);
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final ViewFuture crv = new ViewFuture(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op = new DocsOperationImpl(request, new ViewCallback() {
      private ViewResponse vr = null;

      @Override
      public void receivedStatus(OperationStatus status) {
        if (vr != null) {
          Collection<String> ids = new LinkedList<String>();
          Iterator<ViewRow> itr = vr.iterator();
          while (itr.hasNext()) {
            ids.add(itr.next().getId());
          }
          crv.set(vr, asyncGetBulk(ids), status);
        } else {
          crv.set(null, null, status);
        }
      }

      @Override
      public void complete() {
        couchLatch.countDown();
      }

      @Override
      public void gotData(ViewResponse response) {
        vr = response;
      }
    });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  /**
   * Asynchronously queries a Couchbase view and returns the result.
   * The result can be accessed row-wise via an iterator. This
   * type of query will return the view result but will not
   * get the documents associated with each row of the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  private HttpFuture<ViewResponse> asyncQueryAndExcludeDocs(View view,
      Query query) {
    String uri = view.getURI() + query.toString();
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<ViewResponse> crv =
        new HttpFuture<ViewResponse>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new NoDocsOperationImpl(request, new ViewCallback() {
          private ViewResponse vr = null;

          @Override
          public void receivedStatus(OperationStatus status) {
            crv.set(vr, status);
          }

          @Override
          public void complete() {
            couchLatch.countDown();
          }

          @Override
          public void gotData(ViewResponse response) {
            vr = response;
          }
        });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  /**
   * Asynchronously queries a Couchbase view and returns the result.
   * The result can be accessed row-wise via an iterator.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a Future containing the results of the query.
   */
  private HttpFuture<ViewResponse> asyncQueryAndReduce(final View view,
      final Query query) {
    if (!view.hasReduce()) {
      throw new RuntimeException("This view doesn't contain a reduce function");
    }
    String uri = view.getURI() + query.toString();
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<ViewResponse> crv =
        new HttpFuture<ViewResponse>(couchLatch, 60000);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new ReducedOperationImpl(request, new ViewCallback() {
          private ViewResponse vr = null;

          @Override
          public void receivedStatus(OperationStatus status) {
            crv.set(vr, status);
          }

          @Override
          public void complete() {
            couchLatch.countDown();
          }

          @Override
          public void gotData(ViewResponse response) {
            vr = response;
          }
        });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  /**
   * Queries a Couchbase view and returns the result.
   * The result can be accessed row-wise via an iterator.
   * This type of query will return the view result along
   * with all of the documents for each row in
   * the query.
   *
   * @param view the view to run the query against.
   * @param query the type of query to run against the view.
   * @return a ViewResponseWithDocs containing the results of the query.
   */
  public ViewResponse query(View view, Query query) {
    try {
      return asyncQuery(view, query).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while accessing the view", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to access the view", e);
    }
  }

  /**
   * A paginated query allows the user to get the results of a large query in
   * small chunks allowing for better performance. The result allows you
   * to iterate through the results of the query and when you get to the end
   * of the current result set the client will automatically fetch the next set
   * of results.
   *
   * @param view the view to query against.
   * @param query the query for this request.
   * @param docsPerPage the amount of documents per page.
   * @return A Paginator (iterator) to use for reading the results of the query.
   */
  public Paginator paginatedQuery(View view, Query query, int docsPerPage) {
    return new Paginator(this, view, query, docsPerPage);
  }

  /**
   * Adds an operation to the queue where it waits to be sent to Couchbase. This
   * function is for internal use only.
   */
  public void addOp(final HttpOperation op) {
    vconn.checkState();
    vconn.addOp(op);
  }


  /**
   * Gets and locks the given key asynchronously. By default the maximum allowed
   * timeout is 30 seconds. Timeouts greater than this will be set to 30
   * seconds.
   *
   * @param key the key to fetch and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @param tc the transcoder to serialize and unserialize value
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public <T> OperationFuture<CASValue<T>> asyncGetAndLock(final String key,
      int exp, final Transcoder<T> tc) {
    final CountDownLatch latch = new CountDownLatch(1);
    final OperationFuture<CASValue<T>> rv =
        new OperationFuture<CASValue<T>>(key, latch, operationTimeout);

    Operation op = opFact.getl(key, exp, new GetlOperation.Callback() {
      private CASValue<T> val = null;

      public void receivedStatus(OperationStatus status) {
        if (!status.isSuccess()) {
          val = new CASValue<T>(-1, null);
        }
        rv.set(val, status);
      }

      public void gotData(String k, int flags, long cas, byte[] data) {
        assert key.equals(k) : "Wrong key returned";
        assert cas > 0 : "CAS was less than zero:  " + cas;
        val =
            new CASValue<T>(cas, tc.decode(new CachedData(flags, data, tc
                .getMaxSize())));
      }

      public void complete() {
        latch.countDown();
      }
    });
    rv.setOperation(op);
    mconn.enqueueOperation(key, op);
    return rv;
  }

  /**
   * Get and lock the given key asynchronously and decode with the default
   * transcoder. By default the maximum allowed timeout is 30 seconds. Timeouts
   * greater than this will be set to 30 seconds.
   *
   * @param key the key to fetch and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @return a future that will hold the return value of the fetch
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public OperationFuture<CASValue<Object>> asyncGetAndLock(final String key,
      int exp) {
    return asyncGetAndLock(key, exp, transcoder);
  }

  /**
   * Getl with a single key. By default the maximum allowed timeout is 30
   * seconds. Timeouts greater than this will be set to 30 seconds.
   *
   * @param key the key to get and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @param tc the transcoder to serialize and unserialize value
   * @return the result from the cache (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public <T> CASValue<T> getAndLock(String key, int exp, Transcoder<T> tc) {
    try {
      return asyncGetAndLock(key, exp, tc).get(operationTimeout,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for value", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Exception waiting for value", e);
    } catch (TimeoutException e) {
      throw new OperationTimeoutException("Timeout waiting for value", e);
    }
  }

  /**
   * Get and lock with a single key and decode using the default transcoder. By
   * default the maximum allowed timeout is 30 seconds. Timeouts greater than
   * this will be set to 30 seconds.
   *
   * @param key the key to get and lock
   * @param exp the amount of time the lock should be valid for in seconds.
   * @return the result from the cache (null if there is none)
   * @throws OperationTimeoutException if the global operation timeout is
   *           exceeded
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public CASValue<Object> getAndLock(String key, int exp) {
    return getAndLock(key, exp, transcoder);
  }

  /**
   * Unlock the given key asynchronously from the cache.
   *
   * @param key the key to unlock
   * @param casId the CAS identifier
   * @param tc the transcoder to serialize and unserialize value
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public <T> OperationFuture<Boolean> asyncUnlock(final String key,
          long casId, final Transcoder<T> tc) {
    final CountDownLatch latch = new CountDownLatch(1);
    final OperationFuture<Boolean> rv = new OperationFuture<Boolean>(key,
            latch, operationTimeout);
    Operation op = opFact.unlock(key, casId, new OperationCallback() {

      @Override
      public void receivedStatus(OperationStatus s) {
        rv.set(s.isSuccess(), s);
      }

      @Override
      public void complete() {
        latch.countDown();
      }
    });
    rv.setOperation(op);
    mconn.enqueueOperation(key, op);
    return rv;
  }

  /**
   * Unlock the given key asynchronously from the cache with the default
   * transcoder.
   *
   * @param key the key to unlock
   * @param casId the CAS identifier
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public OperationFuture<Boolean> asyncUnlock(final String key,
          long casId) {
    return asyncUnlock(key, casId, transcoder);
  }

  /**
   * Unlock the given key synchronously from the cache.
   *
   * @param key the key to unlock
   * @param casId the CAS identifier
   * @param tc the transcoder to serialize and unserialize value
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */
  public <T> Boolean unlock(final String key,
          long casId, final Transcoder<T> tc) {
    try {
      return asyncUnlock(key, casId, tc).get(operationTimeout,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for value", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Exception waiting for value", e);
    } catch (TimeoutException e) {
      throw new OperationTimeoutException("Timeout waiting for value", e);
    }

  }
  /**
   * Unlock the given key synchronously from the cache with the default
   * transcoder.
   *
   * @param key the key to unlock
   * @param casId the CAS identifier
   * @return whether or not the operation was performed
   * @throws IllegalStateException in the rare circumstance where queue is too
   *           full to accept any more requests
   */

  public Boolean unlock(final String key,
          long casId) {
    return unlock(key, casId, transcoder);
  }

  /**
   * Gets the number of vBuckets that are contained in the cluster. This
   * function is for internal use only and should rarely be since there
   * are few use cases in which it is necessary.
   */
  @Override
  public int getNumVBuckets() {
    return ((CouchbaseConnectionFactory)connFactory).getVBucketConfig()
      .getVbucketsCount();
  }

  @Override
  public boolean shutdown(long timeout, TimeUnit unit) {
    boolean shutdownResult = false;
    try {
      shutdownResult = super.shutdown(timeout, unit);
      CouchbaseConnectionFactory cf = (CouchbaseConnectionFactory) connFactory;
      cf.getConfigurationProvider().shutdown();
      vconn.shutdown();
    } catch (IOException ex) {
      Logger.getLogger(
         CouchbaseClient.class.getName()).log(Level.SEVERE,
            "Unexpected IOException in shutdown", ex);
      throw new RuntimeException(null, ex);
    }
    return shutdownResult;
  }

}
