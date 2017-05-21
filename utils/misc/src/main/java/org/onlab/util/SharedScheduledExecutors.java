/*
 * Copyright 2016-present Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility for managing a set of shared execution resources, such as a single
 * thread scheduled executor and thread pool scheduled executor for use by
 * various parts of the platform or by applications.
 * <p>
 * Whenever possible, use of these shared resources is encouraged over creating
 * separate ones.
 * </p>
 */
public final class SharedScheduledExecutors {

    public static final int DEFAULT_POOL_SIZE = 30;

    private static SharedScheduledExecutorService singleThreadExecutor =
            new SharedScheduledExecutorService(
                    newSingleThreadScheduledExecutor(
                            groupedThreads("onos/shared/scheduled",
                                           "onos-single-executor")));

    private static SharedScheduledExecutorService poolThreadExecutor =
            new SharedScheduledExecutorService(
                    newScheduledThreadPool(DEFAULT_POOL_SIZE,
                            groupedThreads("onos/shared/scheduled",
                                           "onos-pool-executor-%d")));

    // Ban public construction
    private SharedScheduledExecutors() {
    }

    /**
     * Returns the shared scheduled single thread executor.
     *
     * @return shared scheduled single thread executor
     */
    public static SharedScheduledExecutorService getSingleThreadExecutor() {
        return singleThreadExecutor;
    }

    /**
     * Executes one-shot timer task on shared thread pool.
     *
     * @param task timer task to execute
     * @param delay before executing the task
     * @param unit of delay
     * @return a ScheduledFuture representing pending completion of the task
     *         and whose get() method will return null upon completion
     */
    public static ScheduledFuture<?> newTimeout(Runnable task, long delay, TimeUnit unit) {
        return SharedScheduledExecutors.getPoolThreadExecutor()
                .schedule(task, delay, unit);
    }

    /**
     * Returns the shared scheduled thread pool executor.
     *
     * @return shared scheduled executor pool
     */
    public static SharedScheduledExecutorService getPoolThreadExecutor() {
        return poolThreadExecutor;
    }

    /**
     * Configures the shared scheduled thread pool size.
     *
     * @param poolSize new pool size
     */
    public static void setPoolSize(int poolSize) {
        checkArgument(poolSize > 0, "Shared pool size size must be greater than 0");
        poolThreadExecutor.setBackingExecutor(
                newScheduledThreadPool(poolSize, groupedThreads("onos/shared/scheduled",
                                                                "onos-pool-executor-%d")));
    }

    /**
     * Shuts down all shared scheduled executors.
     * This is not intended to be called by application directly.
     */
    public static void shutdown() {
        singleThreadExecutor.backingExecutor().shutdown();
        poolThreadExecutor.backingExecutor().shutdown();
    }
}
