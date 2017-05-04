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
import org.onosproject.ui.impl.topo.util.TrafficLink;
import org.onosproject.ui.topo.Highlights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Encapsulates the behavior of monitoring specific traffic patterns in the
 * Topology-2 view.
 */
public class Traffic2Monitor extends TrafficMonitorBase {

    private static final Logger log =
            LoggerFactory.getLogger(Traffic2Monitor.class);

    // link back to our message handler (for outbound messages)
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
        Highlights h = trafficSummary(TrafficLink.StatsType.FLOW_STATS);

        // TODO
    }

    @Override
    protected void sendAllPortTrafficBits() {
        log.debug("TOPO-2-TRAFFIC: sendAllPortTrafficBits");
        Highlights h = trafficSummary(TrafficLink.StatsType.PORT_STATS);

        // TODO
    }

    @Override
    protected void sendAllPortTrafficPackets() {
        log.debug("TOPO-2-TRAFFIC: sendAllPortTrafficPackets");
        Highlights h = trafficSummary(TrafficLink.StatsType.PORT_PACKET_STATS);

        // TODO
    }

    @Override
    protected void sendClearHighlights() {
        log.debug("TOPO-2-TRAFFIC: sendClearHighlights");
        Highlights h = new Highlights();

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

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // -- link aggregation


    @Override
    protected Set<TrafficLink> doAggregation(Set<TrafficLink> linksWithTraffic) {
        // TODO: figure out how to aggregate the link data...
        log.debug("Need to aggregate {} links", linksWithTraffic.size());

        return linksWithTraffic;
    }
}
