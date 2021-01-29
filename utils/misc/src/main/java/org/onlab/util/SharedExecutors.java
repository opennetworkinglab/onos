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

import org.onlab.metrics.MetricsService;

import java.util.Timer;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Utility for managing a set of shared execution resources, such as a timer,
 * single thread executor and thread pool executor for use by various parts of
 * the platform or by applications.
 * <p>
 * Whenever possible, use of these shared resources is encouraged over creating
 * separate ones.
 * </p>
 */
public final class SharedExecutors {

    public static final int DEFAULT_POOL_SIZE = 30;

    private static SharedExecutorService singleThreadExecutor;
    private static SharedExecutorService poolThreadExecutor;

    private static SharedTimer sharedTimer;
    private static final Object SHARED_TIMER_LOCK = new Object();

    // Ban public construction
    private SharedExecutors() {
    }

    /**
     * Returns the shared single thread executor.
     *
     * @return shared single thread executor
     */
    public static ExecutorService getSingleThreadExecutor() {
        setup();
        return singleThreadExecutor;
    }

    /**
     * Returns the shared thread pool executor.
     *
     * @return shared executor pool
     */
    public static ExecutorService getPoolThreadExecutor() {
        setup();
        return poolThreadExecutor;
    }

    /**
     * Returns the shared timer.
     *
     * @return shared timer
     */
    public static Timer getTimer() {
        setup();
        return sharedTimer;
    }

    /**
     * Sets the shared thread pool size.
     *
     * @param poolSize new pool size
     */
    public static void setPoolSize(int poolSize) {
        checkArgument(poolSize > 0, "Shared pool size size must be greater than 0");
        poolThreadExecutor.setBackingExecutor(
                newFixedThreadPool(poolSize, groupedThreads("onos/shared",
                                                            "onos-pool-executor-%d")));
    }

    /**
     * Enables or disables calculation of the pool performance metrics. If
     * the metrics service is not null metric collection will be enabled;
     * otherwise it will be disabled.
     *
     * @param metricsService optional metric service
     */
    public static void setMetricsService(MetricsService metricsService) {
        poolThreadExecutor.setMetricsService(metricsService);
    }

    /**
     * Shuts down all shared timers and executors and therefore should be
     * called only by the framework.
     */
    public static void shutdown() {
        synchronized (SHARED_TIMER_LOCK) {
            if (sharedTimer != null) {
                sharedTimer.shutdown();
                singleThreadExecutor.backingExecutor().shutdown();
                poolThreadExecutor.backingExecutor().shutdown();
                sharedTimer = null;
                singleThreadExecutor = null;
                poolThreadExecutor = null;
            }
        }
    }

    private static void setup() {
        synchronized (SHARED_TIMER_LOCK) {
            if (sharedTimer == null) {
                sharedTimer = new SharedTimer();

                singleThreadExecutor =
                    new SharedExecutorService(
                        newSingleThreadExecutor(groupedThreads("onos/shared",
                            "onos-single-executor")));

                poolThreadExecutor =
                    new SharedExecutorService(
                        newFixedThreadPool(DEFAULT_POOL_SIZE,
                            groupedThreads("onos/shared",
                                "onos-pool-executor-%d")));
            }
        }
    }

    // Timer extension which does not allow outside cancel method.
    private static class SharedTimer extends Timer {

        public SharedTimer() {
            super("onos-shared-timer");
        }

        @Override
        public void cancel() {
            throw new UnsupportedOperationException("Cancel of shared timer is not allowed");
        }

        private void shutdown() {
            super.cancel();
        }
    }

}
