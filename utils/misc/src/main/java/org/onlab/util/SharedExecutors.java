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

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(SharedExecutors.class);

    // TODO: make this configurable via setPoolSize static method
    public static final int DEFAULT_THREAD_SIZE = 30;
    private static int poolSize = DEFAULT_THREAD_SIZE;

    private static ExecutorService singleThreadExecutor =
            newSingleThreadExecutor(groupedThreads("onos/shared",
                                                   "onos-single-executor"));

    private static ExecutorService poolThreadExecutor =
            newFixedThreadPool(poolSize, groupedThreads("onos/shared",
                                                               "onos-pool-executor-%d"));

    private static Timer sharedTimer = new Timer("onos-shared-timer");

    // Ban public construction
    private SharedExecutors() {
    }

    /**
     * Returns the shared single thread executor.
     *
     * @return shared single thread executor
     */
    public static ExecutorService getSingleThreadExecutor() {
        return singleThreadExecutor;
    }

    /**
     * Returns the shared thread pool executor.
     *
     * @return shared executor pool
     */
    public static ExecutorService getPoolThreadExecutor() {
        return poolThreadExecutor;
    }

    /**
     * Returns the shared timer.
     *
     * @return shared timer
     */
    public static Timer getTimer() {
        return sharedTimer;
    }

    /**
     * Sets the shared thread pool size.
     * @param poolSize
     */
    public static void setPoolSize(int poolSize) {
        if (poolSize > 0) {
            SharedExecutors.poolSize = poolSize;
            //TODO: wait for execution previous task in the queue .
            poolThreadExecutor.shutdown();
            poolThreadExecutor = newFixedThreadPool(poolSize, groupedThreads("onos/shared",
                    "onos-pool-executor-%d"));
        } else {
            log.warn("Shared Pool Size size must be greater than 0");
        }
    }
}
