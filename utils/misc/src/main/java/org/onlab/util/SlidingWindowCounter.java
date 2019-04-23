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

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

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

    private final AtomicLong totalCount = new AtomicLong();
    private final AtomicLong totalSlots = new AtomicLong(1);

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
        this.counters = ImmutableList.copyOf(IntStream.range(0, windowSlots)
                .mapToObj(i -> new AtomicLong())
                .collect(Collectors.toList()));

        background = newSingleThreadScheduledExecutor(groupedThreads("SlidingWindowCounter", "bg-%d"));
        background.scheduleWithFixedDelay(this::advanceHead, 1,
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

    /**
     * Increments the count of the given window slot by the given value.
     *
     * @param slot the slot to increment
     * @param value the value by which to increment the slot
     */
    private void incrementCount(int slot, long value) {
        counters.get(slot).addAndGet(value);
        totalCount.addAndGet(value);
    }

    /**
     * Gets the total count for the last N window slots.
     *
     * @param slots number of slots to include in the count
     * @return total count for last N slots
     * @deprecated since 1.12
     */
    @Deprecated
    public long get(int slots) {
        return getWindowCount(slots);
    }

    /**
     * Gets the total count for all slots.
     *
     * @return total count for all slots
     */
    public long getWindowCount() {
        return getWindowCount(windowSlots);
    }

    /**
     * Gets the total count for the last N window slots.
     *
     * @param slots number of slots to include in the count
     * @return total count for last N slots
     */
    public long getWindowCount(int slots) {
        checkArgument(slots <= windowSlots,
                      "Requested window must be less than the total window slots");

        long sum = 0;

        slots = getMinSlots(slots);
        for (int i = 0; i < slots; i++) {
            int currentIndex = headSlot - i;
            if (currentIndex < 0) {
                currentIndex = counters.size() + currentIndex;
            }
            sum += counters.get(currentIndex).get();
        }

        return sum;
    }

    /**
     * Returns the average rate over the window.
     *
     * @return the average rate over the window
     */
    public double getWindowRate() {
        return getWindowRate(windowSlots);
    }

    /**
     * Returns the average rate over the given window.
     *
     * @param slots the number of slots to include in the window
     * @return the average rate over the given window
     */
    public double getWindowRate(int slots) {
        // Compute the minimum slots to before computing the window count to ensure
        // the window count and number of slots are for the same window.
        slots = getMinSlots(slots);
        return getWindowCount(slots) / (double) slots;
    }

    private int getMinSlots(int slots) {
        return min(slots, (int) min(totalSlots.get(), Integer.MAX_VALUE));
    }

    /**
     * Returns the overall number of increments.
     *
     * @return the overall number of increments
     */
    public long getOverallCount() {
        return totalCount.get();
    }

    /**
     * Returns the overall rate.
     *
     * @return the overall rate
     */
    public double getOverallRate() {
        return totalCount.get() / (double) totalSlots.get();
    }

    /**
     * Clears the counter.
     */
    public void clear() {
        counters.forEach(value -> value.set(0));
        totalCount.set(0);
        totalSlots.set(1);
        headSlot = 0;
    }

    void advanceHead() {
        counters.get(slotAfter(headSlot)).set(0);
        headSlot = slotAfter(headSlot);
        totalSlots.incrementAndGet();
    }

    private int slotAfter(int slot) {
        return (slot + 1) % windowSlots;
    }

}
