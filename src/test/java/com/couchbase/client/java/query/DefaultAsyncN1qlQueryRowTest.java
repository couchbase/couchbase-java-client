/*
 * Copyright (c) 2018 Couchbase, Inc.
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

import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.error.TranscodingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Verifies the functionality of the {@link DefaultAsyncN1qlQueryRow}.
 *
 * @author Michael Nitschinger
 * @since 2.5.5
 */
public class DefaultAsyncN1qlQueryRowTest {

  @Test
  public void shouldLogValueIfNotDecodable() {
    String invalidValue = "{\"invalid:true}";
    DefaultAsyncN1qlQueryRow row = new DefaultAsyncN1qlQueryRow(invalidValue.getBytes(CharsetUtil.UTF_8));
    try {
      row.value();
      fail("did expect transcoding exception");
    } catch (TranscodingException ex) {
      assertEquals(
        "Error deserializing row value from bytes to JsonObject \"{\"invalid:true}\"",
        ex.getMessage()
      );
    }
  }

}