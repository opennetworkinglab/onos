/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.provider.bmv2.device.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.net.Port;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Utility class to read port statistics from a BMv2 device.
 */
final class Bmv2PortStatisticsGetter {

    // TODO: make counters configuration dependent

    private static final String TABLE_NAME = "port_count_table";
    private static final String ACTION_NAME = "count_packet";
    private static final String EGRESS_COUNTER = "egress_port_counter";
    private static final String INGRESS_COUNTER = "ingress_port_counter";

    private static final Logger log = LoggerFactory.getLogger(Bmv2PortStatisticsGetter.class);

    private Bmv2PortStatisticsGetter() {
        // ban constructor.
    }

    /**
     * Returns a collection of port statistics for given ports using the given BMv2 device agent.
     *
     * @param deviceAgent a device agent
     * @param ports       a collection of ports
     * @return a collection of port statistics
     */
    static Collection<PortStatistics> getPortStatistics(Bmv2DeviceAgent deviceAgent, Collection<Port> ports) {

        List<PortStatistics> ps = Lists.newArrayList();

        for (Port port : ports) {
            int portNumber = (int) port.number().toLong();
            try {
                Pair<Long, Long> egressCounter = deviceAgent.readCounter(EGRESS_COUNTER, portNumber);
                Pair<Long, Long> ingressCounter = deviceAgent.readCounter(INGRESS_COUNTER, portNumber);
                ps.add(DefaultPortStatistics.builder()
                               .setPort(portNumber)
                               .setBytesSent(egressCounter.getLeft())
                               .setPacketsSent(egressCounter.getRight())
                               .setBytesReceived(ingressCounter.getLeft())
                               .setPacketsReceived(ingressCounter.getRight())
                               .build());
            } catch (Bmv2RuntimeException e) {
                log.info("Unable to read port statistics from {}: {}", port, e.explain());
            }
        }

        return ps;
    }

    /**
     * Initialize port counters on the given device agent.
     *
     * @param deviceAgent a device agent.
     */
    static void initCounters(Bmv2DeviceAgent deviceAgent) {
        try {
            deviceAgent.setTableDefaultAction(TABLE_NAME, Bmv2Action.builder().withName(ACTION_NAME).build());
        } catch (Bmv2RuntimeException e) {
            log.debug("Failed to provision counters on {}: {}", deviceAgent.deviceId(), e.explain());
        }
    }
}

