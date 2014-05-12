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

package com.couchbase.client.vbucket.provider;

import com.couchbase.client.CbTestConfig;
import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.vbucket.ConfigurationException;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.config.Bucket;
import net.spy.memcached.TestConfig;
import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.MockUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BucketConfigurationProviderTest {

  private List<URI> seedNodes;
  private String bucket;
  private String password;
  private String seedNode;

  private static final Logger LOGGER =
    LoggerFactory.getLogger(BucketConfigurationProviderTest.class);

  private static boolean isCCCPAware;

  private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
    + ":8091/pools";

  /**
   * Set a flag to see if the target cluster is CCCP ready and it makes sense
   * to run the tests.
   */
  @BeforeClass
  public static void checkCCCPAwareness() throws Exception {
    CouchbaseClient client = new CouchbaseClient(
      Arrays.asList(new URI(SERVER_URI)),
      "default",
      ""
    );

    ArrayList<String> versions = new ArrayList<String>(
      client.getVersions().values());
    if (versions.size() > 0) {
      CbTestConfig.Version version = new CbTestConfig.Version(versions.get(0));
      isCCCPAware = version.isCarrierConfigAware();
    }

    client.shutdown();
  }

  @Before
  public void setup() throws Exception {
    seedNode = TestConfig.IPV4_ADDR;

    seedNodes = Arrays.asList(new URI("http://" + seedNode
     + ":8091/pools"));
    bucket = "default";
    password = "";
  }

  @Before
  public void resetProperties() {
    System.clearProperty("cbclient.disableHttpBootstrap");
    System.clearProperty("cbclient.disableCarrierBootstrap");
  }

  @Test
  public void shouldBootstrapBothBinaryAndHttp() throws Exception {
    if (!isCCCPAware) {
      LOGGER.info("Skipping Test because cluster is not CCCP aware.");
      return;
    }

    BucketConfigurationProvider provider = new BucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password)
    );

    assertTrue(provider.bootstrapBinary());
    Bucket binaryConfig = provider.getConfig();

    assertTrue(provider.bootstrapHttp());
    Bucket httpConfig = provider.getConfig();

    assertEquals(binaryConfig.getConfig().getServersCount(),
      httpConfig.getConfig().getServersCount());
  }

  @Test(expected = ConfigurationException.class)
  public void shouldThrowExceptionIfNoConfigFound() throws Exception {
    BucketConfigurationProvider provider = new FailingBucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password),
      true,
      true
    );

    provider.bootstrap();
  }

  @Test
  public void shouldReloadHttpConfigOnSignalOutdated() throws Exception {
    List<URI> seedNodes = Arrays.asList(
      new URI("http://foobar:8091/pools"),
      new URI("http://" + seedNode + ":8091/pools")
    );

    BucketConfigurationProvider provider = new FailingBucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password),
      true,
      false
    );

    provider.bootstrap();
    provider.signalOutdated();
  }

  @Test
  public void shouldReloadBinaryConfigOnSignalOutdated() throws Exception {
    if (!isCCCPAware) {
      LOGGER.info("Skipping Test because cluster is not CCCP aware.");
      return;
    }

    List<URI> seedNodes = Arrays.asList(
      new URI("http://foobar:8091/pools"),
      new URI("http://" + seedNode + ":8091/pools")
    );

    BucketConfigurationProvider provider = new FailingBucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password),
      false,
      true
    );

    provider.bootstrap();
    provider.signalOutdated();
  }

  @Test
  public void shouldBootstrapFromCouchbaseClient() throws Exception {
    CouchbaseClient c = new CouchbaseClient(seedNodes, bucket, password);

    assertTrue(c.set("foo", "bar").get());
    assertEquals("bar", c.get("foo"));
  }

  @Test
  public void shouldIgnoreInvalidNodeOnBootstrap() throws Exception {
    BucketConfigurationProvider provider = new FailingBucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password),
      false,
      false
    );

    provider.bootstrap();
  }

  @Test
  public void shouldSkipBinaryOnManualDisable() throws Exception {
    if (!isCCCPAware) {
      LOGGER.info("Skipping Test because cluster is not CCCP aware.");
      return;
    }
    System.setProperty("cbclient.disableCarrierBootstrap", "true");

    BucketConfigurationProvider provider = new BucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password)
    );

    assertFalse(provider.bootstrapBinary());
  }

  @Test
  public void shouldSkipHttpOnManualDisable() throws Exception {
    System.setProperty("cbclient.disableHttpBootstrap", "true");

    BucketConfigurationProvider provider = new BucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password)
    );

    assertFalse(provider.bootstrapHttp());
  }

  @Test
  public void shouldIgnoreOutdatedOrCurrentConfig() throws Exception {
    BucketConfigurationProvider provider = new BucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password)
    );

    final AtomicInteger configArrived = new AtomicInteger();
    provider.subscribe(new Reconfigurable() {
      @Override
      public void reconfigure(Bucket bucket) {
        if (new MockUtil().isMock(bucket)) {
          configArrived.incrementAndGet();
        }
      }
    });

    Bucket storedBucket = provider.getConfig();
    assertTrue(storedBucket.getRevision() > 0);


    Bucket oldBucket = mock(Bucket.class);
    when(oldBucket.getName()).thenReturn(bucket);
    when(oldBucket.getRevision()).thenReturn(storedBucket.getRevision() - 1);
    Bucket currentBucket = mock(Bucket.class);
    when(currentBucket.getName()).thenReturn(bucket);
    when(currentBucket.getRevision()).thenReturn(storedBucket.getRevision());
    Bucket newBucket = mock(Bucket.class);
    when(newBucket.getName()).thenReturn(bucket);
    when(newBucket.getRevision()).thenReturn(storedBucket.getRevision() + 1);
    when(newBucket.getConfig()).thenReturn(storedBucket.getConfig());

    provider.setConfig(oldBucket);
    provider.setConfig(currentBucket);
    provider.setConfig(newBucket);

    assertEquals(1, configArrived.get());
  }

  @Test
  public void shouldUseConfigIfRevNotSet() throws Exception {
    BucketConfigurationProvider provider = new BucketConfigurationProvider(
      seedNodes,
      bucket,
      password,
      new CouchbaseConnectionFactory(seedNodes, bucket, password)
    );

    final AtomicInteger configArrived = new AtomicInteger();
    provider.subscribe(new Reconfigurable() {
      @Override
      public void reconfigure(Bucket bucket) {
        if (new MockUtil().isMock(bucket)) {
          configArrived.incrementAndGet();
        }
      }
    });

    Bucket storedBucket = provider.getConfig();
    assertTrue(storedBucket.getRevision() > 0);

    Bucket newBucket = mock(Bucket.class);
    when(newBucket.getName()).thenReturn(bucket);
    when(newBucket.getRevision()).thenReturn(-1L);
    when(newBucket.getConfig()).thenReturn(storedBucket.getConfig());

    provider.setConfig(newBucket);

    assertEquals(1, configArrived.get());
  }

  /**
   * A provider that can fail either one or both of the bootstrap mechanisms.
   */
  static class FailingBucketConfigurationProvider
    extends BucketConfigurationProvider {

    private final boolean failBinary;
    private final boolean failHttp;
    FailingBucketConfigurationProvider(List<URI> seedNodes, String bucket,
      String password, CouchbaseConnectionFactory connectionFactory,
      boolean failBinary, boolean failHttp) {
      super(seedNodes, bucket, password, connectionFactory);
      this.failBinary = failBinary;
      this.failHttp = failHttp;
    }

    @Override
    boolean bootstrapBinary() {
      if (failBinary) {
        return false;
      } else {
        return super.bootstrapBinary();
      }
    }

    @Override
    boolean bootstrapHttp() {
      if (failHttp) {
        return false;
      } else {
        return super.bootstrapHttp();
      }
    }
  }



}
