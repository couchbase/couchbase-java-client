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

import com.couchbase.client.clustermanager.FlushResponse;
import com.couchbase.client.internal.HttpFuture;
import com.couchbase.client.internal.ObserveFuture;
import com.couchbase.client.internal.ReplicaGetFuture;
import com.couchbase.client.internal.ViewFuture;
import com.couchbase.client.protocol.views.AbstractView;
import com.couchbase.client.protocol.views.DesignDocFetcherOperation;
import com.couchbase.client.protocol.views.DesignDocFetcherOperationImpl;
import com.couchbase.client.protocol.views.DesignDocOperationImpl;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.DocsOperationImpl;
import com.couchbase.client.protocol.views.HttpOperation;
import com.couchbase.client.protocol.views.HttpOperationImpl;
import com.couchbase.client.protocol.views.InvalidViewException;
import com.couchbase.client.protocol.views.NoDocsOperationImpl;
import com.couchbase.client.protocol.views.Paginator;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.ReducedOperationImpl;
import com.couchbase.client.protocol.views.SpatialView;
import com.couchbase.client.protocol.views.SpatialViewFetcherOperation;
import com.couchbase.client.protocol.views.SpatialViewFetcherOperationImpl;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewFetcherOperation;
import com.couchbase.client.protocol.views.ViewFetcherOperationImpl;
import com.couchbase.client.protocol.views.ViewOperation.ViewCallback;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.VBucketNodeLocator;
import com.couchbase.client.vbucket.config.Bucket;
import com.couchbase.client.vbucket.config.Config;
import com.couchbase.client.vbucket.config.ConfigType;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.BroadcastOpFactory;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.CachedData;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ObserveResponse;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationCompletionListener;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetlOperation;
import net.spy.memcached.ops.ObserveOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.ReplicaGetOperation;
import net.spy.memcached.ops.StatsOperation;
import net.spy.memcached.transcoders.Transcoder;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A client for Couchbase Server.
 *
 * This class acts as your main entry point while working with your Couchbase
 * cluster (if you want to work with TAP, see the TapClient instead).
 *
 * If you are working with Couchbase Server 2.0, remember to set the appropriate
 * view mode depending on your environment.
 */
public class CouchbaseClient extends MemcachedClient
  implements CouchbaseClientIF, Reconfigurable {

  private static final String MODE_PRODUCTION = "production";
  private static final String MODE_DEVELOPMENT = "development";
  private static final String DEV_PREFIX = "dev_";
  private static final String PROD_PREFIX = "";
  public static final String MODE_PREFIX;
  private static final String MODE_ERROR;

  private ViewConnection vconn = null;
  protected volatile boolean reconfiguring = false;
  private final CouchbaseConnectionFactory cbConnFactory;
  protected final ExecutorService executorService;

  /**
   * Try to load the cbclient.properties file and check for the viewmode.
   *
   * If no viewmode (either through "cbclient.viewmode" or "viewmode")
   * property is set, the fallback is always "production". Possible options
   * are either "development" or "production".
   */
  static {
    CouchbaseProperties.setPropertyFile("cbclient.properties");

    String viewmode = CouchbaseProperties.getProperty("viewmode");
    if (viewmode == null) {
      viewmode = CouchbaseProperties.getProperty("viewmode", true);
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
   * Get a CouchbaseClient based on the initial server list provided.
   *
   * This constructor should be used if the bucket name is the same as the
   * username (which is normally the case). If your bucket does not have
   * a password (likely the "default" bucket), use an empty string instead.
   *
   * This method is only a convenience method so you don't have to create a
   * CouchbaseConnectionFactory for yourself.
   *
   * @param baseList the URI list of one or more servers from the cluster
   * @param bucketName the bucket name in the cluster you wish to use
   * @param pwd the password for the bucket
   * @throws IOException if connections could not be made
   * @throws com.couchbase.client.vbucket.ConfigurationException if the
   *          configuration provided by the server has issues or is not
   *          compatible.
   */
  public CouchbaseClient(final List<URI> baseList, final String bucketName,
    final String pwd)
    throws IOException {
    this(new CouchbaseConnectionFactory(baseList, bucketName, pwd));
  }

  /**
   * Get a CouchbaseClient based on the initial server list provided.
   *
   * Currently, Couchbase Server does not support a different username than the
   * bucket name. Therefore, this method ignores the given username for now
   * but will likely use it in the future.
   *
   * This constructor should be used if the bucket name is NOT the same as the
   * username. If your bucket does not have a password (likely the "default"
   * bucket), use an empty string instead.
   *
   * This method is only a convenience method so you don't have to create a
   * CouchbaseConnectionFactory for yourself.
   *
   * @param baseList the URI list of one or more servers from the cluster
   * @param bucketName the bucket name in the cluster you wish to use
   * @param user the username for the bucket
   * @param pwd the password for the bucket
   * @throws IOException if connections could not be made
   * @throws com.couchbase.client.vbucket.ConfigurationException if the
   *          configuration provided by the server has issues or is not
   *          compatible.
   */
  public CouchbaseClient(final List<URI> baseList, final String bucketName,
    final String user, final String pwd)
    throws IOException {
    this(new CouchbaseConnectionFactory(baseList, bucketName, pwd));
  }

  /**
   * Get a CouchbaseClient based on the settings from the given
   * CouchbaseConnectionFactory.
   *
   * If your bucket does not have a password (likely the "default" bucket), use
   * an empty string instead.
   *
   * The URI list provided here is only used during the initial connection to
   * the cluster. Afterwards, the actual cluster-map is synchronized from the
   * cluster and maintained internally by the client. This allows the client to
   * update the map as needed (when the cluster topology changes).
   *
   * Note that when specifying a ConnectionFactory you must specify a
   * BinaryConnectionFactory (which is the case if you use the
   * CouchbaseConnectionFactory). Also the ConnectionFactory's protocol and
   * locator values are always overwritten. The protocol will always be binary
   * and the locator will be chosen based on the bucket type you are connecting
   * to.
   *
   * The subscribe variable determines whether or not we will subscribe to
   * the configuration changes feed. This constructor should be used when
   * calling super from subclasses of CouchbaseClient since the subclass might
   * want to start the changes feed later.
   *
   * @param cf the ConnectionFactory to use to create connections
   * @throws IOException if connections could not be made
   * @throws com.couchbase.client.vbucket.ConfigurationException if the
   *          configuration provided by the server has issues or is not
   *          compatible.
   */
  public CouchbaseClient(CouchbaseConnectionFactory cf)
    throws IOException {
    super(cf, AddrUtil.getAddresses(cf.getVBucketConfig().getServers()));
    getLogger().info(cf.toString());

    cbConnFactory = cf;

    if(cf.getVBucketConfig().getConfigType() == ConfigType.COUCHBASE) {
      List<InetSocketAddress> addrs =
        AddrUtil.getAddressesFromURL(cf.getVBucketConfig().getCouchServers());
      vconn = cf.createViewConnection(addrs);
    }

    executorService = cbConnFactory.getListenerExecutorService();

    getLogger().info(MODE_ERROR);
    cf.getConfigurationProvider().subscribe(cf.getBucketName(), this);
  }

  @Override
  public void reconfigure(Bucket bucket) {
    reconfiguring = true;
    if (bucket.isNotUpdating()) {
      getLogger().info("Bucket configuration is disconnected from cluster "
        + "configuration updates, attempting to reconnect.");
      CouchbaseConnectionFactory cbcf = (CouchbaseConnectionFactory)connFactory;
      cbcf.requestConfigReconnect(cbcf.getBucketName(), this);
      cbcf.checkConfigUpdate();
    }
    try {
      cbConnFactory.getConfigurationProvider().updateBucket(
        cbConnFactory.getBucketName(), bucket);
      cbConnFactory.updateStoredBaseList(bucket.getConfig());

      if(vconn != null) {
        vconn.reconfigure(bucket);
      }
      if (mconn instanceof CouchbaseConnection) {
        CouchbaseConnection cbConn = (CouchbaseConnection) mconn;
        cbConn.reconfigure(bucket);
      } else {
        CouchbaseMemcachedConnection cbMConn =
          (CouchbaseMemcachedConnection) mconn;
        cbMConn.reconfigure(bucket);
      }
    } catch (IllegalArgumentException ex) {
      getLogger().warn("Failed to reconfigure client, staying with "
          + "previous configuration.", ex);
    } finally {
      reconfiguring = false;
    }
  }

  @Override
  public HttpFuture<View> asyncGetView(String designDocumentName,
      final String viewName) {
    CouchbaseConnectionFactory factory =
      (CouchbaseConnectionFactory) connFactory;

    designDocumentName = MODE_PREFIX + designDocumentName;
    String bucket = factory.getBucketName();
    String uri = "/" + bucket + "/_design/" + designDocumentName;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<View> crv = new HttpFuture<View>(couchLatch,
      factory.getViewTimeout(), executorService);

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

  @Override
  public HttpFuture<SpatialView> asyncGetSpatialView(String designDocumentName,
      final String viewName) {
    CouchbaseConnectionFactory factory =
      (CouchbaseConnectionFactory) connFactory;
    designDocumentName = MODE_PREFIX + designDocumentName;
    String bucket = factory.getBucketName();
    String uri = "/" + bucket + "/_design/" + designDocumentName;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<SpatialView> crv = new HttpFuture<SpatialView>(
      couchLatch, factory.getViewTimeout(), executorService);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new SpatialViewFetcherOperationImpl(request, bucket, designDocumentName,
            viewName, new SpatialViewFetcherOperation.ViewFetcherCallback() {
              private SpatialView view = null;

              @Override
              public void receivedStatus(OperationStatus status) {
                crv.set(view, status);
              }

              @Override
              public void complete() {
                couchLatch.countDown();
              }

              @Override
              public void gotData(SpatialView v) {
                view = v;
              }
            });
    crv.setOperation(op);
    addOp(op);
    assert crv != null : "Problem retrieving spatial view";
    return crv;
  }

  @Override
  public HttpFuture<DesignDocument> asyncGetDesignDoc(
    String designDocumentName) {
    designDocumentName = MODE_PREFIX + designDocumentName;
    String bucket = ((CouchbaseConnectionFactory)connFactory).getBucketName();
    String uri = "/" + bucket + "/_design/" + designDocumentName;
    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<DesignDocument> crv =
        new HttpFuture<DesignDocument>(couchLatch, 60000, executorService);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op = new DesignDocFetcherOperationImpl(
      request,
      designDocumentName,
      new DesignDocFetcherOperation.DesignDocFetcherCallback() {
          private DesignDocument design = null;

          @Override
          public void receivedStatus(OperationStatus status) {
            crv.set(design, status);
          }

          @Override
          public void complete() {
            couchLatch.countDown();
          }

          @Override
          public void gotData(DesignDocument d) {
            design = d;
          }
        });
    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  @Override
  public View getView(final String designDocumentName, final String viewName) {
    try {
      View view = asyncGetView(designDocumentName, viewName).get();
      if(view == null) {
        throw new InvalidViewException("Could not load view \""
          + viewName + "\" for design doc \"" + designDocumentName + "\"");
      }
      return view;
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted getting views", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Failed getting views", e);
      }
    }
  }

  @Override
  public SpatialView getSpatialView(final String designDocumentName,
    final String viewName) {
    try {
      SpatialView view = asyncGetSpatialView(designDocumentName, viewName)
        .get();
      if(view == null) {
        throw new InvalidViewException("Could not load spatial view \""
          + viewName + "\" for design doc \"" + designDocumentName + "\"");
      }
      return view;
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted getting spatial view", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Failed getting views", e);
      }
    }
  }

  @Override
  public DesignDocument getDesignDoc(final String designDocumentName) {
    try {
      DesignDocument design = asyncGetDesignDocument(designDocumentName).get();
      if(design == null) {
        throw new InvalidViewException("Could not load design document \""
          + designDocumentName + "\"");
      }
      return design;
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted getting design document", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Failed getting design document", e);
      }
    }
  }

  @Override
  @Deprecated
  public HttpFuture<DesignDocument> asyncGetDesignDocument(
    final String designDocumentName) {
    return asyncGetDesignDoc(designDocumentName);
  }

  @Override
  @Deprecated
  public DesignDocument getDesignDocument(final String designDocumentName) {
    return getDesignDoc(designDocumentName);
  }

  @Override
  public Boolean createDesignDoc(final DesignDocument doc) {
    try {
      return asyncCreateDesignDoc(doc).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted creating design document", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Failed creating design document", e);
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Failed creating design document", e);
    }
  }

  @Override
  public HttpFuture<Boolean> asyncCreateDesignDoc(String name, String value)
    throws UnsupportedEncodingException {
    getLogger().info("Creating Design Document:" + name);
    String bucket = ((CouchbaseConnectionFactory)connFactory).getBucketName();
    final String uri = "/" + bucket + "/_design/" + MODE_PREFIX + name;

    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<Boolean> crv = new HttpFuture<Boolean>(couchLatch, 60000,
      executorService);
    HttpRequest request = new BasicHttpEntityEnclosingRequest("PUT", uri,
            HttpVersion.HTTP_1_1);
    request.setHeader(new BasicHeader("Content-Type", "application/json"));
    StringEntity entity = new StringEntity(value);
    ((BasicHttpEntityEnclosingRequest) request).setEntity(entity);

    HttpOperationImpl op = new DesignDocOperationImpl(request,
      new OperationCallback() {
        @Override
        public void receivedStatus(OperationStatus status) {
          crv.set(status.getMessage().equals("Error Code: 201"), status);
        }

        @Override
        public void complete() {
          couchLatch.countDown();
        }
      });

    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  @Override
  public HttpFuture<Boolean> asyncCreateDesignDoc(final DesignDocument doc)
    throws UnsupportedEncodingException {
    return asyncCreateDesignDoc(doc.getName(), doc.toJson());
  }

  @Override
  public Boolean deleteDesignDoc(final String name) {
    try {
      return asyncDeleteDesignDoc(name).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted deleting design document", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Failed deleting design document", e);
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Failed deleting design document", e);
    }
  }

  @Override
  public HttpFuture<Boolean> asyncDeleteDesignDoc(final String name)
    throws UnsupportedEncodingException {
    getLogger().info("Deleting Design Document:" + name);
    String bucket = ((CouchbaseConnectionFactory)connFactory).getBucketName();

    final String uri = "/" + bucket + "/_design/" + MODE_PREFIX + name;

    final CountDownLatch couchLatch = new CountDownLatch(1);
    final HttpFuture<Boolean> crv = new HttpFuture<Boolean>(couchLatch, 60000,
      executorService);
    HttpRequest request = new BasicHttpEntityEnclosingRequest("DELETE", uri,
            HttpVersion.HTTP_1_1);
    request.setHeader(new BasicHeader("Content-Type", "application/json"));

    HttpOperationImpl op = new DesignDocOperationImpl(request,
      new OperationCallback() {
        @Override
        public void receivedStatus(OperationStatus status) {
          crv.set(status.getMessage().equals("Error Code: 200"), status);
        }

        @Override
        public void complete() {
          couchLatch.countDown();
        }
      });

    crv.setOperation(op);
    addOp(op);
    return crv;
  }

  @Override
  public HttpFuture<ViewResponse> asyncQuery(AbstractView view, Query query) {
    if(view.hasReduce() && !query.getArgs().containsKey("reduce")) {
      query.setReduce(true);
    }

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
  private HttpFuture<ViewResponse> asyncQueryAndIncludeDocs(AbstractView view,
      Query query) {
    assert view != null : "Who passed me a null view";
    assert query != null : "who passed me a null query";
    String viewUri = view.getURI();
    String queryToRun = query.toString();
    assert viewUri != null : "view URI seems to be null";
    assert queryToRun != null  : "query seems to be null";
    String uri = viewUri + queryToRun;

    final CountDownLatch couchLatch = new CountDownLatch(1);
    int timeout = ((CouchbaseConnectionFactory) connFactory).getViewTimeout();
    final ViewFuture crv = new ViewFuture(couchLatch, timeout, view,
      executorService);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op = new DocsOperationImpl(request, view,
      new ViewCallback() {
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
  private HttpFuture<ViewResponse> asyncQueryAndExcludeDocs(AbstractView view,
      Query query) {
    String uri = view.getURI() + query.toString();
    final CountDownLatch couchLatch = new CountDownLatch(1);
    int timeout = ((CouchbaseConnectionFactory) connFactory).getViewTimeout();
    final HttpFuture<ViewResponse> crv =
        new HttpFuture<ViewResponse>(couchLatch, timeout, executorService);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new NoDocsOperationImpl(request, view, new ViewCallback() {
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
  private HttpFuture<ViewResponse> asyncQueryAndReduce(final AbstractView view,
      final Query query) {
    if (!view.hasReduce()) {
      throw new RuntimeException("This view doesn't contain a reduce function");
    }
    String uri = view.getURI() + query.toString();
    final CountDownLatch couchLatch = new CountDownLatch(1);
    int timeout = ((CouchbaseConnectionFactory) connFactory).getViewTimeout();
    final HttpFuture<ViewResponse> crv =
        new HttpFuture<ViewResponse>(couchLatch, timeout, executorService);

    final HttpRequest request =
        new BasicHttpRequest("GET", uri, HttpVersion.HTTP_1_1);
    final HttpOperation op =
        new ReducedOperationImpl(request, view, new ViewCallback() {
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

  @Override
  public ViewResponse query(AbstractView view, Query query) {
    try {
      return asyncQuery(view, query).get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while accessing the view", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Failed to access the view", e);
      }
    }
  }

  @Override
  public Paginator paginatedQuery(View view, Query query, int docsPerPage) {
    return new Paginator(this, view, query, docsPerPage);
  }

  /**
   * Adds an operation to the queue where it waits to be sent to Couchbase.
   */
  protected void addOp(final HttpOperation op) {
    if(vconn != null) {
      vconn.addOp(op);
    }
  }

  @Override
  public <T> OperationFuture<CASValue<T>> asyncGetAndLock(final String key,
      int exp, final Transcoder<T> tc) {
    final CountDownLatch latch = new CountDownLatch(1);
    final OperationFuture<CASValue<T>> rv =
        new OperationFuture<CASValue<T>>(key, latch, operationTimeout,
          executorService);

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

  @Override
  public OperationFuture<CASValue<Object>> asyncGetAndLock(final String key,
      int exp) {
    return asyncGetAndLock(key, exp, transcoder);
  }

  @Override
  public Object getFromReplica(String key) {
    return getFromReplica(key, transcoder);
  }

  @Override
  public <T> T getFromReplica(String key, Transcoder<T> tc) {
    try {
      return asyncGetFromReplica(key, tc).get(operationTimeout,
        TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for value", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Exception waiting for value", e);
    } catch (TimeoutException e) {
      throw new OperationTimeoutException("Timeout waiting for value", e);
    }
  }

  @Override
  public ReplicaGetFuture<Object> asyncGetFromReplica(final String key) {
    return asyncGetFromReplica(key, transcoder);
  }

  @Override
  public <T> ReplicaGetFuture<T> asyncGetFromReplica(final String key,
    final Transcoder<T> tc) {
    int discardedOps = 0;

    int bucketReplicaCount = cbConnFactory.getVBucketConfig().getReplicasCount();
    if (bucketReplicaCount == 0) {
      getLogger().debug("No replica configured for this bucket, trying to get "
        + "the document from active node only.");
    }

    VBucketNodeLocator locator = (VBucketNodeLocator) mconn.getLocator();
    List<Integer> actualReplicaIndexes = locator.getReplicaIndexes(key);

    final ReplicaGetFuture<T> replicaFuture = new ReplicaGetFuture<T>(
      operationTimeout, executorService);

    for(int index : actualReplicaIndexes) {
      final CountDownLatch latch = new CountDownLatch(1);
      final GetFuture<T> rv =
        new GetFuture<T>(latch, operationTimeout, key, executorService);
      Operation op = createOperationForReplicaGet(key, rv, replicaFuture,
        latch, tc, index, true);

      rv.setOperation(op);
      mconn.enqueueOperation(key, op);

      if (op.isCancelled()) {
        discardedOps++;
        getLogger().debug("Silently discarding replica get for key \""
          + key + "\" (cancelled).");
      } else {
        replicaFuture.addFutureToMonitor(rv);
      }

    }

    if (locator.hasActiveMaster(key)) {
      final CountDownLatch latch = new CountDownLatch(1);
      final GetFuture<T> additionalActiveGet = new GetFuture<T>(latch, operationTimeout, key,
        executorService);
      Operation op = createOperationForReplicaGet(key, additionalActiveGet,
        replicaFuture, latch, tc, 0, false);
      additionalActiveGet.setOperation(op);
      mconn.enqueueOperation(key, op);

      if (op.isCancelled()) {
        discardedOps++;
        getLogger().debug("Silently discarding replica (active) get for key \""
          + key + "\" (cancelled).");
      } else {
        replicaFuture.addFutureToMonitor(additionalActiveGet);
      }
    } else {
      discardedOps++;
    }

    if (discardedOps == actualReplicaIndexes.size() + 1) {
      throw new IllegalStateException("No replica get operation could be "
        + "dispatched because all operations have been cancelled.");
    }

    return replicaFuture;
  }

  /**
   * Helper method to create an operation for the asyncGetFromReplica method.
   *
   * @param replica if the operation should go to a replica node.
   * @return the created {@link Operation}.
   */
  private <T> Operation createOperationForReplicaGet(final String key,
    final GetFuture<T> future, final ReplicaGetFuture<T> replicaFuture,
    final CountDownLatch latch, final Transcoder<T> tc, final int replicaIndex,
    final boolean replica) {
    if (replica) {
      return opFact.replicaGet(key, replicaIndex,
        new ReplicaGetOperation.Callback() {
          private Future<T> val = null;

          @Override
          public void receivedStatus(OperationStatus status) {
            future.set(val, status);
            if (!replicaFuture.isDone() && status.isSuccess()) {
              replicaFuture.setCompletedFuture(future);
            }
          }

          @Override
          public void gotData(String k, int flags, byte[] data) {
            assert key.equals(k) : "Wrong key returned";
            val = tcService.decode(tc, new CachedData(flags, data,
              tc.getMaxSize()));
          }

          @Override
          public void complete() {
            latch.countDown();
          }
        });
    } else {
      return opFact.get(key, new GetOperation.Callback() {
        private Future<T> val = null;

        @Override
        public void receivedStatus(OperationStatus status) {
          future.set(val, status);
          if (!replicaFuture.isDone() && status.isSuccess()) {
            replicaFuture.setCompletedFuture(future);
          }
        }

        @Override
        public void gotData(String k, int flags, byte[] data) {
          assert key.equals(k) : "Wrong key returned";
          val = tcService.decode(tc, new CachedData(flags, data,
            tc.getMaxSize()));
        }

        @Override
        public void complete() {
          latch.countDown();
        }
      });
    }
  }

  @Override
  public <T> CASValue<T> getAndLock(String key, int exp, Transcoder<T> tc) {
    try {
      return asyncGetAndLock(key, exp, tc).get(operationTimeout,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for value", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Exception waiting for value", e);
      }
    } catch (TimeoutException e) {
      throw new OperationTimeoutException("Timeout waiting for value", e);
    }
  }

  @Override
  public CASValue<Object> getAndLock(String key, int exp) {
    return getAndLock(key, exp, transcoder);
  }

  @Override
  public <T> OperationFuture<Boolean> asyncUnlock(final String key,
          long casId, final Transcoder<T> tc) {
    final CountDownLatch latch = new CountDownLatch(1);
    final OperationFuture<Boolean> rv = new OperationFuture<Boolean>(key,
            latch, operationTimeout, executorService);
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

  @Override
  public OperationFuture<Boolean> asyncUnlock(final String key,
          long casId) {
    return asyncUnlock(key, casId, transcoder);
  }

  @Override
  public <T> Boolean unlock(final String key,
          long casId, final Transcoder<T> tc) {
    try {
      return asyncUnlock(key, casId, tc).get(operationTimeout,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for value", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Exception waiting for value", e);
      }
    } catch (TimeoutException e) {
      throw new OperationTimeoutException("Timeout waiting for value", e);
    }

  }

  @Override
  public Boolean unlock(final String key,
          long casId) {
    return unlock(key, casId, transcoder);
  }

  @Override
  public OperationFuture<Boolean> delete(String key,
          PersistTo req, ReplicateTo rep) {

    if(mconn instanceof CouchbaseMemcachedConnection) {
      throw new IllegalArgumentException("Durability options are not supported"
        + " on memcached type buckets.");
    }

    OperationFuture<Boolean> deleteOp = delete(key);
    if(req == PersistTo.ZERO && rep == ReplicateTo.ZERO) {
      return deleteOp;
    }

    return asyncObserveStore(key, deleteOp, req, rep, "Delete", true);
  }

  @Override
  public OperationFuture<Boolean> delete(String key, PersistTo req) {
    return delete(key, req, ReplicateTo.ZERO);
  }

  @Override
  public OperationFuture<Boolean> delete(String key, ReplicateTo req) {
    return delete(key, PersistTo.ZERO, req);
  }

  @Override
  public OperationFuture<Boolean> set(String key,  Object value) {
    return set(key, 0, value);
  }

  @Override
  public OperationFuture<Boolean> set(String key, int exp,
          Object value, PersistTo req, ReplicateTo rep) {

    if(mconn instanceof CouchbaseMemcachedConnection) {
      throw new IllegalArgumentException("Durability options are not supported"
        + " on memcached type buckets.");
    }

    OperationFuture<Boolean> setOp = set(key, exp, value);
    if(req == PersistTo.ZERO && rep == ReplicateTo.ZERO) {
      return setOp;
    }

    return asyncObserveStore(key, setOp, req, rep, "Set", false);
  }

  @Override
  public OperationFuture<Boolean> set(String key, Object value, PersistTo req,
    ReplicateTo rep) {
    return set(key, 0, value, req, rep);
  }

  @Override
  public OperationFuture<Boolean> add(String key, Object value) {
    return add(key, 0, value);
  }

  @Override
  public OperationFuture<Boolean> set(String key, int exp,
    Object value, PersistTo req) {
    return set(key, exp, value, req, ReplicateTo.ZERO);
  }

  @Override
  public OperationFuture<Boolean> set(String key, Object value, PersistTo req) {
    return set(key, 0, value, req);
  }

  @Override
  public OperationFuture<Boolean> set(String key, int exp,
    Object value, ReplicateTo rep) {
    return set(key, exp, value, PersistTo.ZERO, rep);
  }

  @Override
  public OperationFuture<Boolean> set(String key, Object value,
    ReplicateTo rep) {
    return set(key, 0, value, rep);
  }

  @Override
  public OperationFuture<Boolean> add(String key, int exp,
    Object value, PersistTo req, ReplicateTo rep) {

    if(mconn instanceof CouchbaseMemcachedConnection) {
      throw new IllegalArgumentException("Durability options are not supported"
        + " on memcached type buckets.");
    }

    OperationFuture<Boolean> addOp = add(key, exp, value);
    if(req == PersistTo.ZERO && rep == ReplicateTo.ZERO) {
      return addOp;
    }

    return asyncObserveStore(key, addOp, req, rep, "Add", false);
  }

  @Override
  public OperationFuture<Boolean> add(String key, Object value, PersistTo req,
    ReplicateTo rep) {
    return this.add(key, 0, value, req, rep);
  }

  @Override
  public OperationFuture<Boolean> replace(String key, Object value) {
    return replace(key, 0, value);
  }

  @Override
  public OperationFuture<Boolean> add(String key, int exp,
    Object value, PersistTo req) {
    return add(key, exp, value, req, ReplicateTo.ZERO);
  }

  @Override
  public OperationFuture<Boolean> add(String key, Object value, PersistTo req) {
    return add(key, 0, value, req);
  }

  @Override
  public OperationFuture<Boolean> add(String key, int exp,
    Object value, ReplicateTo rep) {
    return add(key, exp, value, PersistTo.ZERO, rep);
  }

  @Override
  public OperationFuture<Boolean> add(String key, Object value,
    ReplicateTo rep) {
    return add(key, 0, value, rep);
  }

  @Override
  public OperationFuture<Boolean> replace(String key, int exp,
    Object value, PersistTo req, ReplicateTo rep) {

    if(mconn instanceof CouchbaseMemcachedConnection) {
      throw new IllegalArgumentException("Durability options are not supported"
        + " on memcached type buckets.");
    }

    OperationFuture<Boolean> replaceOp = replace(key, exp, value);
    if (req == PersistTo.ZERO && rep == ReplicateTo.ZERO) {
      return replaceOp;
    }

    return asyncObserveStore(key, replaceOp, req, rep, "Replace", false);
  }

  /**
   * Helper method to chain asynchronous observe calls.
   *
   * @param key the key of the document.
   * @param original the original mutation future.
   * @param req the persistence setting
   * @param rep the replication setting
   * @param prefix the prefix for log messages
   * @param delete if it is a delete command
   *
   * @return a future containing the observed result.
   */
  private ObserveFuture<Boolean> asyncObserveStore(final String key,
    final OperationFuture<Boolean> original, final PersistTo req,
    final ReplicateTo rep, final String prefix, final boolean delete) {

    final CountDownLatch latch = new CountDownLatch(1);

    final ObserveFuture<Boolean> observeFuture = new ObserveFuture<Boolean>(
      key, latch, cbConnFactory.getObsTimeout(), executorService);

    original.addListener(new OperationCompletionListener() {
      @Override
      public void onComplete(final OperationFuture<?> future) throws Exception {
        boolean replaceStatus = false;

        try {
          replaceStatus = (Boolean) future.get();
          observeFuture.set(replaceStatus, future.getStatus());
          if (future.getCas() != null) {
            observeFuture.setCas(future.getCas());
          }
        } catch (InterruptedException e) {
          observeFuture.set(false, new OperationStatus(false, prefix + " get "
            + "timed out"));
        } catch (ExecutionException e) {
          if(e.getCause() instanceof CancellationException) {
            observeFuture.set(false, new OperationStatus(false, prefix + " get "
              + "cancellation exception "));
          } else {
            observeFuture.set(false, new OperationStatus(false, prefix + " get "
              + "execution exception "));
          }
        }

        if (!replaceStatus) {
          latch.countDown();
          return;
        }

        try {
          observePoll(key, future.getCas(), req, rep, delete);
          observeFuture.set(true, future.getStatus());
        } catch (ObservedException e) {
          observeFuture.set(false, new OperationStatus(false, e.getMessage()));
        } catch (ObservedTimeoutException e) {
          observeFuture.set(false, new OperationStatus(false, e.getMessage()));
        } catch (ObservedModifiedException e) {
          observeFuture.set(false, new OperationStatus(false, e.getMessage()));
        }

        latch.countDown();
      }
    });

    return observeFuture;
  }

  @Override
  public OperationFuture<Boolean> replace(String key, Object value,
    PersistTo req, ReplicateTo rep) {
    return replace(key, 0, value, req, rep);
  }

  @Override
  public OperationFuture<Boolean> replace(String key, int exp,
    Object value, PersistTo req) {
    return replace(key, exp, value, req, ReplicateTo.ZERO);
  }

  @Override
  public OperationFuture<Boolean> replace(String key, Object value,
    PersistTo req) {
    return this.replace(key, 0, value, req);
  }

  @Override
  public OperationFuture<Boolean> replace(String key, int exp,
    Object value, ReplicateTo rep) {
    return replace(key, exp, value, PersistTo.ZERO, rep);
  }

  @Override
  public OperationFuture<Boolean> replace(String key, Object value,
    ReplicateTo rep) {
    return replace(key, 0, value, rep);
  }

  @Override
  public CASResponse cas(String key, long cas,
    Object value, PersistTo req, ReplicateTo rep) {
    return cas(key, cas, 0, value, req, rep);
  }

  @Override
  public CASResponse cas(String key, long cas, int exp,
          Object value, PersistTo req, ReplicateTo rep) {
    CASResponse casr = null;

    try {
      OperationFuture<CASResponse> casOp = asyncCas(key, cas, exp, value, req,
        rep);

      long timeout = cbConnFactory.getObsTimeout();
      if (req == PersistTo.ZERO && rep == ReplicateTo.ZERO) {
        timeout = operationTimeout;
      }

      casr = casOp.get(timeout, TimeUnit.MILLISECONDS);
      return casr;
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for value", e);
    } catch (ExecutionException e) {
      if(e.getCause() instanceof CancellationException) {
        throw (CancellationException) e.getCause();
      } else {
        throw new RuntimeException("Exception waiting for value", e);
      }
    } catch (TimeoutException e) {
      throw new OperationTimeoutException("Timeout waiting for value: ", e);
    }
  }

  @Override
  public CASResponse cas(String key, long cas,
          Object value, PersistTo req) {
    return cas(key, cas, value, req, ReplicateTo.ZERO);
  }

  @Override
  public CASResponse cas(String key, long cas, int exp,
          Object value, PersistTo req) {
    return cas(key, cas, exp, value, req, ReplicateTo.ZERO);
  }

  @Override
  public CASResponse cas(String key, long cas,
          Object value, ReplicateTo rep) {
    return cas(key, cas, value, PersistTo.ZERO, rep);
  }

  @Override
  public CASResponse cas(String key, long cas, int exp,
          Object value, ReplicateTo rep) {
    return cas(key, cas, exp, value, PersistTo.ZERO, rep);
  }

  @Override
  public OperationFuture<CASResponse> asyncCas(String key, long cas,
    Object value, PersistTo req, ReplicateTo rep) {
    return asyncCas(key, cas, 0, value, req, rep);
  }

  @Override
  public OperationFuture<CASResponse> asyncCas(String key, long cas,
    Object value, PersistTo req) {
    return asyncCas(key, cas, value, req, ReplicateTo.ZERO);
  }

  @Override
  public OperationFuture<CASResponse> asyncCas(String key, long cas,
    Object value, ReplicateTo rep) {
    return asyncCas(key, cas, value, PersistTo.ZERO, rep);
  }

  @Override
  public OperationFuture<CASResponse> asyncCas(String key, long cas, int exp,
    Object value, PersistTo req) {
    return asyncCas(key, cas, exp, value, req, ReplicateTo.ZERO);
  }

  @Override
  public OperationFuture<CASResponse> asyncCas(String key, long cas, int exp,
    Object value, ReplicateTo rep) {
    return asyncCas(key, cas, exp, value, PersistTo.ZERO, rep);
  }

  @Override
  public OperationFuture<CASResponse> asyncCas(final String key, long cas,
    int exp, Object value, final PersistTo req, final ReplicateTo rep) {

    if (mconn instanceof CouchbaseMemcachedConnection) {
      throw new IllegalArgumentException("Durability options are not supported"
        + " on memcached type buckets.");
    }

    OperationFuture<CASResponse> casOp = asyncCAS(key, cas, exp, value,
      transcoder);

    final CountDownLatch latch = new CountDownLatch(1);
    final ObserveFuture<CASResponse> observeFuture =
      new ObserveFuture<CASResponse>(key, latch, cbConnFactory.getObsTimeout(),
        executorService);

    casOp.addListener(new OperationCompletionListener() {
      @Override
      public void onComplete(OperationFuture<?> future) throws Exception {
        CASResponse casr;

        try {
          casr = (CASResponse) future.get();
          observeFuture.set(casr, future.getStatus());
          if (future.getCas() != null) {
            observeFuture.setCas(future.getCas());
          }
        } catch (InterruptedException e) {
          casr = CASResponse.EXISTS;
        } catch (ExecutionException e) {
          casr = CASResponse.EXISTS;
        }

        if((casr != CASResponse.OK)
          || (req == PersistTo.ZERO && rep == ReplicateTo.ZERO)) {
          latch.countDown();
          return;
        }

        try {
          observePoll(key, future.getCas(), req, rep, false);
          observeFuture.set(casr, future.getStatus());
        } catch (ObservedException e) {
          observeFuture.set(CASResponse.OBSERVE_ERROR_IN_ARGS,
            new OperationStatus(false, e.getMessage()));
        } catch (ObservedTimeoutException e) {
          observeFuture.set(CASResponse.OBSERVE_TIMEOUT,
            new OperationStatus(false, e.getMessage()));
        } catch (ObservedModifiedException e) {
          observeFuture.set(CASResponse.OBSERVE_MODIFIED,
            new OperationStatus(false, e.getMessage()));
        }

        latch.countDown();
      }
    });

    return observeFuture;
  }

  private Map<MemcachedNode, ObserveResponse> observe(final String key,
    final long cas, final boolean toMaster, final boolean toReplica) {
    Config cfg = ((CouchbaseConnectionFactory) connFactory).getVBucketConfig();
    VBucketNodeLocator locator = (VBucketNodeLocator) mconn.getLocator();

    final int vb = locator.getVBucketIndex(key);
    List<MemcachedNode> bcastNodes = new ArrayList<MemcachedNode>();

    if (toMaster) {
      MemcachedNode primary = locator.getPrimary(key);
      if (primary != null) {
        bcastNodes.add(primary);
      }
    }

    if (toReplica) {
      for (int i = 0; i < cfg.getReplicasCount(); i++) {
        MemcachedNode replica = locator.getReplica(key, i);
        if (replica != null) {
          bcastNodes.add(replica);
        }
      }
    }

    final Map<MemcachedNode, ObserveResponse> response =
      new HashMap<MemcachedNode, ObserveResponse>();

    CountDownLatch blatch = broadcastOp(new BroadcastOpFactory() {
      public Operation newOp(final MemcachedNode n,
                             final CountDownLatch latch) {
        return opFact.observe(key, cas, vb, new ObserveOperation.Callback() {

          @Override
          public void receivedStatus(OperationStatus s) {
          }

          @Override
          public void gotData(String key, long retCas, MemcachedNode node,
            ObserveResponse or) {
            if (cas == retCas) {
              response.put(node, or);
            } else {
              if (or == ObserveResponse.NOT_FOUND_PERSISTED) {
                response.put(node, or);
              } else {
                response.put(node, ObserveResponse.MODIFIED);
              }
            }
          }

          @Override
          public void complete() {
            latch.countDown();
          }
        });
      }
    }, bcastNodes);
    try {
      blatch.await(operationTimeout, TimeUnit.MILLISECONDS);
      return response;
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted waiting for value", e);
    }
  }

  @Override
  public Map<MemcachedNode, ObserveResponse> observe(final String key,
      final long cas) {
    return observe(key, cas, true, true);
  }

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
      if(vconn != null) {
        vconn.shutdown();
      }
    } catch (IOException ex) {
      Logger.getLogger(
         CouchbaseClient.class.getName()).log(Level.SEVERE,
            "Unexpected IOException in shutdown", ex);
      throw new RuntimeException(null, ex);
    }
    return shutdownResult;
  }

  private void checkObserveReplica(String key, int numPersist, int numReplica) {
    Config cfg = ((CouchbaseConnectionFactory) connFactory).getVBucketConfig();
    VBucketNodeLocator locator = (VBucketNodeLocator) mconn.getLocator();

    if(numReplica > 0) {
      int vBucketIndex = locator.getVBucketIndex(key);
      int currentReplicaNum = cfg.getReplica(vBucketIndex, numReplica-1);
      if (currentReplicaNum < 0) {
        throw new ObservedException("Currently, there is no replica node "
          + "available for the given replication index (" + numReplica + ").");
      }
    }

    int replicaCount = Math.min(locator.getAll().size() - 1, cfg.getReplicasCount());
    if (numReplica > replicaCount) {
      throw new ObservedException("Requested replication to " + numReplica
          + " node(s), but only " + replicaCount + " are available.");
    } else if (numPersist > replicaCount + 1) {
      throw new ObservedException("Requested persistence to " + (numPersist + 1)
          + " node(s), but only " + (replicaCount + 1) + " are available.");
    }
  }

  @Override
  public void observePoll(final String key, final long cas, PersistTo persist,
    ReplicateTo replicate, final boolean isDelete) {
    if(persist == null) {
      persist = PersistTo.ZERO;
    }
    if(replicate == null) {
      replicate = ReplicateTo.ZERO;
    }

    final int maxPolls = cbConnFactory.getObsPollMax();
    final long pollInterval = cbConnFactory.getObsPollInterval();
    final VBucketNodeLocator locator = (VBucketNodeLocator) mconn.getLocator();

    final int shouldPersistTo = persist.getValue() > 0 ? persist.getValue() - 1 : 0;
    final int shouldReplicateTo = replicate.getValue();
    final boolean shouldPersistToMaster = persist.getValue() > 0;

    final boolean toMaster = persist.getValue() > 0;
    final boolean toReplica = replicate.getValue() > 0 || persist.getValue() > 1;

    int donePolls = 0;
    int alreadyPersistedTo = 0;
    int alreadyReplicatedTo = 0;
    boolean alreadyPersistedToMaster = false;
    while(shouldReplicateTo > alreadyReplicatedTo
      || shouldPersistTo - 1 > alreadyPersistedTo
      || (!alreadyPersistedToMaster && shouldPersistToMaster)) {
      checkObserveReplica(key, shouldPersistTo, shouldReplicateTo);

      if (++donePolls >= maxPolls) {
        long timeTried = maxPolls * pollInterval;
        throw new ObservedTimeoutException("Observe Timeout - Polled"
          + " Unsuccessfully for at least "
          + TimeUnit.MILLISECONDS.toSeconds(timeTried) + " seconds.");
      }

      Map<MemcachedNode, ObserveResponse> response = observe(key, cas, toMaster,
        toReplica);

      MemcachedNode master = locator.getPrimary(key);
      alreadyPersistedTo = 0;
      alreadyReplicatedTo = 0;
      alreadyPersistedToMaster = false;
      for (Entry<MemcachedNode, ObserveResponse> r : response.entrySet()) {
        MemcachedNode node = r.getKey();
        ObserveResponse observeResponse = r.getValue();

        boolean isMaster = node == master ? true : false;
        if (isMaster && observeResponse == ObserveResponse.MODIFIED) {
          throw new ObservedModifiedException("Key was modified");
        }

        if (isDelete) {
          if (!isMaster && observeResponse == ObserveResponse.NOT_FOUND_NOT_PERSISTED) {
            alreadyReplicatedTo++;
          }
          if (observeResponse == ObserveResponse.NOT_FOUND_PERSISTED) {
            if (isMaster) {
              alreadyPersistedToMaster = true;
            } else {
              alreadyReplicatedTo++;
              alreadyPersistedTo++;
            }
          }
        } else {
          if (!isMaster && observeResponse == ObserveResponse.FOUND_NOT_PERSISTED) {
            alreadyReplicatedTo++;
          }
          if (observeResponse == ObserveResponse.FOUND_PERSISTED) {
            if (isMaster) {
              alreadyPersistedToMaster = true;
            } else {
              alreadyReplicatedTo++;
              alreadyPersistedTo++;
            }
          }
        }
      }
      try {
        if (shouldReplicateTo > alreadyReplicatedTo
          || shouldPersistTo - 1 > alreadyPersistedTo
          || (!alreadyPersistedToMaster && shouldPersistToMaster)) {
          Thread.sleep(pollInterval);
        }
      } catch (InterruptedException e) {
        getLogger().error("Interrupted while in observe loop.", e);
        throw new ObservedException("Observe was Interrupted ");
      }
    }
  }

  @Override
  public OperationFuture<Map<String, String>> getKeyStats(String key) {
    final CountDownLatch latch = new CountDownLatch(1);
    final OperationFuture<Map<String, String>> rv =
        new OperationFuture<Map<String, String>>(key, latch, operationTimeout,
          executorService);
    Operation op = opFact.keyStats(key, new StatsOperation.Callback() {
      private final Map<String, String> stats = new HashMap<String, String>();
      public void gotStat(String name, String val) {
        stats.put(name, val);
      }

      public void receivedStatus(OperationStatus status) {
        rv.set(stats, status);
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
   * Flush all data from the bucket immediately.
   *
   * Note that if the bucket is of type memcached the flush will be nearly
   * instantaneous.  Running a flush() on a Couchbase bucket can take quite
   * a while, depending on the amount of data and the load on the system.
   *
   * @return a OperationFuture indicating the result of the flush.
   */
  @Override
  public OperationFuture<Boolean> flush() {
    return flush(-1);
  }

  /**
   * Flush all caches from all servers with a delay of application.
   *
   * @param delay the period of time to delay, in seconds
   * @return whether or not the operation was accepted
   */
  @Override
  public OperationFuture<Boolean> flush(final int delay) {
    if(connectionShutDown()) {
      throw new IllegalStateException("Flush can not be used after shutdown.");
    }

    final CountDownLatch latch = new CountDownLatch(1);
    final FlushRunner flushRunner = new FlushRunner(latch);

    final OperationFuture<Boolean> rv =
      new OperationFuture<Boolean>("", latch, operationTimeout,
        executorService) {
        private final CouchbaseConnectionFactory factory =
          (CouchbaseConnectionFactory) connFactory;

        @Override
        public boolean cancel() {
          throw new UnsupportedOperationException("Flush cannot be"
            + " canceled");
        }

        @Override
        public boolean isDone() {
          return flushRunner.status();
        }

        @Override
        public Boolean get(long duration, TimeUnit units) throws
          InterruptedException, TimeoutException,
          ExecutionException {
          if (!latch.await(duration, units)) {
            throw new TimeoutException("Flush not completed within"
              + " timeout.");
          }

          return flushRunner.status();
        }

        @Override
        public Boolean get() throws InterruptedException,
          ExecutionException {
          try {
            return get(factory.getViewTimeout(), TimeUnit.MILLISECONDS);
          } catch (TimeoutException e) {
            throw new RuntimeException("Timed out waiting for operation",
              e);
          }
        }

        @Override
        public Long getCas() {
          throw new UnsupportedOperationException("Flush has no CAS"
            + " value.");
        }

        @Override
        public String getKey() {
          throw new UnsupportedOperationException("Flush has no"
            + " associated key.");
        }

        @Override
        public OperationStatus getStatus() {
          throw new UnsupportedOperationException("Flush has no"
            + " OperationStatus.");
        }

        @Override
        public boolean isCancelled() {
          throw new UnsupportedOperationException("Flush cannot be"
            + " canceled.");
        }
      };

    Thread flusher = new Thread(flushRunner, "Temporary Flusher");
    flusher.setDaemon(true);
    flusher.start();

    return rv;
  }

  /**
   * Flush the current bucket.
   */
  private boolean flushBucket() {
    FlushResponse res = cbConnFactory.getClusterManager().flushBucket(
      cbConnFactory.getBucketName());
    return res.equals(FlushResponse.OK);
  }

  // This is a bit of a hack since we don't have async http on this
  // particular interface, but it conforms to the specification
  private class FlushRunner implements Runnable {

    private final CountDownLatch flatch;
    private Boolean flushStatus = false;

    public FlushRunner(CountDownLatch latch) {
      flatch = latch;
    }

    public void run() {
      flushStatus = flushBucket();
      flatch.countDown();
    }

    private boolean status() {
      return flushStatus.booleanValue();
    }
  }

  protected boolean connectionShutDown() {
    if (mconn instanceof CouchbaseConnection) {
      return ((CouchbaseConnection)mconn).isShutDown();
    } else if (mconn instanceof CouchbaseMemcachedConnection) {
      return ((CouchbaseMemcachedConnection)mconn).isShutDown();
    } else {
      throw new IllegalStateException("Unknown connection type: "
        + mconn.getClass().getCanonicalName());
    }
  }

}
