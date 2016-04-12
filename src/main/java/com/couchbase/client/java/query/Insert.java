/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.query;

import com.couchbase.client.java.CouchbaseAsyncBucket;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.query.dsl.element.Element;
import com.couchbase.client.java.query.dsl.path.AbstractPath;
import com.couchbase.client.java.query.dsl.path.DefaultInitialInsertPath;
import com.couchbase.client.java.query.dsl.path.InitialInsertPath;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.x;

public class Insert {

  private Insert() {}

  public static InitialInsertPath insertInto(String bucket) {
    return insertInto(i(bucket));
  }

  public static InitialInsertPath insertInto(Expression bucket) {
    return new DefaultInitialInsertPath(new InsertPath(bucket));
  }

  public static InitialInsertPath insertIntoCurrentBucket() {
    return insertInto(x(CouchbaseAsyncBucket.CURRENT_BUCKET_IDENTIFIER));
  }

  private static class InsertPath extends AbstractPath {
    public InsertPath(final Expression bucket) {
      super(null);
      element(new Element() {
        @Override
        public String export() {
          return "INSERT INTO " + bucket.toString();
        }
      });
    }
  }

}
