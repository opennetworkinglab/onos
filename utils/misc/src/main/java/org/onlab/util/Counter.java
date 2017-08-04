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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Counting mechanism capable of tracking occurrences and rates.
 */
public class Counter {

    private long total = 0;
    private long start = System.currentTimeMillis();
    private long end = 0;

    /**
     * Creates a new counter.
     */
    public Counter() {
    }

    /**
     * Creates a new counter in a specific state. If non-zero end time is
     * specified, the counter will be frozen.
     *
     * @param start start time
     * @param total total number of items to start with
     * @param end   end time; if non-ze
     */
    public Counter(long start, long total, long end) {
        checkArgument(start <= end, "Malformed interval: start > end");
        checkArgument(total >= 0, "Total must be non-negative");
        this.start = start;
        this.total = total;
        this.end = end;
    }

    /**
     * Resets the counter, by zeroing out the count and restarting the timer.
     */
    public synchronized void reset() {
        end = 0;
        total = 0;
        start = System.currentTimeMillis();
    }

    /**
     * Freezes the counter in the current state including the counts and times.
     */
    public synchronized void freeze() {
        end = System.currentTimeMillis();
    }

    /**
     * Adds the specified number of occurrences to the counter.  No-op if the
     * counter has been frozen.
     *
     * @param count number of occurrences
     */
    public synchronized void add(long count) {
        checkArgument(count >= 0, "Count must be non-negative");
        if (end == 0L) {
            total += count;
        }
    }

    /**
     * Returns the number of occurrences per second.
     *
     * @return throughput in occurrences per second
     */
    public synchronized double throughput() {
        return total / duration();
    }

    /**
     * Returns the total number of occurrences counted.
     *
     * @return number of counted occurrences
     */
    public synchronized long total() {
        return total;
    }

    /**
     * Returns the duration expressed in fractional number of seconds.
     *
     * @return fractional number of seconds since the last reset
     */
    public synchronized double duration() {
        //  Protect against 0 return by artificially setting duration to 1ms
        long duration = (end == 0L ? System.currentTimeMillis() : end) - start;
        return (duration == 0 ? 1 : duration) / 1000.0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, start, end);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Counter) {
            final Counter other = (Counter) obj;
            return Objects.equals(this.total, other.total) &&
                    Objects.equals(this.start, other.start) &&
                    Objects.equals(this.end, other.end);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("total", total)
                .add("start", start)
                .add("end", end)
                .toString();
    }
}
