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

package org.onosproject.provider.of.group.impl;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.util.Timer;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsRequest;
import org.projectfloodlight.openflow.protocol.OFGroupStatsRequest;
import org.projectfloodlight.openflow.types.OFGroup;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/*
 * Sends Group Stats Request and collect the group statistics with a time interval.
 */
public class GroupStatsCollector implements TimerTask {

    private final HashedWheelTimer timer = Timer.getTimer();
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
    public GroupStatsCollector(OpenFlowSwitch sw, int interval) {
        this.sw = sw;
        this.refreshInterval = interval;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        log.trace("Collecting stats for {}", sw.getStringId());

        sendGroupStatistic();

        if (!this.stopTimer) {
            log.trace("Scheduling stats collection in {} seconds for {}",
                    this.refreshInterval, this.sw.getStringId());
            timeout.getTimer().newTimeout(this, refreshInterval,
                    TimeUnit.SECONDS);
        }
    }

    private void sendGroupStatistic() {
        if (log.isTraceEnabled()) {
            log.trace("sendGroupStatistics {}:{}", sw.getStringId(), sw.getRole());
        }
        if (sw.getRole() != RoleState.MASTER) {
            return;
        }
        if (!sw.isConnected()) {
            return;
        }
        Long statsXid = OpenFlowGroupProvider.getXidAndAdd(2);
        OFGroupStatsRequest statsRequest = sw.factory().buildGroupStatsRequest()
                .setGroup(OFGroup.ALL)
                .setXid(statsXid)
                .build();
        sw.sendMsg(statsRequest);

        Long descXid = statsXid + 1;
        OFGroupDescStatsRequest descStatsRequest =
                sw.factory().buildGroupDescStatsRequest()
                        .setXid(descXid)
                        .build();
        sw.sendMsg(descStatsRequest);
    }

    /**
     * Starts the collector.
     */
    public void start() {
        log.info("Starting Group Stats collection thread for {}", sw.getStringId());
        timeout = timer.newTimeout(this, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the collector.
     */
    public void stop() {
        log.info("Stopping Group Stats collection thread for {}", sw.getStringId());
        this.stopTimer = true;
        timeout.cancel();
    }
}
