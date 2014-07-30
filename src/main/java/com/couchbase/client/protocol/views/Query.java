/**
 * Copyright (C) 2009-2014 Couchbase, Inc.
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

import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

  private static final int PARAM_REDUCE_OFFSET = 0;
  private static final int PARAM_LIMIT_OFFSET = 2;
  private static final int PARAM_SKIP_OFFSET = 4;
  private static final int PARAM_STALE_OFFSET = 6;
  private static final int PARAM_GROUPLEVEL_OFFSET = 8;
  private static final int PARAM_GROUP_OFFSET = 10;
  private static final int PARAM_ONERROR_OFFSET = 12;
  private static final int PARAM_DEBUG_OFFSET = 14;
  private static final int PARAM_DESCENDING_OFFSET = 16;
  private static final int PARAM_INCLUSIVEEND_OFFSET = 18;
  private static final int PARAM_STARTKEY_OFFSET = 20;
  private static final int PARAM_STARTKEYDOCID_OFFSET = 22;
  private static final int PARAM_ENDKEY_OFFSET = 24;
  private static final int PARAM_ENDKEYDOCID_OFFSET = 26;
  private static final int PARAM_KEYS_OFFSET = 28;
  private static final int PARAM_KEY_OFFSET = 30;
  private static final int PARAM_BBOX_OFFSET = 32;

  /**
   * Number of supported possible params for a query.
   */
  private static final int NUM_PARAMS = 17;

  /**
   * Contains all stored params.
   */
  private final String[] params;

  /**
   * The include docs param is not sent across the wire.
   */
  private boolean includeDocs;

  /**
   * The pattern identifying if the string should be quoted or not.
   */
  private static final Pattern quotePattern =
    Pattern.compile("^(\".*|\\{.*|\\[.*|true|false|null|-?[\\d,]*([.Ee]\\d+)?)$");

  /**
   * Number format to use to find matching numbers.
   */
  private final NumberFormat numberFormat = NumberFormat.getInstance();

  /**
   * Create a new {@link Query}.
   */
  public Query() {
    this(new String[NUM_PARAMS * 2]);
  }

  /**
   * Private constructor used for copying.
   *
   * @param params the params to assign immediately.
   */
  Query(String[] params) {
    this.params = params;
  }

  /**
   * Explicitly enable/disable the reduce function on the query.
   *
   * @param reduce if reduce should be enabled or not.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setReduce(final boolean reduce) {
    params[PARAM_REDUCE_OFFSET] = "reduce";
    params[PARAM_REDUCE_OFFSET+1] = Boolean.toString(reduce);
    return this;
  }

  /**
   * Limit the number of the returned documents to the specified number.
   *
   * @param limit the number of documents to return.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setLimit(final int limit) {
    params[PARAM_LIMIT_OFFSET] = "limit";
    params[PARAM_LIMIT_OFFSET+1] = Integer.toString(limit);
    return this;
  }

  /**
   * Group the results using the reduce function to a group or single row.
   *
   * Important: this setter and {@link #setGroupLevel(int)} should not be used
   * together in the same {@link Query}. It is sufficient to only set the
   * grouping level only and use this setter in cases where you always want the
   * highest group level implictly.
   *
   * @param group True when grouping should be enabled.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setGroup(final boolean group) {
    params[PARAM_GROUP_OFFSET] = "group";
    params[PARAM_GROUP_OFFSET+1] = Boolean.toString(group);
    return this;
  }

  /**
   * Specify the group level to be used.
   *
   * Important: {@link #setGroup(boolean)} and this setter should not be used
   * together in the same {@link Query}. It is sufficient to only use this
   * setter and use {@link #setGroup(boolean)} in cases where you always want
   * the highest group level implictly.
   *
   * @param grouplevel How deep the grouping level should be.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setGroupLevel(final int grouplevel) {
    params[PARAM_GROUPLEVEL_OFFSET] = "group_level";
    params[PARAM_GROUPLEVEL_OFFSET+1] = Integer.toString(grouplevel);
    return this;
  }

  /**
   * If the full documents should be included in the result.
   *
   * @param include True when the full docs should be included in the result.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setIncludeDocs(final boolean include) {
    includeDocs = include;
    return this;
  }

  /**
   * Specifies whether the specified end key should be included in the result.
   *
   * @param inclusiveend True when the key should be included.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setInclusiveEnd(final boolean inclusiveend) {
    params[PARAM_INCLUSIVEEND_OFFSET] = "inclusive_end";
    params[PARAM_INCLUSIVEEND_OFFSET+1] = Boolean.toString(inclusiveend);
    return this;
  }


  /**
   * Skip this number of records before starting to return the results.
   *
   * @param skip The number of records to skip.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setSkip(final int skip) {
    params[PARAM_SKIP_OFFSET] = "skip";
    params[PARAM_SKIP_OFFSET+1] = Integer.toString(skip);
    return this;
  }

  /**
   * Allow the results from a stale view to be used.
   *
   * See the "Stale" enum for more information on the possible options. The
   * default setting is "update_after"!
   *
   * @param stale Which stale mode should be used.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setStale(final Stale stale) {
    params[PARAM_STALE_OFFSET] = "stale";
    params[PARAM_STALE_OFFSET+1] = stale.toString();
    return this;
  }


  /**
   * Sets the response in the event of an error.
   *
   * See the "OnError" enum for more details on the available options.
   *
   * @param onError The appropriate error handling type.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setOnError(final OnError onError) {
    params[PARAM_ONERROR_OFFSET] = "on_error";
    params[PARAM_ONERROR_OFFSET+1] = onError.toString();
    return this;
  }

  /**
   * Enabled debugging on view queries.
   *
   * @param debug True when debugging should be enabled.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setDebug(final boolean debug) {
    params[PARAM_DEBUG_OFFSET] = "debug";
    params[PARAM_DEBUG_OFFSET+1] = Boolean.toString(debug);
    return this;
  }

  /**
   * Return the documents in descending by key order.
   *
   * @param descending True if the sort-order should be descending.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setDescending(final boolean descending) {
    params[PARAM_DESCENDING_OFFSET] = "descending";
    params[PARAM_DESCENDING_OFFSET+1] = Boolean.toString(descending);
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
   * @return the {@link Query} object for proper chaining.
   */
  public Query setKey(final ComplexKey key) {
    params[PARAM_KEY_OFFSET] = "key";
    params[PARAM_KEY_OFFSET+1] = encode(key.toJson());
    return this;
  }

  /**
   * Return only documents that match the specified key.
   *
   * Note that the given key string has to be valid JSON!
   *
   * @param key The document key.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setKey(String key) {
    params[PARAM_KEY_OFFSET] = "key";
    params[PARAM_KEY_OFFSET+1] = encode(quote(key));
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
   * @return the {@link Query} object for proper chaining.
   */
  public Query setKeys(ComplexKey keys) {
    params[PARAM_KEYS_OFFSET] = "keys";
    params[PARAM_KEYS_OFFSET+1] = encode(keys.toJson());
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
   * @return the {@link Query} object for proper chaining.
   */
  public Query setKeys(String keys) {
    params[PARAM_KEYS_OFFSET] = "keys";
    params[PARAM_KEYS_OFFSET+1] = encode(quote(keys));
    return this;
  }

  /**
   * Return records starting with the specified document ID.
   *
   * @param startkeydocid The document ID to match.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setStartkeyDocID(final String startkeydocid) {
    params[PARAM_STARTKEYDOCID_OFFSET] = "startkey_docid";
    params[PARAM_STARTKEYDOCID_OFFSET+1] = encode(startkeydocid);
    return this;
  }

  /**
   * Stop returning records when the specified document ID is reached.
   *
   * @param endkeydocid The document ID that should be used.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setEndkeyDocID(final String endkeydocid) {
    params[PARAM_ENDKEYDOCID_OFFSET] = "endkey_docid";
    params[PARAM_ENDKEYDOCID_OFFSET+1] = encode(endkeydocid);
    return this;
  }


  /**
   * Returns records in the given key range.
   *
   * Note that the given key strings have to be valid JSON!
   *
   * @param startkey The start of the key range.
   * @param endkey The end of the key range.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setRange(final String startkey, final String endkey) {
    setRangeStart(startkey);
    setRangeEnd(endkey);
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
   * @return the {@link Query} object for proper chaining.
   */
  public Query setRange(final ComplexKey startkey, final ComplexKey endkey) {
    setRangeStart(startkey);
    setRangeEnd(endkey);
    return this;
  }

  /**
   * Return records with a value equal to or greater than the specified key.
   *
   * Note that the given key string has to be valid JSON!
   *
   * @param startkey The start of the key range.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setRangeStart(final String startkey) {
    params[PARAM_STARTKEY_OFFSET] = "startkey";
    params[PARAM_STARTKEY_OFFSET+1] = encode(quote(startkey));
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
   * @return the {@link Query} object for proper chaining.
   */
  public Query setRangeStart(final ComplexKey startkey) {
    params[PARAM_STARTKEY_OFFSET] = "startkey";
    params[PARAM_STARTKEY_OFFSET+1] = encode(startkey.toJson());
    return this;
  }

  /**
   * Stop returning records when the specified key is reached.
   *
   * Note that the given key string has to be valid JSON!
   *
   * @param endkey The end of the key range.
   * @return the {@link Query} object for proper chaining.
   */
  public Query setRangeEnd(final String endkey) {
    params[PARAM_ENDKEY_OFFSET] = "endkey";
    params[PARAM_ENDKEY_OFFSET+1] = encode(quote(endkey));
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
   * @return the {@link Query} object for proper chaining.
   */
  public Query setRangeEnd(final ComplexKey endkey) {
    params[PARAM_ENDKEY_OFFSET] = "endkey";
    params[PARAM_ENDKEY_OFFSET+1] = encode(endkey.toJson());
    return this;
  }

  /**
   * Sets the params for a spatial bounding box view query.
   *
   * @param lowerLeftLong The longitude of the lower left corner.
   * @param lowerLeftLat The latitude of the lower left corner.
   * @param upperRightLong The longitude of the upper right corner.
   * @param upperRightLat The latitude of the upper right corner.
   * @return The bench.OldQuery instance.
   */
  public Query setBbox(double lowerLeftLong, double lowerLeftLat,
    double upperRightLong, double upperRightLat) {
    String combined = lowerLeftLong + "," + lowerLeftLat + ','
      + upperRightLong + ',' + upperRightLat;
    params[PARAM_BBOX_OFFSET] = "bbox";
    params[PARAM_BBOX_OFFSET+1] = encode(combined);
    return this;
  }

  /**
   * Read if reduce is enabled or not.
   *
   * @return Whether reduce is enabled or not.
   */
  public boolean willReduce() {
    String reduce = params[PARAM_REDUCE_OFFSET+1];
    if (reduce == null) {
      return false;
    }
    return Boolean.valueOf(reduce);
  }

  /**
   * Read if full documents will be included on the query.
   *
   * @return Whether the full documents will be included or not.
   */
  public boolean willIncludeDocs() {
    return includeDocs;
  }

  /**
   * Returns the currently set limit.
   *
   * @return The current limit (or -1 if none is set).
   */
  public int getLimit() {
    String limit = params[PARAM_LIMIT_OFFSET+1];
    if (limit == null) {
      return -1;
    }
    return Integer.valueOf(limit);
  }

  /**
   * Returns the {@link Query} as a HTTP-compatible query string.
   *
   * @return the stringified query.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean firstParam = true;
    for (int i = 0; i < params.length; i++) {
      if (params[i] == null) {
        i++;
        continue;
      }

      boolean even = i % 2 == 0;
      if (even) {
        sb.append(firstParam ? "?" : "&");
      }
      sb.append(params[i]);
      firstParam = false;
      if (even) {
        sb.append('=');
      }
    }
    return sb.toString();
  }

  /**
   * Helper method which collects all currently set arguments.
   *
   * This method is most suitable for testing and debugging.
   * @return a map containing all args and their values.
   */
  public Map<String, Object> getArgs() {
    Map<String, Object> args = new HashMap<String, Object>();
    for (int i = 0; i < params.length; i++) {
      boolean even = i % 2 == 0;
      if (even && params[i] != null) {
        args.put(params[i], params[i+1]);
      }
    }
    return args;
  }

  /**
   * Helper method to properly encode a string.
   *
   * This method can be overridden if a different encoding logic needs to be
   * used.
   *
   * @param source source string.
   * @return encoded target string.
   */
  protected String encode(final String source) {
    try {
      return URLEncoder.encode(source, "UTF-8");
    } catch(Exception ex) {
      throw new RuntimeException("Could not prepare view argument: " + ex);
    }
  }

  /**
   * Helper method to properly quote the string if its a JSON string.
   *
   * @param source source string.
   * @return maybe quoted target string.
   */
  protected String quote(final String source) {
    if (quotePattern.matcher(source).matches()) {
      ParsePosition parsePosition = new ParsePosition(0);
      Number result = numberFormat.parse(source, parsePosition);
      if (parsePosition.getIndex() == source.length()) {
        return result.toString();
      }
      return source;
    }
    return '"' + source + '"';
  }

  /**
   * Copy the current {@link Query} object into another one.
   *
   * @return an identical copy.
   */
  public Query copy() {
    Query copied = new Query(params.clone());
    copied.setIncludeDocs(willIncludeDocs());
    return copied;
  }

}