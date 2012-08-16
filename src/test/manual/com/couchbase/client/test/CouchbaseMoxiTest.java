/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2012 Couchbase, Inc.
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

package com.couchbase.client.test;

import com.couchbase.client.CouchbaseClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import net.spy.memcached.MemcachedClient;

/**
 * Verify that couchbase client for memcached and moxi
 * behavior is identical and the key/value pairs can be
 * retrieved interchangeably.
 *
 * Essentially, this test sets a few values using moxi and
 * gets the same value without the moxi and vice versa.
 *
 * This test expects a two (or more) node cluster running
 * Couchbase server with the buckets being memcached buckets.
 * Specify the IP address of one of the nodes as below.
 *
 * CouchbaseMoxiTest server_address
 */

public final class CouchbaseMoxiTest {

  private CouchbaseMoxiTest() {
    // Empty
  }
  static final long START = 1;
  static final long MAX_VALUE = 10000;
  static final int TTL = 120;

  public static void main(String[] args) {
    MemcachedClient c = null;
    CouchbaseClient cbc = null;
    Boolean fail = false;

    List<URI> uris = new LinkedList<URI>();

    if (args.length != 1) {
      System.err.println("usage: server_address");
      System.exit(1);
    }

    try {
      c = new MemcachedClient(new InetSocketAddress(args[0], 11211));
      URI base = new URI(String.format("http://%s:8091/pools", args[0]));
      uris.add(base);
      cbc = new CouchbaseClient(uris, "default", "", "");


      for (long key = START; key <= MAX_VALUE; key++) {
        String myKey = String.format("Moxi%010dcbc", key);
        c.set(myKey, TTL, myKey).get();
        if (!cbc.get(myKey).equals(myKey)) {
          System.out.println("Moxi and cbc don't match " + myKey);
          fail = true;
        }
        myKey = String.format("cbc%010dMoxi", key);
        cbc.set(myKey, TTL, myKey).get();
        if (!c.get(myKey).equals(myKey)) {
          System.out.println("cbc and Moxi don't match " + myKey);
          fail = true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (c != null) {
        c.shutdown();
      }
      if (cbc != null) {
        cbc.shutdown();
      }
      if (fail) {
        System.out.println("Couchbase Client and Moxi don't match");
      } else {
        System.out.println("Couchbase Client and Moxi matched from "
                + START + " to " + MAX_VALUE);
      }
    }
  }
}
