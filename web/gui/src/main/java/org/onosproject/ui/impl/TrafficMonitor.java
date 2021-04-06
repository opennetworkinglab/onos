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
 *
 */

package org.onosproject.ui.impl;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.impl.topo.util.TrafficLink;
import org.onosproject.ui.impl.topo.util.TrafficLink.StatsType;
import org.onosproject.ui.impl.topo.util.TrafficLinkMap;
import org.onosproject.ui.topo.Highlights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLinks;
import static org.onosproject.ui.impl.TrafficMonitorBase.Mode.RELATED_INTENTS;
import static org.onosproject.ui.impl.TrafficMonitorBase.Mode.SELECTED_INTENT;

/**
 * Encapsulates the behavior of monitoring specific traffic patterns.
 */
public class TrafficMonitor extends TrafficMonitorBase {

    private static final Logger log =
            LoggerFactory.getLogger(TrafficMonitor.class);

    private final TopologyViewMessageHandler msgHandler;


    /**
     * Constructs a traffic monitor.
     *
     * @param servicesBundle bundle of services
     * @param msgHandler     our message handler
     */
    public TrafficMonitor(ServicesBundle servicesBundle, TopologyViewMessageHandler msgHandler) {
        super(servicesBundle, msgHandler);
        this.msgHandler = msgHandler;

    }

    // =======================================================================
    // === API ===

    // monitor(Mode) is now implemented in the super class

    // TODO: move this out to the "h2h/multi-intent app"

    /**
     * Selects the next intent in the select group (if there is one),
     * and sends highlighting data back to the web client to display
     * which path is selected.
     */
    public synchronized void selectNextIntent() {
        if (selectedIntents != null) {
            selectedIntents.next();
            sendSelectedIntents();
            if (mode == SELECTED_INTENT) {
                mode = RELATED_INTENTS;
            }
        }
    }

    /**
     * Selects the previous intent in the select group (if there is one),
     * and sends highlighting data back to the web client to display
     * which path is selected.
     */
    public synchronized void selectPreviousIntent() {
        if (selectedIntents != null) {
            selectedIntents.prev();
            sendSelectedIntents();
            if (mode == SELECTED_INTENT) {
                mode = RELATED_INTENTS;
            }
        }
    }

    /**
     * Resends selected intent traffic data. This is called, for example,
     * when the system detects an intent update happened.
     */
    public synchronized void pokeIntent() {
        if (mode == SELECTED_INTENT) {
            sendSelectedIntentTraffic();
        }
    }

    // =======================================================================
    // === Abstract method implementations ===

    @Override
    protected void sendAllFlowTraffic() {
        log.debug("sendAllFlowTraffic");
        msgHandler.sendHighlights(trafficSummary(StatsType.FLOW_STATS));
    }

    @Override
    protected void sendAllPortTrafficBits() {
        log.debug("sendAllPortTrafficBits");
        msgHandler.sendHighlights(trafficSummary(StatsType.PORT_STATS));
    }

    @Override
    protected void sendAllPortTrafficPackets() {
        log.debug("sendAllPortTrafficPackets");
        msgHandler.sendHighlights(trafficSummary(StatsType.PORT_PACKET_STATS));
    }

    @Override
    protected void sendDeviceLinkFlows() {
        log.debug("sendDeviceLinkFlows: {}", selectedNodes);
        msgHandler.sendHighlights(deviceLinkFlows());
    }

    @Override
    protected void sendSelectedIntentTraffic() {
        log.debug("sendSelectedIntentTraffic: {}", selectedIntents);
        msgHandler.sendHighlights(intentTraffic());
    }

    @Override
    protected void sendCustomTraffic() {
        log.debug("sendCustomTraffic");
        if (topoHighlighter != null) {
            msgHandler.sendHighlights(topoHighlighter.createHighlights());
        }
    }

    @Override
    protected void sendClearHighlights() {
        log.debug("sendClearHighlights");
        msgHandler.sendHighlights(new Highlights());
    }

    @Override
    protected void clearSelection() {
        selectedNodes = null;
        selectedIntents = null;
    }

    // =======================================================================
    // === Generate messages in JSON object node format

    // NOTE: trafficSummary(StatsType) => Highlights
    //        has been moved to the superclass

    // create highlights for links, showing flows for selected devices.
    private Highlights deviceLinkFlows() {
        Highlights highlights = new Highlights();

        if (selectedNodes != null && !selectedNodes.devicesWithHover().isEmpty()) {
            // capture flow counts on bilinks
            TrafficLinkMap linkMap = new TrafficLinkMap();

            for (Device device : selectedNodes.devicesWithHover()) {
                Map<Link, Integer> counts = getLinkFlowCounts(device.id());
                for (Link link : counts.keySet()) {
                    TrafficLink tlink = linkMap.add(link);
                    tlink.addFlows(counts.get(link));
                }
            }

            // now report on our collated links
            for (TrafficLink tlink : linkMap.biLinks()) {
                highlights.add(tlink.highlight(StatsType.FLOW_COUNT));
            }

        }
        return highlights;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    // Counts all flow entries that egress on the links of the given device.
    private Map<Link, Integer> getLinkFlowCounts(DeviceId deviceId) {
        // get the flows for the device
        List<FlowEntry> entries = new ArrayList<>();
        for (FlowEntry flowEntry : services.flow().getFlowEntries(deviceId)) {
            entries.add(flowEntry);
        }

        // get egress links from device, and include edge links
        Set<Link> links = new HashSet<>(services.link()
                                                .getDeviceEgressLinks(deviceId));
        Set<Host> hosts = services.host().getConnectedHosts(deviceId);
        if (hosts != null) {
            for (Host host : hosts) {
                links.addAll(createEdgeLinks(host, false));
            }
        }

        // compile flow counts per link
        Map<Link, Integer> counts = new HashMap<>();
        for (Link link : links) {
            counts.put(link, getEgressFlows(link, entries));
        }
        return counts;
    }

    // Counts all entries that egress on the link source port.
    private int getEgressFlows(Link link, List<FlowEntry> entries) {
        int count = 0;
        PortNumber out = link.src().port();
        for (FlowEntry entry : entries) {
            TrafficTreatment treatment = entry.treatment();
            for (Instruction instruction : treatment.allInstructions()) {
                if (instruction.type() == Instruction.Type.OUTPUT &&
                        ((OutputInstruction) instruction).port().equals(out)) {
                    count++;
                }
            }
        }
        return count;
    }

}
