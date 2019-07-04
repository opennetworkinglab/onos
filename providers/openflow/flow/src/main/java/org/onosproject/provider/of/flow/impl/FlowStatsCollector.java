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
package org.onosproject.provider.of.flow.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.util.SlidingWindowCounter;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Collects flow statistics for the specified switch.
 */
class FlowStatsCollector implements SwitchDataCollector {

    private final Logger log = getLogger(getClass());

    private static final int MS = 1000;

    // Number of ticks which defines the pause window.
    private static final int PAUSE_WINDOW = 2;
    // Number of ticks which defines the high load window
    private static final int HIGH_WINDOW = 60;
    // Number of ticks which defines the low load window
    private static final int LOW_WINDOW = 15;
    // Multiplier factor of the load
    private static final int LOAD_FACTOR = 2;
    // Event/s defining the min load rate
    private static final int MIN_LOAD_RATE = 50;
    // Event/s defining the max load rate
    private static final int MAX_LOAD_RATE = 500;

    private final OpenFlowSwitch sw;
    private ScheduledExecutorService executorService;
    private TimerTask pauseTask;
    private ScheduledFuture<?> scheduledPauseTask;
    private TimerTask pollTask;
    private ScheduledFuture<?> scheduledPollTask;

    private SlidingWindowCounter loadCounter;
    // Defines whether the collector is in pause or not for high load
    private final AtomicBoolean paused = new AtomicBoolean();
    // Defines whether the collector is in waiting or not for a previous stats reply
    private static final int WAITING_ATTEMPTS = 5;
    private final AtomicInteger waiting = new AtomicInteger(0);

    private int pollInterval;

    /**
     * Creates a new collector for the given switch and poll frequency.
     *
     * @param executorService executor used for scheduling
     * @param sw switch to pull
     * @param pollInterval poll frequency in seconds
     */
    FlowStatsCollector(ScheduledExecutorService executorService, OpenFlowSwitch sw, int pollInterval) {
        this.executorService = executorService;
        this.sw = checkNotNull(sw, "Null switch");
        this.pollInterval = pollInterval;
    }

    /**
     * Adjusts poll frequency.
     *
     * @param pollInterval poll frequency in seconds
     */
    synchronized void adjustPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
        if (pollTask != null) {
            pollTask.cancel();
        }
        if (scheduledPollTask != null) {
            scheduledPollTask.cancel(false);
        }
        // If we went through start - let's schedule it
        if (loadCounter != null) {
            pollTask = new PollTimerTask();
            scheduledPollTask = executorService.scheduleAtFixedRate(pollTask, pollInterval * MS,
                                                pollInterval * MS, TimeUnit.MILLISECONDS);
        }
        waiting.set(0);
    }

    /**
     * Resets the collector's event count.
     */
    @Override
    public synchronized void resetEvents() {
        loadCounter.clear();
        if (paused.compareAndSet(true, false)) {
            resume();
        }
        // Let's reset also waiting, the reply can be discarded/lost
        // during a change of mastership
        waiting.set(0);
    }

    /**
     * Records a number of flow events that have occurred.
     *
     * @param events the number of events that occurred
     */
    @Override
    public void recordEvents(int events) {
        SlidingWindowCounter loadCounter = this.loadCounter;
        if (loadCounter != null) {
            loadCounter.incrementCount(events);
        }
    }

    /**
     * Returns a boolean indicating whether the switch is under high load.
     * <p>
     * The switch is considered under high load if the average rate over the last two seconds is
     * greater than twice the overall rate or 50 flows/sec.
     *
     * @return indicates whether the switch is under high load
     */
    private boolean isHighLoad() {
        return loadCounter.getWindowRate(PAUSE_WINDOW)
            > max(min(loadCounter.getWindowRate(HIGH_WINDOW) * LOAD_FACTOR, MAX_LOAD_RATE), MIN_LOAD_RATE);
    }

    /**
     * Returns a boolean indicating whether the switch is under low load.
     * <p>
     * The switch is considered under low load if the average rate over the last 15 seconds is
     * less than the overall rate.
     *
     * @return indicates whether the switch is under low load
     */
    private boolean isLowLoad() {
        return loadCounter.getWindowRate(LOW_WINDOW) < loadCounter.getWindowRate(HIGH_WINDOW);
    }

    private class PauseTimerTask extends TimerTask {
        @Override
        public void run() {
            if (isHighLoad()) {
                if (paused.compareAndSet(false, true)) {
                    pause();
                }
            } else if (isLowLoad()) {
                if (paused.compareAndSet(true, false)) {
                    resume();
                }
            }
        }
    }

    private class PollTimerTask extends TimerTask {
        @Override
        public void run() {
            // Check whether we are still waiting a previous reply
            if (waiting.getAndDecrement() > 0) {
                log.debug("Skipping stats collection for {} waiting for previous reply", sw.getStringId());
                return;
            }
            // Check whether we are the master of the switch
            if (sw.getRole() == RoleState.MASTER) {
                // Check whether the switch is under high load from this master. This is done here in case a large
                // batch was pushed immediately prior to this task running.
                if (isHighLoad()) {
                    log.debug("Skipping stats collection for {} due to high load; rate: {}; overall: {}",
                              sw.getStringId(),
                              loadCounter.getWindowRate(PAUSE_WINDOW),
                              loadCounter.getWindowRate(HIGH_WINDOW));
                    return;
                } else {
                    log.debug(
                        "Permitting stats collection for {}; rate: {}; overall: {}",
                        sw.getStringId(),
                        loadCounter.getWindowRate(PAUSE_WINDOW),
                        loadCounter.getWindowRate(HIGH_WINDOW));
                }

                log.trace("Collecting stats for {}", sw.getStringId());
                OFFlowStatsRequest request = sw.factory().buildFlowStatsRequest()
                        .setMatch(sw.factory().matchWildcardAll())
                        .setTableId(TableId.ALL)
                        .setOutPort(OFPort.NO_MASK)
                        .build();
                sw.sendMsg(request);
                // Other flow stats will not be asked
                // if we don't see first the reply of this request
                waiting.set(WAITING_ATTEMPTS);
            }
        }
    }

    public synchronized void start() {
        log.debug("Starting Stats collection thread for {}", sw.getStringId());
        loadCounter = new SlidingWindowCounter(HIGH_WINDOW);
        if (pollInterval > 0) {
            pauseTask = new PauseTimerTask();
            scheduledPauseTask = executorService.scheduleAtFixedRate(pauseTask, 1 * MS,
                    1 * MS, TimeUnit.MILLISECONDS);
            pollTask = new PollTimerTask();
            // Initially start polling quickly. Then drop down to configured value
            scheduledPollTask = executorService.scheduleAtFixedRate(pollTask, 1 * MS,
                    pollInterval * MS, TimeUnit.MILLISECONDS);
        } else {
            // Trigger the poll only once
            pollTask = new PollTimerTask();
            executorService.schedule(pollTask, 0, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void pause() {
        if (pollTask != null) {
            log.debug("Pausing stats collection for {}; rate: {}; overall: {}",
                      sw.getStringId(),
                      loadCounter.getWindowRate(PAUSE_WINDOW),
                      loadCounter.getWindowRate(HIGH_WINDOW));
            pollTask.cancel();
            pollTask = null;
        }
        if (scheduledPollTask != null) {
            scheduledPollTask.cancel(false);
            scheduledPollTask = null;
        }
    }

    private synchronized void resume() {
        log.debug("Resuming stats collection for {}; rate: {}; overall: {}",
                  sw.getStringId(),
                  loadCounter.getWindowRate(PAUSE_WINDOW),
                  loadCounter.getWindowRate(HIGH_WINDOW));
        pollTask = new PollTimerTask();
        scheduledPollTask = executorService.scheduleAtFixedRate(pollTask, pollInterval * MS,
                                            pollInterval * MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (pauseTask != null) {
            pauseTask.cancel();
            pauseTask = null;
        }
        if (scheduledPauseTask != null) {
            scheduledPauseTask.cancel(false);
            scheduledPauseTask = null;
        }
        if (pollTask != null) {
            log.debug("Stopping Stats collection thread for {}", sw.getStringId());
            pollTask.cancel();
            pollTask = null;
        }
        if (scheduledPollTask != null) {
            scheduledPollTask.cancel(false);
            scheduledPollTask = null;
        }
        if (loadCounter != null) {
            loadCounter.destroy();
            loadCounter = null;
        }
    }

    public void received() {
        waiting.set(0);
    }

}
