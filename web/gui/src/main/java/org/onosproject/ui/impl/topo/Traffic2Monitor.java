/*
 * Copyright 2017-present Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import org.onosproject.ui.impl.TrafficMonitorBase;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the behavior of monitoring specific traffic patterns in the
 * Topology-2 view.
 */
public class Traffic2Monitor extends TrafficMonitorBase {

    private static final Logger log =
            LoggerFactory.getLogger(Traffic2Monitor.class);

    private final Topo2TrafficMessageHandler msgHandler;

    /**
     * Constructs a traffic monitor.
     *
     * @param trafficPeriod  traffic task period in ms
     * @param servicesBundle bundle of services
     * @param msgHandler     our message handler
     */
    public Traffic2Monitor(long trafficPeriod, ServicesBundle servicesBundle,
                           Topo2TrafficMessageHandler msgHandler) {
        super(trafficPeriod, servicesBundle);
        this.msgHandler = msgHandler;
    }

    @Override
    protected void sendAllFlowTraffic() {
        log.debug("TOPO-2-TRAFFIC: sendAllFlowTraffic");
        // TODO
    }

    @Override
    protected void sendAllPortTrafficBits() {
        log.debug("TOPO-2-TRAFFIC: sendAllPortTrafficBits");
        // TODO
    }

    @Override
    protected void sendAllPortTrafficPackets() {
        log.debug("TOPO-2-TRAFFIC: sendAllPortTrafficPackets");
        // TODO
    }

    @Override
    protected void sendClearHighlights() {
        log.debug("TOPO-2-TRAFFIC: sendClearHighlights");
        // TODO
    }


    // NOTE: currently this monitor holds no state - nothing to do for these...
    @Override
    protected void sendDeviceLinkFlows() {
    }

    @Override
    protected void sendSelectedIntentTraffic() {
    }

    @Override
    protected void clearSelection() {
    }
}
