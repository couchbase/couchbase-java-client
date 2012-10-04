/**
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

package com.couchbase.client.protocol.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.spy.memcached.util.StringUtils;

/**
 * A Query.
 */
public class Query {
  private static final String DESCENDING = "descending";
  private static final String ENDKEY = "endkey";
  private static final String ENDKEYDOCID = "endkey_docid";
  private static final String GROUP = "group";
  private static final String GROUPLEVEL = "group_level";
  private static final String INCLUSIVEEND = "inclusive_end";
  private static final String KEY = "key";
  private static final String KEYS = "keys";
  private static final String LIMIT = "limit";
  private static final String REDUCE = "reduce";
  private static final String SKIP = "skip";
  private static final String STALE = "stale";
  private static final String STARTKEY = "startkey";
  private static final String STARTKEYDOCID = "startkey_docid";
  private static final String ONERROR = "on_error";
  private boolean includedocs = false;

  private Map<String, Object> args;

  public Query() {
    args = new HashMap<String, Object>();
  }

  public boolean willReduce() {
    return (args.containsKey(REDUCE))
      ? ((Boolean)args.get(REDUCE)).booleanValue() : false;
  }

  public boolean willIncludeDocs() {
    return includedocs;
  }

  public Query setDescending(boolean descending) {
    args.put(DESCENDING, Boolean.valueOf(descending));
    return this;
  }

  public Query setEndkeyDocID(String endkeydocid) {
    args.put(ENDKEYDOCID, endkeydocid);
    return this;
  }

  public Query setGroup(boolean group) {
    args.put(GROUP, Boolean.valueOf(group));
    return this;
  }

  public Query setGroupLevel(int grouplevel) {
    args.put(GROUPLEVEL, Integer.valueOf((grouplevel)));
    return this;
  }

  public Query setIncludeDocs(boolean include) {
    this.includedocs = include;
    return this;
  }

  public Query setInclusiveEnd(boolean inclusiveend) {
    args.put(INCLUSIVEEND, Boolean.valueOf(inclusiveend));
    return this;
  }

  /**
   * Return only documents that match the specified key.
   *
   * The "key" param must be specified as a valid JSON string, but the
   * ComplexKey class takes care of this. See the documentation of the
   * ComplexKey class for more information on its usage.
   *
   * @param key The document key.
   * @return The Query instance.
   */
  public Query setKey(ComplexKey key) {
    args.put(KEY, key.toJson());
    return this;
  }

  public Query setKey(String key) {
    args.put(KEY, key);
    return this;
  }

  /**
   * Return only documents that match each of keys specified within the given
   * array.
   *
   * The "keys" param must be specified as a valid JSON string, but the
   * ComplexKey class takes care of this. See the documentation of the
   * ComplexKey class for more information on its usage.
   *
   * Also, sorting is not applied when using this option.
   *
   * @param keys The document keys.
   * @return The Query instance.
   */
  public Query setKeys(ComplexKey keys) {
    args.put(KEYS, keys.toJson());
    return this;
  }

  /**
   * Return only documents that match each of keys specified within the given
   * array.
   *
   * Note that the given key string has to be valid JSON! Also, sorting is not
   * applied when using this option.
   *
   * @param keys The document keys.
   * @return The Query instance.
   */
  public Query setKeys(String keys) {
    args.put(KEYS, keys);
    return this;
  }
  public Query setLimit(int limit) {
    args.put(LIMIT, Integer.valueOf(limit));
    return this;
  }

  public int getLimit() {
    if (args.containsKey(LIMIT)) {
      return(((Integer)args.get(LIMIT)).intValue());
    } else {
      return -1;
    }
  }

  public Query setRange(String startkey, String endkey) {
    args.put(ENDKEY, endkey);
    args.put(STARTKEY, startkey);
    return this;
  }

  /**
   * Returns records in the given key range.
   *
   * The range keys must be specified as a valid JSON strings, but the
   * ComplexKey class takes care of this. See the documentation of the
   * ComplexKey class for more information on its usage.
   *
   * @param startkey The start of the key range.
   * @param endkey The end of the key range.
   * @return The Query instance.
   */
  public Query setRange(ComplexKey startkey, ComplexKey endkey) {
    args.put(ENDKEY, endkey.toJson());
    args.put(STARTKEY, startkey.toJson());
    return this;
  }

  public Query setRangeStart(String startkey) {
    args.put(STARTKEY, startkey);
    return this;
  }

  /**
   * Return records with a value equal to or greater than the specified key.
   *
   * The range key must be specified as a valid JSON string, but the
   * ComplexKey class takes care of this. See the documentation of the
   * ComplexKey class for more information on its usage.
   *
   * @param startkey The start of the key range.
   * @return The Query instance.
   */
  public Query setRangeStart(ComplexKey startkey) {
    args.put(STARTKEY, startkey.toJson());
    return this;
  }

  public Query setReduce(boolean reduce) {
    args.put(REDUCE, new Boolean(reduce));
    return this;
  }

  public Query setRangeEnd(String endkey) {
    args.put(ENDKEY, endkey);
    return this;
  }

  /**
   * Stop returning records when the specified key is reached.
   *
   * The range key must be specified as a valid JSON string, but the
   * ComplexKey class takes care of this. See the documentation of the
   * ComplexKey class for more information on its usage.
   *
   * @param endkey The end of the key range.
   * @return The Query instance.
   */
  public Query setRangeEnd(ComplexKey endkey) {
    args.put(ENDKEY, endkey.toJson());
    return this;
  }

  public Query setSkip(int docstoskip) {
    args.put(SKIP, Integer.valueOf(docstoskip));
    return this;
  }

  public Query setStale(Stale stale) {
    args.put(STALE, stale);
    return this;
  }

  public Query setStartkeyDocID(String startkeydocid) {
    args.put(STARTKEYDOCID, startkeydocid);
    return this;
  }

  public Query setOnError(OnError opt) {
    args.put(ONERROR, opt);
    return this;
  }

  public Query copy() {
    Query query = new Query();

    if (args.containsKey(DESCENDING)) {
      query.setDescending(((Boolean)args.get(DESCENDING)).booleanValue());
    }
    if (args.containsKey(ENDKEY)) {
      query.setRangeEnd(((String)args.get(ENDKEY)));
    }
    if (args.containsKey(ENDKEYDOCID)) {
      query.setEndkeyDocID(((String)args.get(ENDKEYDOCID)));
    }
    if (args.containsKey(GROUP)) {
      query.setGroup(((Boolean)args.get(GROUP)).booleanValue());
    }
    if (args.containsKey(GROUPLEVEL)) {
      query.setGroupLevel(((Integer)args.get(GROUPLEVEL)).intValue());
    }
    if (args.containsKey(INCLUSIVEEND)) {
      query.setInclusiveEnd(((Boolean)args.get(INCLUSIVEEND)).booleanValue());
    }
    if (args.containsKey(KEY)) {
      query.setKey(((String)args.get(KEY)));
    }
    if (args.containsKey(KEYS)) {
      query.setKeys(((String)args.get(KEYS)));
    }
    if (args.containsKey(LIMIT)) {
      query.setLimit(((Integer)args.get(LIMIT)).intValue());
    }
    if (args.containsKey(REDUCE)) {
      query.setReduce(((Boolean)args.get(REDUCE)).booleanValue());
    }
    if (args.containsKey(SKIP)) {
      query.setSkip(((Integer)args.get(SKIP)).intValue());
    }
    if (args.containsKey(STALE)) {
      query.setStale(((Stale)args.get(STALE)));
    }
    if (args.containsKey(STARTKEY)) {
      query.setRangeStart(((String)args.get(STARTKEY)));
    }
    if (args.containsKey(STARTKEYDOCID)) {
      query.setStartkeyDocID(((String)args.get(STARTKEYDOCID)));
    }
    if (args.containsKey(ONERROR)) {
      query.setOnError(((OnError)args.get(ONERROR)));
    }
    query.setIncludeDocs(willIncludeDocs());

    return query;
  }

  @Override
  public String toString() {
    boolean first = true;
    StringBuffer result = new StringBuffer();
    for (Entry<String, Object> arg : args.entrySet()) {
      if (first) {
        result.append("?");
        first = false;
      } else {
        result.append("&");
      }
      result.append(getArg(arg.getKey(), arg.getValue()));
    }
    return result.toString();
  }

  private String getArg(String key, Object value) {
    // Special case
    if (key.equals(STARTKEYDOCID)) {
      return key + "=" + value;
    } else if (value instanceof Stale) {
      return key + "=" + ((Stale) value).toString();
    } else if (value instanceof OnError) {
      return key + "=" + value;
    } else if (StringUtils.isJsonObject(value.toString())) {
      return key + "=" + value.toString();
    } else {
      return key + "=\"" + value + "\"";
    }
  }

  /**
   * Returns all current args for proper inspection.
   *
   * @return returns the currently stored arguments
   */
  public Map<String, Object> getArgs() {
    return args;
  }
}
