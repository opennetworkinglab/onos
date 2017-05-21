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

package org.onosproject.provider.pcep.tunnel.impl;


import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Timer;
import org.onosproject.pcep.api.PcepController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

/*
 * Sends Stats Request and collect the tunnel statistics with a time interval.
 */
public class TunnelStatsCollector implements TimerTask {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PcepController controller;

    private int refreshInterval;

    private String pcepTunnelId;
    private Timeout timeout;
    private volatile boolean stopped;


    /**
     * Create a tunnel status collector object.
     *
     * @param id              tunnel whose status data will be collected
     * @param refreshInterval time interval for collecting statistic
     */
    public TunnelStatsCollector(String id, int refreshInterval) {
        this.pcepTunnelId = id;
        this.refreshInterval = refreshInterval;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        if (stopped || timeout.isCancelled()) {
            return;
        }
        log.trace("Collecting stats for {}", pcepTunnelId);

        sendTunnelStatistic();
        if (!stopped && !timeout.isCancelled()) {
            log.trace("Scheduling stats collection in {} seconds for {}",
                      this.refreshInterval, pcepTunnelId);
            timeout.timer().newTimeout(this, refreshInterval, TimeUnit.SECONDS);
        }

    }

    private void sendTunnelStatistic() {
        controller.getTunnelStatistics(pcepTunnelId);

    }

    synchronized void adjustPollInterval(int pollInterval) {
        this.refreshInterval = pollInterval;
    }

    /**
     * Starts the collector.
     */
    public synchronized void start() {
        log.info("Starting Tunnel Stats collection thread for {}", pcepTunnelId);
        stopped = false;
        timeout = Timer.newTimeout(this, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the collector.
     */
    public synchronized void stop() {
        log.info("Stopping Tunnel Stats collection thread for {}", pcepTunnelId);
        stopped = true;
        timeout.cancel();
    }
}
