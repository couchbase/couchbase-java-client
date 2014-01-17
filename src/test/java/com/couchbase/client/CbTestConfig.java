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

/**
 * A typical configuration for the test suite.
 */
public final class CbTestConfig {

  /**
   * Property name of the cluster admin password.
   */
  public static final String CLUSTER_PASS_PROP = "cluster.password";

  /**
   * Property name of the cluster admin name.
   */
  public static final String CLUSTER_ADMINNAME_PROP = "cluster.adminname";

  /**
   * Password of the cluster administrator.
   */
  public static final String CLUSTER_PASS =
    System.getProperty(CLUSTER_PASS_PROP, "password");

  /**
   * Username of the cluster administrator.
   */
  public static final String CLUSTER_ADMINNAME =
    System.getProperty(CLUSTER_ADMINNAME_PROP, "Administrator");

  private CbTestConfig() {
    // Empty
  }


  /**
   * Simple helper class to detect and compare the cluster node version.
   */
  public static class Version {

    private final int major;
    private final int minor;
    private final int bugfix;

    public Version(String raw) {
      String[] tokens = raw.replaceAll("_.*$", "").split("\\.");
      major = Integer.parseInt(tokens[0]);
      minor = Integer.parseInt(tokens[1]);
      bugfix = Integer.parseInt(tokens[2]);
    }

    public boolean greaterOrEqualThan(int major, int minor, int bugfix) {
      return this.major >= major
        && this.minor >= minor
        && this.bugfix >= bugfix;
    }

    public boolean isCarrierConfigAware() {
      return greaterOrEqualThan(2, 5, 0);
    }
  }

}
