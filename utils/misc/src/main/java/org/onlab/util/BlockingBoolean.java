/*
 * Copyright 2015-present Open Networking Laboratory
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
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Mutable boolean that allows threads to wait for a specified value.
 */
public class BlockingBoolean extends AbstractQueuedSynchronizer {

    private static final int TRUE = 1;
    private static final int FALSE = 0;

    /**
     * Creates a new blocking boolean with the specified value.
     *
     * @param value the starting value
     */
    public BlockingBoolean(boolean value) {
        setState(value ? TRUE : FALSE);
    }

    /**
     * Causes the current thread to wait until the boolean equals the specified
     * value unless the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * @param value specified value
     * @throws InterruptedException if interrupted while waiting
     */
    public void await(boolean value) throws InterruptedException {
        acquireSharedInterruptibly(value ? TRUE : FALSE);
    }

    /**
     * Causes the current thread to wait until the boolean equals the specified
     * value unless the thread is {@linkplain Thread#interrupt interrupted},
     * or the specified waiting time elapses.
     *
     * @param value specified value
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if the count reached zero and {@code false}
     *         if the waiting time elapsed before the count reached zero
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean await(boolean value, long timeout, TimeUnit unit)
            throws InterruptedException {
        return tryAcquireSharedNanos(value ? TRUE : FALSE, unit.toNanos(timeout));
    }

    protected int tryAcquireShared(int acquires) {
        return (getState() == acquires) ? 1 : -1;
    }

    /**
     * Sets the value of the blocking boolean.
     *
     * @param value new value
     */
    public void set(boolean value) {
        releaseShared(value ? TRUE : FALSE);
    }

    /**
     * Gets the value of the blocking boolean.
     *
     * @return current value
     */
    public boolean get() {
        return getState() == TRUE;
    }

    protected boolean tryReleaseShared(int releases) {
        // Signal on state change only
        int state = getState();
        if (state == releases) {
            return false;
        }
        return compareAndSetState(state, releases);
    }

}
