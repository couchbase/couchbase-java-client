/*
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

package com.couchbase.client.java.query;

import static org.junit.Assert.*;

import com.couchbase.client.java.document.json.JsonObject;
import org.junit.Test;
import rx.Observable;

public class DefaultAsyncN1qlQueryResultTest {

    private DefaultAsyncN1qlQueryResult fakeResult(String statusValue) {
        Observable<String> status = Observable.just(statusValue);
        return  new DefaultAsyncN1qlQueryResult(
                Observable.<AsyncN1qlQueryRow>empty(), Observable.empty(),
                Observable.<N1qlMetrics>empty(), Observable.<JsonObject>empty(),
                status
                , true, null, null);
    }

    @Test
    public void testFinalSuccessIsTrueForSuccessStatus() throws Exception {
        DefaultAsyncN1qlQueryResult aqr = fakeResult("success");
        DefaultAsyncN1qlQueryResult aqrWeirdCase = fakeResult("sUccEsS");

        assertEquals(true, aqr.finalSuccess().toBlocking().single());
        assertEquals(true, aqrWeirdCase.finalSuccess().toBlocking().single());
    }

    @Test
    public void testFinalSuccessIsTrueForCompletedStatus() throws Exception {
        DefaultAsyncN1qlQueryResult aqr = fakeResult("completed");
        DefaultAsyncN1qlQueryResult aqrWeirdCase = fakeResult("cOmPlETeD");

        assertEquals(true, aqr.finalSuccess().toBlocking().single());
        assertEquals(true, aqrWeirdCase.finalSuccess().toBlocking().single());
    }

    @Test
    public void testFinalSuccessIsFalseForTimeoutStatus() throws Exception {
        DefaultAsyncN1qlQueryResult aqr = fakeResult("timeout");
        DefaultAsyncN1qlQueryResult aqrWeirdCase = fakeResult("TimeOut");

        assertEquals(false, aqr.finalSuccess().toBlocking().single());
        assertEquals(false, aqrWeirdCase.finalSuccess().toBlocking().single());
    }

    @Test
    public void testFinalSuccessIsFalseForFatalStatus() throws Exception {
        DefaultAsyncN1qlQueryResult aqr = fakeResult("fatal");
        DefaultAsyncN1qlQueryResult aqrWeirdCase = fakeResult("fATaL");

        assertEquals(false, aqr.finalSuccess().toBlocking().single());
        assertEquals(false, aqrWeirdCase.finalSuccess().toBlocking().single());
    }
}