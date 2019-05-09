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

import com.google.common.collect.Sets;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostEvent;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.net.DeviceId;
import org.onosproject.routeservice.RouteInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(deviceId);

        srManager.routeService.getRouteTables().stream()
                .map(srManager.routeService::getRoutes)
                .flatMap(Collection::stream)
                .map(RouteInfo::allRoutes)
                .filter(allRoutes -> allRoutes.stream().allMatch(resolvedRoute ->
                        srManager.nextHopLocations(resolvedRoute).stream().allMatch(cp ->
                            deviceId.equals(cp.deviceId()) ||
                                    (pairDeviceId.isPresent() && pairDeviceId.get().equals(cp.deviceId()))
                        )))
                .forEach(this::processRouteAddedInternal);
    }

    void processRouteAdded(RouteEvent event) {
        processRouteAddedInternal(event.alternatives());
    }

    private void processRouteAddedInternal(Collection<ResolvedRoute> routes) {
        if (!isReady()) {
            log.info("System is not ready. Skip adding route for {}", routes);
            return;
        }

        log.info("processRouteAddedInternal. routes={}", routes);

        if (routes.size() > 2) {
            log.info("Route {} has more than two next hops. Do not process route change", routes);
            return;
        }

        Set<ConnectPoint> allLocations = Sets.newHashSet();
        Set<IpPrefix> allPrefixes = Sets.newHashSet();
        routes.forEach(route -> {
            allLocations.addAll(srManager.nextHopLocations(route));
            allPrefixes.add(route.prefix());
        });
        log.debug("RouteAdded. populateSubnet {}, {}", allLocations, allPrefixes);
        srManager.defaultRoutingHandler.populateSubnet(allLocations, allPrefixes);

        routes.forEach(route -> {
            IpPrefix prefix = route.prefix();
            MacAddress nextHopMac = route.nextHopMac();
            VlanId nextHopVlan = route.nextHopVlan();
            Set<ConnectPoint> locations = srManager.nextHopLocations(route);

            locations.forEach(location -> {
                log.debug("RouteAdded. addSubnet {}, {}", location, prefix);
                srManager.deviceConfiguration.addSubnet(location, prefix);
                log.debug("RouteAdded populateRoute {}, {}, {}, {}", location, prefix, nextHopMac, nextHopVlan);
                srManager.defaultRoutingHandler.populateRoute(location.deviceId(), prefix,
                        nextHopMac, nextHopVlan, location.port(), false);
            });
        });
    }

    void processRouteUpdated(RouteEvent event) {
        processRouteUpdatedInternal(Sets.newHashSet(event.alternatives()),
                Sets.newHashSet(event.prevAlternatives()));
    }

    void processAlternativeRoutesChanged(RouteEvent event) {
        processRouteUpdatedInternal(Sets.newHashSet(event.alternatives()),
                Sets.newHashSet(event.prevAlternatives()));
    }

    private void processRouteUpdatedInternal(Set<ResolvedRoute> routes, Set<ResolvedRoute> oldRoutes) {
        if (!isReady()) {
            log.info("System is not ready. Skip updating route for {} -> {}", oldRoutes, routes);
            return;
        }

        log.info("processRouteUpdatedInternal. routes={}, oldRoutes={}", routes, oldRoutes);

        if (routes.size() > 2) {
            log.info("Route {} has more than two next hops. Do not process route change", routes);
            return;
        }

        Set<ConnectPoint> allLocations = Sets.newHashSet();
        Set<IpPrefix> allPrefixes = Sets.newHashSet();
        routes.forEach(route -> {
            allLocations.addAll(srManager.nextHopLocations(route));
            allPrefixes.add(route.prefix());
        });

        // Just come back from an invalid next hop count
        // Revoke subnet from all locations and reset oldRoutes such that system will be reprogrammed from scratch
        if (oldRoutes.size() > 2) {
            log.info("Revoke subnet {} and reset oldRoutes");
            srManager.defaultRoutingHandler.revokeSubnet(allPrefixes);
            oldRoutes = Sets.newHashSet();
        }

        log.debug("RouteUpdated. populateSubnet {}, {}", allLocations, allPrefixes);
        srManager.defaultRoutingHandler.populateSubnet(allLocations, allPrefixes);

        Set<ResolvedRoute> toBeRemoved = Sets.difference(oldRoutes, routes).immutableCopy();
        Set<ResolvedRoute> toBeAdded = Sets.difference(routes, oldRoutes).immutableCopy();

        toBeRemoved.forEach(route -> {
            srManager.nextHopLocations(route).forEach(oldLocation -> {
                if (toBeAdded.stream().map(srManager::nextHopLocations)
                        .flatMap(Set::stream).map(ConnectPoint::deviceId)
                        .noneMatch(deviceId -> deviceId.equals(oldLocation.deviceId()))) {
                    IpPrefix prefix = route.prefix();
                    log.debug("RouteUpdated. removeSubnet {}, {}", oldLocation, prefix);
                    srManager.deviceConfiguration.removeSubnet(oldLocation, prefix);
                    // We don't remove the flow on the old location in occasion of two next hops becoming one
                    // since the populateSubnet will point the old location to the new location via spine.
                }
            });
        });

        toBeAdded.forEach(route -> {
            IpPrefix prefix = route.prefix();
            MacAddress nextHopMac = route.nextHopMac();
            VlanId nextHopVlan = route.nextHopVlan();
            Set<ConnectPoint> locations = srManager.nextHopLocations(route);

            locations.forEach(location -> {
                log.debug("RouteUpdated. addSubnet {}, {}", location, prefix);
                srManager.deviceConfiguration.addSubnet(location, prefix);
                log.debug("RouteUpdated. populateRoute {}, {}, {}, {}", location, prefix, nextHopMac, nextHopVlan);
                srManager.defaultRoutingHandler.populateRoute(location.deviceId(), prefix,
                        nextHopMac, nextHopVlan, location.port(), false);
            });
        });
    }

    void processRouteRemoved(RouteEvent event) {
        processRouteRemovedInternal(event.alternatives());
    }

    private void processRouteRemovedInternal(Collection<ResolvedRoute> routes) {
        if (!isReady()) {
            log.info("System is not ready. Skip removing route for {}", routes);
            return;
        }

        log.info("processRouteRemovedInternal. routes={}", routes);

        Set<IpPrefix> allPrefixes = Sets.newHashSet();
        routes.forEach(route -> {
            allPrefixes.add(route.prefix());
        });
        log.debug("RouteRemoved. revokeSubnet {}", allPrefixes);
        srManager.defaultRoutingHandler.revokeSubnet(allPrefixes);

        routes.forEach(route -> {
            IpPrefix prefix = route.prefix();
            MacAddress nextHopMac = route.nextHopMac();
            VlanId nextHopVlan = route.nextHopVlan();
            Set<ConnectPoint> locations = srManager.nextHopLocations(route);

            locations.forEach(location -> {
                log.debug("RouteRemoved. removeSubnet {}, {}", location, prefix);
                srManager.deviceConfiguration.removeSubnet(location, prefix);
                // We don't need to call revokeRoute again since revokeSubnet will remove the prefix
                // from all devices, including the ones that next hop attaches to.

                // Also remove redirection flows on the pair device if exists.
                Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(location.deviceId());
                Optional<PortNumber> pairLocalPort = srManager.getPairLocalPort(location.deviceId());
                if (pairDeviceId.isPresent() && pairLocalPort.isPresent()) {
                    // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                    //       when the host is untagged
                    VlanId vlanId = Optional.ofNullable(srManager.getInternalVlanId(location)).orElse(nextHopVlan);

                    log.debug("RouteRemoved. revokeRoute {}, {}, {}, {}", location, prefix, nextHopMac, nextHopVlan);
                    srManager.defaultRoutingHandler.revokeRoute(pairDeviceId.get(), prefix,
                            nextHopMac, vlanId, pairLocalPort.get(), false);
                }
            });
        });
    }

    void processHostMovedEvent(HostEvent event) {
        log.info("processHostMovedEvent {}", event);
        MacAddress hostMac = event.subject().mac();
        VlanId hostVlanId = event.subject().vlan();

        Set<HostLocation> prevLocations = event.prevSubject().locations();
        Set<HostLocation> newLocations = event.subject().locations();
        Set<ConnectPoint> connectPoints = newLocations.stream()
                .map(l -> (ConnectPoint) l).collect(Collectors.toSet());
        List<Set<IpPrefix>> batchedSubnets =
                srManager.deviceConfiguration.getBatchedSubnets(event.subject().id());
        Set<DeviceId> newDeviceIds = newLocations.stream().map(HostLocation::deviceId)
                .collect(Collectors.toSet());

        // Set of deviceIDs of the previous locations where the host was connected
        // Used to determine if host moved to different connect points
        // on same device or moved to a different device altogether
        Set<DeviceId> oldDeviceIds = prevLocations.stream().map(HostLocation::deviceId)
                .collect(Collectors.toSet());

        // L3 Ucast bucket needs to be updated only once per host
        // and only when the no. of routes with the host as next-hop is not zero
        if (!batchedSubnets.isEmpty()) {
           // For each new location, if NextObj exists for the host, update with new location ..
           Sets.difference(newLocations, prevLocations).forEach(newLocation -> {
                  int nextId = srManager.getMacVlanNextObjectiveId(newLocation.deviceId(),
                                                                   hostMac, hostVlanId, null, false);
                  VlanId vlanId = Optional.ofNullable(srManager.getInternalVlanId(newLocation)).orElse(hostVlanId);

                  if (nextId != -1) {
                      //Update the nextId group bucket
                      log.debug("HostMoved. NextId exists, update L3 Ucast Group Bucket {}, {}, {} --> {}",
                                             newLocation, hostMac, vlanId, nextId);
                      srManager.updateMacVlanTreatment(newLocation.deviceId(), hostMac, vlanId,
                                                       newLocation.port(), nextId);
                  } else {
                      log.debug("HostMoved. NextId does not exist for this location {}, host {}/{}",
                                              newLocation, hostMac, vlanId);
                  }
           });
        }

        batchedSubnets.forEach(subnets -> {
            log.debug("HostMoved. populateSubnet {}, {}", newLocations, subnets);
            srManager.defaultRoutingHandler.populateSubnet(connectPoints, subnets);

            subnets.forEach(prefix -> {
                // For each old location
                Sets.difference(prevLocations, newLocations).forEach(prevLocation -> {

                    // Remove flows for unchanged IPs only when the host moves from a switch to another.
                    // Otherwise, do not remove and let the adding part update the old flow
                    if (newDeviceIds.contains(prevLocation.deviceId())) {
                        return;
                    }

                    log.debug("HostMoved. removeSubnet {}, {}", prevLocation, prefix);
                    srManager.deviceConfiguration.removeSubnet(prevLocation, prefix);

                    // Do not remove flow from a device if the route is still reachable via its pair device.
                    // populateSubnet will update the flow to point to its pair device via spine.
                    DeviceId pairDeviceId = srManager.getPairDeviceId(prevLocation.deviceId()).orElse(null);
                    if (newLocations.stream().anyMatch(n -> n.deviceId().equals(pairDeviceId))) {
                        return;
                    }

                    log.debug("HostMoved. revokeRoute {}, {}, {}, {}", prevLocation, prefix, hostMac, hostVlanId);
                    srManager.defaultRoutingHandler.revokeRoute(prevLocation.deviceId(), prefix,
                            hostMac, hostVlanId, prevLocation.port(), false);
                });

                // For each new location, add all new IPs.
                Sets.difference(newLocations, prevLocations).forEach(newLocation -> {
                    log.debug("HostMoved. addSubnet {}, {}", newLocation, prefix);
                    srManager.deviceConfiguration.addSubnet(newLocation, prefix);

                    //its a new connect point, not a move from an existing device, populateRoute
                    if (!oldDeviceIds.contains(newLocation.deviceId())) {
                       log.debug("HostMoved. populateRoute {}, {}, {}, {}", newLocation, prefix, hostMac, hostVlanId);
                       srManager.defaultRoutingHandler.populateRoute(newLocation.deviceId(), prefix,
                              hostMac, hostVlanId, newLocation.port(), false);
                    }
                });
            });
        });

    }

    private boolean isReady() {
        return Objects.nonNull(srManager.deviceConfiguration) &&
                Objects.nonNull(srManager.defaultRoutingHandler);
    }
}
