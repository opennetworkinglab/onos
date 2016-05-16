/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.statistic;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of a load.
 */
public class DefaultLoad implements Load {

    private final boolean isValid;
    private final long current;
    private final long previous;
    private final long time;
    private final long interval;

    /**
     * Indicates the flow statistics poll interval in seconds.
     */
    private static long pollInterval = 10; // same as IMMEDIATE and SHORT flow live type

    /**
     * Creates an invalid load.
     */
    public DefaultLoad() {
        this.isValid = false;
        this.time = System.currentTimeMillis();
        this.current = -1;
        this.previous = -1;
        this.interval = pollInterval;
    }

    /**
     * Creates a load value from the parameters.
     *
     * @param current  the current value
     * @param previous the previous value
     */
    public DefaultLoad(long current, long previous) {
        this(current, previous, pollInterval);
    }

    /**
     * Creates a load value from the parameters.
     *
     * @param current  the current value
     * @param previous the previous value
     * @param interval poll interval for this load
     */
    public DefaultLoad(long current, long previous, long interval) {
        checkArgument(interval > 0, "Interval must be greater than 0");
        this.current = current;
        this.previous = previous;
        this.time = System.currentTimeMillis();
        this.isValid = true;
        this.interval = interval;
    }

    /**
     * Sets the poll interval in seconds. Used solely for the purpose of
     * computing the load.
     *
     * @param newPollInterval poll interval duration in seconds
     */
    public static void setPollInterval(long newPollInterval) {
        pollInterval = newPollInterval;
    }

    @Override
    public long rate() {
        return (current - previous) / interval;
    }

    @Override
    public long latest() {
        return current;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("Load").add("rate", rate())
                .add("latest", latest()).toString();

    }
}
