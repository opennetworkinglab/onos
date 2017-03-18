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

package org.onosproject.segmentrouting;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles RouteEvent and manages routing entries.
 */
public class RouteHandler {
    private static final Logger log = LoggerFactory.getLogger(RouteHandler.class);
    private final SegmentRoutingManager srManager;

    public RouteHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
    }

    protected void init(DeviceId deviceId) {
        srManager.routeService.getRouteTables().forEach(routeTableId -> {
            srManager.routeService.getRoutes(routeTableId).forEach(routeInfo -> {
                routeInfo.allRoutes().stream()
                        .filter(resolvedRoute -> resolvedRoute.location() != null &&
                                resolvedRoute.location().deviceId().equals(deviceId))
                        .forEach(this::processRouteAddedInternal);
            });
        });
    }

    protected void processRouteAdded(RouteEvent event) {
        log.info("processRouteAdded {}", event);
        processRouteAddedInternal(event.subject());
    }

    private void processRouteAddedInternal(ResolvedRoute route) {
        IpPrefix prefix = route.prefix();
        MacAddress nextHopMac = route.nextHopMac();
        VlanId nextHopVlan = route.nextHopVlan();
        ConnectPoint location = route.location();

        srManager.deviceConfiguration.addSubnet(location, prefix);
        srManager.defaultRoutingHandler.populateSubnet(location, ImmutableSet.of(prefix));
        srManager.routingRulePopulator.populateRoute(location.deviceId(), prefix,
                nextHopMac, nextHopVlan, location.port());
    }

    protected void processRouteUpdated(RouteEvent event) {
        log.info("processRouteUpdated {}", event);
        processRouteRemovedInternal(event.prevSubject());
        processRouteAddedInternal(event.subject());
    }

    protected void processRouteRemoved(RouteEvent event) {
        log.info("processRouteRemoved {}", event);
        processRouteRemovedInternal(event.subject());
    }

    private void processRouteRemovedInternal(ResolvedRoute route) {
        IpPrefix prefix = route.prefix();
        MacAddress nextHopMac = route.nextHopMac();
        VlanId nextHopVlan = route.nextHopVlan();
        ConnectPoint location = route.location();

        srManager.deviceConfiguration.removeSubnet(location, prefix);
        srManager.defaultRoutingHandler.revokeSubnet(ImmutableSet.of(prefix));
        srManager.routingRulePopulator.revokeRoute(
                location.deviceId(), prefix, nextHopMac, nextHopVlan, location.port());
    }
}
