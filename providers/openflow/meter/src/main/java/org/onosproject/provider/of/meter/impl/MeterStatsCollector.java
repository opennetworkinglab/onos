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

package org.onosproject.provider.of.meter.impl;

import org.onlab.util.Timer;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFMeterStatsRequest;
import org.slf4j.Logger;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/*
 * Sends Meter Stats Request and collect the Meter statistics with a time interval.
 */
public class MeterStatsCollector implements TimerTask {

    private final OpenFlowSwitch sw;
    private final Logger log = getLogger(getClass());
    private final int refreshInterval;

    private Timeout timeout;

    private boolean stopTimer = false;

    /**
     * Creates a GroupStatsCollector object.
     *
     * @param sw Open Flow switch
     * @param interval time interval for collecting group statistic
     */
    public MeterStatsCollector(OpenFlowSwitch sw, int interval) {
        this.sw = sw;
        this.refreshInterval = interval;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        if (!sw.isConnected()) {
            log.debug("Switch {} disconnected. Aborting meter stats collection", sw.getStringId());
            return;
        }

        log.trace("Collecting stats for {}", sw.getStringId());

        sendMeterStatisticRequest();

        if (!this.stopTimer) {
            log.trace("Scheduling stats collection in {} seconds for {}",
                    this.refreshInterval, this.sw.getStringId());
            timeout.timer().newTimeout(this, refreshInterval,
                    TimeUnit.SECONDS);
        }
    }

    public void sendMeterStatisticRequest() {
        if (log.isTraceEnabled()) {
            log.trace("sendMeterStatistics {}:{}", sw.getStringId(), sw.getRole());
        }
        if (sw.getRole() != RoleState.MASTER) {
            return;
        }

        OFMeterStatsRequest.Builder builder =
                sw.factory().buildMeterStatsRequest();
        builder.setXid(0).setMeterId(0xFFFFFFFF);

        sw.sendMsg(builder.build());

    }

    /**
     * Starts the collector.
     */
    public void start() {
        log.info("Starting Meter Stats collection thread for {}", sw.getStringId());
        timeout = Timer.newTimeout(this, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the collector.
     */
    public void stop() {
        log.info("Stopping Meter Stats collection thread for {}", sw.getStringId());
        this.stopTimer = true;
        timeout.cancel();
    }
}
