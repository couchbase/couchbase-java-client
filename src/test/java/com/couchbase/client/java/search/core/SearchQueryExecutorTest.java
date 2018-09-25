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
package com.couchbase.client.java.search.core;

import com.couchbase.client.core.CouchbaseCore;
import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.search.SearchQueryRequest;
import com.couchbase.client.core.message.search.SearchQueryResponse;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.FtsServerOverloadException;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.queries.QueryStringQuery;
import com.couchbase.client.java.search.result.AsyncSearchQueryResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of the {@link SearchQueryExecutor}.
 */
public class SearchQueryExecutorTest {

    private static final String SUCCESS_RESULT = "{\"status\":{\"total\":1,\"failed\":0,\"successful\":1},\"request\":{\"query\":{\"query\":\"krak\"},\"size\":10,\"from\":0,\"highlight\":{\"style\":null,\"fields\":null},\"fields\":[\"*\"],\"facets\":null,\"explain\":true,\"sort\":[\"-_score\"],\"includeLocations\":false},\"hits\":[],\"total_hits\":0,\"max_score\":0,\"took\":112190,\"facets\":null}";

    private static final String FAIL_RESULT = "{\"error\":\"rest_index: Query, indexName: perf_fts_index, err: search request rejected\" ** ,\"request\":{\"ctl\" {\"consistency\":null,\"timeout\":74499},\"explain\":false,\"facets\":null,\"fields\":null,\"from\":0,\"highlight\":null,\"includeLocations\":false,\"pindexNames\":[\"perf_fts_index_5cc1efc039b2ffe2_18572d87\",\"perf_fts_index_5cc1efc039b2ffe2_6ddbfb54\",\"perf_fts_index_5cc1efc039b2ffe2_f4e0a48a\"],\"query\":{\"field\":\"text\",\"wildcard\":\"ma*a\"},\"size\":10,\"sort\":[\"-_score\"]},\"status\":\"fail\"}\",\"request\":{\"ctl\":{\"timeout\":75000},\"query\":{\"field\":\"text\",\"wildcard\":\"ma*a\"},\"size\":10},\"status\":\"fail\"}";

    private static CouchbaseEnvironment ENV;

    @BeforeClass
    public static void setup() {
        ENV = DefaultCouchbaseEnvironment.create();
    }

    @AfterClass
    public static void cleanup() {
        ENV.shutdown();
    }

    /**
     * Verifies that when a HTTP 429 is returned, the SDK will retry based
     * on its retry strategy and if not successful give up at some point.
     */
    @Test
    public void shouldRetryResponseTooManyRequestsAndComplete() {
        CouchbaseCore core = mock(CouchbaseCore.class);
        SearchQuery query = new SearchQuery("index", new QueryStringQuery("query"));

        final AtomicInteger tried = new AtomicInteger(0);
        when(core.send(any(SearchQueryRequest.class))).thenAnswer(new Answer<Observable<SearchQueryResponse>>() {
            @Override
            public Observable<SearchQueryResponse> answer(InvocationOnMock invocation) {
                if (tried.incrementAndGet() > 5) {
                    return Observable.just(
                        new SearchQueryResponse(SUCCESS_RESULT, ResponseStatus.SUCCESS, 200)
                    );
                } else {
                    return Observable.just(
                        new SearchQueryResponse(FAIL_RESULT, ResponseStatus.FAILURE, 429)
                    );
                }
            }
        });

        SearchQueryExecutor executor = new SearchQueryExecutor(ENV, core, "bucket", "user", "pass");
        AsyncSearchQueryResult result = executor.execute(query, 5, TimeUnit.SECONDS).toBlocking().single();

        assertTrue(result.status().isSuccess());
        verify(core, times(6)).send(any(SearchQueryRequest.class));
    }

    /**
     * Same as {@link #shouldRetryResponseTooManyRequestsAndComplete()} but it doesn't complete
     * eventually so the error must be propagated.
     */
    @Test(expected = FtsServerOverloadException.class)
    public void shouldRetryResponseTooManyRequestsAndPropagate() {
        CouchbaseCore core = mock(CouchbaseCore.class);
        SearchQuery query = new SearchQuery("index", new QueryStringQuery("query"));

        when(core.send(any(SearchQueryRequest.class))).thenAnswer(new Answer<Observable<SearchQueryResponse>>() {
            @Override
            public Observable<SearchQueryResponse> answer(InvocationOnMock invocation) {
                return Observable.just(
                        new SearchQueryResponse(FAIL_RESULT, ResponseStatus.FAILURE, 429)
                );
            }
        });

        SearchQueryExecutor executor = new SearchQueryExecutor(ENV, core, "bucket", "user", "pass");
        AsyncSearchQueryResult result = executor.execute(query, 5, TimeUnit.SECONDS).toBlocking().single();

        assertFalse(result.status().isSuccess());
        result.hits().toBlocking().single();
    }
}