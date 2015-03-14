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


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * SharedExecutors is designed to use thread pool for applications.
 * SharedExecutors is recommended for applications instead of creating new thread groups.
 */
public final class SharedExecutors {

    private static int numberOfThreads = 30;

    private static ExecutorService singleThreadExecutor =
            Executors.newSingleThreadExecutor(groupedThreads("onos/shared", "onos-single-executor"));

    private static ExecutorService poolThreadExecutor =
            Executors.newFixedThreadPool(numberOfThreads, groupedThreads("onos/shared", "onos-pool-executor-%d"));

    private static java.util.Timer sharedTimer =
            new java.util.Timer("onos-shared-timer");



    private SharedExecutors() {

    }

    /**
     * Returns the single thread executor.
     *
     * @return shared single thread executor
     */
    public static ExecutorService getSingleThreadExecutor() {
        return singleThreadExecutor;
    }
    /**
     * Returns the pool thread executor.
     *
     * @return shared executor pool
     */
    public static ExecutorService getPoolThreadExecutor() {
        return poolThreadExecutor;
    }
    /**
     * Returns the pool timer.
     *
     * @return shared timer
     */
    public static java.util.Timer getTimer() {
        return sharedTimer;
    }




}
