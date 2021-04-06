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

package org.onosproject.ui.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.net.Device;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.PortStatisticsService.MetricType;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.statistic.Load;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiTopoHighlighter;
import org.onosproject.ui.UiTopoHighlighterFactory;
import org.onosproject.ui.impl.topo.TopoologyTrafficMessageHandlerAbstract;
import org.onosproject.ui.impl.topo.util.IntentSelection;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.impl.topo.util.TopoIntentFilter;
import org.onosproject.ui.impl.topo.util.TrafficLink;
import org.onosproject.ui.impl.topo.util.TrafficLinkMap;
import org.onosproject.ui.topo.AbstractTopoMonitor;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.LinkHighlight;
import org.onosproject.ui.topo.NodeHighlight;
import org.onosproject.ui.topo.NodeSelection;
import org.onosproject.ui.topo.TopoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.statistic.PortStatisticsService.MetricType.BYTES;
import static org.onosproject.net.statistic.PortStatisticsService.MetricType.PACKETS;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLinks;
import static org.onosproject.ui.impl.TrafficMonitorBase.Mode.*;

/**
 * Base superclass for traffic monitor (both 'classic' and 'topo2' versions).
 */
public abstract class TrafficMonitorBase extends AbstractTopoMonitor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // 4 Kilo Bytes as threshold
    protected static final double BPS_THRESHOLD = 4 * TopoUtils.N_KILO;
    protected final TopoIntentFilter intentFilter;
    protected IntentSelection selectedIntents = null;
    protected final TopoologyTrafficMessageHandlerAbstract msgHandler;
    protected NodeSelection selectedNodes = null;
    protected UiTopoHighlighter topoHighlighter = null;

    protected void sendSelectedIntents() {
        log.debug("sendSelectedIntents: {}", selectedIntents);
        msgHandler.sendHighlights(intentGroup());
    }

    protected void ensureNodePresent(Highlights highlights, ElementId eid) {
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

    protected void colorLinks(Highlights highlights, TrafficLinkMap linkMap) {
        for (TrafficLink tlink : linkMap.biLinks()) {
            highlights.add(tlink.highlight(TrafficLink.StatsType.TAGGED));
        }
    }

    protected void processLinks(TrafficLinkMap linkMap, Iterable<Link> links,
                                LinkHighlight.Flavor flavor, boolean isOptical,
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

    protected void updateHighlights(Highlights highlights, Iterable<Link> links) {
        for (Link link : links) {
            ensureNodePresent(highlights, link.src().elementId());
            ensureNodePresent(highlights, link.dst().elementId());
        }
    }

    protected Iterable<Link> addEdgeLinksIfNeeded(Intent parentIntent,
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

    // Extracts links from the specified flow rule intent resources
    protected Collection<Link> linkResources(Intent installable) {
        ImmutableList.Builder<Link> builder = ImmutableList.builder();
        installable.resources().stream().filter(r -> r instanceof Link)
                .forEach(r -> builder.add((Link) r));
        return builder.build();
    }

    protected void createTrafficLinks(Highlights highlights,
                                      TrafficLinkMap linkMap, Set<Intent> intents,
                                      LinkHighlight.Flavor flavor, boolean showTraffic) {
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

    protected void highlightIntentLinks(Highlights highlights,
                                        Set<Intent> primary, Set<Intent> secondary) {
        TrafficLinkMap linkMap = new TrafficLinkMap();
        // NOTE: highlight secondary first, then primary, so that links shared
        //       by intents are colored correctly ("last man wins")
        createTrafficLinks(highlights, linkMap, secondary, LinkHighlight.Flavor.SECONDARY_HIGHLIGHT, false);
        createTrafficLinks(highlights, linkMap, primary, LinkHighlight.Flavor.PRIMARY_HIGHLIGHT, false);
        colorLinks(highlights, linkMap);
    }

    protected Highlights intentGroup() {
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

    protected Highlights intentTraffic() {
        Highlights highlights = new Highlights();

        if (selectedIntents != null && selectedIntents.single()) {
            Intent current = selectedIntents.current();
            Set<Intent> primary = new HashSet<>();
            primary.add(current);
            log.debug("Highlight traffic for intent: {} ([{}] of {})",
                                     current.id(), selectedIntents.index(), selectedIntents.size());

            highlightIntentLinksWithTraffic(highlights, primary);
            highlights.subdueAllElse(Highlights.Amount.MINIMALLY);
        }
        return highlights;
    }

    private void highlightIntentLinksWithTraffic(Highlights highlights,
                                                 Set<Intent> primary) {
        TrafficLinkMap linkMap = new TrafficLinkMap();
        createTrafficLinks(highlights, linkMap, primary, LinkHighlight.Flavor.PRIMARY_HIGHLIGHT, true);
        colorLinks(highlights, linkMap);
    }


    /**
     * Designates the different modes of operation.
     */
    public enum Mode {
        IDLE,
        ALL_FLOW_TRAFFIC_BYTES,
        ALL_PORT_TRAFFIC_BIT_PS,
        ALL_PORT_TRAFFIC_PKT_PS,
        DEV_LINK_FLOWS,
        RELATED_INTENTS,
        SELECTED_INTENT,
        CUSTOM_TRAFFIC_MONITOR
    }


    /**
     * Holds references to services.
     */
    protected final ServicesBundle services;

    /**
     * Current operating mode.
     */
    protected Mode mode = Mode.IDLE;

    private final Timer timer;
    private TimerTask trafficTask = null;

    /**
     * Constructs the monitor, initializing the task period and
     * services bundle reference.
     *
     * @param servicesBundle bundle of services
     * @param msgHandler Traffic Message handler
     */
    protected TrafficMonitorBase(ServicesBundle servicesBundle,
                                 TopoologyTrafficMessageHandlerAbstract msgHandler) {
        this.services = servicesBundle;
        this.msgHandler = msgHandler;
        timer = new Timer("uiTopo-" + getClass().getSimpleName());
        intentFilter = new TopoIntentFilter(servicesBundle);
    }

    /**
     * Initiates monitoring of traffic for a given mode.
     * This causes a background traffic task to be
     * scheduled to repeatedly compute and transmit the appropriate traffic
     * data to the client.
     * <p>
     * The monitoring mode is expected to be one of:
     * <ul>
     * <li>ALL_FLOW_TRAFFIC_BYTES</li>
     * <li>ALL_PORT_TRAFFIC_BIT_PS</li>
     * <li>ALL_PORT_TRAFFIC_PKT_PS</li>
     * <li>SELECTED_INTENT</li>
     * </ul>
     *
     * @param mode the monitoring mode
     */
    public synchronized void monitor(Mode mode) {
        this.mode = mode;

        switch (mode) {

            case ALL_FLOW_TRAFFIC_BYTES:
                clearSelection();
                scheduleTask();
                sendAllFlowTraffic();
                break;

            case ALL_PORT_TRAFFIC_BIT_PS:
                clearSelection();
                scheduleTask();
                sendAllPortTrafficBits();
                break;

            case ALL_PORT_TRAFFIC_PKT_PS:
                clearSelection();
                scheduleTask();
                sendAllPortTrafficPackets();
                break;

            case SELECTED_INTENT:
                sendSelectedIntentTraffic();
                scheduleTask();
                break;

            default:
                log.warn("Unexpected call to monitor({})", mode);
                clearAll();
                break;
        }
    }


    public synchronized void monitor(int index) {
        mode = CUSTOM_TRAFFIC_MONITOR;
        List<UiTopoHighlighterFactory> factories = services.get(UiExtensionService.class)
                .getTopoHighlighterFactories();
        if (factories.isEmpty()) {
            return;
        }

        UiTopoHighlighterFactory factory = factories.get(index % factories.size());
        topoHighlighter = factory.newTopoHighlighter();
        clearSelection();
        scheduleTask();
        sendCustomTraffic();
    }

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
     * Subclass should compile and send appropriate highlights data showing
     * flow traffic (bytes on links).
     */
    protected abstract void sendAllFlowTraffic();

    /**
     * Subclass should compile and send appropriate highlights data showing
     * bits per second, as computed using port stats.
     */
    protected abstract void sendAllPortTrafficBits();

    /**
     * Subclass should compile and send appropriate highlights data showing
     * packets per second, as computed using port stats.
     */
    protected abstract void sendAllPortTrafficPackets();

    /**
     * Subclass should compile and send appropriate highlights data showing
     * number of flows traversing links for the "selected" device(s).
     */
    protected abstract void sendDeviceLinkFlows();

    /**
     * Subclass should compile and send appropriate highlights data showing
     * traffic traversing links for the "selected" intent.
     */
    protected abstract void sendSelectedIntentTraffic();

    /**
     * Subclass should compile and send appropriate highlights data showing
     * custom traffic on links.
     */
    protected abstract void sendCustomTraffic();

    /**
     * Subclass should send a "clear highlights" event.
     */
    protected abstract void sendClearHighlights();

    /**
     * Subclasses should clear any selection state.
     */
    protected abstract void clearSelection();

    /**
     * Sets the mode to IDLE, clears the selection, cancels the background
     * task, and sends a clear highlights event to the client.
     */
    protected void clearAll() {
        this.mode = Mode.IDLE;
        clearSelection();
        cancelTask();
        sendClearHighlights();
    }

    /**
     * Schedules the background monitor task to run.
     */
    protected synchronized void scheduleTask() {
        if (trafficTask == null) {
            log.debug("Starting up background traffic task...");
            trafficTask = new TrafficUpdateTask();
            timer.schedule(trafficTask, trafficPeriod, trafficPeriod);
        } else {
            log.debug("(traffic task already running)");
        }
    }

    /**
     * Cancels the background monitor task.
     */
    protected synchronized void cancelTask() {
        if (trafficTask != null) {
            trafficTask.cancel();
            trafficTask = null;
        }
    }

    /**
     * Stops monitoring. (Invokes {@link #clearAll}, if not idle).
     */
    public synchronized void stopMonitoring() {
        log.debug("STOP monitoring");
        if (mode != IDLE) {
            clearAll();
        }
    }


    // =======================================================================
    // === Methods for computing traffic on links

    /**
     * Generates a {@link Highlights} object summarizing the traffic on the
     * network, ready to be transmitted back to the client for display on
     * the topology view.
     *
     * @param type the type of statistics to be displayed
     * @return highlights, representing links to be labeled/colored
     */
    protected Highlights trafficSummary(TrafficLink.StatsType type) {
        Highlights highlights = new Highlights();

        // TODO: consider whether a map would be better...
        Set<TrafficLink> linksWithTraffic = computeLinksWithTraffic(type);

        Set<TrafficLink> aggregatedLinks = doAggregation(linksWithTraffic);

        for (TrafficLink tlink : aggregatedLinks) {
            highlights.add(tlink.highlight(type));
        }
        return highlights;
    }

    /**
     * Generates a set of "traffic links" encapsulating information about the
     * traffic on each link (that is deemed to have traffic).
     *
     * @param type the type of statistics to be displayed
     * @return the set of links with traffic
     */
    protected Set<TrafficLink> computeLinksWithTraffic(TrafficLink.StatsType type) {
        TrafficLinkMap linkMap = new TrafficLinkMap();
        compileLinks(linkMap);
        addEdgeLinks(linkMap);

        Set<TrafficLink> linksWithTraffic = new HashSet<>();

        for (TrafficLink tlink : linkMap.biLinks()) {
            if (type == TrafficLink.StatsType.FLOW_STATS) {
                attachFlowLoad(tlink);
            } else if (type == TrafficLink.StatsType.PORT_STATS) {
                attachPortLoad(tlink, BYTES);
            } else if (type == TrafficLink.StatsType.PORT_PACKET_STATS) {
                attachPortLoad(tlink, PACKETS);
            }

            // we only want to report on links deemed to have traffic
            if (tlink.hasTraffic()) {
                linksWithTraffic.add(tlink);
            }
        }
        return linksWithTraffic;
    }

    /**
     * Iterates across the set of links in the topology and generates the
     * appropriate set of traffic links.
     *
     * @param linkMap link map to augment with traffic links
     */
    protected void compileLinks(TrafficLinkMap linkMap) {
        services.link().getLinks().forEach(linkMap::add);
    }

    /**
     * Iterates across the set of hosts in the topology and generates the
     * appropriate set of traffic links for the edge links.
     *
     * @param linkMap link map to augment with traffic links
     */
    protected void addEdgeLinks(TrafficLinkMap linkMap) {
        services.host().getHosts().forEach(host -> {
            // Ingress edge links
            Set<DefaultEdgeLink> edgeLinks = createEdgeLinks(host, true);
            edgeLinks.forEach(linkMap::add);
            // Egress edge links
            edgeLinks = createEdgeLinks(host, false);
            edgeLinks.forEach(linkMap::add);
        });
    }

    /**
     * Processes the given traffic link to attach the "flow load" attributed
     * to the underlying topology links.
     *
     * @param link the traffic link to process
     */
    protected void attachFlowLoad(TrafficLink link) {
        link.addLoad(getLinkFlowLoad(link.one()));
        link.addLoad(getLinkFlowLoad(link.two()));
    }

    /**
     * Returns the load for the given link, as determined by the statistics
     * service. May return null.
     *
     * @param link the link on which to look up the stats
     * @return the corresponding load (or null)
     */
    protected Load getLinkFlowLoad(Link link) {
        if (link != null && link.src().elementId() instanceof DeviceId) {
            return services.flowStats().load(link);
        }
        return null;
    }

    /**
     * Processes the given traffic link to attach the "port load" attributed
     * to the underlying topology links, for the specified metric type (either
     * bytes/sec or packets/sec).
     *
     * @param link       the traffic link to process
     * @param metricType the metric type (bytes or packets)
     */
    protected void attachPortLoad(TrafficLink link, MetricType metricType) {
        // For bi-directional traffic links, use
        // the max link rate of either direction
        // (we choose 'one' since we know that is never null)
        Link one = link.one();
        Load egressSrc = services.portStats().load(one.src(), metricType);
        Load egressDst = services.portStats().load(one.dst(), metricType);
        link.addLoad(maxLoad(egressSrc, egressDst), metricType == BYTES ? BPS_THRESHOLD : 0);
    }

    /**
     * Returns the load with the greatest rate.
     *
     * @param a load a
     * @param b load b
     * @return the larger of the two
     */
    protected Load maxLoad(Load a, Load b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.rate() > b.rate() ? a : b;
    }


    /**
     * Subclasses (well, Traffic2Monitor really) can override this method and
     * process the traffic links before generating the highlights object.
     * In particular, links that roll up into "synthetic links" between
     * regions should show aggregated data from the constituent links.
     * <p>
     * This default implementation does nothing.
     *
     * @param linksWithTraffic link data for all links
     * @return transformed link data appropriate to the region display
     */
    protected Set<TrafficLink> doAggregation(Set<TrafficLink> linksWithTraffic) {
        return linksWithTraffic;
    }


    // =======================================================================
    // === Background Task

    // Provides periodic update of traffic information to the client
    private class TrafficUpdateTask extends TimerTask {
        @Override
        public void run() {
            try {
                switch (mode) {
                    case ALL_FLOW_TRAFFIC_BYTES:
                        sendAllFlowTraffic();
                        break;
                    case ALL_PORT_TRAFFIC_BIT_PS:
                        sendAllPortTrafficBits();
                        break;
                    case ALL_PORT_TRAFFIC_PKT_PS:
                        sendAllPortTrafficPackets();
                        break;
                    case DEV_LINK_FLOWS:
                        sendDeviceLinkFlows();
                        break;
                    case SELECTED_INTENT:
                        sendSelectedIntentTraffic();
                        break;
                    case CUSTOM_TRAFFIC_MONITOR:
                        sendCustomTraffic();
                        break;

                    default:
                        // RELATED_INTENTS and IDLE modes should never invoke
                        // the background task, but if they do, they have
                        // nothing to do
                        break;
                }

            } catch (Exception e) {
                log.warn("Unable to process traffic task due to {}", e.getMessage());
                log.warn("Boom!", e);
            }
        }
    }
}
