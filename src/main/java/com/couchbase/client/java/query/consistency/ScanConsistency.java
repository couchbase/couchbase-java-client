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
