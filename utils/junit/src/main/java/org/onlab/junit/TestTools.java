/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.junit;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.fail;

/**
 * Utilities to aid in producing JUnit tests.
 */
public final class TestTools {

    // Prohibit construction
    private TestTools() {
    }

    public static void print(String msg) {
        System.out.print(msg);
    }

    /**
     * Suspends the current thread for a specified number of millis.
     *
     * @param ms number of millis
     */
    public static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            fail("test interrupted");
        }
    }

    /**
     * Returns the current time in millis since epoch.
     *
     * @return current time
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Runs the specified runnable until it completes successfully or until the
     * specified time expires. If the latter occurs, the first encountered
     * assertion on the last attempt will be re-thrown. Errors other than
     * assertion errors will be propagated immediately.
     * <p>
     * Assertions attempts will not be closer than 10 millis apart and no
     * further than 50 millis.
     * </p>
     *
     * @param delay      number of millis to delay before the first attempt
     * @param duration   number of milliseconds beyond the current time
     * @param assertions test assertions runnable
     */
    public static void assertAfter(int delay, int duration, Runnable assertions) {
        checkArgument(delay < duration, "delay >= duration");
        long start = now();
        int step = Math.max(Math.min((duration - delay) / 100, 50), 10);

        // Is there an initial delay?
        if (delay > 0) {
            delay(delay);
        }

        // Keep going until the assertions succeed or until time runs-out.
        while (true) {
            try {
                assertions.run();
                break;
            } catch (AssertionError e) {
                // If there was an error and time ran out, re-throw it.
                if (now() - start > duration) {
                    throw e;
                }
            }
            delay(step);
        }
    }

    /**
     * Runs the specified runnable until it completes successfully or until the
     * specified time expires. If the latter occurs, the first encountered
     * assertion on the last attempt will be re-thrown. Errors other than
     * assertion errors will be propagated immediately.
     * <p>
     * Assertions attempts will not be closer than 10 millis apart and no
     * further than 50 millis.
     * </p>
     *
     * @param duration   number of milliseconds beyond the current time
     * @param assertions test assertions runnable
     */
    public static void assertAfter(int duration, Runnable assertions) {
        assertAfter(0, duration, assertions);
    }

}
