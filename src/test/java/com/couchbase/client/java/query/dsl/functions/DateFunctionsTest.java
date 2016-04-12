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

package com.couchbase.client.java.query.dsl.functions;

import static com.couchbase.client.java.query.dsl.Expression.x;
import static com.couchbase.client.java.query.dsl.functions.DateFunctions.*;
import static org.junit.Assert.assertEquals;

import com.couchbase.client.java.query.dsl.Expression;
import org.junit.Test;

public class DateFunctionsTest {

    @Test
    public void testDatePartEnumToString() {
        assertEquals("millenium", DatePart.millenium.toString());
        assertEquals("century", DatePart.century.toString());
        assertEquals("decade", DatePart.decade.toString());
        assertEquals("year", DatePart.year.toString());
        assertEquals("week", DatePart.week.toString());
        assertEquals("day", DatePart.day.toString());
        assertEquals("hour", DatePart.hour.toString());
        assertEquals("minute", DatePart.minute.toString());
        assertEquals("second", DatePart.second.toString());
        assertEquals("millisecond", DatePart.millisecond.toString());
    }
    
    @Test
    public void testDatePartExtEnumToString() {
        assertEquals("millenium", DatePartExt.millenium.toString());
        assertEquals("century", DatePartExt.century.toString());
        assertEquals("decade", DatePartExt.decade.toString());
        assertEquals("year", DatePartExt.year.toString());
        assertEquals("week", DatePartExt.week.toString());
        assertEquals("day", DatePartExt.day.toString());
        assertEquals("hour", DatePartExt.hour.toString());
        assertEquals("minute", DatePartExt.minute.toString());
        assertEquals("second", DatePartExt.second.toString());
        assertEquals("millisecond", DatePartExt.millisecond.toString());

        assertEquals("day_of_year", DatePartExt.day_of_year.toString());
        assertEquals("day_of_week", DatePartExt.day_of_week.toString());
        assertEquals("iso_week", DatePartExt.iso_week.toString());
        assertEquals("iso_year", DatePartExt.iso_year.toString());
        assertEquals("iso_dow", DatePartExt.iso_dow.toString());
        assertEquals("timezone", DatePartExt.timezone.toString());
        assertEquals("timezone_hour", DatePartExt.timezone_hour.toString());
        assertEquals("timezone_minute", DatePartExt.timezone_minute.toString());
    }

    @Test
    public void testClockMillis() throws Exception {
        assertEquals("CLOCK_MILLIS()", clockMillis().toString());
    }

    @Test
    public void testClockStr() throws Exception {
        assertEquals("CLOCK_STR()", clockStr(null).toString());
        assertEquals("CLOCK_STR()", clockStr("").toString());
        assertEquals("CLOCK_STR(\"testFormat\")", clockStr("testFormat").toString());
    }

    @Test
    public void testDateAddMillis() throws Exception {
        Expression dateAdd = dateAddMillis(x("dateField"), 10, DatePart.millenium);

        assertEquals("DATE_ADD_MILLIS(dateField, 10, \"millenium\")", dateAdd.toString());
        assertEquals(dateAdd.toString(), dateAddMillis("dateField", 10, DatePart.millenium).toString());
    }

    @Test
    public void testDateAddStr() throws Exception {
        Expression dateAdd = dateAddStr(x("stringField"), 10, DatePart.millenium);

        assertEquals("DATE_ADD_STR(stringField, 10, \"millenium\")", dateAdd.toString());
        assertEquals(dateAdd.toString(), dateAddStr("stringField", 10, DatePart.millenium).toString());
    }

    @Test
    public void testDateDiffMillis() throws Exception {
        Expression ex = dateDiffMillis(x("stringField1"), x("stringField2"), DatePart.millenium);

        assertEquals("DATE_DIFF_MILLIS(stringField1, stringField2, \"millenium\")", ex.toString());
        assertEquals(ex.toString(), dateDiffMillis("stringField1", "stringField2", DatePart.millenium).toString());
    }

    @Test
    public void testDateDiffStr() throws Exception {
        Expression ex = dateDiffStr(x("stringField1"), x("stringField2"), DatePart.millenium);

        assertEquals("DATE_DIFF_STR(stringField1, stringField2, \"millenium\")", ex.toString());
        assertEquals(ex.toString(), dateDiffStr("stringField1", "stringField2", DatePart.millenium).toString());
    }

    @Test
    public void testDatePartMillis() throws Exception {
        Expression ex = datePartMillis(x("dateField"), DatePartExt.iso_year);

        assertEquals("DATE_PART_MILLIS(dateField, \"iso_year\")", ex.toString());
        assertEquals(ex.toString(), datePartMillis("dateField", DatePartExt.iso_year).toString());
    }

    @Test
    public void testDatePartStr() throws Exception {
        Expression ex = datePartStr(x("dateField"), DatePartExt.iso_year);

        assertEquals("DATE_PART_STR(dateField, \"iso_year\")", ex.toString());
        assertEquals(ex.toString(), datePartStr("dateField", DatePartExt.iso_year).toString());
    }


    @Test
    public void testDateTruncMillis() throws Exception {
        Expression ex = dateTruncMillis(x("dateField"), DatePart.quarter);

        assertEquals("DATE_TRUNC_MILLIS(dateField, \"quarter\")", ex.toString());
        assertEquals(ex.toString(), dateTruncMillis("dateField", DatePart.quarter).toString());

    }

    @Test
    public void testDateTruncStr() throws Exception {
        Expression ex = dateTruncStr(x("dateField"), DatePart.quarter);

        assertEquals("DATE_TRUNC_STR(dateField, \"quarter\")", ex.toString());
        assertEquals(ex.toString(), dateTruncStr("dateField", DatePart.quarter).toString());
    }

    @Test
    public void testMillis() throws Exception {
        Expression ex = millis(x("dateField"));

        assertEquals("MILLIS(dateField)", ex.toString());
        assertEquals(ex.toString(), millis("dateField").toString());
    }

    @Test
    public void testStrToMillis() throws Exception {
        Expression ex = strToMillis(x("dateField"));

        assertEquals("STR_TO_MILLIS(dateField)", ex.toString());
        assertEquals(ex.toString(), strToMillis("dateField").toString());    }

    @Test
    public void testMillisToStr() throws Exception {
        Expression ex = millisToStr(x(1000), "hh:mm");

        assertEquals("MILLIS_TO_STR(1000, \"hh:mm\")", ex.toString());
        assertEquals(ex.toString(), millisToStr("1000", "hh:mm").toString());

        Expression exWithoutFormat = millisToStr(x(1000), null);

        assertEquals("MILLIS_TO_STR(1000)", exWithoutFormat.toString());
        //also test empty string same as null
        assertEquals(exWithoutFormat.toString(), millisToStr("1000", "").toString());
    }
    
    @Test
    public void testMillisToUtc() throws Exception {
        Expression ex = millisToUtc(x(1000), "hh:mm");

        assertEquals("MILLIS_TO_UTC(1000, \"hh:mm\")", ex.toString());
        assertEquals(ex.toString(), millisToUtc("1000", "hh:mm").toString());

        Expression exWithoutFormat = millisToUtc(x(1000), null);

        assertEquals("MILLIS_TO_UTC(1000)", exWithoutFormat.toString());
        //also test empty string same as null
        assertEquals(exWithoutFormat.toString(), millisToUtc("1000", "").toString());
    }

    @Test
    public void testMillisToZone() throws Exception {
        Expression ex = millisToZone(x("dateField"), "CET", "hh:mm");

        assertEquals("MILLIS_TO_ZONE(dateField, \"CET\", \"hh:mm\")", ex.toString());
        assertEquals(ex.toString(), millisToZone("dateField", "CET", "hh:mm").toString());

        Expression exWithoutFormat = millisToZone(x("dateField"), "CET", null);

        assertEquals("MILLIS_TO_ZONE(dateField, \"CET\")", exWithoutFormat.toString());
        assertEquals(exWithoutFormat.toString(), millisToZone("dateField", "CET", "").toString());
    }

    @Test
    public void testNowMillis() throws Exception {
        assertEquals("NOW_MILLIS()", nowMillis().toString());
    }

    @Test
    public void testNowStr() throws Exception {
        assertEquals("NOW_STR()", nowStr(null).toString());
        assertEquals("NOW_STR()", nowStr("").toString());
        assertEquals("NOW_STR(\"hh:mm\")", nowStr("hh:mm").toString());
    }

    @Test
    public void testStrToUtc() throws Exception {
        Expression ex = strToUtc(x("dateField"));

        assertEquals("STR_TO_UTC(dateField)", ex.toString());
        assertEquals(ex.toString(), strToUtc("dateField").toString());
    }

    @Test
    public void testStrToZoneName() throws Exception {
        Expression ex = strToZoneName(x("dateField"), "CET");

        assertEquals("STR_TO_ZONE_NAME(dateField, \"CET\")", ex.toString());
        assertEquals(ex.toString(), strToZoneName("dateField", "CET").toString());
    }
}