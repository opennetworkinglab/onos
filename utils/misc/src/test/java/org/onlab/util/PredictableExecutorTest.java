/*
 * Copyright 2016-present Open Networking Foundation
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

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.PredictableExecutor.PickyRunnable;
import com.google.common.testing.EqualsTester;

public class PredictableExecutorTest {

    private PredictableExecutor pexecutor;
    private ExecutorService executor;

    @Before
    public void setUp() {
        pexecutor = new PredictableExecutor(3, Tools.namedThreads("Thread-%d"));
        executor = pexecutor;
    }

    @After
    public void tearDown() {
        pexecutor.shutdownNow();
    }

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(7);
        AtomicReference<String> hintValue0 = new AtomicReference<>("");
        AtomicReference<String> hintValue1 = new AtomicReference<>("");
        AtomicReference<String> hintFunction0 = new AtomicReference<>("");
        AtomicReference<String> pickyRunnable0 = new AtomicReference<>("");
        AtomicReference<String> pickyRunnable1 = new AtomicReference<>("");
        AtomicReference<String> pickyCallable0 = new AtomicReference<>("");
        AtomicReference<String> hashCode0 = new AtomicReference<>("");

        pexecutor.execute(() -> {
            hintValue0.set(Thread.currentThread().getName());
            latch.countDown();
        }, 0);

        pexecutor.execute(() -> {
            hintValue1.set(Thread.currentThread().getName());
            latch.countDown();
        }, 1);

        pexecutor.execute(() -> {
            hintFunction0.set(Thread.currentThread().getName());
            latch.countDown();
        }, (runnable) -> 0);

        pexecutor.execute(new PickyRunnable() {

            @Override
            public void run() {
                pickyRunnable0.set(Thread.currentThread().getName());
                latch.countDown();
            }

            @Override
            public int hint() {
                return 0;
            }
        });

        executor.execute(new PickyRunnable() {

            @Override
            public void run() {
                pickyRunnable1.set(Thread.currentThread().getName());
                latch.countDown();
            }

            @Override
            public int hint() {
                return 1;
            }
        });

        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() {
                pickyCallable0.set(Thread.currentThread().getName());
                latch.countDown();
                return null;
            }
        };

        executor.submit(PredictableExecutor.picky(callable, 0));


        executor.execute(new Runnable() {

            @Override
            public void run() {
                hashCode0.set(Thread.currentThread().getName());
                latch.countDown();

            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public boolean equals(Object that) {
                return false;
            }
        });

        latch.await(1, TimeUnit.SECONDS);

        new EqualsTester()
            .addEqualityGroup(hintValue0.get(),
                              hintFunction0.get(),
                              pickyRunnable0.get(),
                              pickyCallable0.get(),
                              hashCode0.get())
            .addEqualityGroup(hintValue1.get(),
                              pickyRunnable1.get())
            .testEquals();
    }
}
