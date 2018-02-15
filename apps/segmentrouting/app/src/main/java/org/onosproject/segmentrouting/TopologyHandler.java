/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.segmentrouting;

import com.google.common.collect.Maps;
import org.onosproject.event.Event;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.link.LinkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler for topology events.
 */
class TopologyHandler {

    // Logging instance
    private static final Logger log = LoggerFactory.getLogger(TopologyHandler.class);
    // Internal reference for SR manager and its services
    private final SegmentRoutingManager srManager;

    /**
     * Constructs the TopologyHandler.
     *
     * @param srManager Segment Routing manager
     */
    TopologyHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
    }

    // Check if the link event is valid
    private boolean isValid(LinkEvent linkEvent) {
        Link link = linkEvent.subject();
        // Verify if the link is valid with the link handler
        if (!srManager.linkHandler.isLinkValid(link)) {
            log.debug("Link {} ignored by the LinkHandler", link);
            return false;
        }
        // Processing for LINK_REMOVED
        if (linkEvent.type() == LinkEvent.Type.LINK_REMOVED) {
            // device availability check helps to ensure that multiple link-removed
            // events are actually treated as a single switch removed event.
            // purgeSeenLink is necessary so we do rerouting (instead of rehashing)
            // when switch comes back.
            if (link.src().elementId() instanceof DeviceId
                    && !srManager.deviceService.isAvailable(link.src().deviceId())) {
                log.debug("Link {} ignored device {} is down", link, link.src().deviceId());
                return false;
            }
            if (link.dst().elementId() instanceof DeviceId
                    && !srManager.deviceService.isAvailable(link.dst().deviceId())) {
                log.debug("Link {} ignored device {} is down", link, link.dst().deviceId());
                return false;
            }
            // LINK_REMOVED is ok
            return true;
        }
        // Processing for LINK_ADDED and LINK_UPDATED
        // Verify if source device is configured
        if (srManager.deviceConfiguration == null ||
                !srManager.deviceConfiguration.isConfigured(link.src().deviceId())) {
            // Source device is not configured, not valid for us
            log.warn("Source device of this link is not configured.. "
                             + "not processing further");
            return false;
        }
        // LINK_ADDED/LINK_UPDATED is ok
        return true;
    }

    // Check if the device event is valid
    private boolean isValid(DeviceEvent deviceEvent) {
        Device device = deviceEvent.subject();
        // We don't process the event if the device is available
        return !srManager.deviceService.isAvailable(device.id());
    }

    /**
     * Process the TOPOLOGY_CHANGE event. An initial optimization
     * is performed to avoid the processing of not relevant events.
     *
     * @param reasons list of events that triggered topology change
     */
    void processTopologyChange(List<Event> reasons) {
        // Store temporary in the map all the link events,
        // events having the same subject will be automatically
        // overridden.
        Map<Link, LinkEvent> linkEvents = Maps.newHashMap();
        // Store temporary in the map all the device events,
        // events having the same subject will be automatically
        // overridden.
        Map<DeviceId, DeviceEvent> deviceEvents = Maps.newHashMap();
        // Pre-process all the events putting them in the right map
        reasons.forEach(reason -> {
            // Relevant events for devices
            if (reason.type() == DeviceEvent.Type.DEVICE_ADDED ||
                    reason.type() == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                    reason.type() == DeviceEvent.Type.DEVICE_UPDATED) {
                // Take the event and save in the map
                DeviceEvent deviceEvent = (DeviceEvent) reason;
                deviceEvents.put(deviceEvent.subject().id(), deviceEvent);
                /// Relevant events for links
            } else if (reason.type() == LinkEvent.Type.LINK_ADDED ||
                    reason.type() == LinkEvent.Type.LINK_UPDATED ||
                    reason.type() == LinkEvent.Type.LINK_REMOVED) {
                // Take the event and store the link in the map
                LinkEvent linkEvent = (LinkEvent) reason;
                linkEvents.put(linkEvent.subject(), linkEvent);
                // Other events are not relevant
            } else {
                log.debug("Unhandled event type: {}", reason.type());
            }
        });
        // Verify if the link events are valid
        // before sent for mcast handling
        List<LinkEvent> toProcessLinkEvent = linkEvents.values()
                .stream()
                .filter(this::isValid)
                .collect(Collectors.toList());
        // Verify if the device events are valid
        // before sent for mcast handling
        List<DeviceEvent> toProcessDeviceEvent = deviceEvents.values()
                .stream()
                .filter(this::isValid)
                .collect(Collectors.toList());

        // Processing part of the received events
        // We don't need to process all LINK_ADDED
        boolean isLinkAdded = false;
        // Iterate on link events
        for (LinkEvent linkEvent : toProcessLinkEvent) {
            // We process just the first one
            if (linkEvent.type() == LinkEvent.Type.LINK_ADDED ||
                    linkEvent.type() == LinkEvent.Type.LINK_UPDATED) {
                // Other ones are skipped
                if (isLinkAdded) {
                    continue;
                }
                log.info("Processing - Event: {}", linkEvent);
                // First time, let's process it
                isLinkAdded = true;
                // McastHandler, reroute all the mcast tree
                srManager.mcastHandler.init();
            } else {
                log.info("Processing - Event: {}", linkEvent);
                // We compute each time a LINK_DOWN event
                srManager.mcastHandler.processLinkDown(linkEvent.subject());
            }
        }
        // Process all the device events
        toProcessDeviceEvent.forEach(deviceEvent -> {
            log.info("Processing - Event: {}", deviceEvent);
            srManager.mcastHandler.processDeviceDown(deviceEvent.subject().id());
        });
    }
}
