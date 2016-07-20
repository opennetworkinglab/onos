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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Maintains a sliding window of value counts. The sliding window counter is
 * initialized with a number of window slots. Calls to #incrementCount() will
 * increment the value in the current window slot. Periodically the window
 * slides and the oldest value count is dropped. Calls to #get() will get the
 * total count for the last N window slots.
 */
public final class SlidingWindowCounter {

    private volatile int headSlot;
    private final int windowSlots;

    private final List<AtomicLong> counters;

    private final ScheduledExecutorService background;

    private static final int SLIDE_WINDOW_PERIOD_SECONDS = 1;

    /**
     * Creates a new sliding window counter with the given total number of
     * window slots.
     *
     * @param windowSlots total number of window slots
     */
    public SlidingWindowCounter(int windowSlots) {
        checkArgument(windowSlots > 0, "Window size must be a positive integer");

        this.windowSlots = windowSlots;
        this.headSlot = 0;

        // Initialize each item in the list to an AtomicLong of 0
        this.counters = Collections.nCopies(windowSlots, 0)
                .stream()
                .map(AtomicLong::new)
                .collect(Collectors.toCollection(ArrayList::new));

        background = Executors.newSingleThreadScheduledExecutor();
        background.scheduleWithFixedDelay(this::advanceHead, 0,
                                          SLIDE_WINDOW_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Releases resources used by the SlidingWindowCounter.
     */
    public void destroy() {
        background.shutdownNow();
    }

    /**
     * Increments the count of the current window slot by 1.
     */
    public void incrementCount() {
        incrementCount(headSlot, 1);
    }

    /**
     * Increments the count of the current window slot by the given value.
     *
     * @param value value to increment by
     */
    public void incrementCount(long value) {
        incrementCount(headSlot, value);
    }

    private void incrementCount(int slot, long value) {
        counters.get(slot).addAndGet(value);
    }

    /**
     * Gets the total count for the last N window slots.
     *
     * @param slots number of slots to include in the count
     * @return total count for last N slots
     */
    public long get(int slots) {
        checkArgument(slots <= windowSlots,
                      "Requested window must be less than the total window slots");

        long sum = 0;

        for (int i = 0; i < slots; i++) {
            int currentIndex = headSlot - i;
            if (currentIndex < 0) {
                currentIndex = counters.size() + currentIndex;
            }
            sum += counters.get(currentIndex).get();
        }

        return sum;
    }

    void advanceHead() {
        counters.get(slotAfter(headSlot)).set(0);
        headSlot = slotAfter(headSlot);
    }

    private int slotAfter(int slot) {
        return (slot + 1) % windowSlots;
    }

}
