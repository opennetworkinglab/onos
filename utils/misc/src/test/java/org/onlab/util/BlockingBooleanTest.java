/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onlab.util;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Tests of the BlockingBoolean utility.
 */
public class BlockingBooleanTest  {

    private static final int FAIL_TIMEOUT = 1000; //ms

    @Test
    public void basics() {
        BlockingBoolean b = new BlockingBoolean(false);
        assertEquals(false, b.get());
        b.set(true);
        assertEquals(true, b.get());
        b.set(true);
        assertEquals(true, b.get());
        b.set(false);
        assertEquals(false, b.get());
    }

    private void waitChange(boolean value, int numThreads) {
        BlockingBoolean b = new BlockingBoolean(!value);

        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            exec.submit(() -> {
                try {
                    b.await(value);
                    latch.countDown();
                } catch (InterruptedException e) {
                    fail();
                }
            });
        }
        b.set(value);
        try {
            assertTrue(latch.await(FAIL_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail();
        }
        exec.shutdown();
    }

    @Test
    public void waitTrueChange() {
        waitChange(true, 4);
    }

    @Test
    public void waitFalseChange() {
        waitChange(false, 4);
    }

    @Test
    public void waitSame() {
        BlockingBoolean b = new BlockingBoolean(true);

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(() -> {
            try {
                b.await(true);
                latch.countDown();
            } catch (InterruptedException e) {
                fail();
            }
        });
        try {
            assertTrue(latch.await(FAIL_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail();
        }
        exec.shutdown();
    }

    @Test
    public void someWait() {
        BlockingBoolean b = new BlockingBoolean(false);

        int numThreads = 4;
        CountDownLatch sameLatch = new CountDownLatch(numThreads / 2);
        CountDownLatch waitLatch = new CountDownLatch(numThreads / 2);

        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            final boolean value = (i % 2 == 1);
            exec.submit(() -> {
                try {
                    b.await(value);
                    if (value) {
                        waitLatch.countDown();
                    } else {
                        sameLatch.countDown();
                    }
                } catch (InterruptedException e) {
                    fail();
                }
            });
        }
        try {
            assertTrue(sameLatch.await(FAIL_TIMEOUT, TimeUnit.MILLISECONDS));
            assertEquals(waitLatch.getCount(), numThreads / 2);
        } catch (InterruptedException e) {
            fail();
        }
        b.set(true);
        try {
            assertTrue(waitLatch.await(FAIL_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail();
        }
        exec.shutdown();
    }

    @Test
    public void waitTimeout() {
        BlockingBoolean b = new BlockingBoolean(true);

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.submit(() -> {
            try {
                if (!b.await(false, 1, TimeUnit.NANOSECONDS)) {
                    latch.countDown();
                } else {
                    fail();
                }
            } catch (InterruptedException e) {
                fail();
            }
        });
        try {
            assertTrue(latch.await(FAIL_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail();
        }
        exec.shutdown();

    }

    @Test
    @Ignore
    public void samePerf() {
        int iters = 10_000;

        BlockingBoolean b1 = new BlockingBoolean(false);
        long t1 = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            b1.set(false);
        }
        long t2 = System.nanoTime();
        MutableBoolean b2 = new MutableBoolean(false);
        for (int i = 0; i < iters; i++) {
            b2.setValue(false);
        }
        long t3 = System.nanoTime();
        System.out.println((t2 - t1) + " " + (t3 - t2) + " " + ((t2 - t1) <= (t3 - t2)));
    }

    @Test
    @Ignore
    public void changePerf() {
        int iters = 10_000;

        BlockingBoolean b1 = new BlockingBoolean(false);
        boolean v = true;
        long t1 = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            b1.set(v);
            v = !v;
        }
        long t2 = System.nanoTime();
        MutableBoolean b2 = new MutableBoolean(false);
        for (int i = 0; i < iters; i++) {
            b2.setValue(v);
            v = !v;
        }
        long t3 = System.nanoTime();
        System.out.println((t2 - t1) + " " + (t3 - t2) + " " + ((t2 - t1) <= (t3 - t2)));
    }

}
