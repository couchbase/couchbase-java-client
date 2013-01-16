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

import com.couchbase.client.vbucket.ConfigurationProvider;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.config.Bucket;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.naming.ConfigurationException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionObserver;

/**
 * A TapConnectionProvider for Couchbase Server.
 */
public class TapConnectionProvider
  extends net.spy.memcached.TapConnectionProvider
  implements Reconfigurable {

  private final CouchbaseConnectionFactory cf;
  private final ConfigurationProvider cp;

  /**
   * Get a tap connection based on the REST response from a Couchbase server.
   *
   * @param baseList A list of URI's to use for getting cluster information.
   * @param bucketName The name of the bucket to connect to.
   * @param pwd The password for the bucket.
   * @throws IOException
   * @throws ConfigurationException
   */
  public TapConnectionProvider(final List<URI> baseList,
      final String bucketName, final String pwd)
    throws IOException, ConfigurationException {
    this(new CouchbaseConnectionFactory(baseList, bucketName, pwd));
  }

  /**
   * Get a tap connection based on the REST response from a Couchbase server.
   *
   * @param cf A connection factory to create the tap stream with
   * @throws IOException
   * @throws ConfigurationException
   */
  public TapConnectionProvider(CouchbaseConnectionFactory cf)
    throws IOException, ConfigurationException{
    super(cf, AddrUtil.getAddresses(cf.getVBucketConfig().getServers()));
    this.cf=cf;
    cp = cf.getConfigurationProvider();
    cp.subscribe(cf.getBucketName(), this);
  }

  /**
   * Remove a connection observer.
   *
   * @param obs the ConnectionObserver you wish to add
   * @return true if the observer existed, but no longer does
   */
  public boolean removeObserver(ConnectionObserver obs) {
    return conn.removeObserver(obs);
  }

  public void reconfigure(Bucket bucket) {
    ((CouchbaseConnection)conn).reconfigure(bucket);
  }

  public void shutdown() {
    super.shutdown();
    cf.getConfigurationProvider().shutdown();
  }
}
