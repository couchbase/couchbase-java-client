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
package com.couchbase.client.http;

import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;

/**
 *
 */
public class HttpUtil {

  /**
   * Generate the payload of an authorization header given a username and
   * password.
   *
   * Since our HTTP needs are modest in this library and our dependencies
   * are lightweight enough to not carry all of the auth capabilities, this
   * method will generate the payload needed for an authorization header.
   *
   * @return a value for an HTTP Basic Auth Header
   */
  public static String buildAuthHeader(String username, String password)
          throws UnsupportedEncodingException {
    // apparently netty isn't familiar with HTTP Basic Auth
    StringBuilder clearText = new StringBuilder(username);
    clearText.append(':');
    if (password != null) {
      clearText.append(password);
    }
    String headerResult;
    headerResult = "Basic "
            + Base64.encodeBase64String(clearText.toString().getBytes("UTF-8"));

    if (headerResult.endsWith("\r\n")) {
      headerResult = headerResult.substring(0, headerResult.length() - 2);
    }
    return headerResult;
  }
}
