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
package com.couchbase.client.java;

import static com.couchbase.client.java.search.facet.SearchFacet.date;
import static com.couchbase.client.java.search.facet.SearchFacet.numeric;
import static com.couchbase.client.java.search.facet.SearchFacet.term;
import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeFalse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.message.kv.subdoc.multi.Mutation;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.FtsConsistencyTimeoutException;
import com.couchbase.client.java.search.HighlightStyle;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.facet.SearchFacet;
import com.couchbase.client.java.search.queries.AbstractFtsQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.search.result.facets.DateRange;
import com.couchbase.client.java.search.result.facets.DateRangeFacetResult;
import com.couchbase.client.java.search.result.facets.FacetResult;
import com.couchbase.client.java.search.result.facets.NumericRange;
import com.couchbase.client.java.search.result.facets.NumericRangeFacetResult;
import com.couchbase.client.java.search.result.facets.TermFacetResult;
import com.couchbase.client.java.search.result.facets.TermRange;
import com.couchbase.client.java.subdoc.DocumentFragment;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import rx.exceptions.CompositeException;

/**
 * Integration tests of the Search Query / FTS features.
 *
 * The FTS index "beer-search" must be created on the server with the following mapping for the type "beer":
 *
 *  - category | text | index | store | include in _all field | include term vectors
 *  - style | text | index | store | include in _all field | include term vectors
 *  - abv | number | index | include in _all field
 *  - updated | datetime | index | include in _all field
 *  - description | text | index | store | include in _all field | include term vectors
 *  - name | text | index | store | include in _all field | include term vectors
 *
 * @author Simon Baslé
 * @since 2.3
 */
public class SearchQueryTest {

    private static CouchbaseTestContext ctx;
    private static final String INDEX = "beer-search";

    @BeforeClass
    public static void init() throws InterruptedException {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
                .withEnv(DefaultCouchbaseEnvironment.builder()
                        .mutationTokensEnabled(true))
                .bucketName("beer-sample")
                .flushOnInit(false)
                .adhoc(false)
                .build()
                .ignoreIfMissing(CouchbaseFeature.FTS_BETA)
                .ignoreIfSearchServiceNotFound()
                .ignoreIfSearchIndexDoesNotExist("beer-search");
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void demoBeerSearch() {
        //prepare a compound query
        AbstractFtsQuery cq = SearchQuery.disjuncts(
                //either hop in the name (higher score)
                SearchQuery.match("hop").field("name").boost(3.4),
                //OR something like hop in the description (base score)
                SearchQuery.match("hop").field("description").fuzziness(1)
        );

        //set index "beer-search" and parameters for the whole request
        SearchQuery query = new SearchQuery("beer-search", cq)
                //will include name & desc fragments
                .highlight(HighlightStyle.HTML, "name", "description")
                //will have max 3 hits
                .limit(3)
                //will have a "strength" facet on the abv with 2 strength categories
                .addFacet("strength", SearchFacet.numeric("abv", 2)
                        .addRange("light", null, 5.0)
                        .addRange("strong", 5.0, null));

        //execute the FTS search on the "beer-search" index
        SearchQueryResult result = ctx.bucket().query(query);

        //prints the hits
        for (SearchQueryRow hit : result.hitsOrFail()) {
            System.out.println("\nHIT ON " + hit.id());
            System.out.println(hit.fragments());
        }

        //prints the facet
        System.out.println("\nSTRENGTH DISTRIBUTION OF ALL BEERS:");
        NumericRangeFacetResult facet = (NumericRangeFacetResult) result.facets().get("strength");
        for (NumericRange range : facet.numericRanges()) {
            double min = range.min() == null ? 0d : range.min();
            double max = range.max() == null ? 0d : range.max();
            System.out.println(range.name() + " (abv between " + min + " and " + max + "):");
            System.out.println(range.count() + " beers");
        }
    }

    /*
     Index travel-search:
        content | text | index | include in _all field | include term vectors
        name | text | index | store | include in _all field | include term vectors
        country | text | index | store | include in _all field | include term vectors
        activity | text | index | store | include in _all field | include term vectors
     */
    @Test
    @Ignore("Only beer-search is required for SearchQueryTest")
    public void demoTravelSearch() {
        //use the travel sample bucket
        Bucket bucket = ctx.cluster().openBucket("travel-sample");

        //prepare a compound query
        AbstractFtsQuery cq =
                SearchQuery.disjuncts(
                        //either something like "schnitzle" in the content (higher boosted score)
                        SearchQuery.match("schnitzle").field("content").fuzziness(2).boost(4),
                        //OR some form of "fast food" (base score)
                        SearchQuery.matchPhrase("fast food").field("content")
                );

        //set index "travel-search" and parameters for the whole request
        SearchQuery query = new SearchQuery("travel-search", cq)
                //will show value for activity and country fields
                .fields("activity", "country")
                //will include name & content fragments
                .highlight(HighlightStyle.HTML, "name", "content")
                //will have max 3 hits
                .limit(3)
                //will have a "countries" facet on the top 5 countries having landmarks
                .addFacet("countries", SearchFacet.term("country", 5));

        //execute the FTS search on the "travel-search" index
        SearchQueryResult result = bucket.query(query);

        //prints the hits
        for (SearchQueryRow hit : result.hitsOrFail()) {
            System.out.println("\nHIT ON " + hit.id() + ", score = " + hit.score());
            System.out.println(hit.fields());
            System.out.println(hit.fragments());
        }

        //prints the facet
        TermFacetResult facet = (TermFacetResult) result.facets().get("countries");
        System.out.println("\nCOUNTRY DISTRIBUTION (total " + result.metrics().totalHits() + "):");
        for (TermRange range : facet.terms()) {
            System.out.println(range.name() + " (" + range.count() + ")");
        }
    }

    @Test
    public void shouldSearchWithLimit() {
        AbstractFtsQuery fts = SearchQuery.matchPhrase("hop beer");
        SearchQuery query = new SearchQuery(INDEX, fts)
                .limit(3);

        SearchQueryResult result = ctx.bucket().query(query);

        assertThat(result).as("result").isNotNull();
        assertThat(result.metrics()).as("metrics").isNotNull();
        assertThat(result.metrics().totalHits()).as("totalHits").isLessThanOrEqualTo(3L);
        assertThat(result.hits()).as("hits").isNotEmpty();
        assertThat(result.hitsOrFail()).as("hitsOrFail").isNotEmpty();
        assertThat(result.hits()).as("hits == hitsOrFail").isEqualTo(result.hitsOrFail());
        assertThat(result.hits().size()).as("hits size").isEqualTo((int) result.metrics().totalHits());
        assertThat(result.errors()).as("errors").isEmpty();

        for (SearchQueryRow row : result.hits()) {
            assertThat(row.id()).as("row id").isNotNull();
            assertThat(row.index()).as("row index").startsWith(INDEX);
            assertThat(row.score()).as("row score").isGreaterThan(0d);
            assertThat(row.explanation()).as("row explanation").isEqualTo(JsonObject.empty());
            assertThat(row.fields()).as("row fields").isEmpty();
            assertThat(row.fragments()).as("row fragments").isEmpty();
        }
    }

    @Test
    public void shouldSearchWithNoHits() {
        AbstractFtsQuery fts = SearchQuery.matchPhrase("noabfaobf nnda");
        SearchQuery query = new SearchQuery(INDEX, fts)
                .limit(3);

        SearchQueryResult result = ctx.bucket().query(query);

        assertThat(result).as("result").isNotNull();
        assertThat(result.metrics()).as("metrics").isNotNull();
        assertThat(result.hits()).as("hits").isEmpty();
        assertThat(result.hitsOrFail()).as("hitsOrFail").isEmpty();
        assertThat(result.hits()).as("hits == hitsOrFail").isEqualTo(result.hitsOrFail());
        assertThat(result.hits().size()).as("hits size").isEqualTo((int) result.metrics().totalHits());
        assertThat(result.metrics().totalHits()).as("totalHits").isEqualTo(0L);
        assertThat(result.errors()).as("errors").isEmpty();
    }

    @Test
    public void shouldSearchWithFields() {
        AbstractFtsQuery fts = SearchQuery.matchPhrase("hop beer");
        SearchQuery query = new SearchQuery(INDEX, fts)
                .limit(3)
                .fields("name");

        SearchQueryResult result = ctx.bucket().query(query);

        for (SearchQueryRow row : result.hits()) {
            final Map<String, String> fields = row.fields();
            assertThat(fields).as("row fields size").hasSize(1);
            assertThat(fields).as("row field name").containsKey("name");
            assertThat(fields).as("row field empty name").doesNotContainEntry("name", "");
            assertThat(fields).as("row field null name").doesNotContainEntry("name", null);

            //sanity checks
            assertThat(row.id()).as("row id").isNotNull();
            assertThat(row.index()).as("row index").startsWith(INDEX);
            assertThat(row.score()).as("row score").isGreaterThan(0d);
            assertThat(row.explanation()).as("row explanation").isEqualTo(JsonObject.empty());
            assertThat(row.fragments()).as("row fragments").isEmpty();
        }
        //top level sanity checks
        assertThat(result).as("result").isNotNull();
        assertThat(result.metrics()).as("metrics").isNotNull();
        assertThat(result.metrics().totalHits()).as("totalHits").isLessThanOrEqualTo(3L);
        assertThat(result.hits()).as("hits").isNotEmpty();
        assertThat(result.hitsOrFail()).as("hitsOrFail").isNotEmpty();
        assertThat(result.hits()).as("hits == hitsOrFail").isEqualTo(result.hitsOrFail());
        assertThat(result.hits().size()).as("hits size").isEqualTo((int) result.metrics().totalHits());
        assertThat(result.errors()).as("errors").isEmpty();
    }

    @Test
    public void shouldSearchWithFragments() {
        AbstractFtsQuery fts = SearchQuery.matchPhrase("hop beer");
        SearchQuery query = new SearchQuery(INDEX, fts)
                .limit(3)
                .highlight(HighlightStyle.HTML, "name");

        SearchQueryResult result = ctx.bucket().query(query);

        for (SearchQueryRow row : result.hits()) {
            assertThat(row.fragments()).as("row fragments").isNotEmpty();

            //sanity checks
            assertThat(row.id()).as("row id").isNotNull();
            assertThat(row.index()).as("row index").startsWith(INDEX);
            assertThat(row.score()).as("row score").isGreaterThan(0d);
            assertThat(row.explanation()).as("row explanation").isEqualTo(JsonObject.empty());
            assertThat(row.fields()).as("row fields").isEmpty();
        }
        //top level sanity checks
        assertThat(result).as("result").isNotNull();
        assertThat(result.metrics()).as("metrics").isNotNull();
        assertThat(result.metrics().totalHits()).as("totalHits").isLessThanOrEqualTo(3L);
        assertThat(result.hits()).as("hits").isNotEmpty();
        assertThat(result.hitsOrFail()).as("hitsOrFail").isNotEmpty();
        assertThat(result.hits()).as("hits == hitsOrFail").isEqualTo(result.hitsOrFail());
        assertThat(result.hits().size()).as("hits size").isEqualTo((int) result.metrics().totalHits());
        assertThat(result.errors()).as("errors").isEmpty();
    }

    @Test
    public void shouldSearchWithExplanation() {
        AbstractFtsQuery fts = SearchQuery.matchPhrase("hop beer");
        SearchQuery query = new SearchQuery(INDEX, fts)
                .limit(3)
                .explain();

        SearchQueryResult result = ctx.bucket().query(query);
        System.out.println(query.export());

        for (SearchQueryRow row : result.hits()) {
            assertThat(row.explanation()).isNotEqualTo(JsonObject.empty());

            //sanity checks
            assertThat(row.id()).as("row id").isNotNull();
            assertThat(row.index()).as("row index").startsWith(INDEX);
            assertThat(row.score()).as("row score").isGreaterThan(0d);
            assertThat(row.fragments()).as("row fragments").isEmpty();
            assertThat(row.fields()).as("row fields").isEmpty();
        }
        //top level sanity checks
        assertThat(result).as("result").isNotNull();
        assertThat(result.metrics()).as("metrics").isNotNull();
        assertThat(result.metrics().totalHits()).as("totalHits").isLessThanOrEqualTo(3L);
        assertThat(result.hits()).as("hits").isNotEmpty();
        assertThat(result.hitsOrFail()).as("hitsOrFail").isNotEmpty();
        assertThat(result.hits()).as("hits == hitsOrFail").isEqualTo(result.hitsOrFail());
        assertThat(result.hits().size()).as("hits size").isEqualTo((int) result.metrics().totalHits());
        assertThat(result.errors()).as("errors").isEmpty();
    }

    @Test
    public void shouldSearchWithFacets() {
        AbstractFtsQuery fts = SearchQuery.match("beer");
        SearchQuery query = new SearchQuery(INDEX, fts)
                .addFacet("foo", term("name", 3))
                .addFacet("bar", date("updated", 1).addRange("old", null, "2014-01-01T00:00:00"))
                .addFacet("baz", numeric("abv", 2).addRange("strong", 4.9, null).addRange("light", null, 4.89));

        SearchQueryResult result = ctx.bucket().query(query);

        System.out.println(query.export());
        System.out.println(result.facets());

        FacetResult f = result.facets().get("foo");
        assertThat(f).as("foo facet result").isInstanceOf(TermFacetResult.class);
        TermFacetResult foo = (TermFacetResult) f;
        assertThat(foo.name()).as("foo name").isEqualTo("foo");
        assertThat(foo.field()).as("foo field").isEqualTo("name");
        assertThat(foo.terms()).as("foo terms").hasSize(3);
        int totalFound = 0;
        for (TermRange range : foo.terms()) {
            totalFound += range.count();
            assertThat(range.count()).as("term count").isGreaterThan(0L);
        }
        assertThat(foo.total()).as("foo total == terms + other").isEqualTo(totalFound + foo.other());

        f = result.facets().get("bar");
        assertThat(f).as("bar facet result").isInstanceOf(DateRangeFacetResult.class);
        DateRangeFacetResult bar = (DateRangeFacetResult) f;
        assertThat(bar.name()).as("bar name").isEqualTo("bar");
        assertThat(bar.field()).as("bar field").isEqualTo("updated");
        assertThat(bar.dateRanges()).as("bar ranges").hasSize(1);
        totalFound = 0;
        for (DateRange range : bar.dateRanges()) {
            totalFound += range.count();
            assertThat(range.count()).as("bar range count").isGreaterThan(0L);
            assertThat(range.name()).as("bar range name").isEqualTo("old");
        }
        assertThat(bar.total()).as("bar total == ranges + other").isEqualTo(totalFound + bar.other());

        f = result.facets().get("baz");
        assertThat(f).as("baz").isInstanceOf(NumericRangeFacetResult.class);
        NumericRangeFacetResult baz = (NumericRangeFacetResult) f;
        assertThat(baz.name()).as("baz name").isEqualTo("baz");
        assertThat(baz.field()).as("baz field").isEqualTo("abv");
        assertThat(baz.numericRanges()).as("baz ranges").hasSize(2);
        totalFound = 0;
        for (NumericRange range : baz.numericRanges()) {
            totalFound += range.count();
            assertThat(range.count()).as("baz range count").isGreaterThan(0);
            assertThat(range.name()).as("baz range name").isIn("light", "strong");
        }
        assertThat(baz.total()).as("baz total == ranges + other").isEqualTo(totalFound + baz.other());
    }

    @Test
    public void shouldSetServerSideTimeoutInParamsBeforeExecuting() {
        AbstractFtsQuery fts = SearchQuery.matchPhrase("salty beer");
        SearchQuery query = new SearchQuery(INDEX, fts);

        SearchQueryResult result = ctx.bucket().query(query);

        assertThat(result.status().isSuccess()).isTrue();
        assertThat(query.getServerSideTimeout()).isNotNull();
        assertThat(query.getServerSideTimeout()).isEqualTo(ctx.env().searchTimeout());
    }

    @Test
    public void shouldAcceptAtPlusConsistencyVector() {
        String key = "21st_amendment_brewery_cafe-21a_ipa";
        String category = "North American Ale";

        SearchQueryResult result = null;
        try {
            //this test essentially validates that the query service accepts the tokens
            //but we'll still attempt to throw the indexer off by doing a first dummy mutation
            ctx.bucket().mutateIn(key)
                    .replace("category", "batman")
                    .execute();

            //this is the mutation we want to be consistent with
            DocumentFragment<Mutation> mutation = ctx
                    .bucket().mutateIn(key)
                    .replace("category", "superman")
                    .execute();

            SearchQuery query = new SearchQuery("beer-search",
                    SearchQuery.match("superman").field("category"))
                    .consistentWith(mutation)
                    .limit(3);
            result = ctx.bucket().query(query);
        } finally {
            //restore old values
            ctx.bucket().mutateIn(key).replace("category", category).execute();
        }

        assertThat(result).isNotNull();
        assertThat(result.hits()).hasSize(1);
        assertThat(result.hits().get(0).id()).isEqualToIgnoringCase(key);
    }

    @Test
    @Ignore("FTS 412 error case is hard to trigger, test too brittle")
    public void shouldThrowFtsConsistencyTimeoutException() {
        //FIXME WARNING test is brittle, won't cause the expected error all the time on server side :(

        //artificially generate and get a mutation token by identity-upserting a doc
        Document d = ctx.bucket().get("21st_amendment_brewery_cafe-21a_ipa");
        d = ctx.bucket().upsert(d);
        AbstractFtsQuery fts = SearchQuery.match("beer");
        //trigger 412 by setting a consistency and a very fast timeout
        SearchQuery query = new SearchQuery(INDEX, fts)
                .consistentWith(d)
                .serverSideTimeout(1, TimeUnit.MILLISECONDS);

        SearchQueryResult result = ctx.bucket().query(query);

        assertThat(result.status().isSuccess()).isFalse();
        catchException(result).hitsOrFail();
        assertThat(caughtException()).isInstanceOf(FtsConsistencyTimeoutException.class);
    }

    @Test
    public void shouldThrowCouchbaseExceptionWhenTimeout() {
        AbstractFtsQuery fts = SearchQuery.match("beer");
        //set a very fast timeout
        SearchQuery query = new SearchQuery(INDEX, fts)
                .serverSideTimeout(3, TimeUnit.MILLISECONDS);

        SearchQueryResult result = ctx.bucket().query(query);

        assertThat(result.status().isSuccess()).isFalse();
        catchException(result).hitsOrFail();
        assertThat(caughtException())
                .isInstanceOf(CompositeException.class);
        CompositeException compositeException = caughtException();
        Throwable inner = compositeException.getExceptions().get(0);
        assertThat(inner).hasMessageContaining("context deadline exceeded");
    }

}
