/*
 * Copyright 2014-present Open Networking Foundation
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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

/**
 * Hashed-wheel timer singleton. Care must be taken to shutdown the timer
 * only when the VM is ready to exit.
 */
public final class Timer {

    private static volatile org.jboss.netty.util.HashedWheelTimer timer;

    private static final Supplier<HashedWheelTimer> TIMER =
            Suppliers.memoize(HashedWheelTimer::new);


    // Ban public construction
    private Timer() {
    }

    /**
     * Executes one-shot timer task on shared thread pool.
     *
     * @param task timer task to execute
     * @param delay before executing the task
     * @param unit of delay
     * @return a handle which is associated with the specified task
     */
    public static Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        return TIMER.get().newTimeout(task, delay, unit);
    }

}
