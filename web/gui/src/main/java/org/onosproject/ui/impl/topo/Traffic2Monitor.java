/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.model.topo.UiSynthLink;
import org.onosproject.ui.topo.Highlights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.onosproject.ui.model.topo.UiLinkId.uiLinkId;

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
     * @param servicesBundle bundle of services
     * @param msgHandler     our message handler
     */
    public Traffic2Monitor(ServicesBundle servicesBundle,
                           Topo2TrafficMessageHandler msgHandler) {
        super(servicesBundle, msgHandler);
        this.msgHandler = msgHandler;
    }

    @Override
    protected void sendAllFlowTraffic() {
        log.debug("TOPO-2-TRAFFIC: sendAllFlowTraffic");
        msgHandler.sendHighlights(trafficSummary(TrafficLink.StatsType.FLOW_STATS));
    }

    @Override
    protected void sendCustomTraffic() {
    }

    @Override
    protected void sendAllPortTrafficBits() {
        log.debug("TOPO-2-TRAFFIC: sendAllPortTrafficBits");
        msgHandler.sendHighlights(trafficSummary(TrafficLink.StatsType.PORT_STATS));
    }

    @Override
    protected void sendAllPortTrafficPackets() {
        log.debug("TOPO-2-TRAFFIC: sendAllPortTrafficPackets");
        msgHandler.sendHighlights(trafficSummary(TrafficLink.StatsType.PORT_PACKET_STATS));
    }

    @Override
    protected void sendClearHighlights() {
        log.debug("TOPO-2-TRAFFIC: sendClearHighlights");
        msgHandler.sendHighlights(new Highlights());
    }


    // NOTE: currently this monitor holds no state - nothing to do for these...
    @Override
    protected void sendDeviceLinkFlows() {
    }

    @Override
    protected void sendSelectedIntentTraffic() {
        log.debug("sendSelectedIntentTraffic: {}", selectedIntents);
        msgHandler.sendHighlights(intentTraffic());
    }

    @Override
    protected void clearSelection() {
        selectedNodes = null;
        selectedIntents = null;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // -- link aggregation


    @Override
    protected Set<TrafficLink> doAggregation(Set<TrafficLink> linksWithTraffic) {
        log.debug("Need to aggregate {} links", linksWithTraffic.size());

        // first, retrieve from the shared topology model those synth links that
        // are part of the region currently being viewed by the user...
        Map<UiLinkId, UiSynthLink> synthLinkMap =
                msgHandler.retrieveRelevantSynthLinks();

        // NOTE: compute Set<TrafficLink> which represents the consolidated links

        Map<UiLinkId, TrafficLink> mappedByUiLinkId = new HashMap<>();

        for (TrafficLink tl : linksWithTraffic) {
            UiLinkId tlid = uiLinkId(tl.key());
            UiSynthLink sl = synthLinkMap.get(tlid);
            if (sl != null) {
                UiLinkId aggrid = sl.link().id();
                TrafficLink aggregated =
                        mappedByUiLinkId.computeIfAbsent(aggrid, TrafficLink::new);
                aggregated.mergeStats(tl);
            }
        }

        Set<TrafficLink> result = new HashSet<>();
        result.addAll(mappedByUiLinkId.values());
        return result;
    }
}
