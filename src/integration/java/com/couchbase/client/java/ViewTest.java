/**
 * Copyright (C) 2014 Couchbase, Inc.
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
package com.couchbase.client.java;

import com.couchbase.client.java.query.ViewQuery;
import com.couchbase.client.java.query.ViewResult;
import com.couchbase.client.java.query.ViewRow;
import com.couchbase.client.java.util.ClusterDependentTest;
import org.junit.Test;
import rx.functions.Action1;

public class ViewTest extends ClusterDependentTest {


  @Test
  public void shouldQueryView() throws Exception {
     ViewResult result = bucket().query(ViewQuery.from("beer", "brewery_beers").debug().skip(1).limit(-1)).toBlocking().single();

      result.rows().toBlocking().forEach(new Action1<ViewRow>() {
          @Override
          public void call(ViewRow viewRow) {
              System.err.println(viewRow.key().getClass());
          }
      });

      Thread.sleep(10000);
  }
}
