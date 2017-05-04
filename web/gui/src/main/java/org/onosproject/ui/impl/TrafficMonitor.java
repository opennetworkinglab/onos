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
 *
 */

package org.onosproject.ui.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.impl.topo.util.IntentSelection;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.impl.topo.util.TopoIntentFilter;
import org.onosproject.ui.impl.topo.util.TrafficLink;
import org.onosproject.ui.impl.topo.util.TrafficLink.StatsType;
import org.onosproject.ui.impl.topo.util.TrafficLinkMap;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.Highlights.Amount;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.LinkHighlight.Flavor;
import org.onosproject.ui.topo.NodeHighlight;
import org.onosproject.ui.topo.NodeSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.ui.impl.TrafficMonitorBase.Mode.RELATED_INTENTS;
import static org.onosproject.ui.impl.TrafficMonitorBase.Mode.SELECTED_INTENT;

/**
 * Encapsulates the behavior of monitoring specific traffic patterns.
 */
public class TrafficMonitor extends TrafficMonitorBase {

    private static final Logger log =
            LoggerFactory.getLogger(TrafficMonitor.class);

    private final TopologyViewMessageHandler msgHandler;
    private final TopoIntentFilter intentFilter;

    private NodeSelection selectedNodes = null;
    private IntentSelection selectedIntents = null;


    /**
     * Constructs a traffic monitor.
     *
     * @param trafficPeriod  traffic task period in ms
     * @param servicesBundle bundle of services
     * @param msgHandler     our message handler
     */
    public TrafficMonitor(long trafficPeriod, ServicesBundle servicesBundle,
                          TopologyViewMessageHandler msgHandler) {
        super(trafficPeriod, servicesBundle);
        this.msgHandler = msgHandler;

        intentFilter = new TopoIntentFilter(servicesBundle);
    }

    // =======================================================================
    // === API ===

    // monitor(Mode) is now implemented in the super class

    /**
     * Monitor for traffic data to be sent back to the web client, under
     * the given mode, using the given selection of devices and hosts.
     * In the case of "device link flows", this causes a background traffic
     * task to be scheduled to repeatedly compute and transmit the appropriate
     * traffic data to the client. In the case of "related intents", no
     * repeating task is scheduled.
     * <p>
     * The monitoring mode is expected to be one of:
     * <ul>
     * <li>DEV_LINK_FLOWS</li>
     * <li>RELATED_INTENTS</li>
     * </ul>
     *
     * @param mode          monitoring mode
     * @param nodeSelection how to select a node
     */
    public synchronized void monitor(Mode mode, NodeSelection nodeSelection) {
        log.debug("monitor: {} -- {}", mode, nodeSelection);
        this.mode = mode;
        this.selectedNodes = nodeSelection;

        switch (mode) {
            case DEV_LINK_FLOWS:
                // only care about devices (not hosts)
                if (selectedNodes.devicesWithHover().isEmpty()) {
                    clearAll();
                } else {
                    scheduleTask();
                    sendDeviceLinkFlows();
                }
                break;

            case RELATED_INTENTS:
                if (selectedNodes.none()) {
                    clearAll();
                } else {
                    selectedIntents = new IntentSelection(selectedNodes, intentFilter);
                    if (selectedIntents.none()) {
                        clearAll();
                    } else {
                        sendSelectedIntents();
                    }
                }
                break;

            default:
                log.debug("Unexpected call to monitor({}, {})", mode, nodeSelection);
                clearAll();
                break;
        }
    }

    // TODO: move this out to the "h2h/multi-intent app"

    /**
     * Monitor for traffic data to be sent back to the web client, for the
     * given intent.
     *
     * @param intent the intent to monitor
     */
    public synchronized void monitor(Intent intent) {
        log.debug("monitor intent: {}", intent.id());
        selectedNodes = null;
        selectedIntents = new IntentSelection(intent);
        mode = SELECTED_INTENT;
        scheduleTask();
        sendSelectedIntentTraffic();
    }

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
    protected void sendClearHighlights() {
        log.debug("sendClearHighlights");
        msgHandler.sendHighlights(new Highlights());
    }

    @Override
    protected void clearSelection() {
        selectedNodes = null;
        selectedIntents = null;
    }


    private void sendSelectedIntents() {
        log.debug("sendSelectedIntents: {}", selectedIntents);
        msgHandler.sendHighlights(intentGroup());
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

    private Highlights intentGroup() {
        Highlights highlights = new Highlights();

        if (selectedIntents != null && !selectedIntents.none()) {
            // If 'all' intents are selected, they will all have primary
            // highlighting; otherwise, the specifically selected intent will
            // have primary highlighting, and the remainder will have secondary
            // highlighting.
            Set<Intent> primary;
            Set<Intent> secondary;
            int count = selectedIntents.size();

            Set<Intent> allBut = new HashSet<>(selectedIntents.intents());
            Intent current;

            if (selectedIntents.all()) {
                primary = allBut;
                secondary = Collections.emptySet();
                log.debug("Highlight all intents ({})", count);
            } else {
                current = selectedIntents.current();
                primary = new HashSet<>();
                primary.add(current);
                allBut.remove(current);
                secondary = allBut;
                log.debug("Highlight intent: {} ([{}] of {})",
                          current.id(), selectedIntents.index(), count);
            }

            highlightIntentLinks(highlights, primary, secondary);
        }
        return highlights;
    }

    private Highlights intentTraffic() {
        Highlights highlights = new Highlights();

        if (selectedIntents != null && selectedIntents.single()) {
            Intent current = selectedIntents.current();
            Set<Intent> primary = new HashSet<>();
            primary.add(current);
            log.debug("Highlight traffic for intent: {} ([{}] of {})",
                      current.id(), selectedIntents.index(), selectedIntents.size());

            highlightIntentLinksWithTraffic(highlights, primary);
            highlights.subdueAllElse(Amount.MINIMALLY);
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
                links.add(createEdgeLink(host, false));
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

    private void highlightIntentLinks(Highlights highlights,
                                      Set<Intent> primary, Set<Intent> secondary) {
        TrafficLinkMap linkMap = new TrafficLinkMap();
        // NOTE: highlight secondary first, then primary, so that links shared
        //       by intents are colored correctly ("last man wins")
        createTrafficLinks(highlights, linkMap, secondary, Flavor.SECONDARY_HIGHLIGHT, false);
        createTrafficLinks(highlights, linkMap, primary, Flavor.PRIMARY_HIGHLIGHT, false);
        colorLinks(highlights, linkMap);
    }

    private void highlightIntentLinksWithTraffic(Highlights highlights,
                                                 Set<Intent> primary) {
        TrafficLinkMap linkMap = new TrafficLinkMap();
        createTrafficLinks(highlights, linkMap, primary, Flavor.PRIMARY_HIGHLIGHT, true);
        colorLinks(highlights, linkMap);
    }

    private void createTrafficLinks(Highlights highlights,
                                    TrafficLinkMap linkMap, Set<Intent> intents,
                                    Flavor flavor, boolean showTraffic) {
        for (Intent intent : intents) {
            List<Intent> installables = services.intent()
                    .getInstallableIntents(intent.key());
            Iterable<Link> links = null;
            if (installables != null) {
                for (Intent installable : installables) {

                    if (installable instanceof PathIntent) {
                        links = ((PathIntent) installable).path().links();
                    } else if (installable instanceof FlowRuleIntent) {
                        Collection<Link> l = new ArrayList<>();
                        l.addAll(linkResources(installable));
                        // Add cross connect links
                        if (intent instanceof OpticalConnectivityIntent) {
                            OpticalConnectivityIntent ocIntent = (OpticalConnectivityIntent) intent;
                            LinkService linkService = services.link();
                            DeviceService deviceService = services.device();
                            l.addAll(linkService.getDeviceIngressLinks(ocIntent.getSrc().deviceId()).stream()
                                    .filter(i ->
                                            deviceService.getDevice(i.src().deviceId()).type() == Device.Type.SWITCH)
                                    .collect(Collectors.toList()));
                            l.addAll(linkService.getDeviceEgressLinks(ocIntent.getDst().deviceId()).stream()
                                    .filter(e ->
                                            deviceService.getDevice(e.dst().deviceId()).type() == Device.Type.SWITCH)
                                    .collect(Collectors.toList()));
                        }
                        links = l;
                    } else if (installable instanceof FlowObjectiveIntent) {
                        links = linkResources(installable);
                    } else if (installable instanceof LinkCollectionIntent) {
                        links = ((LinkCollectionIntent) installable).links();
                    } else if (installable instanceof OpticalPathIntent) {
                        links = ((OpticalPathIntent) installable).path().links();
                    }

                    if (links == null) {
                        links = Lists.newArrayList();
                    }

                    links = addEdgeLinksIfNeeded(intent, Lists.newArrayList(links));

                    boolean isOptical = intent instanceof OpticalConnectivityIntent;
                    processLinks(linkMap, links, flavor, isOptical, showTraffic);
                    updateHighlights(highlights, links);
                }
            }
        }
    }

    private Iterable<Link> addEdgeLinksIfNeeded(Intent parentIntent,
                                                Collection<Link> links) {
        if (parentIntent instanceof HostToHostIntent) {
            links = new HashSet<>(links);
            HostToHostIntent h2h = (HostToHostIntent) parentIntent;
            Host h1 = services.host().getHost(h2h.one());
            Host h2 = services.host().getHost(h2h.two());
            links.add(createEdgeLink(h1, true));
            links.add(createEdgeLink(h2, true));
        }
        return links;
    }

    private void updateHighlights(Highlights highlights, Iterable<Link> links) {
        for (Link link : links) {
            ensureNodePresent(highlights, link.src().elementId());
            ensureNodePresent(highlights, link.dst().elementId());
        }
    }

    private void ensureNodePresent(Highlights highlights, ElementId eid) {
        String id = eid.toString();
        NodeHighlight nh = highlights.getNode(id);
        if (nh == null) {
            if (eid instanceof DeviceId) {
                nh = new DeviceHighlight(id);
                highlights.add((DeviceHighlight) nh);
            } else if (eid instanceof HostId) {
                nh = new HostHighlight(id);
                highlights.add((HostHighlight) nh);
            }
        }
    }

    // Extracts links from the specified flow rule intent resources
    private Collection<Link> linkResources(Intent installable) {
        ImmutableList.Builder<Link> builder = ImmutableList.builder();
        installable.resources().stream().filter(r -> r instanceof Link)
                .forEach(r -> builder.add((Link) r));
        return builder.build();
    }

    private void processLinks(TrafficLinkMap linkMap, Iterable<Link> links,
                              Flavor flavor, boolean isOptical,
                              boolean showTraffic) {
        if (links != null) {
            for (Link link : links) {
                TrafficLink tlink = linkMap.add(link);
                tlink.tagFlavor(flavor);
                tlink.optical(isOptical);
                if (showTraffic) {
                    tlink.addLoad(getLinkFlowLoad(link));
                    tlink.antMarch(true);
                }
            }
        }
    }

    private void colorLinks(Highlights highlights, TrafficLinkMap linkMap) {
        for (TrafficLink tlink : linkMap.biLinks()) {
            highlights.add(tlink.highlight(StatsType.TAGGED));
        }
    }

}
