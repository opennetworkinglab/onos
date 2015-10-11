/*
 * Copyright 2015 Open Networking Laboratory
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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.Ignore;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.onlab.util.BoundedThreadPool.*;
import static org.onlab.util.Tools.namedThreads;

/**
 * Test of BoundedThreadPool.
 */
public final class BoundedThreadPoolTest {

    @Test
    public void simpleJob() {
        final Thread myThread = Thread.currentThread();
        final AtomicBoolean sameThread = new AtomicBoolean(true);
        final CountDownLatch latch = new CountDownLatch(1);

        BoundedThreadPool exec = newSingleThreadExecutor(namedThreads("test"));
        exec.submit(() -> {
            sameThread.set(myThread.equals(Thread.currentThread()));
            latch.countDown();
        });

        try {
            assertTrue("Job not run", latch.await(100, TimeUnit.MILLISECONDS));
            assertFalse("Runnable used caller thread", sameThread.get());
        } catch (InterruptedException e) {
            fail();
        } finally {
            exec.shutdown();
        }

        // TODO perhaps move to tearDown
        try {
            assertTrue(exec.awaitTermination(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail();
        }
    }

    private List<CountDownLatch> fillExecutor(BoundedThreadPool exec) {
        int numThreads = exec.getMaximumPoolSize();
        List<CountDownLatch> latches = Lists.newArrayList();
        final CountDownLatch started = new CountDownLatch(numThreads);
        List<CountDownLatch> finished = Lists.newArrayList();

        // seed the executor's threads
        for (int i = 0; i < numThreads; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch fin = new CountDownLatch(1);
            latches.add(latch);
            finished.add(fin);
            exec.submit(() -> {
                try {
                    started.countDown();
                    latch.await();
                    fin.countDown();
                } catch (InterruptedException e) {
                    fail();
                }
            });
        }
        try {
            assertTrue(started.await(100, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail();
        }
        // fill the queue
        CountDownLatch startedBlocked = new CountDownLatch(1);
        while (exec.getQueue().remainingCapacity() > 0) {
            final CountDownLatch latch = new CountDownLatch(1);
            latches.add(latch);
            exec.submit(() -> {
                try {
                    startedBlocked.countDown();
                    latch.await();
                } catch (InterruptedException e) {
                    fail();
                }
            });
        }

        latches.remove(0).countDown(); // release one of the executors
        // ... we need to do this because load is recomputed when jobs are taken
        // Note: For this to work, 1 / numThreads must be less than the load threshold (0.2)

        // verify that the old job has terminated
        try {
            assertTrue("Job didn't finish",
                       finished.remove(0).await(100, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail();
        }

        // verify that a previously blocked thread has started
        try {
            assertTrue(startedBlocked.await(10, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail();
        }


        // add another job to fill the queue
        final CountDownLatch latch = new CountDownLatch(1);
        latches.add(latch);
        exec.submit(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                fail();
            }
        });
        assertEquals(exec.getQueue().size(), maxQueueSize);

        return latches;
    }

    @Ignore("Ignored when running CircleCI")
    @Test
    public void releaseOneThread() {
        maxQueueSize = 10;
        BoundedThreadPool exec = newFixedThreadPool(4, namedThreads("test"));
        List<CountDownLatch> latches = fillExecutor(exec);

        CountDownLatch myLatch = new CountDownLatch(1);
        ExecutorService myExec = Executors.newSingleThreadExecutor();
        Future<Thread> expected = myExec.submit(Thread::currentThread);

        assertEquals(exec.getQueue().size(), maxQueueSize);
        long start = System.nanoTime();
        Future<Thread> actual = myExec.submit(() -> {
            return exec.submit(() -> {
                myLatch.countDown();
                return Thread.currentThread();
            }).get();
        });

        try {
            assertFalse("Thread should still be blocked",
                        myLatch.await(10, TimeUnit.MILLISECONDS));

            latches.remove(0).countDown(); // release the first thread
            assertFalse("Thread should still be blocked",
                        myLatch.await(10, TimeUnit.MILLISECONDS));
            latches.remove(0).countDown(); // release the second thread

            assertTrue("Thread should be unblocked",
                       myLatch.await(10, TimeUnit.MILLISECONDS));
            long delta = System.nanoTime() - start;
            double load = exec.getQueue().size() / (double) maxQueueSize;
            assertTrue("Load is greater than threshold", load <= 0.8);
            assertTrue("Load is less than threshold", load >= 0.6);
            assertEquals("Work done on wrong thread", expected.get(), actual.get());
            assertTrue("Took more than one second", delta < Math.pow(10, 9));
        } catch (InterruptedException | ExecutionException e) {
            fail();
        } finally {
            latches.forEach(CountDownLatch::countDown);
            exec.shutdown();
        }

        // TODO perhaps move to tearDown
        try {
            assertTrue(exec.awaitTermination(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail();
        }

    }

    @Test
    public void highLoadTimeout() {
        maxQueueSize = 10;
        BoundedThreadPool exec = newFixedThreadPool(2, namedThreads("test"));
        List<CountDownLatch> latches = fillExecutor(exec);

        // true if the job is executed and it is done on the test thread
        final AtomicBoolean sameThread = new AtomicBoolean(false);
        final Thread myThread = Thread.currentThread();
        long start = System.nanoTime();
        exec.submit(() -> {
            sameThread.set(myThread.equals(Thread.currentThread()));
        });

        long delta = System.nanoTime() - start;
        assertEquals(maxQueueSize, exec.getQueue().size());
        assertTrue("Work done on wrong thread (or didn't happen)", sameThread.get());
        assertTrue("Took less than one second. Actual: " + delta / 1_000_000.0 + "ms",
                   delta > Math.pow(10, 9));
        assertTrue("Took more than two seconds", delta < 2 * Math.pow(10, 9));
        latches.forEach(CountDownLatch::countDown);
        exec.shutdown();

        // TODO perhaps move to tearDown
        try {
            assertTrue(exec.awaitTermination(1, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            fail();
        }
    }
}
