/**
 * Copyright (C) 2014 Couchbase, Inc.
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