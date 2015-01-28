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
package com.couchbase.client.java.query.consistency;

/**
 * The possible values for scan_consistency in a N1QL request.
 *
 * @author Simon Baslé
 * @since 2.1
 */
public enum ScanConsistency {

    /**
     * This is the default (for single-statement requests). No timestamp vector is used
     * in the index scan.
     * This is also the fastest mode, because we avoid the cost of obtaining the vector,
     * and we also avoid any wait for the index to catch up to the vector.
     */
    NOT_BOUNDED,
    /**
     * This implements strong consistency per request.
     * Before processing the request, a current vector is obtained.
     * The vector is used as a lower bound for the statements in the request.
     * If there are DML statements in the request, RYOW is also applied within the request.
     */
    REQUEST_PLUS,
    /**
     * This implements strong consistency per statement.
     * Before processing each statement, a current vector is obtained
     * and used as a lower bound for that statement.
     */
    STATEMENT_PLUS;

    public String n1ql() {
        return this.name().toLowerCase();
    }
}
