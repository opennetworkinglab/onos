/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.metrics.topology;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.metrics.EventMetric;
import org.onlab.metrics.MetricsService;
import org.onlab.onos.event.Event;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyListener;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

/**
 * ONOS Topology Metrics Application that collects topology-related metrics.
 */
@Component(immediate = true)
@Service
public class TopologyMetrics implements TopologyMetricsService {
    private static final Logger log = getLogger(TopologyMetrics.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    private LinkedList<Event> lastEvents = new LinkedList<>();
    private static final int LAST_EVENTS_MAX_N = 100;

    private final DeviceListener deviceListener = new InnerDeviceListener();
    private final HostListener hostListener = new InnerHostListener();
    private final LinkListener linkListener = new InnerLinkListener();
    private final TopologyListener topologyListener =
        new InnerTopologyListener();

    //
    // Metrics
    //
    private static final String COMPONENT_NAME = "Topology";
    private static final String FEATURE_DEVICE_NAME = "DeviceEvent";
    private static final String FEATURE_HOST_NAME = "HostEvent";
    private static final String FEATURE_LINK_NAME = "LinkEvent";
    private static final String FEATURE_GRAPH_NAME = "GraphEvent";
    //
    // Event metrics:
    //  - Device events
    //  - Host events
    //  - Link events
    //  - Topology Graph events
    //
    private EventMetric topologyDeviceEventMetric;
    private EventMetric topologyHostEventMetric;
    private EventMetric topologyLinkEventMetric;
    private EventMetric topologyGraphEventMetric;

    @Activate
    protected void activate() {
        clear();
        registerMetrics();

        // Register for all topology-related events
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        linkService.addListener(linkListener);
        topologyService.addListener(topologyListener);

        log.info("ONOS Topology Metrics started.");
    }

    @Deactivate
    public void deactivate() {
        // De-register from all topology-related events
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        linkService.removeListener(linkListener);
        topologyService.removeListener(topologyListener);

        removeMetrics();
        clear();
        log.info("ONOS Topology Metrics stopped.");
    }

    @Override
    public List<Event> getEvents() {
        synchronized (lastEvents) {
            return ImmutableList.<Event>copyOf(lastEvents);
        }
    }

    @Override
    public EventMetric topologyDeviceEventMetric() {
        return topologyDeviceEventMetric;
    }

    @Override
    public EventMetric topologyHostEventMetric() {
        return topologyHostEventMetric;
    }

    @Override
    public EventMetric topologyLinkEventMetric() {
        return topologyLinkEventMetric;
    }

    @Override
    public EventMetric topologyGraphEventMetric() {
        return topologyGraphEventMetric;
    }

    /**
     * Records an event.
     *
     * @param event the event to record
     * @param eventMetric the Event Metric to use
     */
    private void recordEvent(Event event, EventMetric eventMetric) {
        synchronized (lastEvents) {
            eventMetric.eventReceived();

            //
            // Keep only the last N events, where N = LAST_EVENTS_MAX_N
            //
            while (lastEvents.size() >= LAST_EVENTS_MAX_N) {
                lastEvents.remove();
            }
            lastEvents.add(event);
        }
    }

    /**
     * Inner Device Event Listener class.
     */
    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            recordEvent(event, topologyDeviceEventMetric);
            log.debug("Device Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
        }
    }

    /**
     * Inner Host Event Listener class.
     */
    private class InnerHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            recordEvent(event, topologyHostEventMetric);
            log.debug("Host Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
        }
    }

    /**
     * Inner Link Event Listener class.
     */
    private class InnerLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            recordEvent(event, topologyLinkEventMetric);
            log.debug("Link Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
        }
    }

    /**
     * Inner Topology Event Listener class.
     */
    private class InnerTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            recordEvent(event, topologyGraphEventMetric);
            log.debug("Topology Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
            for (Event reason : event.reasons()) {
                log.debug("Topology Event Reason: time = {} type = {} event = {}",
                          reason.time(), reason.type(), reason);
            }
        }
    }

    /**
     * Clears the internal state.
     */
    private void clear() {
        synchronized (lastEvents) {
            lastEvents.clear();
        }
    }

    /**
     * Registers the metrics.
     */
    private void registerMetrics() {
        topologyDeviceEventMetric =
            new EventMetric(metricsService, COMPONENT_NAME,
                            FEATURE_DEVICE_NAME);
        topologyHostEventMetric =
            new EventMetric(metricsService, COMPONENT_NAME,
                            FEATURE_HOST_NAME);
        topologyLinkEventMetric =
            new EventMetric(metricsService, COMPONENT_NAME,
                            FEATURE_LINK_NAME);
        topologyGraphEventMetric =
            new EventMetric(metricsService, COMPONENT_NAME,
                            FEATURE_GRAPH_NAME);

        topologyDeviceEventMetric.registerMetrics();
        topologyHostEventMetric.registerMetrics();
        topologyLinkEventMetric.registerMetrics();
        topologyGraphEventMetric.registerMetrics();
    }

    /**
     * Removes the metrics.
     */
    private void removeMetrics() {
        topologyDeviceEventMetric.removeMetrics();
        topologyHostEventMetric.removeMetrics();
        topologyLinkEventMetric.removeMetrics();
        topologyGraphEventMetric.removeMetrics();
    }
}
