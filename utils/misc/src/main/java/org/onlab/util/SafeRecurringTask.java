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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for a recurring task which catches all exceptions to prevent task
 * being suppressed in a ScheduledExecutorService.
 */
public final class SafeRecurringTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SafeRecurringTask.class);

    private final Runnable runnable;

    /**
     * Constructor.
     *
     * @param runnable runnable to wrap
     */
    private SafeRecurringTask(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted()) {
            log.info("Task interrupted, quitting");
            return;
        }

        try {
            runnable.run();
        } catch (Exception e) {
            // Catch all exceptions to avoid task being suppressed
            log.error("Exception thrown during task", e);
        }
    }

    /**
     * Wraps a runnable in a safe recurring task.
     *
     * @param runnable runnable to wrap
     * @return safe recurring task
     */
    public static SafeRecurringTask wrap(Runnable runnable) {
        return new SafeRecurringTask(runnable);
    }

}
