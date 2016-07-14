/*
 * Copyright 2014-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.intent;

import static org.junit.Assert.fail;

/**
 * Set of test tools.
 */
public final class TestTools {

    // Disallow construction
    private TestTools() {
    }

    /**
     * Utility method to pause the current thread for the specified number of
     * milliseconds.
     *
     * @param ms number of milliseconds to pause
     */
    public static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            fail("unexpected interrupt");
        }
    }

    /**
     * Periodically runs the given runnable, which should contain a series of
     * test assertions until all the assertions succeed, in which case it will
     * return, or until the the time expires, in which case it will throw the
     * first failed assertion error.
     *
     * @param start start time, in millis since start of epoch from which the
     *        duration will be measured
     * @param delay initial delay (in milliseconds) before the first assertion
     *        attempt
     * @param step delay (in milliseconds) between successive assertion
     *        attempts
     * @param duration number of milliseconds beyond the given start time,
     *        after which the failed assertions will be propagated and allowed
     *        to fail the test
     * @param assertions runnable housing the test assertions
     */
    public static void assertAfter(long start, int delay, int step,
                                   int duration, Runnable assertions) {
        delay(delay);
        while (true) {
            try {
                assertions.run();
                break;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() - start > duration) {
                    throw e;
                }
            }
            delay(step);
        }
    }

    /**
     * Periodically runs the given runnable, which should contain a series of
     * test assertions until all the assertions succeed, in which case it will
     * return, or until the the time expires, in which case it will throw the
     * first failed assertion error.
     * <p>
     * The start of the period is the current time.
     *
     * @param delay initial delay (in milliseconds) before the first assertion
     *        attempt
     * @param step delay (in milliseconds) between successive assertion
     *        attempts
     * @param duration number of milliseconds beyond the current time time,
     *        after which the failed assertions will be propagated and allowed
     *        to fail the test
     * @param assertions runnable housing the test assertions
     */
    public static void assertAfter(int delay, int step, int duration,
                                   Runnable assertions) {
        assertAfter(System.currentTimeMillis(), delay, step, duration,
                    assertions);
    }

    /**
     * Periodically runs the given runnable, which should contain a series of
     * test assertions until all the assertions succeed, in which case it will
     * return, or until the the time expires, in which case it will throw the
     * first failed assertion error.
     * <p>
     * The start of the period is the current time and the first assertion
     * attempt is delayed by the value of {@code step} parameter.
     *
     * @param step delay (in milliseconds) between successive assertion
     *        attempts
     * @param duration number of milliseconds beyond the current time time,
     *        after which the failed assertions will be propagated and allowed
     *        to fail the test
     * @param assertions runnable housing the test assertions
     */
    public static void assertAfter(int step, int duration,
                                   Runnable assertions) {
        assertAfter(step, step, duration, assertions);
    }

    /**
     * Periodically runs the given runnable, which should contain a series of
     * test assertions until all the assertions succeed, in which case it will
     * return, or until the the time expires, in which case it will throw the
     * first failed assertion error.
     * <p>
     * The start of the period is the current time and each successive
     * assertion attempt is delayed by at least 10 milliseconds unless the
     * {@code duration} is less than that, in which case the one and only
     * assertion is made after that delay.
     *
     * @param duration number of milliseconds beyond the current time,
     *        after which the failed assertions will be propagated and allowed
     *        to fail the test
     * @param assertions runnable housing the test assertions
     */
    public static void assertAfter(int duration, Runnable assertions) {
        int step = Math.min(duration, Math.max(10, duration / 10));
        assertAfter(step, duration, assertions);
    }

}
