package org.onlab.onos.event;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base implementation of an event accumulator. It allows triggering based on
 * event inter-arrival time threshold, maximum batch life threshold and maximum
 * batch size.
 */
public abstract class AbstractEventAccumulator implements EventAccumulator {

    private final Timer timer;
    private final int maxEvents;
    private final int maxBatchMillis;
    private final int maxIdleMillis;

    private TimerTask idleTask = new ProcessorTask();
    private TimerTask maxTask = new ProcessorTask();

    private List<Event> events = Lists.newArrayList();

    /**
     * Creates an event accumulator capable of triggering on the specified
     * thresholds.
     *
     * @param timer          timer to use for scheduling check-points
     * @param maxEvents      maximum number of events to accumulate before
     *                       processing is triggered
     * @param maxBatchMillis maximum number of millis allowed since the first
     *                       event before processing is triggered
     * @param maxIdleMillis  maximum number millis between events before
     *                       processing is triggered
     */
    protected AbstractEventAccumulator(Timer timer, int maxEvents,
                                       int maxBatchMillis, int maxIdleMillis) {
        this.timer = checkNotNull(timer, "Timer cannot be null");

        checkArgument(maxEvents > 1, "Maximum number of events must be > 1");
        checkArgument(maxBatchMillis > 0, "Maximum millis must be positive");
        checkArgument(maxIdleMillis > 0, "Maximum idle millis must be positive");

        this.maxEvents = maxEvents;
        this.maxBatchMillis = maxBatchMillis;
        this.maxIdleMillis = maxIdleMillis;
    }

    @Override
    public void add(Event event) {
        idleTask = cancelIfActive(idleTask);
        events.add(event);

        // Did we hit the max event threshold?
        if (events.size() == maxEvents) {
            maxTask = cancelIfActive(maxTask);
            schedule(1);
        } else {
            // Otherwise, schedule idle task and if this is a first event
            // also schedule the max batch age task.
            idleTask = schedule(maxIdleMillis);
            if (events.size() == 1) {
                maxTask = schedule(maxBatchMillis);
            }
        }
    }

    // Schedules a new processor task given number of millis in the future.
    private TimerTask schedule(int millis) {
        TimerTask task = new ProcessorTask();
        timer.schedule(task, millis);
        return task;
    }

    // Cancels the specified task if it is active.
    private TimerTask cancelIfActive(TimerTask task) {
        if (task != null) {
            task.cancel();
        }
        return task;
    }

    // Task for triggering processing of accumulated events
    private class ProcessorTask extends TimerTask {
        @Override
        public void run() {
            idleTask = cancelIfActive(idleTask);
            maxTask = cancelIfActive(maxTask);
            processEvents(finalizeCurrentBatch());
        }
    }

    // Demotes and returns the current batch of events and promotes a new one.
    private synchronized List<Event> finalizeCurrentBatch() {
        List<Event> toBeProcessed = events;
        events = Lists.newArrayList();
        return toBeProcessed;
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
     * Returns the maximum number of events allowed to accumulate before
     * processing is triggered.
     *
     * @return max number of events
     */
    public int maxEvents() {
        return maxEvents;
    }

    /**
     * Returns the maximum number of millis allowed to expire since the first
     * event before processing is triggered.
     *
     * @return max number of millis a batch is allowed to last
     */
    public int maxBatchMillis() {
        return maxBatchMillis;
    }

    /**
     * Returns the maximum number of millis allowed to expire since the last
     * event arrival before processing is triggered.
     *
     * @return max number of millis since the last event
     */
    public int maxIdleMillis() {
        return maxIdleMillis;
    }
}
