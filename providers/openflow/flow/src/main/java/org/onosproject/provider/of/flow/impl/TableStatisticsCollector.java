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
package org.onosproject.provider.of.flow.impl;

import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFTableStatsRequest;
import org.slf4j.Logger;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Collects Table statistics for the specified switch.
 */
class TableStatisticsCollector implements SwitchDataCollector {

    private final Logger log = getLogger(getClass());

    public static final long MS = 1000;

    private final OpenFlowSwitch sw;
    private ScheduledExecutorService executorService;
    private TimerTask task;
    private ScheduledFuture<?> scheduledTask;

    private int pollInterval;

    /**
     * Creates a new table statistics collector for the given switch and poll frequency.
     *
     * @param executorService executor used for scheduling
     * @param sw switch to pull
     * @param pollInterval poll frequency in seconds
     */
    TableStatisticsCollector(ScheduledExecutorService executorService, OpenFlowSwitch sw, int pollInterval) {
        this.executorService = executorService;
        this.sw = sw;
        this.pollInterval = pollInterval;
    }

    /**
     * Adjusts poll frequency.
     *
     * @param pollInterval poll frequency in seconds
     */
    synchronized void adjustPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
        if (task != null) {
            task.cancel();
        }
        task = new InternalTimerTask();
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduledTask = executorService.scheduleAtFixedRate(task, pollInterval * MS,
                                            pollInterval * MS, TimeUnit.MILLISECONDS);
    }

    private class InternalTimerTask extends TimerTask {
        @Override
        public void run() {
            if (sw.getRole() == RoleState.MASTER) {
                log.trace("Collecting stats for {}", sw.getStringId());
                OFTableStatsRequest request = sw.factory().buildTableStatsRequest()
                        .build();
                sw.sendMsg(request);
            }
        }
    }

    public synchronized void start() {
        log.debug("Starting Table Stats collection thread for {}", sw.getStringId());
        task = new InternalTimerTask();
        if (pollInterval > 0) {
            // Initially start polling quickly. Then drop down to configured value
            scheduledTask = executorService.scheduleAtFixedRate(task, 1 * MS,
                    pollInterval * MS, TimeUnit.MILLISECONDS);
        } else {
            // Trigger the poll only once
            executorService.schedule(task, 0, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() {
        log.debug("Stopping Table Stats collection thread for {}", sw.getStringId());
        if (task != null) {
            task.cancel();
        }
        task = null;
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduledTask = null;
    }

}
