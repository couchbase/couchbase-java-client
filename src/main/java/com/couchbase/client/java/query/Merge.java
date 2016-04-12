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
import com.couchbase.client.java.query.dsl.path.DefaultMergeSourcePath;
import com.couchbase.client.java.query.dsl.path.MergeSourcePath;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.x;

public class Merge {

  private Merge() {}


  public static MergeSourcePath mergeInto(String bucket) {
    return mergeInto(i(bucket));
  }

  public static MergeSourcePath mergeInto(Expression bucket) {
    return new DefaultMergeSourcePath(new MergePath(bucket));
  }

  public static MergeSourcePath mergeIntoCurrentBucket() {
    return mergeInto(x(CouchbaseAsyncBucket.CURRENT_BUCKET_IDENTIFIER));
  }

  private static class MergePath extends AbstractPath {
    public MergePath(final Expression bucket) {
      super(null);
      element(new Element() {
        @Override
        public String export() {
          return "MERGE INTO " + bucket.toString();
        }
      });
    }
  }
}
