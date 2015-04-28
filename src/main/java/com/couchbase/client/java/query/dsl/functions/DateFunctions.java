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
package com.couchbase.client.java.query.dsl.functions;

import static com.couchbase.client.java.query.dsl.Expression.x;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.query.dsl.Expression;

/**
 * DSL for N1QL functions in the Date category.
 *
 * Date functions return the system clock value or manipulate the date string.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public class DateFunctions {
    /**
     * Returned expression results in system clock at function evaluation time, as UNIX milliseconds.
     * Varies during a query.
     */
    public static Expression clockMillis() {
        return x("CLOCK_MILLIS()");
    }

    /**
     * Returned expression results in system clock at function evaluation time, as a string in a supported format.
     * Varies during a query. Supported formats:
     *
     *  - "2006-01-02T15:04:05.999Z07:00": Default format. (ISO8601 / RFC3339)
     *  - "2006-01-02T15:04:05Z07:00" (ISO8601 / RFC3339)
     *  - "2006-01-02T15:04:05.999"
     *  - "2006-01-02T15:04:05"
     *  - "2006-01-02 15:04:05.999Z07:00"
     *  - "2006-01-02 15:04:05Z07:00"
     *  - "2006-01-02 15:04:05.999"
     *  - "2006-01-02 15:04:05"
     *  - "2006-01-02"
     *  - "15:04:05.999Z07:00"
     *  - "15:04:05Z07:00"
     *  - "15:04:05.999"
     *  - "15:04:05"
     */
    public static Expression clockStr(String format) {
        if (format == null || format.isEmpty()) {
            return x("CLOCK_STR()");
        }
        return x("CLOCK_STR(\"" + format + "\")");
    }

    /**
     * Returned expression performs Date arithmetic, and returns result of computation.
     * n and part are used to define an interval or duration, which is then added (or subtracted) to the UNIX timestamp,
     * returning the result.
     */
    public static Expression dateAddMillis(Expression expression, int n, DatePart part) {
        return x("DATE_ADD_MILLIS(" + expression.toString() + ", " + n + ", \"" + part + "\")");
    }

    /**
     * Returned expression performs Date arithmetic, and returns result of computation.
     * n and part are used to define an interval or duration, which is then added (or subtracted) to the UNIX timestamp,
     * returning the result.
     */
    public static Expression dateAddMillis(String expression, int n, DatePart part) {
        return dateAddMillis(x(expression), n, part);
    }

    /**
     * Returned expression results in Performs Date arithmetic. n and part are used to define an interval or duration,
     * which is then added (or subtracted) to the date string in a supported format, returning the result.
     */
    public static Expression dateAddStr(Expression expression, int n, DatePart part) {
        return x("DATE_ADD_STR(" + expression.toString() + ", " + n + ", \"" + part + "\")");
    }

    /**
     * Returned expression results in Performs Date arithmetic. n and part are used to define an interval or duration,
     * which is then added (or subtracted) to the date string in a supported format, returning the result.
     */
    public static Expression dateAddStr(String expression, int n, DatePart part) {
        return dateAddStr(x(expression), n, part);
    }

    /**
     * Returned expression results in Date arithmetic.
     * Returns the elapsed time between two UNIX timestamps as an integer whose unit is part.
     */
    public static Expression dateDiffMillis(Expression expression1, Expression expression2, DatePart part) {
        return x("DATE_DIFF_MILLIS(" + expression1.toString() + ", " + expression2.toString()
                + ", \"" + part.toString() + "\")");
    }

    /**
     * Returned expression results in Date arithmetic.
     * Returns the elapsed time between two UNIX timestamps as an integer whose unit is part.
     */
    public static Expression dateDiffMillis(String expression1, String expression2, DatePart part) {
        return dateDiffMillis(x(expression1), x(expression2), part);
    }

    /**
     * Returned expression results in Performs Date arithmetic.
     * Returns the elapsed time between two date strings in a supported format, as an integer whose unit is part.
     */
    public static Expression dateDiffStr(Expression expression1, Expression expression2, DatePart part) {
        return x("DATE_DIFF_STR(" + expression1.toString() + ", " + expression2.toString()
                + ", \"" + part.toString() + "\")");
    }

    /**
     * Returned expression results in Performs Date arithmetic.
     * Returns the elapsed time between two date strings in a supported format, as an integer whose unit is part.
     */
    public static Expression dateDiffStr(String expression1, String expression2, DatePart part) {
        return dateDiffStr(x(expression1), x(expression2), part);
    }

    /**
     * Returned expression results in Date part as an integer.
     * The date expression is a number representing UNIX milliseconds, and part is a {@link DatePartExt}.
     */
    public static Expression datePartMillis(Expression expression, DatePartExt part) {
        return x("DATE_PART_MILLIS(" + expression.toString() + ", \"" + part.toString() + "\")");
    }

    /**
     * Returned expression results in Date part as an integer. The date expression is a number representing
     * UNIX milliseconds, and part is a {@link DatePartExt}.
     */
    public static Expression datePartMillis(String expression, DatePartExt part) {
        return datePartMillis(x(expression), part);
    }

    /**
     * Returned expression results in Date part as an integer.
     * The date expression is a string in a supported format, and part is one of the supported date part strings.
     */
    public static Expression datePartStr(Expression expression, DatePartExt part) {
        return x("DATE_PART_STR(" + expression.toString() + ", \"" + part.toString() + "\")");
    }

    /**
     * Returned expression results in Date part as an integer.
     * The date expression is a string in a supported format, and part is one of the supported date part strings.
     */
    public static Expression datePartStr(String expression, DatePartExt part) {
        return datePartStr(x(expression), part);
    }

    /**
     * Returned expression results in UNIX timestamp that has been truncated so that the given date part
     * is the least significant.
     */
    public static Expression dateTruncMillis(Expression expression, DatePart part) {
        return x("DATE_TRUNC_MILLIS(" + expression.toString() + ", \"" + part.toString() + "\")");
    }

    /**
     * Returned expression results in UNIX timestamp that has been truncated so that the given date part
     * is the least significant.
     */
    public static Expression dateTruncMillis(String expression, DatePart part) {
        return dateTruncMillis(x(expression), part);
    }

    /**
     * Returned expression results in ISO 8601 timestamp that has been truncated
     * so that the given date part is the least significant.
     */
    public static Expression dateTruncStr(Expression expression, DatePart part) {
        return x("DATE_TRUNC_STR(" + expression.toString() + ", \"" + part.toString() + "\")");
    }

    /**
     * Returned expression results in ISO 8601 timestamp that has been truncated
     * so that the given date part is the least significant.
     */
    public static Expression dateTruncStr(String expression, DatePart part) {
        return dateTruncStr(x(expression), part);
    }

    /**
     * Returned expression results in date that has been converted in a supported format to UNIX milliseconds.
     */
    public static Expression millis(Expression expression) {
        return x("MILLIS(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in date that has been converted in a supported format to UNIX milliseconds.
     */
    public static Expression millis(String expression) {
        return millis(x(expression));
    }

    /**
     * Returned expression results in date that has been converted in a supported format to UNIX milliseconds.
     */
    public static Expression strToMillis(Expression expression) {
        return x("STR_TO_MILLIS(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in date that has been converted in a supported format to UNIX milliseconds.
     */
    public static Expression strToMillis(String expression) {
        return strToMillis(x(expression));
    }

    /**
     * Returned expression results in the string in the supported format to which
     * the UNIX milliseconds has been converted.
     */
    public static Expression millisToStr(Expression expression, String format) {
        if (format == null || format.isEmpty()) {
            return x("MILLIS_TO_STR(" + expression.toString() + ")");
        }
        return x("MILLIS_TO_STR(" + expression.toString() + ", \"" + format + "\")");
    }

    /**
     * Returned expression results in the string in the supported format to which
     * the UNIX milliseconds has been converted.
     */
    public static Expression millisToStr(String expression, String format) {
        return millisToStr(x(expression), format);
    }

    /**
     * Returned expression results in the UTC string to which the UNIX time stamp
     * has been converted in the supported format.
     */
    public static Expression millisToUtc(Expression expression, String format) {
        if (format == null || format.isEmpty()) {
            return x("MILLIS_TO_UTC(" + expression.toString() + ")");
        }
        return x("MILLIS_TO_UTC(" + expression.toString() + ", \"" + format + "\")");
    }

    /**
     * Returned expression results in the UTC string to which the UNIX time stamp
     * has been converted in the supported format.
     */
    public static Expression millisToUtc(String expression, String format) {
        return millisToUtc(x(expression), format);
    }

    /**
     * Returned expression results in a convertion of the UNIX time stamp to a string in the named time zone.
     */
    public static Expression millisToZone(Expression expression, String timeZoneName, String format) {
        if (format == null || format.isEmpty()) {
            return x("MILLIS_TO_ZONE(" + expression.toString() + ", \"" + timeZoneName + "\")");
        }
        return x("MILLIS_TO_ZONE(" + expression.toString() + ", \"" + timeZoneName + "\"" +
                ", \"" + format + "\")");
    }

    /**
     * Returned expression results in a convertion of the UNIX time stamp to a string in the named time zone.
     */
    public static Expression millisToZone(String expression, String timeZoneName, String format) {
        return millisToZone(x(expression), timeZoneName, format);
    }

    /**
     * Returned expression results in statement time stamp as UNIX milliseconds; does not vary during a query.
     */
    public static Expression nowMillis() {
        return x("NOW_MILLIS()");
    }

    /**
     * Returned expression results in statement time stamp as a string in a supported format;
     * does not vary during a query.
     */
    public static Expression nowStr(String format) {
        if (format == null || format.isEmpty()) {
            return x("NOW_STR()");
        }
        return x("NOW_STR(\"" + format + "\")");
    }

    /**
     * Returned expression results in a conversion of the ISO 8601 time stamp to UTC.
     */
    public static Expression strToUtc(Expression expression) {
        return x("STR_TO_UTC(" + expression.toString() + ")");
    }

    /**
     * Returned expression results in a conversion of the ISO 8601 time stamp to UTC.
     */
    public static Expression strToUtc(String expression) {
        return strToUtc(x(expression));
    }
    /**
     * Returned expression results in a conversion of the supported time stamp string to the named time zone.
     */
    public static Expression strToZoneName(Expression expression, String zoneName) {
        return x("STR_TO_ZONE_NAME(" + expression.toString() + ", \"" + zoneName + "\")");
    }

    /**
     * Returned expression results in a conversion of the supported time stamp string to the named time zone.
     */
    public static Expression strToZoneName(String expression, String zoneName) {
        return strToZoneName(x(expression), zoneName);
    }

    public enum DatePart {
        millenium,
        century,
        decade,
        year,
        quarter,
        month,
        week,
        day,
        hour,
        minute,
        second,
        millisecond
    }
    
    public enum DatePartExt {
        millenium,
        century,
        /** Floor(year / 10) **/
        decade,
        year,
        /** Valid values: 1 to 4 **/
        quarter,
        /** Valid values: 1 to 12 **/
        month,
        /** Valid values: 1 to 53; ceil(day_of_year / 7.0) **/
        week,
        /** Valid values: 1 to 31**/
        day,
        /** Valid values: 0 to 23**/
        hour,
        /** Valid values: 0 to 59**/
        minute,
        /** Valid values: 0 to 59**/
        second,
        /** Valid values: 0 to 999**/
        millisecond,
        /** Valid values: 1 to 366**/
        day_of_year,
        /** Valid values: 0 to 6**/
        day_of_week,
        /** Valid values: 1 to 53. Use with "iso_year"**/
        iso_week,
        /** Use with "iso_week" **/
        iso_year,
        /** Valid values: 1 to 7**/
        iso_dow,
        /** Offset from UTC in seconds**/
        timezone,
        /** Hour component of timezone offset**/
        timezone_hour,
        /** Minute component of timezone offset. Valid values: 0 to 59**/
        timezone_minute
    }
}
