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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Blocking-aware future test.
 */
public class BlockingAwareFutureTest {

    /**
     * Tests normal callback execution.
     */
    @Test
    public void testNonBlockingThread() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        Executor executor = SharedExecutors.getPoolThreadExecutor();
        BlockingAwareFuture<String> blockingFuture =
                (BlockingAwareFuture<String>) Tools.orderedFuture(future, new OrderedExecutor(executor), executor);
        CountDownLatch latch = new CountDownLatch(1);
        blockingFuture.thenRun(() -> latch.countDown());
        executor.execute(() -> future.complete("foo"));
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
        assertEquals("foo", blockingFuture.join());
        assertFalse(blockingFuture.isBlocked());
    }

    /**
     * Tests blocking an ordered thread.
     */
    @Test
    public void testBlockingThread() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        Executor executor = SharedExecutors.getPoolThreadExecutor();
        BlockingAwareFuture<String> blockingFuture =
                (BlockingAwareFuture<String>) Tools.orderedFuture(future, new OrderedExecutor(executor), executor);
        CountDownLatch latch = new CountDownLatch(2);
        CompletableFuture<String> wrappedFuture = blockingFuture.thenApply(v -> {
            assertEquals("foo", v);
            latch.countDown();
            return v;
        });
        wrappedFuture.thenRun(() -> latch.countDown());
        executor.execute(() -> wrappedFuture.join());
        Thread.sleep(100);
        assertTrue(blockingFuture.isBlocked());
        future.complete("foo");
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
        assertEquals("foo", blockingFuture.join());
        assertEquals("foo", wrappedFuture.join());
        assertFalse(blockingFuture.isBlocked());
    }

}
