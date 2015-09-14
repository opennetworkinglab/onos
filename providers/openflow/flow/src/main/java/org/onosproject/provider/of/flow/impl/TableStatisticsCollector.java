/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onlab.util.SharedExecutors;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFTableStatsRequest;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Collects Table statistics for the specified switch.
 */
class TableStatisticsCollector {

    private final Logger log = getLogger(getClass());

    public static final int SECONDS = 1000;

    private final OpenFlowSwitch sw;
    private Timer timer;
    private TimerTask task;

    private int pollInterval;

    /**
     * Creates a new table statistics collector for the given switch and poll frequency.
     *
     * @param timer        timer to use for scheduling
     * @param sw           switch to pull
     * @param pollInterval poll frequency in seconds
     */
    TableStatisticsCollector(Timer timer, OpenFlowSwitch sw, int pollInterval) {
        this.timer = timer;
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
        task.cancel();
        task = new InternalTimerTask();
        timer.scheduleAtFixedRate(task, pollInterval * SECONDS, pollInterval * 1000);
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
        // Initially start polling quickly. Then drop down to configured value
        log.debug("Starting Table Stats collection thread for {}", sw.getStringId());
        task = new InternalTimerTask();
        SharedExecutors.getTimer().scheduleAtFixedRate(task, 1 * SECONDS,
                                                       pollInterval * SECONDS);
    }

    public synchronized void stop() {
        log.debug("Stopping Table Stats collection thread for {}", sw.getStringId());
        task.cancel();
        task = null;
    }

}
