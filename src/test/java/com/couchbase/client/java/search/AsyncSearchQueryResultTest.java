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
package com.couchbase.client.java.search;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.data.MapEntry.entry;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.result.AsyncSearchQueryResult;
import com.couchbase.client.java.search.result.SearchMetrics;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.search.result.SearchStatus;
import com.couchbase.client.java.search.result.facets.DateRange;
import com.couchbase.client.java.search.result.facets.DateRangeFacetResult;
import com.couchbase.client.java.search.result.facets.FacetResult;
import com.couchbase.client.java.search.result.facets.NumericRange;
import com.couchbase.client.java.search.result.facets.NumericRangeFacetResult;
import com.couchbase.client.java.search.result.facets.TermFacetResult;
import com.couchbase.client.java.search.result.facets.TermRange;
import com.couchbase.client.java.search.result.hits.HitLocation;
import com.couchbase.client.java.search.result.hits.HitLocations;
import com.couchbase.client.java.search.result.impl.DefaultAsyncSearchQueryResult;
import org.junit.Test;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

public class AsyncSearchQueryResultTest {

    @Test
    public void testJsonConversionSuccessResponse() {
        InputStream stream = AsyncSearchQueryResultTest.class.getResourceAsStream("/data/fts/success_response.json");
        java.util.Scanner s = new java.util.Scanner(stream).useDelimiter("\\A");
        String response = s.next();
        s.close();
        JsonObject json = JsonObject.fromJson(response);

        AsyncSearchQueryResult result = DefaultAsyncSearchQueryResult.fromJson(json);
        assertThat(result).isNotNull();

        SearchStatus status = result.status();
        Map<String, FacetResult> facets = result.facets()
                .toMap(new Func1<FacetResult, String>() {
                    @Override
                    public String call(FacetResult facetResult) {
                        return facetResult.name();
                    }
                })
                .toBlocking().singleOrDefault(null);
        SearchMetrics metrics = result.metrics().toBlocking().singleOrDefault(null);
        List<SearchQueryRow> hits = result.hits().toList().toBlocking().singleOrDefault(null);

        assertSuccessResponse(status, hits, facets, metrics);
    }

    public static void assertSuccessResponse(SearchStatus status, List<SearchQueryRow> hits,
            Map<String, FacetResult> facets, SearchMetrics metrics) {
        JsonObject expectedExplanation = JsonObject.create().put("fake", true);

        assertThat(status).isNotNull();
        assertThat(status.errorCount()).isEqualTo(0);
        assertThat(status.totalCount()).isEqualTo(2);
        assertThat(status.successCount()).isEqualTo(2);
        assertThat(status.isSuccess()).isTrue();

        System.out.println(facets);
        assertThat(facets).hasSize(3);
        NumericRangeFacetResult strength = (NumericRangeFacetResult) facets.get("strength");
        assertThat(strength).isNotNull();
        assertThat(strength.name()).isEqualTo("strength");
        assertThat(strength.field()).isEqualTo("abv");
        assertThat(strength.total()).isEqualTo(5891);
        assertThat(strength.missing()).isEqualTo(192);
        assertThat(strength.other()).isEqualTo(0);
        assertThat(strength.numericRanges())
                .hasSize(3)
                .containsOnly(
                        new NumericRange("light", 0d, 3d, 2861),
                        new NumericRange("strong", 3d, 5d, 601),
                        new NumericRange("extra-strong", 5d, null, 2429)
                );
        DateRangeFacetResult updated = (DateRangeFacetResult) facets.get("updateRange");
        assertThat(updated).isNotNull();
        assertThat(updated.name()).isEqualTo("updateRange");
        assertThat(updated.field()).isEqualTo("updated");
        assertThat(updated.total()).isEqualTo(6085);
        assertThat(updated.missing()).isEqualTo(0);
        assertThat(updated.other()).isEqualTo(0);
        assertThat(updated.dateRanges())
                .hasSize(3)
                .containsOnly(
                        new DateRange("old", "2010-01-01T00:00:00Z", "2011-01-01T00:00:00Z", 6035),
                        new DateRange("middle", "2011-01-01T00:00:00Z", "2012-09-27T00:36:14Z", 48),
                        new DateRange("new", "2011-09-27T00:36:14Z", null, 2)
                );
        TermFacetResult category = (TermFacetResult) facets.get("category");
        assertThat(category).isNotNull();
        assertThat(category.name()).isEqualTo("category");
        assertThat(category.field()).isEqualTo("style");
        assertThat(category.total()).isEqualTo(15942);
        assertThat(category.missing()).isEqualTo(1660);
        assertThat(category.other()).isEqualTo(9864);
        assertThat(category.terms())
                .hasSize(2)
                .containsOnly(
                        new TermRange("style", 3509),
                        new TermRange("american", 2569)
                );

        assertThat(metrics).isNotNull();
        assertThat(metrics.maxScore()).isEqualTo(0.19801861346523805);
        assertThat(metrics.took()).isEqualTo(632150621); //about 631ms
        assertThat(metrics.totalHits()).isEqualTo(6083);

        assertThat(hits).hasSize(5);

        for (SearchQueryRow hit : hits) {
            assertThat(hit.index()).startsWith("beer-search");
            assertThat(hit.explanation()).isEqualTo(expectedExplanation); //edited in the file
            assertThat(hit.id()).startsWith("beer_"); //edited in the file
            assertThat(hit.score()).isBetween(0.00001, 2.0);

            if (!hit.fields().isEmpty()) {
                assertThat(hit.fields()).containsOnlyKeys("name");
                assertThat(hit.fields().get("name")).isNotEmpty();
            }

            assertThat(hit.fragments().size()).isBetween(0, 2);
            for (Map.Entry<String, List<String>> entry : hit.fragments().entrySet()) {
                assertThat(entry.getKey()).isIn("name", "description", "style");
                assertThat(entry.getValue()).hasSize(1);
                assertThat(entry.getValue().get(0)).contains("<mark>");
            }

            HitLocations locations = hit.locations();
            assertThat(locations.getAll()).hasSize((int) locations.count());
            for (HitLocation location : hit.locations().getAll()) {
                assertThat(location.field()).isIn("name", "description", "style", "type");
                assertThat(location.term()).isIn("beer", "beers", "beet", "beef", "bee", "bear");
                assertThat(location.start()).isLessThan(location.end());
                if (location.arrayPositions() != null) {
                    assertThat(hit.id()).as("only beer_cricket_hill has arrayPosition").isEqualTo("beer_cricket_hill");
                    assertThat(location.term()).as("only beer_cricket_hill's beef has arrayPosition").isEqualTo("beef");
                    assertThat(location.arrayPositions()).containsExactly(1, 3, 4); //edited in file
                }
                assertThat(location.pos()).isGreaterThan(0);
            }
        }

        //assert the first hit in more details
        SearchQueryRow firstHit = hits.get(0);
        assertThat(firstHit.index()).isEqualTo("beer-search_5cb0396d1a98fa15_b7ff6b68");
        assertThat(firstHit.explanation()).isEqualTo(expectedExplanation); //edited in the file
        assertThat(firstHit.id()).isEqualTo("beer_cricket_hill"); //edited in the file
        assertThat(firstHit.score()).isEqualTo(0.19801861346523805);
        assertThat(firstHit.fields())
                .isNotEmpty()
                .containsOnlyKeys("name")
                .containsEntry("name", "cricket hill"); //edited in file
        assertThat(firstHit.fragments()) //edited in file
                .hasSize(2)
                .containsOnly(
                        entry("name", singletonList("fake <mark>beer</mark> name fragment")),
                        entry("description", singletonList("fake <mark>Beer</mark> description fragment"))
                );
        //assert only the "beef" location
        HitLocations firstLocations = firstHit.locations();
        assertThat(firstLocations.fields()).containsOnly("description");
        assertThat(firstLocations.termsFor("description")).containsOnly("beef", "beer", "beers");
        assertThat(firstLocations.get("description", "beer")).hasSize(4);
        assertThat(firstLocations.get("description", "beers")).hasSize(5);
        assertThat(firstLocations.get("description", "beef"))
                .hasSize(1)
                .containsExactly(new HitLocation("description", "beef", 94, 535, 539,
                        new long[] { 1L, 3L, 4L }));
    }

    @Test
    public void testHttp400Conversion() {
        AsyncSearchQueryResult result = DefaultAsyncSearchQueryResult.fromHttp400("some error message");

        assertThat(result).isNotNull();
        assertThat(result.status()).isNotNull();
        assertThat(result.status().errorCount()).isEqualTo(1);
        assertThat(result.status().totalCount()).isEqualTo(1);
        assertThat(result.status().successCount()).isEqualTo(0);

        List<FacetResult> facets = result.facets().toList()
                .toBlocking().singleOrDefault(null);
        assertThat(facets)
                .isNotNull()
                .isEmpty();

        SearchMetrics metrics = result.metrics().toBlocking().singleOrDefault(null);
        assertThat(metrics).isNotNull();
        assertThat(metrics.maxScore()).isEqualTo(0d);
        assertThat(metrics.took()).isEqualTo(0);
        assertThat(metrics.totalHits()).isEqualTo(0);

        BlockingObservable<SearchQueryRow> hits = result.hits().toBlocking();
        try {
            hits.single();
            fail("expected exception while getting hits with HTTP 400");
        } catch (Throwable t) {
            assertThat(t)
                    .isInstanceOf(CouchbaseException.class)
                    .hasMessageEndingWith("some error message");
        }
    }
}
