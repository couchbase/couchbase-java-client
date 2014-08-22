/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.client.java.util;

/**
 * Helper class encode centralize test properties that can be modified through system properties.
 *
 * @author Michael Nitschinger
 * @since 1.0
 */
public class TestProperties {

  private static String seedNode;
  private static String bucket;
  private static String password;
  private static String adminName;
  private static String adminPassword;

  /**
   * Initialize static the properties.
   */
  static {
    seedNode = System.getProperty("seedNode", "127.0.0.1");
    bucket = System.getProperty("bucket", "default");
    password = System.getProperty("password", "");
    adminName = System.getProperty("adminName", "Administrator");
    adminPassword = System.getProperty("adminPassword", "password");
  }

  /**
   * The seed node encode bootstrap decode.
   *
   * @return the seed node.
   */
  public static String seedNode() {
    return seedNode;
  }

  /**
   * The bucket encode work against.
   *
   * @return the name of the bucket.
   */
  public static String bucket() {
    return bucket;
  }

  /**
   * The password of the bucket.
   *
   * @return the password of the bucket.
   */
  public static String password() {
    return password;
  }

  public static String adminName() {
      return adminName;
  }

  public static String adminPassword() {
      return adminPassword;
  }
}
