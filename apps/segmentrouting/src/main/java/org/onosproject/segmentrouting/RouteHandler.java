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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
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

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles RouteEvent and manages routing entries.
 */
public class RouteHandler {
    private static final Logger log = LoggerFactory.getLogger(RouteHandler.class);
    private final SegmentRoutingManager srManager;

    private static final int WAIT_TIME_MS = 1000;
    /**
     * The routeEventCache is implemented to avoid race condition by giving more time to the
     * underlying flow subsystem to process previous populateSubnet call.
     */
    private Cache<IpPrefix, RouteEvent> routeEventCache = CacheBuilder.newBuilder()
            .expireAfterWrite(WAIT_TIME_MS, TimeUnit.MILLISECONDS)
            .removalListener((RemovalNotification<IpPrefix, RouteEvent> notification) -> {
                IpPrefix prefix = notification.getKey();
                RouteEvent routeEvent = notification.getValue();
                RemovalCause cause = notification.getCause();
                log.debug("routeEventCache removal event. prefix={}, routeEvent={}, cause={}",
                        prefix, routeEvent, cause);

                switch (notification.getCause()) {
                    case REPLACED:
                    case EXPIRED:
                        dequeueRouteEvent(routeEvent);
                        break;
                    default:
                        break;
                }
            }).build();

    RouteHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(routeEventCache::cleanUp, 0, WAIT_TIME_MS, TimeUnit.MILLISECONDS);
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
        enqueueRouteEvent(event);
    }

    private void processRouteAddedInternal(ResolvedRoute route) {
        processRouteAddedInternal(Sets.newHashSet(route));
    }

    private void processRouteAddedInternal(Collection<ResolvedRoute> routes) {
        if (!isReady()) {
            log.info("System is not ready. Skip adding route for {}", routes);
            return;
        }

        log.info("processRouteAddedInternal. routes={}", routes);

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
                        nextHopMac, nextHopVlan, location.port());
            });
        });
    }

    void processRouteUpdated(RouteEvent event) {
        enqueueRouteEvent(event);
    }

    void processAlternativeRoutesChanged(RouteEvent event) {
        enqueueRouteEvent(event);
    }

    private void processRouteUpdatedInternal(Set<ResolvedRoute> routes, Set<ResolvedRoute> oldRoutes) {
        if (!isReady()) {
            log.info("System is not ready. Skip updating route for {} -> {}", oldRoutes, routes);
            return;
        }

        log.info("processRouteUpdatedInternal. routes={}, oldRoutes={}", routes, oldRoutes);

        Set<ConnectPoint> allLocations = Sets.newHashSet();
        Set<IpPrefix> allPrefixes = Sets.newHashSet();
        routes.forEach(route -> {
            allLocations.addAll(srManager.nextHopLocations(route));
            allPrefixes.add(route.prefix());
        });
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
                   MacAddress nextHopMac = route.nextHopMac();
                   VlanId nextHopVlan = route.nextHopVlan();

                   log.debug("RouteUpdated. removeSubnet {}, {}", oldLocation, prefix);
                   srManager.deviceConfiguration.removeSubnet(oldLocation, prefix);
                   log.debug("RouteUpdated. revokeRoute {}, {}, {}, {}", oldLocation, prefix, nextHopMac, nextHopVlan);
                   srManager.defaultRoutingHandler.revokeRoute(oldLocation.deviceId(), prefix,
                           nextHopMac, nextHopVlan, oldLocation.port());
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
                        nextHopMac, nextHopVlan, location.port());
            });
        });

    }

    void processRouteRemoved(RouteEvent event) {
        enqueueRouteEvent(event);
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
                log.debug("RouteRemoved. revokeRoute {}, {}, {}, {}", location, prefix, nextHopMac, nextHopVlan);
                srManager.defaultRoutingHandler.revokeRoute(location.deviceId(), prefix,
                        nextHopMac, nextHopVlan, location.port());

                // Also remove redirection flows on the pair device if exists.
                Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(location.deviceId());
                Optional<PortNumber> pairLocalPort = srManager.getPairLocalPorts(location.deviceId());
                if (pairDeviceId.isPresent() && pairLocalPort.isPresent()) {
                    // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                    //       when the host is untagged
                    VlanId vlanId = Optional.ofNullable(srManager.getInternalVlanId(location)).orElse(nextHopVlan);

                    log.debug("RouteRemoved. revokeRoute {}, {}, {}, {}", location, prefix, nextHopMac, nextHopVlan);
                    srManager.defaultRoutingHandler.revokeRoute(pairDeviceId.get(), prefix,
                            nextHopMac, vlanId, pairLocalPort.get());
                }
            });
        });
    }

    void processHostMovedEvent(HostEvent event) {
        log.info("processHostMovedEvent {}", event);
        MacAddress hostMac = event.subject().mac();
        VlanId hostVlanId = event.subject().vlan();

        affectedRoutes(hostMac, hostVlanId).forEach(affectedRoute -> {
            IpPrefix prefix = affectedRoute.prefix();
            Set<HostLocation> prevLocations = event.prevSubject().locations();
            Set<HostLocation> newLocations = event.subject().locations();

            // For each old location
            Sets.difference(prevLocations, newLocations).stream().filter(srManager::isMasterOf)
                    .forEach(prevLocation -> {
                // Redirect the flows to pair link if configured
                // Note: Do not continue removing any rule
                Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(prevLocation.deviceId());
                Optional<PortNumber> pairLocalPort = srManager.getPairLocalPorts(prevLocation.deviceId());
                if (pairDeviceId.isPresent() && pairLocalPort.isPresent() && newLocations.stream()
                        .anyMatch(location -> location.deviceId().equals(pairDeviceId.get()))) {
                    // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                    //       when the host is untagged
                    VlanId vlanId = Optional.ofNullable(srManager.getInternalVlanId(prevLocation)).orElse(hostVlanId);
                    log.debug("HostMoved. populateRoute {}, {}, {}, {}", prevLocation, prefix, hostMac, vlanId);
                    srManager.defaultRoutingHandler.populateRoute(prevLocation.deviceId(), prefix,
                            hostMac, vlanId, pairLocalPort.get());
                    return;
                }

                // No pair information supplied. Remove route
                log.debug("HostMoved. revokeRoute {}, {}, {}, {}", prevLocation, prefix, hostMac, hostVlanId);
                srManager.defaultRoutingHandler.revokeRoute(prevLocation.deviceId(), prefix,
                        hostMac, hostVlanId, prevLocation.port());
            });

            // For each new location, add all new IPs.
            Sets.difference(newLocations, prevLocations).stream().filter(srManager::isMasterOf)
                    .forEach(newLocation -> {
                log.debug("HostMoved. populateRoute {}, {}, {}, {}", newLocation, prefix, hostMac, hostVlanId);
                srManager.defaultRoutingHandler.populateRoute(newLocation.deviceId(), prefix,
                        hostMac, hostVlanId, newLocation.port());
            });

        });
    }

    private Set<ResolvedRoute> affectedRoutes(MacAddress mac, VlanId vlanId) {
        return srManager.routeService.getRouteTables().stream()
                .map(routeTableId -> srManager.routeService.getRoutes(routeTableId))
                .flatMap(Collection::stream)
                .map(RouteInfo::allRoutes)
                .flatMap(Collection::stream)
                .filter(resolvedRoute -> mac.equals(resolvedRoute.nextHopMac()) &&
                        vlanId.equals(resolvedRoute.nextHopVlan())).collect(Collectors.toSet());
    }

    private boolean isReady() {
        return Objects.nonNull(srManager.deviceConfiguration) &&
                Objects.nonNull(srManager.defaultRoutingHandler);
    }

    void enqueueRouteEvent(RouteEvent routeEvent) {
        log.debug("Enqueue routeEvent {}", routeEvent);
        routeEventCache.put(routeEvent.subject().prefix(), routeEvent);
    }

    void dequeueRouteEvent(RouteEvent routeEvent) {
        log.debug("Dequeue routeEvent {}", routeEvent);
        switch (routeEvent.type()) {
            case ROUTE_ADDED:
                processRouteAddedInternal(routeEvent.alternatives());
                break;
            case ROUTE_REMOVED:
                processRouteRemovedInternal(routeEvent.alternatives());
                break;
            case ROUTE_UPDATED:
            case ALTERNATIVE_ROUTES_CHANGED:
                processRouteUpdatedInternal(Sets.newHashSet(routeEvent.alternatives()),
                        Sets.newHashSet(routeEvent.prevAlternatives()));
                break;
            default:
                break;
        }
    }
}
