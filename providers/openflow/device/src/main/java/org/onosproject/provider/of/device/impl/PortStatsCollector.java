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

package org.onosproject.provider.of.device.impl;

import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFPortStatsRequest;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sends Port Stats Request and collect the port statistics with a time interval.
 */
public class PortStatsCollector {

    private final Logger log = getLogger(getClass());

    private static final long SECONDS = 1000L;

    private OpenFlowSwitch sw;
    private Timer timer;
    private TimerTask task;

    private int refreshInterval;
    private final AtomicLong xidAtomic = new AtomicLong(1);

    /**
     * Creates a port states collector object.
     *
     * @param timer     timer to use for scheduling
     * @param sw        switch to pull
     * @param interval  interval for collecting port statistic
     */
    PortStatsCollector(Timer timer, OpenFlowSwitch sw, int interval) {
        this.timer = timer;
        this.sw = checkNotNull(sw, "Null switch");
        this.refreshInterval = interval;
    }

    private class InternalTimerTask extends TimerTask {

        @Override
        public void run() {
            sendPortStatisticRequest();
        }
    }

    /**
     * Starts the port statistic collector.
     */
    public synchronized void start() {
        log.info("Starting Port Stats collection thread for {}", sw.getStringId());
        task = new InternalTimerTask();
        timer.scheduleAtFixedRate(task, 1 * SECONDS,
                                  refreshInterval * SECONDS);
    }

    /**
     * Stops the port statistic collector.
     */
    public synchronized void stop() {
        log.info("Stopping Port Stats collection thread for {}", sw.getStringId());
        task.cancel();
        task = null;
    }

    /**
     * Adjusts poll interval of the port statistic collector and restart.
     *
     * @param pollInterval period of collecting port statistic
     */
    public synchronized void adjustPollInterval(int pollInterval) {
        this.refreshInterval = pollInterval;
        task.cancel();
        task = new InternalTimerTask();
        timer.scheduleAtFixedRate(task, refreshInterval * SECONDS,
                                  refreshInterval * SECONDS);
    }

    /**
     * Sends port statistic request to switch.
     */
    private void sendPortStatisticRequest() {
        if (sw.getRole() != RoleState.MASTER) {
            return;
        }
        Long statsXid = xidAtomic.getAndIncrement();
        OFPortStatsRequest statsRequest = sw.factory().buildPortStatsRequest()
                .setPortNo(OFPort.ANY)
                .setXid(statsXid)
                .build();
        sw.sendMsg(statsRequest);
    }
}