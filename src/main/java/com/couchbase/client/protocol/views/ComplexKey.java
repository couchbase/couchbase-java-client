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

import java.util.Arrays;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 * Allows simple definition of complex JSON keys for query inputs.
 *
 * If you use the ComplexKex class, the stored objects ultimately get converted
 * into a JSON string. As a result, make sure your custom objects implement the
 * "toString" method accordingly (unless you work with trivial types like
 * Strings or numbers).
 *
 * Instead of using a constructor, use the static "of" method to generate your
 * objects. You can also use a special empty object or array.
 *
 * Here are some simple examples:
 *
 * // generated JSON: [2012,9,7]
 * ComplexKey.of(2012, 9, 7);
 *
 * // generated JSON: ["Hello","World",5.12]
 * ComplexKey.of("Hello", "World", 5.12);
 *
 * // generated JSON: {}
 * ComplexKey.of(ComplexKey.emptyObject());
 *
 * // generated JSON: []
 * ComplexKey.of(ComplexKey.emptyArray());
 *
 * This was inspired by the Ektorp project, which queries Apache CouchDB.
 */
public final class ComplexKey {

  /**
   * Holds the list of object components to convert.
   */
  private final List<Object> components;

  /**
   * Defines the empty object to use.
   */
  private static final Object EMPTY_OBJECT = new Object();

  /**
   * Defines the empty array to use.
   */
  private static final Object[] EMPTY_ARRAY = new Object[0];

  /**
   * Private constructor used by the "of" or other factory methods.
   *
   * @param components List of objects that should be converted
   */
  private ComplexKey(Object[] components) {
    this.components = Arrays.asList(components);
  }

  /**
   * Generate a ComplexKey based on the input Object arguments (varargs).
   *
   * This method is most often used along with the Query object and done
   * when new a complex key is used as a query input. For example, to query
   * with the array of integers 2012, 9, 5 (a common method of setting up
   * reduceable date queries) one may do something like:
   *
   * ComplexKey.of(2012, 9, 5);
   *
   * @param components List of objects that should be converted
   * @return Returns a new instance of ComplexKey
   */
  public static ComplexKey of(Object... components) {
    return new ComplexKey(components);
  }

  /**
   * Returns a single empty object.
   *
   * @return Returns the empty object
   */
  public static Object emptyObject() {
    return EMPTY_OBJECT;
  }

  /**
   * Returns an empty array of objects.
   *
   * @return Returns an empty array of objects
   */
  public static Object[] emptyArray() {
    return EMPTY_ARRAY;
  }


  /**
   * Generate a JSON string of the ComplexKey.
   *
   * This method is responsible for processing and converting the
   * complex key list and returning it as a JSON string. This string
   * heavily depends on the structure of the stored objects.
   *
   * @return the JSON of the underlying complex key
   */
  public String toJson() {
    if(components.size() == 1) {
      Object component = components.get(0);
      if(component == EMPTY_OBJECT) {
        return new JSONObject().toString();
      } else if(component instanceof String) {
        return "\"" + component + "\"";
      }
      return component.toString();
    }

    JSONArray key = new JSONArray();
    for (Object component : components) {
      if (component == EMPTY_OBJECT) {
        key.put(new JSONObject());
      } else {
        key.put(component);
      }
    }

    return key.toString();
  }

}
