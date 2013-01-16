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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.compat.CloseUtil;

/**
 * The CouchbaseProperties class abstracts system properties and those set
 * via a file exposed through a unified class.
 *
 * The main purpose of this class is to centralize property management which
 * can be called throughout the stack. If a filename is set and the properties
 * file can be loaded, the information is used. All the time, properties set
 * in the code override the file-given ones.
 *
 * The default namespace is "cbclient", which means that all properties need
 * to adhere to the "cbclient.<property-name>" syntax. The getProperty method
 * also allows you to pass a boolean param to control this behavior (ignore
 * the namespace at all).
 */
final class CouchbaseProperties {

  private static Properties fileProperties = new Properties();

  private static final Logger LOGGER = Logger.getLogger(
    CouchbaseProperties.class.getName());

  private CouchbaseProperties() {}

  /**
   * The default namespace of the properties.
   */
  private static String namespace = "cbclient";

  /**
   * Set the filename of the properties file and load it (if possible).
   *
   * @param filename the filename of the properties file.
   */
  public static void setPropertyFile(String filename) {
    FileInputStream fs = null;
    try {
      if(filename == null) {
        throw new IllegalArgumentException(
          "Given property filename is null.");
      }

      URL url =  ClassLoader.getSystemResource(filename);
      if (url != null) {
        String clFilename = url.getFile();
        File propFile = new File(clFilename);
        fs = new FileInputStream(propFile);
        fileProperties.load(fs);
      } else {
        throw new IOException("File not found with system classloader.");
      }
      LOGGER.log(Level.INFO, "Successfully loaded properties file \"{0}\".",
        filename);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Could not load properties file \"{0}\" "
        + "because: {1}", new Object[]{filename, e.getMessage()});
    } finally {
      if (fs != null) {
        CloseUtil.close(fs);
      }
    }
  }

  /**
   * Returns the property for the given name (or given default if not set).
   *
   * If a System property is found, it is returned. Otherwise, if a file
   * is set and it contains the property, it is returned. If no property
   * is found, the default is returned.
   *
   * @param name the name of the property.
   * @param def the default value to return.
   * @param ignore ignore the property namespace.
   * @return returns the property or default if not set.
   */
  public static String getProperty(String name, String def, boolean ignore) {
    if(!ignore) {
      name = namespace + "." + name;
    }

    String systemProperty = System.getProperty(name, null);
    if(systemProperty != null) {
      return systemProperty;
    }

    if(fileProperties != null
      && fileProperties.getProperty(name, null) != null) {
      return fileProperties.getProperty(name, null);
    }

    return def;
  }

  /**
   * Returns the property for the given name (or null if not set).
   *
   * @param name the name of the property.
   * @param ignore ignore the property namespace.
   * @return returns the property or null if not set.
   */
  public static String getProperty(String name, boolean ignore) {
    return getProperty(name, null, ignore);
  }

  /**
   * Returns the property for the given name (or null if not set).
   *
   * @param name the name of the property.
   * @return returns the property or null if not set.
   */
  public static String getProperty(String name) {
    return getProperty(name, null, false);
  }

  /**
   * Returns the property for the given name (or given default if not set).
   *
   * @param name the name of the property.
   * @param def the default value to return.
   * @return returns the property or the default if not set.
   */
  public static String getProperty(String name, String def) {
    return getProperty(name, def, false);
  }

  /**
   * Returns the namespace to be used for the properties.
   *
   * @return the namespace.
   */
  public static String getNamespace() {
    return namespace;
  }

  /**
   * Replaces the default (or old) namespace with this new one.
   *
   * @param ns the namespace to be used.
   */
  public static void setNamespace(String ns) {
    namespace = ns;
  }

  /**
   * Checks if there are any properties set through a properties file.
   *
   * @return true if there are file properties set.
   */
  static boolean hasFileProperties() {
    return !fileProperties.isEmpty();
  }

  /**
   * Reset the file properties to an empty state.
   */
  static void resetFileProperties() {
    fileProperties = new Properties();
  }

}
