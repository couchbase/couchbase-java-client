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

package com.couchbase.client.java.analytics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Default implementation of {@link AnalyticsDeferredResultHandle}
 *
 * @author Subhashni Balakrishnan
 * @since 2.7.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DefaultAnalyticsDeferredResultHandle implements AnalyticsDeferredResultHandle {

    private final AsyncAnalyticsDeferredResultHandle asyncHandle;

    public DefaultAnalyticsDeferredResultHandle(AsyncAnalyticsDeferredResultHandle asyncHandle) {
        Objects.requireNonNull(asyncHandle, "The asynchronous deferred handle is required");
        this.asyncHandle = asyncHandle;
    }

    @Override
    public String getStatusHandleUri() {
        return this.asyncHandle.getStatusHandleUri();
    }

    @Override
    public String getResultHandleUri() {
        return this.asyncHandle.getResultHandleUri();
    }

    @Override
    public List<AnalyticsQueryRow> allRows() {
        List<AsyncAnalyticsQueryRow> rows = this.asyncHandle.rows().toList().toBlocking().single();
        List<AnalyticsQueryRow> res = new ArrayList<AnalyticsQueryRow>(rows.size());
        for (AsyncAnalyticsQueryRow row : rows) {
            res.add(new DefaultAnalyticsQueryRow(row));
        }
        return res;
    }

    @Override
    public Iterator<AnalyticsQueryRow> rows() {
        return this.allRows().iterator();
    }

    @Override
    public String status() {
        return this.asyncHandle.status().toBlocking().single();
    }

    @Override
    public String toString() {
        return "DefaultAnalyticsDeferredResultHandle{" +
                "statusUri='" + getStatusHandleUri() + '\'' +
                ", resultUri='" + getResultHandleUri() + '\'' +
                '}';
    }
}
