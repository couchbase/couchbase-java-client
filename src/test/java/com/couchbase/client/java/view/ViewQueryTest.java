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
package com.couchbase.client.java.view;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.core.message.view.ViewQueryResponse;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.SerializationHelper;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Verifies the correct functionality of the {@link ViewQuery} DSL.
 *
 * @author Michael Nitschinger
 * @since 2.0
 */
public class ViewQueryTest {

    @Test
    public void shouldSetDefaults() {
        ViewQuery query = ViewQuery.from("design", "view");
        assertEquals("design", query.getDesign());
        assertEquals("view", query.getView());
        assertFalse(query.isDevelopment());
        assertEquals("", query.toQueryString());
        assertEquals("ViewQuery(design/view){params=\"\"}", query.toString());
    }

    @Test
    public void shouldReduce() {
        ViewQuery query = ViewQuery.from("design", "view").reduce();
        assertEquals("reduce=true", query.toQueryString());

        query = ViewQuery.from("design", "view").reduce(true);
        assertEquals("reduce=true", query.toQueryString());

        query = ViewQuery.from("design", "view").reduce(false);
        assertEquals("reduce=false", query.toQueryString());
    }

    @Test
    public void shouldLimit() {
        ViewQuery query = ViewQuery.from("design", "view").limit(10);
        assertEquals("limit=10", query.toQueryString());
    }

    @Test
    public void shouldSkip() {
        ViewQuery query = ViewQuery.from("design", "view").skip(3);
        assertEquals("skip=3", query.toQueryString());
    }

    @Test
    public void shouldGroup() {
        ViewQuery query = ViewQuery.from("design", "view").group();
        assertEquals("group=true", query.toQueryString());

        query = ViewQuery.from("design", "view").group(false);
        assertEquals("group=false", query.toQueryString());
    }

    @Test
    public void shouldGroupLevel() {
        ViewQuery query = ViewQuery.from("design", "view").groupLevel(2);
        assertEquals("group_level=2", query.toQueryString());
    }

    @Test
    public void shouldSetInclusiveEnd() {
        ViewQuery query = ViewQuery.from("design", "view").inclusiveEnd();
        assertEquals("inclusive_end=true", query.toQueryString());

        query = ViewQuery.from("design", "view").inclusiveEnd(false);
        assertEquals("inclusive_end=false", query.toQueryString());
    }

    @Test
    public void shouldSetStale() {
        ViewQuery query = ViewQuery.from("design", "view").stale(Stale.FALSE);
        assertEquals("stale=false", query.toQueryString());

        query = ViewQuery.from("design", "view").stale(Stale.TRUE);
        assertEquals("stale=ok", query.toQueryString());

        query = ViewQuery.from("design", "view").stale(Stale.UPDATE_AFTER);
        assertEquals("stale=update_after", query.toQueryString());
    }

    @Test
    public void shouldSetOnError() {
        ViewQuery query = ViewQuery.from("design", "view").onError(OnError.CONTINUE);
        assertEquals("on_error=continue", query.toQueryString());

        query = ViewQuery.from("design", "view").onError(OnError.STOP);
        assertEquals("on_error=stop", query.toQueryString());

    }

    @Test
    public void shouldSetDebug() {
        ViewQuery query = ViewQuery.from("design", "view").debug();
        assertEquals("debug=true", query.toQueryString());

        query = ViewQuery.from("design", "view").debug(false);
        assertEquals("debug=false", query.toQueryString());
    }

    @Test
    public void shouldSetDescending() {
        ViewQuery query = ViewQuery.from("design", "view").descending();
        assertEquals("descending=true", query.toQueryString());

        query = ViewQuery.from("design", "view").descending(false);
        assertEquals("descending=false", query.toQueryString());
    }

    @Test
    public void shouldHandleKey() {
        ViewQuery query = ViewQuery.from("design", "view").key("key");
        assertEquals("key=%22key%22", query.toQueryString());

        query = ViewQuery.from("design", "view").key(1);
        assertEquals("key=1", query.toQueryString());

        query = ViewQuery.from("design", "view").key(true);
        assertEquals("key=true", query.toQueryString());

        query = ViewQuery.from("design", "view").key(3.55);
        assertEquals("key=3.55", query.toQueryString());

        query = ViewQuery.from("design", "view").key(JsonArray.from("foo", 3));
        assertEquals("key=%5B%22foo%22%2C3%5D", query.toQueryString());

        query = ViewQuery.from("design", "view").key(JsonObject.empty().put("foo", true));
        assertEquals("key=%7B%22foo%22%3Atrue%7D", query.toQueryString());
    }

    @Test
    public void shouldHandleKeys() {
        JsonArray keysArray = JsonArray.from("foo", 3, true);
        ViewQuery query = ViewQuery.from("design", "view").keys(keysArray);
        assertEquals("", query.toQueryString());
        assertEquals(keysArray.toString(), query.getKeys());
    }

    @Test
    public void shouldOutputSmallKeysInToString() {
        JsonArray keysArray = JsonArray.from("foo", 3, true);
        ViewQuery query = ViewQuery.from("design", "view").keys(keysArray);
        assertEquals("", query.toQueryString());
        assertEquals("ViewQuery(design/view){params=\"\", keys=\"[\"foo\",3,true]\"}", query.toString());
    }

    @Test
    public void shouldTruncateLargeKeysInToString() {
        StringBuilder largeString = new StringBuilder(142);
        for (int i = 0; i < 140; i++) {
            largeString.append('a');
        }
        largeString.append("bc");
        JsonArray keysArray = JsonArray.from(largeString.toString());
        ViewQuery query = ViewQuery.from("design", "view").keys(keysArray);
        assertEquals("", query.toQueryString());
        assertEquals("ViewQuery(design/view){params=\"\", keys=\"[\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                "aaaaaaaaaaaaaaaaaaaaaaaaaaa...\"(146 chars total)}", query.toString());
    }

    @Test
    public void shouldOutputDesignDocViewDevAndIncludeDocsInToString() {
        ViewQuery query = ViewQuery.from("a", "b").includeDocs().development();
        assertEquals("", query.toQueryString());
        assertEquals("ViewQuery(a/b){params=\"\", dev, includeDocs}", query.toString());
    }

    @Test
    public void shouldHandleStartKey() {
        ViewQuery query = ViewQuery.from("design", "view").startKey("key");
        assertEquals("startkey=%22key%22", query.toQueryString());

        query = ViewQuery.from("design", "view").startKey(1);
        assertEquals("startkey=1", query.toQueryString());

        query = ViewQuery.from("design", "view").startKey(true);
        assertEquals("startkey=true", query.toQueryString());

        query = ViewQuery.from("design", "view").startKey(3.55);
        assertEquals("startkey=3.55", query.toQueryString());

        query = ViewQuery.from("design", "view").startKey(JsonArray.from("foo", 3));
        assertEquals("startkey=%5B%22foo%22%2C3%5D", query.toQueryString());

        query = ViewQuery.from("design", "view").startKey(JsonObject.empty().put("foo", true));
        assertEquals("startkey=%7B%22foo%22%3Atrue%7D", query.toQueryString());
    }

    @Test
    public void shouldHandleEndKey() {
        ViewQuery query = ViewQuery.from("design", "view").endKey("key");
        assertEquals("endkey=%22key%22", query.toQueryString());

        query = ViewQuery.from("design", "view").endKey(1);
        assertEquals("endkey=1", query.toQueryString());

        query = ViewQuery.from("design", "view").endKey(true);
        assertEquals("endkey=true", query.toQueryString());

        query = ViewQuery.from("design", "view").endKey(3.55);
        assertEquals("endkey=3.55", query.toQueryString());

        query = ViewQuery.from("design", "view").endKey(JsonArray.from("foo", 3));
        assertEquals("endkey=%5B%22foo%22%2C3%5D", query.toQueryString());

        query = ViewQuery.from("design", "view").endKey(JsonObject.empty().put("foo", true));
        assertEquals("endkey=%7B%22foo%22%3Atrue%7D", query.toQueryString());
    }

    @Test
    public void shouldHandleStartKeyDocID() {
        ViewQuery query = ViewQuery.from("design", "view").startKeyDocId("mykey");
        assertEquals("startkey_docid=mykey", query.toQueryString());
    }

    @Test
    public void shouldHandleEndKeyDocID() {
        ViewQuery query = ViewQuery.from("design", "view").endKeyDocId("mykey");
        assertEquals("endkey_docid=mykey", query.toQueryString());
    }

    @Test
    public void shouldRespectDevelopmentParam() {
        ViewQuery query = ViewQuery.from("design", "view").development(true);
        assertTrue(query.isDevelopment());

        query = ViewQuery.from("design", "view").development(false);
        assertFalse(query.isDevelopment());
    }

    @Test
    public void shouldConcatMoreParams() {
        ViewQuery query = ViewQuery.from("design", "view")
            .descending()
            .debug()
            .development()
            .group()
            .reduce(false)
            .startKey(JsonArray.from("foo", true));
        assertEquals("reduce=false&group=true&debug=true&descending=true&startkey=%5B%22foo%22%2Ctrue%5D",
                query.toQueryString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeLimit() {
        ViewQuery.from("design", "view").limit(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeSkip() {
        ViewQuery.from("design", "view").skip(-1);
    }

    @Test
    public void shouldToggleDevelopment() {
        ViewQuery query = ViewQuery.from("design", "view").development(true);
        assertTrue(query.isDevelopment());

        query = ViewQuery.from("design", "view").development(false);
        assertFalse(query.isDevelopment());
    }

    @Test
    public void shouldSupportSerialization() throws Exception {
        ViewQuery query = ViewQuery.from("design", "view")
            .descending()
            .debug()
            .development()
            .group()
            .reduce(false)
            .keys(JsonArray.from("1", "2"))
            .startKey(JsonArray.from("foo", true));

        byte[] serialized = SerializationHelper.serializeToBytes(query);
        assertNotNull(serialized);

        ViewQuery deserialized = SerializationHelper.deserializeFromBytes(serialized, ViewQuery.class);
        assertEquals(query, deserialized);
    }

    @Test
    public void shouldIncludeDocs() {
        ViewQuery query = ViewQuery.from("design", "view").includeDocs();
        assertTrue(query.isIncludeDocs());
        assertEquals(JsonDocument.class, query.includeDocsTarget());

        query = ViewQuery.from("design", "view").includeDocs(JsonDocument.class);
        assertTrue(query.isIncludeDocs());
        assertEquals(JsonDocument.class, query.includeDocsTarget());

        query = ViewQuery.from("design", "view");
        assertFalse(query.isIncludeDocs());
        assertNull(query.includeDocsTarget());

        query = ViewQuery.from("design", "view").includeDocs(false, RawJsonDocument.class);
        assertFalse(query.isIncludeDocs());
        assertEquals(RawJsonDocument.class, query.includeDocsTarget());
    }

    @Test
    public void shouldStoreKeysAsJsonOutsideParams() {
        JsonArray keys = JsonArray.create().add("1").add("2").add("3");
        String keysJson = keys.toString();
        ViewQuery query = ViewQuery.from("design", "view");
        assertNull(query.getKeys());

        query.keys(keys);
        assertEquals(keysJson, query.getKeys());
        assertFalse(query.toQueryString().contains("keys="));
        assertFalse(query.toQueryString().contains("3"));
    }

    @Test
    public void shouldFlagOrderRetainedWhenUsingIncludeDocsOrdered() {
        ViewQuery query1 = ViewQuery.from("a", "b").includeDocsOrdered();
        ViewQuery query2 = ViewQuery.from("a", "b").includeDocsOrdered(true);
        ViewQuery query3 = ViewQuery.from("a", "b").includeDocsOrdered(JsonDocument.class);
        ViewQuery query4 = ViewQuery.from("a", "b").includeDocsOrdered(true, JsonDocument.class);

        assertEquals(true, query1.isOrderRetained());
        assertEquals(true, query2.isOrderRetained());
        assertEquals(true, query3.isOrderRetained());
        assertEquals(true, query4.isOrderRetained());
    }


    @Test
    public void shouldDeactivateOrderRetainedWhenSettingIncludeDocsOrderedToFalse() {
        ViewQuery query1 = ViewQuery.from("a", "b").includeDocsOrdered();
        assertEquals(true, query1.isOrderRetained());
        query1.includeDocsOrdered(false);
        assertEquals(false, query1.isOrderRetained());

        ViewQuery query2 = ViewQuery.from("a", "b").includeDocsOrdered(true, JsonDocument.class);
        assertEquals(true, query2.isOrderRetained());
        query2.includeDocsOrdered(false, JsonDocument.class);
        assertEquals(false, query2.isOrderRetained());
    }


    @Test
    public void shouldLoadDocumentsOutOfOrderWithIncludeDocs() {
        StringBuilder trace = new StringBuilder();
        Bucket bucket = mockDelayedBucket(2, trace, "A", "B", "C", "D");
        ViewResult result = bucket.query(ViewQuery.from("any", "view")
                .includeDocs());

        //to assert reception is out of order
        String[] expected = new String[]{"C", "D", "A", "B"};
        //to assert requests are in order, emissions are out of order (A and B delayed)
        String expectedTrace = "\nGET A\nGET B\nGET C\nGot C\nGET D\nGot D\nDelayed A by 100ms\nGot A\nDelayed B by 200ms\nGot B";

        assertOrder(expected, expectedTrace, result.allRows(), trace.toString());
    }

    @Test
    public void shouldLoadDocumentsInOrderWithIncludeDocsOrdered() {
        StringBuilder trace = new StringBuilder();
        Bucket bucket = mockDelayedBucket(2, trace, "A", "B", "C", "D");
        ViewResult result = bucket.query(ViewQuery.from("any", "view")
                .includeDocsOrdered());

        //to assert reception is in order
        String[] expectedIds = new String[]{"A", "B", "C", "D"};
        //to assert requests are in order and emissions are out of order (A and B delayed)
        String expectedTrace = "\nGET A\nGET B\nGET C\nGot C\nGET D\nGot D\nDelayed A by 100ms\nGot A\nDelayed B by 200ms\nGot B";
        assertOrder(expectedIds, expectedTrace, result.allRows(), trace.toString());
    }

    private void assertOrder(String[] expectedIds, String expectedTrace, List<ViewRow> rows, String trace) {
        for (int i = 0; i < rows.size(); i++) {
            ViewRow row = rows.get(i);
            assertNotNull(row);
            JsonDocument doc = row.document();
            assertEquals(row.id(), doc.id());
            assertEquals(expectedIds[i], row.id());
        }

        assertEquals(expectedTrace, trace);
    }

    private Bucket mockDelayedBucket(final int numberDelayed, final StringBuilder trace, final String... keys) {
        final Set<String> delayed = new HashSet<String>(numberDelayed);
        delayed.addAll(Arrays.asList(keys).subList(0, numberDelayed));

        List<ByteBuf> fakeRows = new ArrayList<ByteBuf>(keys.length);
        for (String key : keys) {
            String fakeRowJson = JsonObject.create()
                    .put("id", key)
                    .toString();
            ByteBuf fakeBuffer = Unpooled.copiedBuffer(fakeRowJson, CharsetUtil.UTF_8);
            fakeRows.add(fakeBuffer);
        }
        final Observable fakeRowObs = Observable.from(fakeRows);
        final AtomicInteger delay = new AtomicInteger(100);

        final AsyncBucket spyBucket = Mockito.mock(AsyncBucket.class);

        //this will induce a delay on the first n keys when includeDocs' get is triggered, and trace the invocations
        when(spyBucket.get(Matchers.anyString(), any(Class.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String key = (String) invocation.getArguments()[0];
                Observable<JsonDocument> obs = Observable.just(JsonDocument.create(key))
                        .doOnNext(new Action1<JsonDocument>() {
                            @Override
                            public void call(JsonDocument jsonDocument) {
                                trace.append("\nGET ").append(jsonDocument.id());
                            }
                        });
                if (delayed.contains(key)) {
                    final int d = delay.getAndAdd(100);
                    obs = obs.delay(d, TimeUnit.MILLISECONDS)
                            .doOnNext(new Action1<JsonDocument>() {
                                @Override
                                public void call(JsonDocument jsonDocument) {
                                    trace.append("\nDelayed ").append(jsonDocument.id()).append(" by ").append(d).append("ms");
                                }
                            });
                }
                return obs.doOnNext(new Action1<JsonDocument>() {
                    @Override
                    public void call(JsonDocument jsonDocument) {
                        trace.append("\nGot ").append(jsonDocument.id());
                    }
                });
            }
        });

        //this simulates a view response with the preconstructed buffers above, and calls the view result
        //mapper so that it uses the mock get for its includeDocs calls.
        when(spyBucket.query(any(ViewQuery.class), any(Long.class), any(TimeUnit.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final ViewQuery query = (ViewQuery) invocation.getArguments()[0];

                ViewQueryResponse response = new ViewQueryResponse(fakeRowObs, Observable.<ByteBuf>empty(),
                        Observable.<String>empty(), 0, "", ResponseStatus.SUCCESS, null);

                return Observable.just(response)
                        .flatMap(new Func1<ViewQueryResponse, Observable<AsyncViewResult>>() {
                            @Override
                            public Observable<AsyncViewResult> call(final ViewQueryResponse response) {
                                return ViewQueryResponseMapper.mapToViewResult(spyBucket, query, response);
                            }
                        });
            }
        });
        return new CouchbaseBucket(spyBucket, DefaultCouchbaseEnvironment.create(), null, "", "", "");
    }
}
