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
package org.onlab.nio;

import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.fail;
import static org.onlab.util.Tools.namedThreads;

/**
 * Base class for various NIO loop unit tests.
 */
public abstract class AbstractLoopTest {

    protected static final long MAX_MS_WAIT = 1500;

    /** Block on specified countdown latch. Return when countdown reaches
     * zero, or fail the test if the {@value #MAX_MS_WAIT} ms timeout expires.
     *
     * @param latch the latch
     * @param label an identifying label
     */
    protected void waitForLatch(CountDownLatch latch, String label) {
        try {
            boolean ok = latch.await(MAX_MS_WAIT, TimeUnit.MILLISECONDS);
            if (!ok) {
                fail("Latch await timeout! [" + label + "]");
            }
        } catch (InterruptedException e) {
            System.out.println("Latch interrupt [" + label + "] : " + e);
            fail("Unexpected interrupt");
        }
    }

    protected ExecutorService exec;

    @Before
    public void setUp() {
        exec = newSingleThreadExecutor(namedThreads("test"));
    }

}
