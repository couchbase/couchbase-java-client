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

package com.couchbase.client.vbucket.config;

/**
 * A VBucket.
 */
public class VBucket {

  public static final short MAX_REPLICAS = 3;
  public static final int MAX_BUCKETS = 65536;
  public static final short REPLICA_NOT_USED = -1;

  private volatile short master;
  private final short replica1;
  private final short replica2;
  private final short replica3;

  public VBucket(final short m) {
    this(m, REPLICA_NOT_USED, REPLICA_NOT_USED, REPLICA_NOT_USED);
  }

  public VBucket(final short m, final short r1) {
    this(m, r1, REPLICA_NOT_USED, REPLICA_NOT_USED);
  }

  public VBucket(final short m, final short r1, final short r2) {
    this(m, r1, r2, REPLICA_NOT_USED);
  }

  public VBucket(final short m, final short r1, final short r2, final short r3) {
    master = m;
    replica1 = r1;
    replica2 = r2;
    replica3 = r3;
  }

  public int getMaster() {
    return master;
  }

  public int getReplica(int n) {
    switch (n) {
      case 0: return replica1;
      case 1: return replica2;
      case 2: return replica3;
      default:
        throw new IllegalArgumentException("No more than " + MAX_REPLICAS
          + " replicas allowed.");
    }
  }

  public void setMaster(short rv) {
    master = rv;
  }

  @Override
  public String toString() {
    return "m: " + master + ", r: " + "[" + replica1 + ", " + replica2 + ", "
      + replica3 + "]";
  }
}
