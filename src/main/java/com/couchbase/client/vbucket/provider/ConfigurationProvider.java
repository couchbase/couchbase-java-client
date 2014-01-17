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

import com.couchbase.client.vbucket.ConfigurationException;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.config.Bucket;

/**
 * Defines common methods for a {@link ConfigurationProvider}.
 *
 * One {@link ConfigurationProvider} is needed per bucket.
 */
public interface ConfigurationProvider {

  /**
   * Initiate the config fetching process, eventually returning a valid
   * configuration.
   *
   * @return a valid configuration.
   * @throws ConfigurationException if no valid configuration could be loaded.
   */
  Bucket bootstrap() throws ConfigurationException;

  /**
   * Returns the current {@link Bucket} configuration.
   *
   * @return the current bucket configuration.
   */
  Bucket getConfig();

  /**
   * Explictly set the current {@link Bucket} configuration.
   */
  void setConfig(Bucket config);

  /**
   * Replace the current config with a raw JSON string configuration.
   *
   * @param config the raw string configuration.
   */
  void setConfig(String config);

  /**
   * Can be used as a hint for the {@link ConfigurationProvider} to signal that
   * his current configuration may be outdated.
   */
  void signalOutdated();

  /**
   * Shut down the {@link ConfigurationProvider}.
   */
  void shutdown();

  /**
   * Retrieves a default bucket name i.e. 'default'.
   *
   * @return the anonymous bucket's name i.e. 'default'
   */
  String getAnonymousAuthBucket();

  /**
   * Subscribes for configuration updates.
   *
   * @param rec reconfigurable that will receive updates
   * @throws ConfigurationException
   */
  void subscribe(Reconfigurable rec);

  /**
   * Unsubscribe from updates on the connected bucket.
   *
   * @param rec reconfigurable
   */
  void unsubscribe(Reconfigurable rec);

}
