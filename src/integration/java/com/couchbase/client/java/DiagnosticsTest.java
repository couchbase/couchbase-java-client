/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java;

import com.couchbase.client.core.message.internal.DiagnosticsReport;
import com.couchbase.client.core.message.internal.EndpointHealth;
import com.couchbase.client.core.message.internal.PingReport;
import com.couchbase.client.core.message.internal.PingServiceHealth;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.core.state.LifecycleState;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Verifies the basic functionality of both the diagnostics and ping commands.
 *
 * @author Michael Nitschinger
 * @since 2.5.5
 */
public class DiagnosticsTest {

  private static CouchbaseTestContext ctx;

  @BeforeClass
  public static void connect() {
    ctx = CouchbaseTestContext.builder()
      .bucketQuota(256)
      .bucketType(BucketType.COUCHBASE)
      .build();
  }

  @AfterClass
  public static void disconnect() {
    ctx.disconnect();
  }

  @Test
  public void shouldRunDiagnostics() {
    DiagnosticsReport sh = ctx.cluster().diagnostics("myReportId");

    assertNotNull(sh);

    List<EndpointHealth> eph = sh.endpoints();
    assertFalse(eph.isEmpty());

    for (EndpointHealth eh : eph) {
      assertNotNull(eh.type());
      assertEquals(LifecycleState.CONNECTED, eh.state());
      assertNotNull(eh.local());
      assertNotNull(eh.remote());
      assertTrue(eh.lastActivity() > 0);
      assertTrue(eh.id().startsWith("0x"));
    }

    assertNotNull(sh.sdk());
    assertEquals(sh.sdk(), ctx.env().userAgent());
    assertEquals("myReportId", sh.id());

    assertNotNull(sh.exportToJson());
  }

  @Test
  public void shouldAllowFromCluster() {
    Observable<DiagnosticsReport> report = ctx.cluster().async().diagnostics();
    DiagnosticsReport extracted = report.toBlocking().single();
    assertNotNull(extracted);
    assertNotNull(extracted.exportToJson());
  }

  @Test
  public void shouldRunPing() {
    PingReport pr = ctx.bucket().ping("myReportId");

    assertNotNull(pr.sdk());
    assertEquals(pr.sdk(), ctx.env().userAgent());
    assertEquals("myReportId", pr.id());
    assertTrue(pr.version() > 0);
    if (!CouchbaseTestContext.isMockEnabled()) {
      // current mock version does not include rev
      assertTrue(pr.configRev() > 0);
    }

    assertNotNull(pr.exportToJson());

    for (PingServiceHealth ph : pr.services()) {
      assertEquals(PingServiceHealth.PingState.OK, ph.state());
      assertTrue(ph.latency() > 0);
      assertNotNull(ph.id());
      assertNotNull(ph.remote());
      assertNotNull(ph.local());
      if (ph.type() == ServiceType.BINARY) {
        assertNotNull(ph.scope());
      } else {
        assertNull(ph.scope());
      }
    }
  }

}
