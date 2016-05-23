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
package com.couchbase.client.java.search.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class SearchUtilsTest {

    @Test
    public void testNullDateToNullString() {
        assertThat(SearchUtils.toFtsUtcString(null)).isNull();
    }

    @Test
    public void testNullStringToNullDate() {
        assertThat(SearchUtils.fromFtsString(null)).isNull();
    }

    @Test
    public void testUtcToFtsUtcString() throws Exception {
        String expected = "2014-01-03T23:44:55Z";
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2014, Calendar.JANUARY, 3, 23, 44, 55);
        Date date = cal.getTime();

        String formatted = SearchUtils.toFtsUtcString(date);

        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    public void testPdtToFtsUtcString() throws Exception {
        String expected = "2014-01-03T16:44:55Z"; // "2014-01-03T08:44:55-0800" + 8 hours
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
        cal.set(2014, Calendar.JANUARY, 3, 8, 44, 55);
        Date date = cal.getTime();

        String formatted = SearchUtils.toFtsUtcString(date);

        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    public void testFromFtsUtcString() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2014, Calendar.JANUARY, 3, 23, 44, 55);
        Date expected = cal.getTime();
        String date = "2014-01-03T23:44:55Z";

        Date parsed = SearchUtils.fromFtsString(date);

        assertThat(parsed).isEqualTo(expected);
    }

    @Test
    public void testFromFtsNegativeTimezonedStrings() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2014, Calendar.JANUARY, 3, 23, 44, 55);
        Date expected = cal.getTime();

        String date1 = "2014-01-03T15:44:55-08:00";
        String date2 = "2014-01-03T15:44:55-8:00";
        String date3 = "2014-01-03T15:44:55-08:0";
        String date4 = "2014-01-03T15:44:55-8:0";
        String date5 = "2014-01-03T15:44:55-08";
        String date6 = "2014-01-03T15:44:55-8";

        Date parsed1 = SearchUtils.fromFtsString(date1);
        Date parsed2 = SearchUtils.fromFtsString(date2);
        Date parsed3 = SearchUtils.fromFtsString(date3);
        Date parsed4 = SearchUtils.fromFtsString(date4);
        Date parsed5 = SearchUtils.fromFtsString(date5);
        Date parsed6 = SearchUtils.fromFtsString(date6);

        assertThat(parsed1).isEqualTo(expected);
        assertThat(parsed2).isEqualTo(expected);
        assertThat(parsed3).isEqualTo(expected);
        assertThat(parsed4).isEqualTo(expected);
        assertThat(parsed5).isEqualTo(expected);
        assertThat(parsed6).isEqualTo(expected);
    }

    @Test
    public void testFromFtsPositiveTimezonedStrings() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2014, Calendar.JANUARY, 3, 10, 44, 55);
        Date expected = cal.getTime();

        String date1 = "2014-01-03T15:44:55+05:00";
        String date2 = "2014-01-03T15:44:55+5:00";
        String date3 = "2014-01-03T15:44:55+05:0";
        String date4 = "2014-01-03T15:44:55+5:0";
        String date5 = "2014-01-03T15:44:55+05";
        String date6 = "2014-01-03T15:44:55+5";

        Date parsed1 = SearchUtils.fromFtsString(date1);
        Date parsed2 = SearchUtils.fromFtsString(date2);
        Date parsed3 = SearchUtils.fromFtsString(date3);
        Date parsed4 = SearchUtils.fromFtsString(date4);
        Date parsed5 = SearchUtils.fromFtsString(date5);
        Date parsed6 = SearchUtils.fromFtsString(date6);

        assertThat(parsed1).isEqualTo(expected);
        assertThat(parsed2).isEqualTo(expected);
        assertThat(parsed3).isEqualTo(expected);
        assertThat(parsed4).isEqualTo(expected);
        assertThat(parsed5).isEqualTo(expected);
        assertThat(parsed6).isEqualTo(expected);
    }
}