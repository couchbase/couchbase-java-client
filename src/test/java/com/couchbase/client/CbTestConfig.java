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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*");

    private final int major;
    private final int minor;
    private final int bugfix;

    public Version(String raw) {
      Matcher matcher = VERSION_PATTERN.matcher(raw);
      if (matcher.matches() && matcher.groupCount() == 3) {
        major = Integer.parseInt(matcher.group(1));
        minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        bugfix = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
      } else {
        throw new IllegalArgumentException(
            "Expected a version string starting with X[.Y[.Z]], was " + raw);
      }
    }

    public boolean greaterOrEqualThan(int major, int minor, int bugfix) {
      if (this.major > major) {
        return true;
      } else if (this.major == major) {
        if (this.minor > minor) {
          return true;
        } else if (this.minor == minor) {
          if (this.bugfix >= bugfix) {
            return true;
          }
        }
      }
      return false;
    }

    public boolean isCarrierConfigAware() {
      return greaterOrEqualThan(2, 5, 0);
    }

    public boolean isOldSpatialAware() {
      return !greaterOrEqualThan(3, 0, 0);
    }
  }

}
