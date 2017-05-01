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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Ordered executor test.
 */
public class OrderedExecutorTest {

    @Test
    public void testSerialExecution() throws Throwable {
        Executor executor = new OrderedExecutor(SharedExecutors.getPoolThreadExecutor());
        CountDownLatch latch = new CountDownLatch(2);
        executor.execute(latch::countDown);
        executor.execute(latch::countDown);
        latch.await();
        assertEquals(0, latch.getCount());
    }

}
