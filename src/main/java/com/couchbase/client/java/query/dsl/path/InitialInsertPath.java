/**
 * Copyright (C) 2015 Couchbase, Inc.
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
package com.couchbase.client.java.query.dsl.path;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.Expression;

public interface InitialInsertPath extends Path {

  ReturningPath select(Expression key, Statement select);
  ReturningPath select(Expression key, Expression value, Statement select);
  ReturningPath select(Expression key, String value, Statement select);

  ReturningPath select(String key, Statement select);
  ReturningPath select(String key, String value, Statement select);
  ReturningPath select(String key, Expression value, Statement select);

  InsertValuesPath values(String id, Expression value);
  InsertValuesPath values(String id, JsonObject value);
  InsertValuesPath values(String id, JsonArray value);
  InsertValuesPath values(String id, String value);
  InsertValuesPath values(String id, int value);
  InsertValuesPath values(String id, long value);
  InsertValuesPath values(String id, double value);
  InsertValuesPath values(String id, float value);
  InsertValuesPath values(String id, boolean value);

  InsertValuesPath values(Expression id, Expression value);
  InsertValuesPath values(Expression id, JsonObject value);
  InsertValuesPath values(Expression id, JsonArray value);
  InsertValuesPath values(Expression id, String value);
  InsertValuesPath values(Expression id, int value);
  InsertValuesPath values(Expression id, long value);
  InsertValuesPath values(Expression id, double value);
  InsertValuesPath values(Expression id, float value);
  InsertValuesPath values(Expression id, boolean value);

}
