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

package com.couchbase.client.protocol.views;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.spy.memcached.util.StringUtils;

/**
 * The Query class allows custom view-queries to the Couchbase cluster.
 *
 * The Query class supports all arguments that can be passed along with a
 * Couchbase view query. For example, this makes it possible to change the
 * sorting order, query only a range of keys or include the full docs.
 *
 * By default, the full docs are not included and no reduce job is executed.
 *
 * Here is a short example on how to use the Query object - for more
 * information on the allowed arguments see the corresponding setter method.
 *
 * // Run the reduce phase as well:
 * Query query = new Query();
 * query.setReduce(true);
 *
 * // Include the full docs:
 * Query query = new Query();
 * query.setIncludeDocs(true);
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
  private static final String BBOX = "bbox";
  private static final String DEBUG = "debug";
  private boolean includedocs = false;

  private Map<String, Object> args;

  /**
   * Creates a new Query object with default settings.
   */
  public Query() {
    args = new HashMap<String, Object>();
  }

  /**
   * Read if reduce is enabled or not.
   *
   * @return Whether reduce is enabled or not.
   */
  public boolean willReduce() {
    return (args.containsKey(REDUCE))
      ? ((Boolean)args.get(REDUCE)).booleanValue() : false;
  }

  /**
   * Read if full documents will be included on the query.
   *
   * @return Whether the full documents will be included or not.
   */
  public boolean willIncludeDocs() {
    return includedocs;
  }

  /**
   * Return the documents in descending by key order.
   *
   * @param descending True if the sort-order should be descending.
   * @return The Query instance.
   */
  public Query setDescending(boolean descending) {
    args.put(DESCENDING, Boolean.valueOf(descending));
    return this;
  }

  /**
   * Stop returning records when the specified document ID is reached.
   *
   * @param endkeydocid The document ID that should be used.
   * @return The Query instance.
   */
  public Query setEndkeyDocID(String endkeydocid) {
    args.put(ENDKEYDOCID, endkeydocid);
    return this;
  }

  /**
   * Group the results using the reduce function to a group or single row.
   *
   * @param group True when grouping should be enabled.
   * @return The Query instance.
   */
  public Query setGroup(boolean group) {
    args.put(GROUP, Boolean.valueOf(group));
    return this;
  }

  /**
   * Specify the group level to be used.
   *
   * @param grouplevel How deep the grouping level should be.
   * @return The Query instance.
   */
  public Query setGroupLevel(int grouplevel) {
    args.put(GROUPLEVEL, Integer.valueOf((grouplevel)));
    return this;
  }

  /**
   * If the full documents should be included in the result.
   *
   * @param include True when the full docs should be included in the result.
   * @return The Query instance.
   */
  public Query setIncludeDocs(boolean include) {
    this.includedocs = include;
    return this;
  }

  /**
   * Specifies whether the specified end key should be included in the result.
   *
   * @param inclusiveend True when the key should be included.
   * @return The Query instance.
   */
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

  /**
   * Return only documents that match the specified key.
   *
   * Note that the given key string has to be valid JSON!
   *
   * @param key The document key.
   * @return The Query instance.
   */
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

  /**
   * Limit the number of the returned documents to the specified number.
   *
   * @param limit The number of documents to return.
   * @return The Query instance.
   */
  public Query setLimit(int limit) {
    args.put(LIMIT, Integer.valueOf(limit));
    return this;
  }

  /**
   * Returns the currently set limit.
   *
   * @return The current limit (or -1 if none is set).
   */
  public int getLimit() {
    if (args.containsKey(LIMIT)) {
      return(((Integer)args.get(LIMIT)).intValue());
    } else {
      return -1;
    }
  }

  /**
   * Returns records in the given key range.
   *
   * Note that the given key strings have to be valid JSON!
   *
   * @param startkey The start of the key range.
   * @param endkey The end of the key range.
   * @return The Query instance.
   */
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

  /**
   * Return records with a value equal to or greater than the specified key.
   *
   * Note that the given key string has to be valid JSON!
   *
   * @param startkey The start of the key range.
   * @return The Query instance.
   */
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

  /**
   * Use the reduction function.
   *
   * @param reduce True if the reduce phase should also be executed.
   * @return The Query instance.
   */
  public Query setReduce(Boolean reduce) {
    args.put(REDUCE, reduce);
    return this;
  }

  /**
   * Stop returning records when the specified key is reached.
   *
   * Note that the given key string has to be valid JSON!
   *
   * @param endkey The end of the key range.
   * @return The Query instance.
   */
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

  /**
   * Skip this number of records before starting to return the results.
   *
   * @param docstoskip The number of records to skip.
   * @return The Query instance.
   */
  public Query setSkip(int docstoskip) {
    args.put(SKIP, Integer.valueOf(docstoskip));
    return this;
  }

  /**
   * Allow the results from a stale view to be used.
   *
   * See the "Stale" enum for more information on the possible options. The
   * default setting is "update_after"!
   *
   * @param stale Which stale mode should be used.
   * @return The Query instance.
   */
  public Query setStale(Stale stale) {
    args.put(STALE, stale);
    return this;
  }

  /**
   * Return records starting with the specified document ID.
   *
   * @param startkeydocid The document ID to match.
   * @return The Query instance.
   */
  public Query setStartkeyDocID(String startkeydocid) {
    args.put(STARTKEYDOCID, startkeydocid);
    return this;
  }

  /**
   * Sets the response in the event of an error.
   *
   * See the "OnError" enum for more details on the available options.
   *
   * @param opt The appropriate error handling type.
   * @return The Query instance.
   */
  public Query setOnError(OnError opt) {
    args.put(ONERROR, opt);
    return this;
  }

  /**
   * Sets the params for a spatial bounding box view query.
   *
   * @param lowerLeftLong The longitude of the lower left corner.
   * @param lowerLeftLat The latitude of the lower left corner.
   * @param upperRightLong The longitude of the upper right corner.
   * @param upperRightLat The latitude of the upper right corner.
   * @return The Query instance.
   */
  public Query setBbox(double lowerLeftLong, double lowerLeftLat,
    double upperRightLong, double upperRightLat) {
    String combined = lowerLeftLong + "," + lowerLeftLat + ","
      + upperRightLong + "," + upperRightLat;
    args.put(BBOX, combined);
    return this;
  }

  /**
   * Enabled debugging on view queries.
   *
   * @param debug True when debugging should be enabled.
   * @return The Query instance.
   */
  public Query setDebug(boolean debug) {
    args.put(DEBUG, Boolean.valueOf(debug));
    return this;
  }

  /**
   * Creates a new query instance and returns it with the properties
   * bound to the current object.
   *
   * @return The new Query object.
   */
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
    if (args.containsKey(BBOX)) {
      String[] bbox = ((String)args.get(BBOX)).split(",");
      query.setBbox(Double.parseDouble(bbox[0]), Double.parseDouble(bbox[1]),
        Double.parseDouble(bbox[2]), Double.parseDouble(bbox[3]));
    }
    if (args.containsKey(DEBUG)) {
      query.setDebug(((Boolean)args.get(DEBUG)).booleanValue());
    }
    query.setIncludeDocs(willIncludeDocs());

    return query;
  }

  /**
   * Returns the Query object as a string, suitable for the HTTP queries.
   *
   * @return Returns the query object as its string representation
   */
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
      String argument;
      try {
        argument = arg.getKey() + "=" + prepareValue(
          arg.getKey(), arg.getValue()
        );
      } catch (Exception ex) {
        throw new RuntimeException("Could not prepare view argument: " + ex);
      }
      result.append(argument);
    }
    return result.toString();
  }

  /**
   * Takes a given object, inspects its type and returns
   * its string representation.
   *
   * This helper method aids the toString() method so that it does
   * not need to transform map entries to their string representations
   * for itself. It also checks for various special cases and makes
   * sure the correct string representation is returned.
   *
   * When no previous match was found, the final try is to cast it to a
   * long value and treat it as a numeric value. If this doesn't succeed
   * either, then it is treated as a string.
   *
   * @param key The key for the corresponding value.
   * @param value The value to prepared.
   * @return The correctly formatted and encoded value.
   */
  private String prepareValue(String key, Object value)
    throws UnsupportedEncodingException {
    String encoded;

    if (key.equals(STARTKEYDOCID) || key.equals(BBOX)) {
      encoded = (String) value;
    } else if (value instanceof Stale) {
      encoded = ((Stale) value).toString();
    } else if (value instanceof OnError) {
      encoded = ((OnError) value).toString();
    } else if (StringUtils.isJsonObject(value.toString())) {
      encoded = value.toString();
    } else if(value.toString().startsWith("\"")) {
      encoded = value.toString();
    } else {
      ParsePosition pp = new ParsePosition(0);
      NumberFormat numberFormat = NumberFormat.getInstance();
      Number result = numberFormat.parse(value.toString(), pp);
      if (pp.getIndex() == value.toString().length()) {
        encoded = result.toString();
      } else {
        encoded = "\"" + value.toString() + "\"";
      }
    }

    return URLEncoder.encode(encoded, "UTF-8");
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
