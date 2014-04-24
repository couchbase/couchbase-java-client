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

  /**
   * Initialize static the properties.
   */
  static {
    seedNode = System.getProperty("seedNode", "127.0.0.1");
    bucket = System.getProperty("bucket", "default");
    password = System.getProperty("password", "");
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
}
