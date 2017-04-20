/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Best effort serial executor test.
 */
public class BestEffortSerialExecutorTest {

    @Test
    public void testSerialExecution() throws Throwable {
        Executor executor = new BestEffortSerialExecutor(SharedExecutors.getPoolThreadExecutor());
        CountDownLatch latch = new CountDownLatch(2);
        executor.execute(latch::countDown);
        executor.execute(latch::countDown);
        latch.await();
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testBlockedExecution() throws Throwable {
        Executor executor = new BestEffortSerialExecutor(SharedExecutors.getPoolThreadExecutor());
        CountDownLatch latch = new CountDownLatch(3);
        executor.execute(() -> {
            try {
                Thread.sleep(2000);
                latch.countDown();
            } catch (InterruptedException e) {
            }
        });
        Thread.sleep(10);
        executor.execute(() -> {
            try {
                new CompletableFuture<>().get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                latch.countDown();
            }
        });
        Thread.sleep(10);
        executor.execute(latch::countDown);
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(2, latch.getCount());
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

}
