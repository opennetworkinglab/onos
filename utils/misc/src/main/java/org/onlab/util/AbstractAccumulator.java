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

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base implementation of an item accumulator. It allows triggering based on
 * item inter-arrival time threshold, maximum batch life threshold and maximum
 * batch size.
 */
public abstract class AbstractAccumulator<T> implements Accumulator<T> {

    private Logger log = LoggerFactory.getLogger(AbstractAccumulator.class);

    private final Timer timer;
    private final int maxItems;
    private final int maxBatchMillis;
    private final int maxIdleMillis;

    private volatile TimerTask idleTask = new ProcessorTask();
    private volatile TimerTask maxTask = new ProcessorTask();

    private List<T> items = Lists.newArrayList();

    /**
     * Creates an item accumulator capable of triggering on the specified
     * thresholds.
     *
     * @param timer          timer to use for scheduling check-points
     * @param maxItems       maximum number of items to accumulate before
     *                       processing is triggered
     * @param maxBatchMillis maximum number of millis allowed since the first
     *                       item before processing is triggered
     * @param maxIdleMillis  maximum number millis between items before
     *                       processing is triggered
     */
    protected AbstractAccumulator(Timer timer, int maxItems,
                                  int maxBatchMillis, int maxIdleMillis) {
        this.timer = checkNotNull(timer, "Timer cannot be null");

        checkArgument(maxItems > 1, "Maximum number of items must be > 1");
        checkArgument(maxBatchMillis > 0, "Maximum millis must be positive");
        checkArgument(maxIdleMillis > 0, "Maximum idle millis must be positive");

        this.maxItems = maxItems;
        this.maxBatchMillis = maxBatchMillis;
        this.maxIdleMillis = maxIdleMillis;
    }

    @Override
    public synchronized void add(T item) {
        idleTask = cancelIfActive(idleTask);
        items.add(checkNotNull(item, "Item cannot be null"));

        // Did we hit the max item threshold?
        if (items.size() >= maxItems) {
            maxTask = cancelIfActive(maxTask);
            scheduleNow();
        } else {
            // Otherwise, schedule idle task and if this is a first item
            // also schedule the max batch age task.
            idleTask = schedule(maxIdleMillis);
            if (items.size() == 1) {
                maxTask = schedule(maxBatchMillis);
            }
        }
    }

    /**
     * Finalizes the current batch, if ready, and schedules a new processor
     * in the immediate future.
     */
    private void scheduleNow() {
        if (isReady()) {
            TimerTask task = new ProcessorTask(finalizeCurrentBatch());
            timer.schedule(task, 1);
        }
    }

    /**
     * Schedules a new processor task given number of millis in the future.
     * Batch finalization is deferred to time of execution.
     */
    private TimerTask schedule(int millis) {
        TimerTask task = new ProcessorTask();
        timer.schedule(task, millis);
        return task;
    }

    /**
     * Cancels the specified task if it is active.
     */
    private TimerTask cancelIfActive(TimerTask task) {
        if (task != null) {
            task.cancel();
        }
        return task;
    }

    // Task for triggering processing of accumulated items
    private class ProcessorTask extends TimerTask {

        private final List<T> items;

        // Creates a new processor task with deferred batch finalization.
        ProcessorTask() {
            this.items = null;
        }

        // Creates a new processor task with pre-emptive batch finalization.
        ProcessorTask(List<T> items) {
            this.items = items;
        }

        @Override
        public void run() {
            synchronized (AbstractAccumulator.this) {
                idleTask = cancelIfActive(idleTask);
            }
            if (isReady()) {
                try {
                    synchronized (AbstractAccumulator.this) {
                        maxTask = cancelIfActive(maxTask);
                    }
                    List<T> batch = items != null ? items : finalizeCurrentBatch();
                    if (!batch.isEmpty()) {
                        processItems(batch);
                    }
                } catch (Exception e) {
                    log.warn("Unable to process batch due to", e);
                }
            } else {
                synchronized (AbstractAccumulator.this) {
                    idleTask = schedule(maxIdleMillis);
                }
            }
        }
    }

    // Demotes and returns the current batch of items and promotes a new one.
    private synchronized List<T> finalizeCurrentBatch() {
        List<T> toBeProcessed = items;
        items = Lists.newArrayList();
        return toBeProcessed;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * Returns the backing timer.
     *
     * @return backing timer
     */
    public Timer timer() {
        return timer;
    }

    /**
     * Returns the maximum number of items allowed to accumulate before
     * processing is triggered.
     *
     * @return max number of items
     */
    public int maxItems() {
        return maxItems;
    }

    /**
     * Returns the maximum number of millis allowed to expire since the first
     * item before processing is triggered.
     *
     * @return max number of millis a batch is allowed to last
     */
    public int maxBatchMillis() {
        return maxBatchMillis;
    }

    /**
     * Returns the maximum number of millis allowed to expire since the last
     * item arrival before processing is triggered.
     *
     * @return max number of millis since the last item
     */
    public int maxIdleMillis() {
        return maxIdleMillis;
    }

}
