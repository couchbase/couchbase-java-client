/**
 * Copyright (C) 2012 Couchbase, Inc.
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

package com.couchbase.client.vbucket;

import com.couchbase.client.vbucket.config.Config;
import java.util.ArrayList;
import java.util.List;
import net.spy.memcached.MemcachedNode;

/**
 * A NodeLocator designed to inject responses during rebalance.
 */
public class NMVInjectingVBucketNodeLocator extends VBucketNodeLocator {

  ArrayList<String> bogused; // chosen for size over speed, only 20 ops needed

  public NMVInjectingVBucketNodeLocator(List<MemcachedNode> nodes,
    Config jsonConfig) {
    super(nodes, jsonConfig);
    bogused = new ArrayList<String>();
  }

  /**
   * Returns a vbucket index for the given key, but returns a bogus vbucket
   * number if the key begins with "bogus".
   *
   * @param key the key
   * @return vbucket index
   */
  @Override
  public int getVBucketIndex(String key) {

    int vBucketIndex = super.getVBucketIndex(key);

    if (bogused.contains(key)) {
      System.err.println("Already bogused once: " + key);
      return vBucketIndex;
    }

    if (key.startsWith("bogus")) {
      vBucketIndex = 1025; // make the vbucket index bogus first time through
      System.err.println("BOGUS!!!!! " + key );
      bogused.add(key);
    }

    return vBucketIndex;
  }

}
