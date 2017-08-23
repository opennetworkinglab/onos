/*
 * Copyright 2016-present Open Networking Foundation
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
import com.google.common.collect.Sets;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Handles RouteEvent and manages routing entries.
 */
public class RouteHandler {
    private static final Logger log = LoggerFactory.getLogger(RouteHandler.class);
    private final SegmentRoutingManager srManager;

    RouteHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
    }

    protected void init(DeviceId deviceId) {
        srManager.routeService.getRouteTables().forEach(routeTableId ->
            srManager.routeService.getRoutes(routeTableId).forEach(routeInfo ->
                routeInfo.allRoutes().forEach(resolvedRoute ->
                    srManager.nextHopLocations(resolvedRoute).stream()
                            .filter(location -> deviceId.equals(location.deviceId()))
                            .forEach(location -> processRouteAddedInternal(resolvedRoute)
                    )
                )
            )
        );
    }

    void processRouteAdded(RouteEvent event) {
        log.info("processRouteAdded {}", event);
        processRouteAddedInternal(event.subject());
    }

    private void processRouteAddedInternal(ResolvedRoute route) {
        if (!isReady()) {
            log.info("System is not ready. Skip adding route for {}", route.prefix());
            return;
        }

        IpPrefix prefix = route.prefix();
        MacAddress nextHopMac = route.nextHopMac();
        VlanId nextHopVlan = route.nextHopVlan();
        ConnectPoint location = srManager.nextHopLocations(route).stream().findFirst().orElse(null);

        if (location == null) {
            log.info("{} ignored. Cannot find nexthop location", prefix);
            return;
        }

        srManager.deviceConfiguration.addSubnet(location, prefix);
        // XXX need to handle the case where there are two connectpoints
        srManager.defaultRoutingHandler.populateSubnet(Sets.newHashSet(location),
                                                       Sets.newHashSet(prefix));
        srManager.routingRulePopulator.populateRoute(location.deviceId(), prefix,
                nextHopMac, nextHopVlan, location.port());
    }

    void processRouteUpdated(RouteEvent event) {
        log.info("processRouteUpdated {}", event);
        processRouteRemovedInternal(event.prevSubject());
        processRouteAddedInternal(event.subject());
    }

    void processRouteRemoved(RouteEvent event) {
        log.info("processRouteRemoved {}", event);
        processRouteRemovedInternal(event.subject());
    }

    private void processRouteRemovedInternal(ResolvedRoute route) {
        if (!isReady()) {
            log.info("System is not ready. Skip removing route for {}", route.prefix());
            return;
        }

        IpPrefix prefix = route.prefix();
        MacAddress nextHopMac = route.nextHopMac();
        VlanId nextHopVlan = route.nextHopVlan();
        ConnectPoint location = srManager.nextHopLocations(route).stream().findFirst().orElse(null);

        if (location == null) {
            log.info("{} ignored. Cannot find nexthop location", prefix);
            return;
        }

        srManager.deviceConfiguration.removeSubnet(location, prefix);
        srManager.defaultRoutingHandler.revokeSubnet(ImmutableSet.of(prefix));
        srManager.routingRulePopulator.revokeRoute(
                location.deviceId(), prefix, nextHopMac, nextHopVlan, location.port());
    }

    private boolean isReady() {
        return Objects.nonNull(srManager.deviceConfiguration) &&
                Objects.nonNull(srManager.defaultRoutingHandler) &&
                Objects.nonNull(srManager.routingRulePopulator);
    }
}
