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

package org.onosproject.provider.of.device.impl;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.util.Timer;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFPortStatsRequest;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.slf4j.LoggerFactory.getLogger;

/*
 * Sends Group Stats Request and collect the group statistics with a time interval.
 */
public class PortStatsCollector implements TimerTask {

    // TODO: Refactoring is required using ScheduledExecutorService

    private final HashedWheelTimer timer = Timer.getTimer();
    private final OpenFlowSwitch sw;
    private final Logger log = getLogger(getClass());
    private int refreshInterval;
    private final AtomicLong xidAtomic = new AtomicLong(1);

    private Timeout timeout;
    private volatile boolean stopped;

    /**
     * Creates a GroupStatsCollector object.
     *
     * @param sw Open Flow switch
     * @param interval time interval for collecting group statistic
     */
    public PortStatsCollector(OpenFlowSwitch sw, int interval) {
        this.sw = sw;
        this.refreshInterval = interval;
    }

    @Override
    public void run(Timeout to) throws Exception {
        if (stopped || timeout.isCancelled()) {
            return;
        }
        log.trace("Collecting stats for {}", sw.getStringId());

        sendPortStatistic();

        if (!stopped && !timeout.isCancelled()) {
            log.trace("Scheduling stats collection in {} seconds for {}",
                    this.refreshInterval, this.sw.getStringId());
            timeout.getTimer().newTimeout(this, refreshInterval, TimeUnit.SECONDS);
        }
    }

    synchronized void adjustPollInterval(int pollInterval) {
        this.refreshInterval = pollInterval;
        // task.cancel();
        // task = new InternalTimerTask();
        // timer.scheduleAtFixedRate(task, pollInterval * SECONDS, pollInterval * 1000);
    }

    private void sendPortStatistic() {
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

    /**
     * Starts the collector.
     */
    public synchronized void start() {
        log.info("Starting Port Stats collection thread for {}", sw.getStringId());
        stopped = false;
        timeout = timer.newTimeout(this, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the collector.
     */
    public synchronized void stop() {
        log.info("Stopping Port Stats collection thread for {}", sw.getStringId());
        stopped = true;
        timeout.cancel();
    }
}
