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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * Utility class around FTS (Full Text Search), and especially handling of the default FTS date format
 * (which corresponds to RFC 3339).
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
@InterfaceStability.Uncommitted
@InterfaceAudience.Public
public class SearchUtils {

    //TODO when baseline is Java 7, one can directly use the X code for timezone and get rid of zDate to xDate conversions
    //TODO when baseline is Java 8, use new Java Time package that has support for RFC 3339 directly
    //(zDate: ISO 822 timezone format, xDate: ISO 8601 timezone format)
    private static final String FTS_SIMPLE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZZ";

    private static final Pattern PATTERN = Pattern.compile("([\\+-])(\\d{1,2})(?::(\\d{1,2}))?$");

    private static final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(FTS_SIMPLE_DATE_FORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            sdf.setLenient(false);
            return sdf;
        }
    };

    private SearchUtils() {}

    /**
     * Converts a date to the default string representation in FTS (RFC 3339).
     *
     * @param date the {@link Date} to convert.
     * @return the RFC 3339 representation of the date, in UTC timezone (eg. "2016-01-19T14:44:01Z")
     */
    public static String toFtsUtcString(Date date) {
        if (date == null) {
            return null;
        }
        DateFormat rfc3339 = df.get();
        String zDate = rfc3339.format(date);
        String xDate = zDate.replaceFirst("\\+0000$", "Z");
        return xDate;
    }

    /**
     * Attempts to convert a date string, as returned by the FTS service, into a {@link Date}.
     *
     * The FTS service is expected to return date strings in RFC 3339 format, including the timezone information.
     * For example: <code>2016-01-10T14:44:01-08:00</code> for a date in the UTC-8/PDT timezone.
     *
     * @param date the date in RFC 3339 format.
     * @return the corresponding {@link Date}.
     * @throws CouchbaseException when the date could not be parsed.
     */
    public static Date fromFtsString(String date) {
        if (date == null) {
            return null;
        }
        String zDate = iso822TimezoneToRfc3339Timezone(date);
        try {
            return df.get().parse(zDate);
        } catch (ParseException e) {
            throw new CouchbaseException("Cannot parse FTS date '" + date + "' despite convertion to RFC 822 timezone '"
                    + zDate + "'", e);
        }
    }

    private static String iso822TimezoneToRfc3339Timezone(String xDate) {
        String zDate;
        if (xDate.endsWith("Z")) {
            zDate = xDate.replaceFirst("Z$", "+0000");
        } else {
            final Matcher matcher = PATTERN.matcher(xDate);
            if (matcher.find()) {
                String sign = matcher.group(1);
                String hours = matcher.group(2);
                String minutes = "00";
                if (matcher.groupCount() == 3 && matcher.group(3) != null) {
                    minutes = matcher.group(3);
                }

                if (hours.length() == 1) {
                    hours = "0" + hours;
                }
                if (minutes.length() == 1) {
                    minutes = "0" + minutes;
                }

                zDate = matcher.replaceFirst(sign + hours + minutes);
            } else {
                throw new CouchbaseException("Cannot convert timezone to RFC 822 in '" + xDate + "'");
            }
        }
        return zDate;
    }
}
