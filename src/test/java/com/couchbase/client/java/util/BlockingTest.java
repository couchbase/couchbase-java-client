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
package com.couchbase.client.java.util;

import org.junit.Test;
import rx.Observable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the functionality of the {@link Blocking} utility methods.
 *
 * @author Michael Nitschinger
 * @since 2.0.2
 */
public class BlockingTest {

    @Test
    public void blockForSingleShouldSucceed() {
        String result = Blocking.blockForSingle(Observable.just("Hello World"), 1, TimeUnit.SECONDS);
        assertEquals("Hello World", result);
    }

    @Test
    public void blockForSingleShouldAllowNull() {
        String result = Blocking.blockForSingle(Observable.<String>empty().singleOrDefault(null), 1, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    public void blockForSingleShouldTimeout() {
        try {
            Blocking.blockForSingle(Observable.timer(500, TimeUnit.MILLISECONDS).first(), 100, TimeUnit.MILLISECONDS);
            assertTrue(false);
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TimeoutException);
        } catch (Exception ex) {
            assertTrue(false);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void blockForSingleShouldRaiseError() {
        Blocking.blockForSingle(Observable.<String>error(new IllegalArgumentException()), 1, TimeUnit.SECONDS);
    }

}