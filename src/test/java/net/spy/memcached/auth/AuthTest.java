/**
 * Copyright (C) 2009-2014 Couchbase, Inc.
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

package net.spy.memcached.auth;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.TestConfig;

import org.junit.Before;
import org.junit.Test;

import com.couchbase.client.BucketTool;
import com.couchbase.client.BucketTool.FunctionCallback;
import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.clustermanager.BucketType;


/**
 * Verifies the correct auth mechanism corresponding to the correct server versions.
 */
public class AuthTest {

  /**
   * Client instance
   */
  private static CouchbaseClient saslClient;

  /**
   * Connection factory
   */
  CouchbaseConnectionFactory cf;

  /**
   * Server URI
   */
  private final List<URI> uris = Arrays.asList(URI.create("http://"
        + TestConfig.IPV4_ADDR + ":8091/pools"));

  /**
   * Initializes the client object.
   * @throws Exception
   */
  @Before
  public void initClient() throws Exception {
    BucketTool bucketTool = new BucketTool();
    bucketTool.deleteAllBuckets();
    bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 1, true);
    bucketTool.createSaslBucket("SaslBucket", BucketType.COUCHBASE, 256, 1, true);

    BucketTool.FunctionCallback callback = new FunctionCallback() {
      @Override
      public void callback() throws Exception {
        cf = new CouchbaseConnectionFactory(uris, "SaslBucket", "SaslBucket");
        initClient(cf);
      }

      @Override
      public String success(long elapsedTime) {
        return "Client Initialization took " + elapsedTime + "ms";
      }
    };
    bucketTool.poll(callback);
    bucketTool.waitForWarmup(saslClient);
  }

  /**
   * Creating a new instance of the couchbase client object.
   */
  protected void initClient(CouchbaseConnectionFactory cf) throws Exception {
    saslClient = new CouchbaseClient(cf);
  }

  /**
   * Test for Auth Mechanisms.
   *
   * @pre Prepare a new instance of client and extract
   * the versions of the server from it and then check
   * for the loaded Auth mechanism to be SASL Cram MD5
   * for version 2.2+
   *
   * @post Asserts pass if the auth mechanism is
   * correct for the respective server version.
   * @throws Exception
   */
  @Test
  public void testAuthMech() throws Exception {
    double serverVersion = getServerVersion();
    Object[] authMechanism = saslClient.listSaslMechanisms().toArray();
    if(serverVersion>=2.2){
      assertEquals("PLAIN", authMechanism[0]);
      assertEquals("CRAM-MD5", authMechanism[1]);
      assertEquals(2, authMechanism.length);
    }else{
      assertEquals("PLAIN", authMechanism[0]);
      assertEquals(1, authMechanism.length);
    }
  }

  /**
   * Test for Auth via AuthThread.
   *
   * @pre Prepare a new instance of client and extract
   * the versions of the server from it and then check
   * for the right Auth mechanism to be SASL Cram MD5
   * for version 2.2+
   *
   * @post Asserts pass if the auth mechanism is
   * correct for the respective server version.
   * @throws Exception
   */
  @Test
  public void testAuthThread() throws Exception {
    OperationFactory opFact = cf.getOperationFactory();
    assert opFact != null : "Connection factory failed to make op factory";
    MemcachedConnection mconn =
      cf.createConnection(AddrUtil.getAddresses(cf.getVBucketConfig().getServers()));
    assert mconn != null : "Connection factory failed to make a connection";
    AuthDescriptor authDescriptor = cf.getAuthDescriptor();
    AuthThreadMonitor monitor = new AuthThreadMonitor();
    List<MemcachedNode> connectedNodes = new ArrayList<MemcachedNode>(
      mconn.getLocator().getAll());
    for (MemcachedNode connectedNode : connectedNodes) {
      monitor.authConnection(mconn, opFact,
        authDescriptor, connectedNode);
    }
    AuthThread at = (AuthThread)monitor.getNodeMap().values().toArray()[0];
    String[] saslMechs = at.listSupportedSASLMechanisms(new AtomicBoolean());
    double serverVersion = getServerVersion();
    if(serverVersion>=2.2){
      assertEquals("CRAM-MD5", saslMechs[0]);
      assertEquals("PLAIN", saslMechs[1]);
      assertEquals(2, saslMechs.length);
    }else{
      assertEquals("PLAIN", saslMechs[0]);
      assertEquals(1, saslMechs.length);
    }
  }

  /**
   * Extracts server version from the client.
   * @return
   */
  private double getServerVersion() {
    String versionString =
      (saslClient.getVersions().toString().split("=")[1]).split("_")[0];
    double serverVersion =
      Double.valueOf(versionString.substring(0,versionString.lastIndexOf('.')));
    return serverVersion;
  }
}