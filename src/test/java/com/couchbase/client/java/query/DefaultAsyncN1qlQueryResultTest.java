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
                Observable.<JsonObject>empty(),
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