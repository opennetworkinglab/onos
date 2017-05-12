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

package org.onosproject.ui.impl;

import org.onosproject.incubator.net.PortStatisticsService.MetricType;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.statistic.Load;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.impl.topo.util.TrafficLink;
import org.onosproject.ui.impl.topo.util.TrafficLinkMap;
import org.onosproject.ui.topo.AbstractTopoMonitor;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.TopoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.onosproject.incubator.net.PortStatisticsService.MetricType.BYTES;
import static org.onosproject.incubator.net.PortStatisticsService.MetricType.PACKETS;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.ui.impl.TrafficMonitorBase.Mode.IDLE;

/**
 * Base superclass for traffic monitor (both 'classic' and 'topo2' versions).
 */
public abstract class TrafficMonitorBase extends AbstractTopoMonitor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // 4 Kilo Bytes as threshold
    protected static final double BPS_THRESHOLD = 4 * TopoUtils.N_KILO;


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
        SELECTED_INTENT
    }

    /**
     * Number of milliseconds between invocations of sending traffic data.
     */
    protected final long trafficPeriod;

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
     * @param trafficPeriod  traffic task period in ms
     * @param servicesBundle bundle of services
     */
    protected TrafficMonitorBase(long trafficPeriod,
                                 ServicesBundle servicesBundle) {
        this.trafficPeriod = trafficPeriod;
        this.services = servicesBundle;
        timer = new Timer("uiTopo-" + getClass().getSimpleName());
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
            linkMap.add(createEdgeLink(host, true));
            linkMap.add(createEdgeLink(host, false));
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
